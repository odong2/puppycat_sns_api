package com.architecture.admin.services.follow;

import com.architecture.admin.libraries.PaginationLibray;
import com.architecture.admin.libraries.exception.CurlException;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.contents.ContentsLikeDao;
import com.architecture.admin.models.dao.follow.FollowDao;
import com.architecture.admin.models.daosub.follow.FollowDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.contents.ContentsLikeDto;
import com.architecture.admin.models.dto.follow.FollowDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

/*****************************************************
 * 팔로워 모델러
 ****************************************************/
@Service
@RequiredArgsConstructor
@Transactional
public class FollowerService extends BaseService {

    private final FollowDao followDao;
    private final FollowDaoSub followDaoSub;
    private final ContentsLikeDao contentsLikeDao;
    private final FollowCurlService followCurlService;

    /*****************************************************
     *  Modules
     ****************************************************/
    /**
     * 회원 팔로워 전체 목록
     *
     * @param searchDto memberUuid
     * @return 팔로워 리스트
     */
    public List<FollowDto> getTotalFollowerList(String token, SearchDto searchDto) {
        MemberDto memberDto;
        if (token != null && !token.equals("")) {
            // 회원 UUID 조회
            memberDto = super.getMemberUuidByToken(token);
            searchDto.setLoginMemberUuid(memberDto.getUuid());
        }

        // 회원 uuid 정상인지 curl 통신
        String jsonString = memberCurlService.checkMemberUuid(searchDto.getMemberUuid());

        JSONObject jsonObject = new JSONObject(jsonString);
        JSONObject dataObject = (JSONObject) jsonObject.get("data");
        boolean isExist = (boolean) dataObject.get("isExist"); // 이미지 내 태그 존재 유무

        if (!isExist) {
            // 존재하지 않는 회원입니다
            throw new CustomException(CustomError.MEMBER_UUID_ERROR);
        }

        List<FollowDto> followerList = new ArrayList<>();

        // 목록 전체 count
        Long totalCount = followDaoSub.getTotalFollowerCnt(searchDto.getMemberUuid());
        if (totalCount == null) {
            totalCount = 0L;
        }

        Long blockCount = 0L;
        // 로그인 회원 조회라면
        if (searchDto.getLoginMemberUuid() != null && !searchDto.getLoginMemberUuid().equals("")) {
            // 조회 한 회원의 팔로워 중 로그인 한 회원이 차단한 회원 카운트 가져오기
            blockCount = followDaoSub.getBlockFollowerCnt(searchDto);
        }

        totalCount = totalCount - blockCount;

        if (totalCount > 0) {
            // 팔로워 회원 UUID 리스트
            List<String> followerUuidList = followDaoSub.getFollowerUuidList(searchDto);
            searchDto.setMemberUuidList(followerUuidList);

            // uuid로 팔로워 회원 MEMBER 정보 가져오기
            String getFollowerMemberInfo = memberCurlService.getMemberInfoOrderByNick(searchDto);
            JSONObject followerMemberInfoObject = new JSONObject(getFollowerMemberInfo);
            if (!((boolean) followerMemberInfoObject.get("result"))) {
                throw new CurlException(followerMemberInfoObject);
            }

            JSONObject followerMemberInfoResult = (JSONObject) followerMemberInfoObject.get("data");
            // 목록 전체 count
            JSONArray followerMemberInfo = followerMemberInfoResult.getJSONArray("memberInfo");
            // UUID 초기화
            searchDto.setMemberUuidList(new ArrayList<>());

            if (followerMemberInfo != null && !followerMemberInfo.isEmpty()) {
                followerMemberInfo.forEach(memberInfo -> {
                    JSONObject member = (JSONObject) memberInfo;
                    searchDto.setFollowUuid(member.getString("uuid"));
                    FollowDto getFollowSnsInfo = followDaoSub.getFollowerSnsInfo(searchDto);

                    getFollowSnsInfo.setFollowerNick(member.getString("nick"));
                    getFollowSnsInfo.setIntro(member.getString("intro"));
                    getFollowSnsInfo.setProfileImgUrl(member.getString("profileImgUrl"));
                    followerList.add(getFollowSnsInfo);
                });
            }

            // paging
            PaginationLibray pagination = new PaginationLibray(Math.toIntExact(totalCount), searchDto);
            searchDto.setPagination(pagination);

        }

        // 회원리스트
        return followerList;
    }

