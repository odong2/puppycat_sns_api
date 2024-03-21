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
import com.architecture.admin.models.dto.noti.NotiDto;
import com.architecture.admin.models.dto.push.PushDto;
import com.architecture.admin.services.BaseService;
import com.architecture.admin.services.noti.FollowNotiService;
import com.architecture.admin.services.push.FollowPushService;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/*****************************************************
 * 팔로우 모델러
 ****************************************************/
@Service
@RequiredArgsConstructor
@Transactional
public class FollowService extends BaseService {

    private final FollowDao followDao;
    private final FollowDaoSub followDaoSub;
    private final ContentsLikeDao contentsLikeDao;
    private final FollowCurlService followCurlService;
    private final FollowPushService followPushService;
    private final FollowNotiService followNotiService;

    @Value("${use.push.follow}")
    private boolean useFollowPush; // 푸시 알림 true/false

    @Value("${use.noti.follow}")
    private boolean useFollowNoti; // 알림함 알림 true/false

    /*****************************************************
     *  Modules
     ****************************************************/
    /**
     * 회원 팔로잉 전체 목록
     *
     * @param searchDto memberUuid
     * @return 팔로잉 리스트
     */
    public List<FollowDto> getTotalFollowList(String token, SearchDto searchDto) throws ParseException {
        MemberDto memberDto;
        if (token != null && !token.equals("")) {
            // 회원 UUID 조회
            memberDto = super.getMemberUuidByToken(token);
            searchDto.setLoginMemberUuid(memberDto.getUuid());
        }

        // 팔로우 회원 uuid 정상인지 curl 통신
        Boolean isExist = super.getCheckMemberByUuid(searchDto.getMemberUuid());

        if (Boolean.FALSE.equals(isExist)) {
            // 회원 UUID가 유효하지 않습니다.
            throw new CustomException(CustomError.MEMBER_UUID_ERROR);
        }

        List<FollowDto> followList = new ArrayList<>();

        // 목록 전체 count
        Long totalCount = followDaoSub.getTotalFollowingCnt(searchDto.getMemberUuid());
        if (totalCount == null) {
            totalCount = 0L;
        }
        Long blockCount = 0L;

        // 로그인 회원 조회라면
        if (searchDto.getLoginMemberUuid() != null && !searchDto.getLoginMemberUuid().equals("")) {
            // 조회 한 회원의 팔로워 중 로그인 한 회원이 차단한 회원 카운트 가져오기
            blockCount = followDaoSub.getBlockFollowCnt(searchDto);
        }

        totalCount = totalCount - blockCount;

        if (totalCount > 0) {
            // 팔로우 한 회원 UUID 리스트
            List<String> followUuidList = followDaoSub.getFollowUuidList(searchDto);
            searchDto.setMemberUuidList(followUuidList);

            // uuid로 팔로우 회원 MEMBER 정보 가져오기
            String getFollowMemberInfo = memberCurlService.getMemberInfoOrderByNick(searchDto);
            JSONObject followMemberInfoObject = new JSONObject(getFollowMemberInfo);
            if (!((boolean) followMemberInfoObject.get("result"))) {
                throw new CurlException(followMemberInfoObject);
            }

            JSONObject followMemberInfoResult = (JSONObject) followMemberInfoObject.get("data");
            // 목록 전체 count
            JSONArray followMemberInfo = followMemberInfoResult.getJSONArray("memberInfo");

            // UUID 초기화
            searchDto.setMemberUuidList(new ArrayList<>());

            if (followMemberInfo != null && !followMemberInfo.isEmpty()) {
                followMemberInfo.forEach(memberInfo -> {
                    JSONObject member = (JSONObject) memberInfo;
                    searchDto.setFollowUuid(member.getString("uuid"));
                    FollowDto getFollowSnsInfo = followDaoSub.getFollowSnsInfo(searchDto);

                    getFollowSnsInfo.setFollowNick(member.getString("nick"));
                    getFollowSnsInfo.setIntro(member.getString("intro"));
                    getFollowSnsInfo.setProfileImgUrl(member.getString("profileImgUrl"));
                    followList.add(getFollowSnsInfo);
                });
            }

            // paging
            PaginationLibray pagination = new PaginationLibray(Math.toIntExact(totalCount), searchDto);
            searchDto.setPagination(pagination);

            if (!followList.isEmpty()) {
                // 문자 변환
                remakeInfo(followList);
            }
        }

        // 회원리스트
        return followList;
    }

