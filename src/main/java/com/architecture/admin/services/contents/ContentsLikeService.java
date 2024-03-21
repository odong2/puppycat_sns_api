package com.architecture.admin.services.contents;

import com.architecture.admin.libraries.PaginationLibray;
import com.architecture.admin.libraries.exception.CurlException;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.contents.ContentsLikeDao;
import com.architecture.admin.models.daosub.contents.ContentsDaoSub;
import com.architecture.admin.models.daosub.contents.ContentsLikeDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.contents.ContentsDto;
import com.architecture.admin.models.dto.contents.ContentsLikeDto;
import com.architecture.admin.models.dto.follow.FollowDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;
import com.architecture.admin.models.dto.noti.NotiDto;
import com.architecture.admin.models.dto.push.PushDto;
import com.architecture.admin.services.BaseService;
import com.architecture.admin.services.follow.FollowService;
import com.architecture.admin.services.noti.ContentsNotiService;
import com.architecture.admin.services.push.LikePushService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ContentsLikeService extends BaseService {

    private final ContentsLikeDao contentsLikeDao;
    private final ContentsLikeDaoSub contentsLikeDaoSub;
    private final ContentsDaoSub contentsDaoSub;
    private final LikePushService likePushService;
    private final ContentsNotiService contentsNotiService;
    private final ContentsService contentsService;
    private final FollowService followService;
    @Value("${cloud.aws.s3.img.url}")
    private String imgDomain;
    @Value("${use.push.contents.like}")
    private boolean useLikePush; // 푸시 알림 true/false
    @Value("${use.noti.contents.like}")
    private boolean useLikeNoti; // 알림 true/false

    /*****************************************************
     *  Modules
     ****************************************************/
    /**
     * 콘텐츠에 좋아요한 회원 리스트
     *
     * @param token     access token
     * @param searchDto contentsIdx
     * @return 좋아요한 회원 리스트
     */
    @Transactional(readOnly = true)
    public List<MemberInfoDto> getContentsLikeList(String token, SearchDto searchDto) throws JsonProcessingException {
        List<MemberInfoDto> likeMemberList = new ArrayList<>();
        List<String> uuidList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);
        searchDto.setMemberUuid(memberDto.getUuid());

        // validate
        contentsLikeListValidate(searchDto);

        // 목록 전체 count
        int iTotalCount = contentsLikeDaoSub.iGetTotalLikeCount(searchDto);

        if (iTotalCount > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(iTotalCount, searchDto);
            searchDto.setPagination(pagination);

            // list
            likeMemberList = contentsLikeDaoSub.lGetContentsLikeList(searchDto);

            // list 에서 memberUuid 추출
            likeMemberList.forEach(item -> uuidList.add(item.getUuid()));

            // curl 회원 조회
            String jsonString = memberCurlService.getMemberInfoByUuidList(uuidList);

            JSONObject jsonObject = new JSONObject(jsonString);
            if (!(jsonObject.getBoolean("result"))) {
                throw new CurlException(jsonObject);
            }

            JsonObject listJson = (JsonObject) JsonParser.parseString(jsonObject.get("data").toString()); // data 안 list 파싱
            List<MemberInfoDto> memberInfoList = mapper.readValue(listJson.get("memberInfoList").toString(), new TypeReference<>() {
            }); // 회원 정보 list

            // uuid 로 회원 정보 매핑
            for (MemberInfoDto memberInfo : memberInfoList) {
                for (MemberInfoDto likeMember : likeMemberList) {
                    if (Objects.equals(likeMember.getUuid(), memberInfo.getUuid())) {
                        likeMember.setNick(memberInfo.getNick());
                        likeMember.setProfileImgUrl(memberInfo.getProfileImgUrl());
                        likeMember.setIntro(memberInfo.getIntro());
                    }
                }
            }

        }

        return likeMemberList;
    }

    /**
     * 내가 좋아요 한 콘텐츠 리스트
     *
     * @param searchDto MemberIdx
     * @return 내가 좋아요 한 콘텐츠 리스트
     */
    @Transactional(readOnly = true)
    public List<ContentsDto> getMyLikeContentsList(String token, SearchDto searchDto) {

        List<ContentsDto> contentsList = new ArrayList<>();

        // 로그인 회원 uuid curl 조회
        MemberDto loginUserInfo = super.getMemberUuidByToken(token);

        // 로그인 회원 uuid set
        searchDto.setLoginMemberUuid(loginUserInfo.getUuid());

        // 목록 전체 count
        int iTotalCount = contentsLikeDaoSub.iGetTotalMyLikeContentsCount(searchDto);

        if (iTotalCount > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(iTotalCount, searchDto);
            searchDto.setPagination(pagination);

            // list
            contentsList = contentsLikeDaoSub.lGetMyLikeContentsList(searchDto);

            // 첫 번째 이미지 url setting
            contentsService.setFirstImgUrl(contentsList);
        }

        return contentsList;
    }

    /**
     * 콘텐츠 좋아요
     *
     * @param token           access token
     * @param contentsLikeDto contentsIdx
     */
    @Transactional
    public void contentsLike(String token, ContentsLikeDto contentsLikeDto) {
        Integer iResult = 0;

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);
        contentsLikeDto.setLoginMemberUuid(memberDto.getUuid());

        // 컨텐츠 좋아요 공통 유효성
        likeCommonValidate(contentsLikeDto, "register");

        // 등록일 set
        contentsLikeDto.setRegDate(dateLibrary.getDatetime());

        // 좋아요 내역 가져오기
        ContentsLikeDto oTargetInfo = getTargetInfo(contentsLikeDto);

        // 기존 내역이 존재한다면
        if (oTargetInfo != null) {
            // 이미 좋아요한 경우
            if (oTargetInfo.getState() == 1) {
                throw new CustomException(CustomError.LIKE_STATE); // 이미 좋아요를 누르셨습니다.
            }

            // 좋아요 취소 후 다시 요청한 경우
            if (oTargetInfo.getState() == 0) {
                // idx set [sns_contents_like]
                contentsLikeDto.setIdx(oTargetInfo.getIdx());
                // 좋아요 상태 업데이트
                iResult = updateLikeState(contentsLikeDto);
                // cnt +1
                updateContentsLikeCntUp(contentsLikeDto);
                // 팔로우 컨텐츠 좋아요 인서트
                registFollowContentsLike(contentsLikeDto);
            }
        } else {
            // 좋아요 등록
            iResult = insertContentsLike(contentsLikeDto);

            // cnt 테이블에 해당 idx 있는지 확인
            Boolean bResult = checkCntByIdx(contentsLikeDto);
            // cnt 테이블에 값 없으면
            if (Boolean.FALSE.equals(bResult)) {
                insertContentsLikeCnt(contentsLikeDto);
            }
            // cnt +1
            updateContentsLikeCntUp(contentsLikeDto);

            // 팔로우 컨텐츠 좋아요 인서트
            registFollowContentsLike(contentsLikeDto);
        }

        // 좋아요 push/알림
        if (iResult > 0) {
            // 콘텐츠 작성자 idx 가져오기 by 콘텐츠 idx
            String receiverUuid = contentsDaoSub.getMemberUuidByIdx(contentsLikeDto.getContentsIdx());
            if (receiverUuid != null) {

                // 콘텐츠 첫번째 이미지 가져오기
                String imgUrl = contentsDaoSub.getContentsImg(contentsLikeDto.getContentsIdx());
                // 이미지 full url
                String img = imgDomain + imgUrl;

                // 알림 보내기
                notiAction(token, contentsLikeDto, receiverUuid, img);
                // 푸시 보내기
                pushAction(token, contentsLikeDto, receiverUuid, img);
            }
        }

    }

    /**
     * 콘텐츠 좋아요 취소
     *
     * @param contentsLikeDto memberIdx, contentsIdx
     */
    @Transactional
    public void contentsLikeCancel(String token, ContentsLikeDto contentsLikeDto) throws ParseException {

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);
        contentsLikeDto.setLoginMemberUuid(memberDto.getUuid());

        // 컨텐츠 좋아요 공통 유효성
        likeCommonValidate(contentsLikeDto, "cancel");

        // 좋아요 내역 가져오기
        ContentsLikeDto oTargetInfo = getTargetInfo(contentsLikeDto);

        // 좋아요 한 적 없는데 취소 요청한 경우
        if (oTargetInfo == null) {
            throw new CustomException(CustomError.LIKE_DATA_ERROR); // 잘못된 요청입니다.
        }

        // 좋아요 insert 된 적이 있고 상태값이 정상인 경우
        if (oTargetInfo.getState() == 1) {

            // 등록일 set
            contentsLikeDto.setRegDate(dateLibrary.getDatetime());

            // idx set [sns_contents_like]
            contentsLikeDto.setIdx(oTargetInfo.getIdx());

            // 좋아요 상태값 0으로 변경
            updateUnLikeState(contentsLikeDto);

            // 좋아요 cnt -1
            updateContentsLikeCntDown(contentsLikeDto);

            // 팔로우 컨텐츠 좋아요 cnt - 1
            cancelFollowContentsLike(contentsLikeDto);

        }
        // 이미 취소되어 있다면
        else {
            throw new CustomException(CustomError.LIKE_CANCEL_STATE); // 이미 좋아요 취소하였습니다.
        }

    }

    /**
     * 콘텐츠 좋아요 push
     *
     * @param token
     * @param contentsLikeDto contentsIdx, memberUuid
     * @param memberUuid      받는 사람 uuid
     * @param img             콘텐츠 썸네일
     */
    public void pushAction(String token, ContentsLikeDto contentsLikeDto, String memberUuid, String img) {
        // 콘텐츠 좋아요 푸시알림 스위치 ON
        if (useLikePush) {

            // 푸시 DTO 세팅 - 콘텐츠 좋아요 푸시 타입: 3
            PushDto pushDto = PushDto.builder()
                    .senderUuid(contentsLikeDto.getLoginMemberUuid())     // 좋아요를 누른 사람
                    .receiverUuid(memberUuid)                        // 콘텐츠 작성자 (푸시대상)
                    .typeIdx(3)                                     // 푸시 타입
                    .contentsIdx(contentsLikeDto.getContentsIdx())  // 콘텐츠 idx
                    .img(img)                                       // 콘텐츠 썸네일
                    .build();

            // 푸시 보내기
            likePushService.sendLikePush(token, pushDto);

        }
    }

    /**
     * 콘텐츠 좋아요 알림
     *
     * @param token
     * @param contentsLikeDto contentsIdx, memberUuid
     * @param memberUuid      받는 사람 uuid
     * @param img             콘텐츠 썸네일
     */
    public void notiAction(String token, ContentsLikeDto contentsLikeDto, String memberUuid, String img) {
        // 콘텐츠 좋아요시 알림 스위치 ON
        if (useLikeNoti) {

            // 알림 등록
            NotiDto notiDto = NotiDto.builder()
                    .senderUuid(contentsLikeDto.getLoginMemberUuid())      // 콘텐츠를 좋아요한 회원idx
                    .memberUuid(memberUuid)                           // 받는 사람 idx
                    .contentsIdx(contentsLikeDto.getContentsIdx())    // 컨텐츠 IDX
                    .img(img)                                         // 콘텐츠 썸네일
                    .subType("like_contents")                         // sub_type
                    .build();
            contentsNotiService.contentsLikeNoti(token, notiDto);

        }
    }

    /**
     * 팔로우 콘텐츠 좋아요 cnt 업데이트
     *
     * @param contentsLikeDto contentsIdx, memberIdx
     */
    public void registFollowContentsLike(ContentsLikeDto contentsLikeDto) {

        // 콘텐츠 작성자 idx 가져오기 by 콘텐츠 idx
        String contentsWriterMemberUuid = contentsDaoSub.getMemberUuidByIdx(contentsLikeDto.getContentsIdx());

        if (!ObjectUtils.isEmpty(contentsWriterMemberUuid)) {
            // 컨텐츠 작성자와 팔로우 상태인지 체크
            FollowDto followDto = new FollowDto();
            followDto.setMemberUuid(contentsLikeDto.getLoginMemberUuid());
            followDto.setFollowUuid(contentsWriterMemberUuid);
            FollowDto followInfo = followService.selectFollowInfo(followDto);

            // 팔로우 상태이면
            if (followInfo != null && followInfo.getIdx() != null && followInfo.getIdx() > 0) {
                // followIdx 세팅
                contentsLikeDto.setFollowIdx(followInfo.getIdx());
                // 팔로우 컨텐츠 좋아요 기록 있는지 체크
                boolean checkFollowLikeCnt = checkFollowCntByIdx(contentsLikeDto);
                // 없으면 인서트
                if (!checkFollowLikeCnt) {
                    insertFollowContentsLikeCnt(contentsLikeDto);
                }
                // 카운트 업데이트
                updateFollowContentsLikeCntUp(contentsLikeDto);
            }
        }
    }

    /**
     * 팔로우 컨텐츠 좋아요 cnt -1
     *
     * @param contentsLikeDto
     * @throws ParseException
     */
    public void cancelFollowContentsLike(ContentsLikeDto contentsLikeDto) throws ParseException {
        // 콘텐츠 작성자 idx 가져오기 by 콘텐츠 idx
        String contentsWriterMemberUuid = contentsDaoSub.getMemberUuidByIdx(contentsLikeDto.getContentsIdx());

        if (!ObjectUtils.isEmpty(contentsWriterMemberUuid)) {

            // 댓글 작성자가 컨텐츠 작성자와 팔로우 상태인지 체크
            FollowDto followDto = new FollowDto();
            followDto.setMemberUuid(contentsLikeDto.getLoginMemberUuid());
            followDto.setFollowUuid(contentsWriterMemberUuid);

            // 팔로우 정보 가져오기
            FollowDto followInfo = followService.selectFollowInfo(followDto);

            // 팔로우 상태이면
            if (followInfo != null && followInfo.getIdx() != null && followInfo.getIdx() > 0) {
                // followIdx 세팅
                contentsLikeDto.setFollowIdx(followInfo.getIdx());
                // 좋아요 내역 가져오기
                ContentsLikeDto oTargetInfo = getTargetInfo(contentsLikeDto);

                // 날짜 비교 위해 String -> Date 변환
                SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                dateformat.setLenient(false);
                // 좋아요 한 날짜
                Date likeDate = dateformat.parse(oTargetInfo.getRegDate());
                // 팔로우 한 날짜
                Date followDate = dateformat.parse(followInfo.getRegDate());
                if (likeDate.after(followDate)) {
                    updateFollowContentsLikeCntDown(contentsLikeDto);
                }
            }
        }
    }

    /*****************************************************
     *  SubFunction - Select
     ****************************************************/
    /**
     * 컨텐츠 좋아요 여부 가져오기
     *
     * @param contentsLikeDto contentsIdx,memberUuid
     * @return
     */
    public Boolean getContentsLikeCheck(ContentsLikeDto contentsLikeDto) {
        int iCount = contentsLikeDaoSub.getContentsLikeCheck(contentsLikeDto);

        return iCount > 0;
    }

    /**
     * 좋아요 내역 가져오기
     *
     * @param contentsLikeDto contentsIdx, loginMemberUuid
     * @return ContentsLikeDto
     */
    public ContentsLikeDto getTargetInfo(ContentsLikeDto contentsLikeDto) {
        return contentsLikeDaoSub.oGetTargetInfo(contentsLikeDto);
    }

    /**
     * cnt 테이블에 해당 idx 있는지 확인
     *
     * @param contentsLikeDto contentsIdx
     * @return Boolean
     */
    public Boolean checkCntByIdx(ContentsLikeDto contentsLikeDto) {
        return contentsLikeDaoSub.lCheckCntByIdx(contentsLikeDto) != null;
    }

    /**
     * sns_follow_contents_like_cnt 테이블에 해당 idx 있는지 확인
     *
     * @param contentsLikeDto followIdx
     * @return Boolean
     */
    public Boolean checkFollowCntByIdx(ContentsLikeDto contentsLikeDto) {
        int iCount = contentsLikeDaoSub.lCheckFollowCntByIdx(contentsLikeDto);

        return iCount > 0;
    }

    /*****************************************************
     *  SubFunction - Insert
     ****************************************************/
    /**
     * 콘텐츠 좋아요 등록
     *
     * @param contentsLikeDto contentsIdx, memberIdx, regDate
     */
    public Integer insertContentsLike(ContentsLikeDto contentsLikeDto) {
        Integer iResult = contentsLikeDao.insertContentsLike(contentsLikeDto);
        if (iResult < 1) {
            throw new CustomException(CustomError.LIKE_ERROR); // 좋아요 실패하였습니다.
        }
        return iResult;
    }

    /**
     * 콘텐츠 좋아요 cnt 등록
     *
     * @param contentsLikeDto contentsIdx, regDate
     */
    public void insertContentsLikeCnt(ContentsLikeDto contentsLikeDto) {
        // 콘텐츠 idx set
        contentsLikeDao.insertContentsLikeCnt(contentsLikeDto);
    }

    /**
     * 팔로우 콘텐츠 좋아요 cnt 등록
     *
     * @param contentsLikeDto followIdx, regDate
     */
    public void insertFollowContentsLikeCnt(ContentsLikeDto contentsLikeDto) {
        // 콘텐츠 idx set
        contentsLikeDao.insertFollowContentsLikeCnt(contentsLikeDto);
    }

    /*****************************************************
     *  SubFunction - Update
     ****************************************************/
    /**
     * 좋아요 상태값 변경 (state : 1)
     *
     * @param contentsLikeDto idx, state, regDate
     */
    public Integer updateLikeState(ContentsLikeDto contentsLikeDto) {
        contentsLikeDto.setState(1);
        Integer iResult = contentsLikeDao.updateContentsLike(contentsLikeDto);
        if (iResult != 1) {
            throw new CustomException(CustomError.LIKE_ERROR);  // 좋아요 실패하였습니다.
        }
        return iResult;
    }

    /**
     * 좋아요 취소 상태값 변경 (state : 0)
     *
     * @param contentsLikeDto memberIdx, contentsIdx
     */
    public void updateUnLikeState(ContentsLikeDto contentsLikeDto) {
        contentsLikeDto.setState(0);
        Integer iResult = contentsLikeDao.updateContentsLike(contentsLikeDto);
        if (iResult != 1) {
            throw new CustomException(CustomError.LIKE_CANCEL_ERROR); // 좋아요 취소 실패하였습니다.
        }
    }

    /**
     * 좋아요 cnt +1
     *
     * @param contentsLikeDto memberUuid, contentsIdx
     */
    public void updateContentsLikeCntUp(ContentsLikeDto contentsLikeDto) {
        contentsLikeDao.updateContentsLikeCntUp(contentsLikeDto);
    }

    /**
     * sns_follow_contents_like_cnt 좋아요 cnt +1
     *
     * @param contentsLikeDto followIdx
     */
    public void updateFollowContentsLikeCntUp(ContentsLikeDto contentsLikeDto) {
        contentsLikeDao.updateFollowContentsLikeCntUp(contentsLikeDto);
    }

    /**
     * 좋아요 cnt -1
     *
     * @param contentsLikeDto memberUuid, contentsIdx
     */
    public void updateContentsLikeCntDown(ContentsLikeDto contentsLikeDto) {
        contentsLikeDao.updateContentsLikeCntDown(contentsLikeDto);
    }

    /**
     * sns_follow_contents_like_cnt 좋아요 cnt -1
     *
     * @param contentsLikeDto followIdx
     */
    public void updateFollowContentsLikeCntDown(ContentsLikeDto contentsLikeDto) {
        contentsLikeDao.updateFollowContentsLikeCntDown(contentsLikeDto);
    }

    /*****************************************************
     *  SubFunction - Delete
     ****************************************************/


    /*****************************************************
     *  SubFunction - Validation
     ****************************************************/

    /**
     * 컨텐츠 좋아요 유효성
     *
     * @param contentsLikeDto : loginMemberUuid[로그인한 회원 uuid], contentsIdx[컨텐츠 idx]
     */
    private void likeCommonValidate(ContentsLikeDto contentsLikeDto, String actionType) {

        // 컨텐츠 idx 유효성
        Long idx = contentsLikeDto.getContentsIdx();
        contentsService.contentsIdxValidate(idx);

        String loginMemberUuid = contentsLikeDto.getLoginMemberUuid(); // 로그인 회원 uuid
        String writerUuid = contentsDaoSub.getMemberUuidByIdx(idx);    // 작성자 idx

        SearchDto searchDto = new SearchDto();       // 조회용 dto
        searchDto.setLoginMemberUuid(loginMemberUuid); // 회원 idx
        searchDto.setContentsIdx(idx);               // 컨텐츠 idx
        searchDto.setMemberUuid(writerUuid);           // 작성자 idx

        if (!loginMemberUuid.equals(writerUuid)) { // 내가 작성한 게시물이 아닌 경우
            // 3. 회원 차단했거나 차단 당했는지 체크
            boolean isBlock = super.bChkBlock(loginMemberUuid, writerUuid);

            // 차단 내역이 존재하고 좋아요인 경우
            if (isBlock && actionType.equals("register")) {
                throw new CustomException(CustomError.LIKE_ERROR_BY_MEMBER_BLOCK); // 좋아요를 할 수 없습니다.

                // 차단 내역이 존재하고 좋아요 취소인 경우
            } else if (isBlock && actionType.equals("cancel")) {
                throw new CustomException(CustomError.LIKE_CANCEL_ERROR_BY_MEMBER_BLOCK); // 좋아요를 취소할 수 없습니다.
            }

            // 컨텐츠 신고했는지 체크
            contentsService.reportContentsValidate(searchDto);

            // 팔로우 공개 게시물인지 체크
            contentsService.followContentsValidate(searchDto);

            // 숨김 게시물인지 체크
            contentsService.hideValidate(idx, loginMemberUuid);

            // 보관 게시물인지 체크
            contentsService.keepValidate(idx);
        }
    }

    /**
     * 컨텐츠 좋아요 한 회원 리스트 유효성
     *
     * @param searchDto : memberUuid[로그인 회원 uuid], contentsIdx[컨텐츠 idx]
     */
    private void contentsLikeListValidate(SearchDto searchDto) {

        String loginMemberUuid = searchDto.getMemberUuid();
        Long contentsIdx = searchDto.getContentsIdx();

        SearchDto searchContents = new SearchDto();
        searchContents.setContentsIdx(contentsIdx);
        searchContents.setLoginMemberUuid(loginMemberUuid);

        /** 컨텐츠 공통 유효성 **/
        contentsService.commonContentsValidate(searchContents);
    }

}