    /**
     * 회원 팔로워 검색 목록
     *
     * @param searchDto memberUuid
     * @return 팔로워 리스트
     */
    public List<FollowDto> getSearchFollowerList(String token, SearchDto searchDto) {
        MemberDto memberDto;
        if (token != null && !token.equals("")) {
            // 회원 UUID 조회
            memberDto = super.getMemberUuidByToken(token);
            searchDto.setLoginMemberUuid(memberDto.getUuid());
        }

        // 팔로우 회원 uuid 정상인지 curl 통신
        String jsonString = memberCurlService.checkMemberUuid(searchDto.getMemberUuid());
        JSONObject jsonObject = new JSONObject(jsonString);
        JSONObject dataObject = (JSONObject) jsonObject.get("data");
        boolean isExist = (boolean) dataObject.get("isExist"); // 이미지 내 태그 존재 유무

        if (!isExist) {
            // 존재하지 않는 회원입니다
            throw new CustomException(CustomError.MEMBER_UUID_ERROR);
        }

        if (searchDto.getSearchType() == null || "".equals(searchDto.getSearchType())) {
            // 검색 타입이 비어있습니다
            throw new CustomException(CustomError.FOLLOW_SEARCHTYPE_EMPTY);
        }
        if (!(searchDto.getSearchType().equals("id")) && !("nick".equals(searchDto.getSearchType()))) {
            // 검색은 id 와 nick만 가능합니다
            throw new CustomException(CustomError.FOLLOW_SEARCHTYPE_ERROR);
        }
        List<FollowDto> followerList = new ArrayList<>();

        // 팔로워 회원 UUID 리스트
        List<String> followerUuidList = followDaoSub.getFollowerUuidList(searchDto);
        searchDto.setMemberUuidList(followerUuidList);

        if (followerUuidList != null && !followerUuidList.isEmpty()) {
            // 카운트 가져오기 curl
            String getFollowSearchCnt = followCurlService.getFollowSearchCnt(searchDto);
            JSONObject followSearchCntObject = new JSONObject(getFollowSearchCnt);

            if (!((boolean) followSearchCntObject.get("result"))) {
                throw new CurlException(followSearchCntObject);
            }

            JSONObject followSearchCntResult = (JSONObject) followSearchCntObject.get("data");

            // 목록 전체 count
            long totalCount = followSearchCntResult.getLong("followSearchCnt");
            Long blockCount = 0L;

            // 로그인 회원 조회라면
            if (!ObjectUtils.isEmpty(searchDto.getLoginMemberUuid())) {
                // 조회 한 회원의 팔로워 중 로그인 한 회원이 차단한 회원 카운트 가져오기
                blockCount = followDaoSub.getBlockFollowerCnt(searchDto);
            }

            totalCount = totalCount - blockCount;

            if (totalCount > 0) {

                // uuid로 팔로워 회원 MEMBER 정보 가져오기
                String getFollowerMemberInfo = memberCurlService.getMemberInfoOrderByNick(searchDto);
                JSONObject followerMemberInfoObject = new JSONObject(getFollowerMemberInfo);
                if (!((boolean) followerMemberInfoObject.get("result"))) {
                    throw new CurlException(followerMemberInfoObject);
                }

                JSONObject followerMemberInfoResult = (JSONObject) followerMemberInfoObject.get("data");
                // 목록 전체 count
                JSONArray followerMemberInfo = followerMemberInfoResult.getJSONArray("memberInfo");
                // UUID 초기화
                searchDto.setMemberUuidList(new ArrayList<>());

                if (followerMemberInfo != null && !followerMemberInfo.isEmpty()) {
                    followerMemberInfo.forEach(memberInfo -> {
                        JSONObject member = (JSONObject) memberInfo;
                        searchDto.setFollowUuid(member.getString("uuid"));
                        FollowDto getFollowSnsInfo = followDaoSub.getFollowerSnsInfo(searchDto);

                        getFollowSnsInfo.setFollowerNick(member.getString("nick"));
                        getFollowSnsInfo.setIntro(member.getString("intro"));
                        getFollowSnsInfo.setProfileImgUrl(member.getString("profileImgUrl"));
                        followerList.add(getFollowSnsInfo);
                    });
                }

                // paging
                PaginationLibray pagination = new PaginationLibray(Math.toIntExact(totalCount), searchDto);
                searchDto.setPagination(pagination);
            }
        }
        return followerList;
    }