    /**
     * 회원 팔로잉 검색 목록
     *
     * @param searchDto memberUuid
     * @return 팔로잉 리스트
     */
    public List<FollowDto> getSearchFollowingList(String token, SearchDto searchDto) throws ParseException {
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

        if (searchDto.getSearchType() == null || "".equals(searchDto.getSearchType())) {
            // 검색 타입이 비어있습니다
            throw new CustomException(CustomError.FOLLOW_SEARCHTYPE_EMPTY);
        }
        if (!(searchDto.getSearchType().equals("id")) && !("nick".equals(searchDto.getSearchType()))) {
            // 검색은 id 와 nick만 가능합니다
            throw new CustomException(CustomError.FOLLOW_SEARCHTYPE_ERROR);
        }
        List<FollowDto> followList = new ArrayList<>();

        // 팔로우 한 회원 UUID 리스트
        List<String> followUuidList = followDaoSub.getFollowUuidList(searchDto);
        searchDto.setMemberUuidList(followUuidList);

        if (followUuidList != null && !followUuidList.isEmpty()) {
            // 카운트 가져오기 curl
            String getFollowSearchCnt = followCurlService.getFollowSearchCnt(searchDto);
            JSONObject followSearchCntObject = new JSONObject(getFollowSearchCnt);

            if (!((boolean) followSearchCntObject.get("result"))) {
                throw new CurlException(followSearchCntObject);
            }

            JSONObject followSearchCntResult = (JSONObject) followSearchCntObject.get("data");

            long totalCount = followSearchCntResult.getLong("followSearchCnt");
            Long blockCount = 0L;

            // 로그인 회원 조회라면
            if (searchDto.getLoginMemberUuid() != null && !searchDto.getLoginMemberUuid().equals("")) {
                // 조회 한 회원의 팔로워 중 로그인 한 회원이 차단한 회원 카운트 가져오기
                blockCount = followDaoSub.getBlockFollowCnt(searchDto);
            }
            totalCount = totalCount - blockCount;

            if (totalCount > 0) {

                // uuid로 팔로우 회원 MEMBER 정보 가져오기
                String getFollowMemberInfo = memberCurlService.getMemberInfoOrderByNick(searchDto);
                JSONObject followMemberInfoObject = new JSONObject(getFollowMemberInfo);
                if (!((boolean) followMemberInfoObject.get("result"))) {
                    throw new CurlException(followMemberInfoObject);
                }

                JSONObject followMemberInfoResult = (JSONObject) followMemberInfoObject.get("data");
                // 목록 전체 count
                JSONArray followMemberInfo = followMemberInfoResult.getJSONArray("memberInfo");

                // UUID 초기화
                searchDto.setMemberUuidList(new ArrayList<>());

                if (followMemberInfo != null && !followMemberInfo.isEmpty()) {
                    followMemberInfo.forEach(memberInfo -> {
                        JSONObject member = (JSONObject) memberInfo;
                        searchDto.setFollowUuid(member.getString("uuid"));
                        FollowDto getFollowSnsInfo = followDaoSub.getFollowSnsInfo(searchDto);

                        getFollowSnsInfo.setFollowNick(member.getString("nick"));
                        getFollowSnsInfo.setIntro(member.getString("intro"));
                        getFollowSnsInfo.setProfileImgUrl(member.getString("profileImgUrl"));
                        followList.add(getFollowSnsInfo);
                    });
                }

                // paging
                PaginationLibray pagination = new PaginationLibray(Math.toIntExact(totalCount), searchDto);
                searchDto.setPagination(pagination);

                if (!followList.isEmpty()) {
                    // 문자 변환
                    remakeInfo(followList);
                }
            }
        }

        // 회원리스트
        return followList;
    }

    /**
     * 팔로우 등록
     *
     * @param followDto memberUuid followUuid
     * @return insertedIdx / affectedRow
     */
    public Integer registFollow(String token, FollowDto followDto) {

        // 회원 UUID 조회
        MemberDto memberDto = super.getMemberUuidByToken(token);
        // 회원 UUID 세팅
        followDto.setMemberUuid(memberDto.getUuid());

        // 회원 UUID가 비어있을때
        if (followDto.getMemberUuid() == null || "".equals(followDto.getMemberUuid())) {
            // 회원 UUID가 비어있습니다
            throw new CustomException(CustomError.FOLLOW_MEMBER_EMPTY);
        }
        // 팔로우 UUID가 비어있을때
        if (followDto.getFollowUuid() == null || "".equals(followDto.getFollowUuid())) {
            // 팔로우 UUID가 비어있습니다
            throw new CustomException(CustomError.FOLLOW_FOLLOW_EMPTY);
        }
        // 회원 인덱스와 팔로우인덱스가 같을때
        if (followDto.getFollowUuid().equals(followDto.getMemberUuid())) {
            // 자기 자신은 팔로우 할 수 없습니다
            throw new CustomException(CustomError.FOLLOW_SELF_FOLLOW);
        }

        // 팔로우 회원 uuid 정상인지 curl 통신
        String jsonString = memberCurlService.checkMemberUuid(followDto.getFollowUuid());

        JSONObject jsonObject = new JSONObject(jsonString);
        JSONObject dataObject = (JSONObject) jsonObject.get("data");
        boolean isExist = (boolean) dataObject.get("isExist"); // 이미지 내 태그 존재 유무

        if (!isExist) {
            // 존재하지 않는 회원입니다
            throw new CustomException(CustomError.MEMBER_UUID_ERROR);
        }

        // 이미 팔로우가 되었는지 확인
        boolean checkFollow = checkFollow(followDto);

        if (checkFollow) {
            // 이미 팔로우 된 회원 입니다
            throw new CustomException(CustomError.FOLLOW_DUPLE);
        }

        Integer iResult;
        // 팔로우 기록이 있는지 체크
        boolean checkFollowInfo = checkFollowInfo(followDto);

        if (checkFollowInfo) {
            // 있으면 업데이트
            iResult = modifyFollow(followDto);
        } else {
            // 없으면 인서트
            iResult = insert(followDto);
            // 팔로우 컨텐츠 좋아요 테이블 인서트
            insertFollowContentsLikeCnt(followDto);
        }

        // 카운트 테이블 인서트/업데이트
        registFollowCnt(followDto);

        // 알림 사용 여부
        if (useFollowNoti) {
            NotiDto notiDto = NotiDto.builder()
                    .senderUuid(followDto.getMemberUuid())     // 팔로우를 한 사람
                    .memberUuid(followDto.getFollowUuid())     // 팔로우를 당한 사람(알림 대상)
                    .subType("follow")
                    .build();
            followNotiService.followSendNoti(token, notiDto);
        }

        if (iResult > 0) {
            // 푸시 사용 여부
            if (useFollowPush) {
                PushDto pushDto = PushDto.builder()
                        .senderUuid(followDto.getMemberUuid())       // 팔로우를 한 사람
                        .receiverUuid(followDto.getFollowUuid())     // 팔로우를 당한 사람(푸시대상)
                        .typeIdx(1)
                        .build();

                // 푸시 보내기
                followPushService.sendFollowPush(token, pushDto);
            }
        }
        return iResult;
    }

    /**
     * 내가 다른 사람을 팔로우 했을때 memberUuid = followCnt +1 / followUuid = followerCnt +1
     *
     * @param followDto memberUuid followUuid
     */
    public void registFollowCnt(FollowDto followDto) {
        // 팔로우 한 회원의 cnt테이블 정보
        boolean checkMemberCnt = selectMemberCntCheck(followDto);
        if (!checkMemberCnt) {
            insertFollowCnt(followDto);
        }
        // 업데이트
        updateFollowCntUp(followDto);

        // 팔로우 당한 회원의 cnt테이블 정보
        boolean checkFollowCnt = selectFollowerCntCheck(followDto);
        if (!checkFollowCnt) {
            // 인서트
            insertFollowerCnt(followDto);
        }

        // 업데이트
        updateFollowerCntUp(followDto);
    }