    /**
     * 언팔로워 [ 나를 팔로잉 한 회원을 삭제 ]
     *
     * @param followDto memberUuid followUuid
     * @return insertedIdx / affectedRow
     */
    public Integer removeFollower(String token, FollowDto followDto) {

        // 회원 UUID 조회
        MemberDto memberDto = super.getMemberUuidByToken(token);

        String memberUuid = memberDto.getUuid();
        String followUuid = followDto.getFollowUuid();

        // 회원 UUID가 비어있을때
        if (memberUuid == null || "".equals(memberUuid)) {
            // 회원 UUID가 비어있습니다
            throw new CustomException(CustomError.FOLLOW_MEMBER_EMPTY);
        }
        // 팔로우 UUID가 비어있을때
        if (followUuid == null || "".equals(followUuid)) {
            // 팔로우 UUID가 비어있습니다
            throw new CustomException(CustomError.FOLLOW_FOLLOW_EMPTY);
        }

        followDto.setFollowUuid(memberUuid);
        followDto.setMemberUuid(followUuid);

        // 팔로우가 되었는지 확인
        boolean checkFollow = checkFollow(followDto);

        if (!checkFollow) {
            // 나를 팔로우 한 회원이 아닙니다.
            throw new CustomException(CustomError.FOLLOW_NOT_FOLLOWER);
        }

        Integer iResult = modifyUnFollow(followDto);

        // 카운트 테이블 업데이트 / 인서트
        removeFollowerCnt(followDto);

        // 팔로워 contents like 0으로 셋
        followerContentsLikeReset(followDto);

        return iResult;
    }

    /**
     * 언팔로워 했을때
     * memberUuid = followUuid -1 / followUuid = followerIdx -1
     *
     * @param followDto memberUuid followUuid
     */
    public void removeFollowerCnt(FollowDto followDto) {
        // 언팔로우 한 회원의 cnt 정보
        boolean checkMemberCnt = selectMemberCntCheck(followDto);
        if (checkMemberCnt) {
            // 업데이트
            updateFollowCntDown(followDto);
        }

        // 언팔로우 당한 회원의 cnt 정보
        boolean checkFollowCnt = selectFollowerCntCheck(followDto);
        if (checkFollowCnt) {
            // 업데이트
            updateFollowerCntDown(followDto);
        }
    }


    /**
     * 팔로우가 되어있는지 확인
     *
     * @param followDto memberUuid followUuid
     * @return count
     */
    public Boolean checkFollow(FollowDto followDto) {
        followDto.setState(1);

        return selectCntCheck(followDto);
    }

    /**
     * 언팔로우
     *
     * @param followDto memberUuid followUuid
     * @return insertedIdx
     */
    public Integer modifyUnFollow(FollowDto followDto) {
        // 등록일
        followDto.setRegDate(dateLibrary.getDatetime());
        followDto.setState(0);

        return followDao.update(followDto);
    }

    /**
     * 언팔로워시 팔로우 컨텐츠 좋아요 0으로 리셋
     *
     * @param followDto
     */
    public void followerContentsLikeReset(FollowDto followDto) {
        FollowDto followInfo = followDaoSub.getFollowInfo(followDto);

        ContentsLikeDto contentsLikeDto = new ContentsLikeDto();
        contentsLikeDto.setFollowIdx(followInfo.getIdx());

        contentsLikeDao.updateFollowContentsLikeReset(contentsLikeDto);
    }
    /*****************************************************
     *  SubFunction - select
     ****************************************************/
    /**
     * sns_follow 테이블 데이터 확인
     *
     * @param followDto memberUuid followidx state
     * @return count
     */
    public Boolean selectCntCheck(FollowDto followDto) {
        int iCount = followDaoSub.getCntCheck(followDto);

        return iCount > 0;
    }

    /**
     * follow_cnt 데이터 있는지 확인
     *
     * @param followDto memberUuid
     * @return count
     */
    public Boolean selectMemberCntCheck(FollowDto followDto) {
        int iCount = followDaoSub.getMemberCntCheck(followDto);

        return iCount > 0;
    }

    /**
     * follow_cnt 팔로우 회원 데이터 있는지 확인
     *
     * @param followDto followUuid
     * @return count
     */
    public Boolean selectFollowerCntCheck(FollowDto followDto) {
        int iCount = followDaoSub.getFollowerCntCheck(followDto);

        return iCount > 0;
    }

    /*****************************************************
     *  SubFunction - insert
     ****************************************************/

    /*****************************************************
     *  SubFunction - Update
     ****************************************************/

    /**
     * sns_member_follow_cnt.follow_cnt - 1
     *
     * @param followDto memberUuid
     */
    public void updateFollowCntDown(FollowDto followDto) {
        // 등록일
        followDto.setRegDate(dateLibrary.getDatetime());

        followDao.updateFollowCntDown(followDto);
    }

    /**
     * sns_member_follow_cnt.follower_cnt - 1
     *
     * @param followDto followUuid
     */
    public void updateFollowerCntDown(FollowDto followDto) {
        // 등록일
        followDto.setRegDate(dateLibrary.getDatetime());

        followDao.updateFollowerCntDown(followDto);
    }

    /*****************************************************
     *  SubFunction - Delete
     ****************************************************/
    /*****************************************************
     *  SubFunction - Etc
     ****************************************************/
}