    /**
     * 언팔로우 [ 내가 팔로우 한 회원을 삭제 ]
     *
     * @param followDto memberUuid followUuid
     * @return insertedIdx / affectedRow
     */
    public Integer removeFollow(String token, FollowDto followDto) {

        // 회원 UUID 조회
        MemberDto memberDto = super.getMemberUuidByToken(token);

        // 회원 UUID 세팅
        followDto.setMemberUuid(memberDto.getUuid());

        // 회원 UUID가 비어있을때
        if (followDto.getMemberUuid() == null || "".equals(followDto.getMemberUuid())) {
            // 회원 UUID가 비어있습니다
            throw new CustomException(CustomError.FOLLOW_MEMBER_EMPTY);
        }
        // 팔로우 UUID가 비어있을때
        if (followDto.getFollowUuid() == null || "".equals(followDto.getFollowUuid())) {
            // 팔로우 UUID가 비어있습니다
            throw new CustomException(CustomError.FOLLOW_FOLLOW_EMPTY);
        }

        // 팔로우 회원 uuid 정상인지 curl 통신
        String jsonString = memberCurlService.checkMemberUuid(followDto.getFollowUuid());

        JSONObject jsonObject = new JSONObject(jsonString);
        JSONObject dataObject = (JSONObject) jsonObject.get("data");
        boolean isExist = (boolean) dataObject.get("isExist"); // 이미지 내 태그 존재 유무
        if (!isExist) {
            // 존재하지 않는 회원입니다
            throw new CustomException(CustomError.MEMBER_UUID_ERROR);
        }

        // 이미 팔로우가 되었는지 확인
        boolean checkFollow = checkFollow(followDto);

        if (!checkFollow) {
            throw new CustomException(CustomError.FOLLOW_NOT_FOLLOW);
        }

        Integer iResult = modifyUnFollow(followDto);

        // 카운트 테이블 업데이트/인서트
        removeFollowCnt(followDto);

        // 팔로우 contents like 0으로 셋
        followContentsLikeReset(followDto);

        return iResult;
    }

    /**
     * 언팔로잉 했을때
     * memberUuid = followCnt -1 / followUuid = followCnt -1
     *
     * @param followDto memberUuid followUuid
     */
    public void removeFollowCnt(FollowDto followDto) {
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
     * 팔로우 기록이 있는지 확인
     *
     * @param followDto memberUuid followUuid
     * @return count
     */
    public Boolean checkFollowInfo(FollowDto followDto) {
        followDto.setState(0);

        return selectCntCheck(followDto);
    }

    /**
     * 팔로우가 되어있는지 확인
     *
     * @param followDto memberUuid followUuid
     * @return true/false
     */
    public Boolean checkFollow(FollowDto followDto) {
        followDto.setState(1);

        return selectCntCheck(followDto);
    }

    /**
     * 팔로우
     *
     * @param followDto memberUuid followUuid
     * @return affectedRow
     */
    public Integer modifyFollow(FollowDto followDto) {
        // 등록일
        followDto.setRegDate(dateLibrary.getDatetime());
        followDto.setState(1);

        return followDao.update(followDto);
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
     * 언팔로우시 팔로우 컨텐츠 좋아요 0으로 리셋
     *
     * @param followDto
     */
    public void followContentsLikeReset(FollowDto followDto) {
        FollowDto followInfo = selectFollowInfo(followDto);

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
     * @param followDto memberUuid followUuid state
     * @return count
     */
    public Boolean selectCntCheck(FollowDto followDto) {
        int iCount = followDaoSub.getCntCheck(followDto);

        return iCount > 0;
    }

    /**
     * sns_member_follow 테이블 idx가져오기
     *
     * @param followDto memberUuid followUuid state
     * @return count
     */
    public FollowDto selectFollowInfo(FollowDto followDto) {
        return followDaoSub.getFollowInfo(followDto);
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
    /**
     * memeber_follow 테이블 등록
     *
     * @param followDto memberUuid followUuid
     * @return insertedIdx
     */
    public Integer insert(FollowDto followDto) {
        // 등록일
        followDto.setRegDate(dateLibrary.getDatetime());

        return followDao.insert(followDto);
    }

    /**
     * cnt테이블에 follow_cnt 신규 인서트
     *
     * @param followDto memberUuid
     */
    public void insertFollowCnt(FollowDto followDto) {
        // 등록일
        followDto.setRegDate(dateLibrary.getDatetime());

        followDao.insertFollowCnt(followDto);
    }

    /**
     * cnt 테이블에 follower_cnt 신규 인서트
     *
     * @param followDto followUuid
     */
    public void insertFollowerCnt(FollowDto followDto) {
        // 등록일
        followDto.setRegDate(dateLibrary.getDatetime());

        followDao.insertFollowerCnt(followDto);
    }

    /**
     * 팔로우 컨텐츠 좋아요 cnt 인서트
     *
     * @param followDto
     */
    public void insertFollowContentsLikeCnt(FollowDto followDto) {
        ContentsLikeDto contentsLikeDto = new ContentsLikeDto();
        contentsLikeDto.setFollowIdx(followDto.getInsertedIdx());

        contentsLikeDao.insertFollowContentsLikeCnt(contentsLikeDto);
    }

    /*****************************************************
     *  SubFunction - Update
     ****************************************************/
    /**
     * memeber_follow 테이블 수정
     *
     * @param followDto memberUuid followUuid
     * @return affectedRow
     */
    public Integer update(FollowDto followDto) {
        // 등록일
        followDto.setRegDate(dateLibrary.getDatetime());

        return followDao.update(followDto);
    }

    /**
     * sns_member_follow_cnt.follower_cnt + 1
     *
     * @param followDto followUuid
     */
    public void updateFollowerCntUp(FollowDto followDto) {
        followDao.updateFollowerCntUp(followDto);
    }

    /**
     * sns_member_follow_cnt.follow_cnt + 1
     *
     * @param followDto memberUuid
     */
    public void updateFollowCntUp(FollowDto followDto) {
        followDao.updateFollowCntUp(followDto);
    }

    /**
     * sns_member_follow_cnt.follow_cnt - 1
     *
     * @param followDto memberUuid
     */
    public void updateFollowCntDown(FollowDto followDto) {
        followDao.updateFollowCntDown(followDto);
    }

    /**
     * sns_member_follow_cnt.follower_cnt - 1
     *
     * @param followDto followUuid
     */
    public void updateFollowerCntDown(FollowDto followDto) {
        followDao.updateFollowerCntDown(followDto);
    }

    /*****************************************************
     *  SubFunction - Delete
     ****************************************************/


    /*****************************************************
     *  SubFunction - Etc
     ****************************************************/
    /**
     * 정보 변경 list
     *
     * @param list
     */
    public void remakeInfo(List<FollowDto> list) throws ParseException {
        for (FollowDto l : list) {
            remakeInfo(l);
        }
    }

    /**
     * 정보 변경 dto
     *
     * @param dto
     */
    public void remakeInfo(FollowDto dto) throws ParseException {
        if (dto.getRegDate() != null) {
            // 현재 시간 UTC 가져오기
            String nowDate = dateLibrary.getDatetime();
            // 로컬 시간으로 변경
            nowDate = dateLibrary.utcToLocalTime(nowDate);
            // 년 월 일 만 받아오기
            String nowYMD = nowDate.substring(0, 10);
            // 오늘 시작 시간 세팅
            String sStartDate = nowYMD + " 00:00:00";
            // 오늘 끝 시간 세팅
            String sEndDate = nowYMD + " 23:59:59";

            // 날짜 변환 string -> date
            SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dateformat.setLenient(false);

            Date startDate = dateformat.parse(sStartDate);
            Date endDate = dateformat.parse(sEndDate);
            Date thisDate = dateformat.parse(dto.getRegDate());

            // 오늘이면
            if (startDate.before(thisDate) && endDate.after(thisDate)) {
                // new 값 노출
                dto.setNewState(1);
            } else {
                // new 값 비 노출
                dto.setNewState(0);
            }
        }
    }
}
