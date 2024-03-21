package com.architecture.admin.services.comment;

import com.architecture.admin.libraries.PaginationLibray;
import com.architecture.admin.libraries.exception.CurlException;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.comment.CommentLikeDao;
import com.architecture.admin.models.daosub.comment.CommentLikeDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.comment.CommentDto;
import com.architecture.admin.models.dto.comment.CommentLikeDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;
import com.architecture.admin.models.dto.noti.NotiDto;
import com.architecture.admin.models.dto.push.PushDto;
import com.architecture.admin.services.BaseService;
import com.architecture.admin.services.block.BlockMemberService;
import com.architecture.admin.services.contents.ContentsService;
import com.architecture.admin.services.noti.CommentNotiService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CommentLikeService extends BaseService {

    private final CommentLikeDao commentLikeDao;
    private final CommentLikeDaoSub commentLikeDaoSub;
    private final CommentService commentService;                     // 댓글
    private final CommentNotiService commentNotiService;             // 알림
    private final ContentsService contentsService;                   // 컨텐츠
    private final LikePushService likePushService;                   // 푸시
    private final BlockMemberService blockMemberService;             // 차단
    @Value("${use.push.comment.like}")
    private boolean useLikePush; // 푸시 알림 true/false

    @Value("${use.noti.comment.like}")
    private boolean useCommentLikeNoti; // 알림 true/false

    /*****************************************************
     *  Modules
     ****************************************************/
    /**
     * 댓글에 좋아요한 회원 리스트
     *
     * @param token     access token
     * @param searchDto commentIdx
     * @return 좋아요한 회원 리스트
     */
    @Transactional(readOnly = true)
    public List<MemberInfoDto> getCommentLikeList(String token, SearchDto searchDto) throws JsonProcessingException {
        List<MemberInfoDto> likeMemberList = new ArrayList<>();
        List<String> uuidList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);
        searchDto.setMemberUuid(memberDto.getUuid());

        // 유효성 검사
        likeListValidate(searchDto);

        // 목록 전체 count
        int iTotalCount = commentLikeDaoSub.iGetTotalLikeCount(searchDto);

        if (iTotalCount > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(iTotalCount, searchDto);
            searchDto.setPagination(pagination);

            // list
            likeMemberList = commentLikeDaoSub.lGetCommentLikeList(searchDto);

            // list 에서 memberUuid 추출
            likeMemberList.forEach(item -> {
                uuidList.add(item.getUuid());
            });

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
     * 댓글 좋아요
     *
     * @param token          access token
     * @param commentLikeDto commentIdx
     */
    @Transactional
    public void commentLike(String token, CommentLikeDto commentLikeDto) {

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);
        commentLikeDto.setMemberUuid(memberDto.getUuid());

        // 좋아요 유효성 검사
        likeCommonValidate(commentLikeDto);

        // 등록일 set
        commentLikeDto.setRegDate(dateLibrary.getDatetime());
        // idx 기반 필요 데이터 get
        CommentLikeDto info = commentLikeDaoSub.getCommentInfo(commentLikeDto);
        // ContentsIdx set
        commentLikeDto.setContentsIdx(info.getContentsIdx());
        // Receiver set
        commentLikeDto.setReceiverUuid(info.getReceiverUuid());
        // contents set
        commentLikeDto.setContents(info.getContents());
        // 좋아요 내역 가져오기
        CommentLikeDto oTargetInfo = getTargetInfo(commentLikeDto);

        // 기존 내역이 존재한다면
        if (oTargetInfo != null) {

            // 이미 좋아요한 경우
            if (oTargetInfo.getState() == 1) {
                throw new CustomException(CustomError.LIKE_STATE); // 이미 좋아요 표시한 댓글입니다.
            }

            // 좋아요 취소 후 다시 요청한 경우
            if (oTargetInfo.getState() == 0) {
                // idx set [sns_comment_like]
                commentLikeDto.setIdx(oTargetInfo.getIdx());
                // 좋아요 상태 업데이트
                updateLikeState(commentLikeDto);
                // cnt +1
                updateCommentLikeCntUp(commentLikeDto);

                // 알림 보내기
                notiAction(token, commentLikeDto);
                // 푸시 보내기
                pushAction(token, commentLikeDto);
            }
        } else {
            // 좋아요 등록
            Integer iResult = insertCommentLike(commentLikeDto);

            // cnt 테이블에 해당 idx 있는지 확인
            Boolean bResult = checkCntByIdx(commentLikeDto);

            // cnt 테이블에 값 없으면
            if (Boolean.FALSE.equals(bResult)) {
                insertCommentLikeCnt(commentLikeDto);
            }
            // cnt +1
            updateCommentLikeCntUp(commentLikeDto);

            if (iResult > 0) {
                // 알림 보내기
                notiAction(token, commentLikeDto);
                // 푸시 보내기
                pushAction(token, commentLikeDto);
            }
        }
    }

    /**
     * 댓글 push
     *
     * @param token
     * @param commentLikeDto
     */
    public void pushAction(String token, CommentLikeDto commentLikeDto) {
        // 콘텐츠 좋아요 푸시알림 ON
        if (useLikePush) {

            // 푸시 DTO 세팅 - 콘텐츠 좋아요 푸시 타입: 3
            PushDto pushDto = PushDto.builder()
                    .senderUuid(commentLikeDto.getMemberUuid())         // 좋아요를 누른 사람
                    .receiverUuid(commentLikeDto.getReceiverUuid())     // 콘텐츠 작성자 (푸시대상)
                    .typeIdx(2)                                         // 푸시 타입
                    .contentsIdx(commentLikeDto.getContentsIdx())       // 콘텐츠 idx
                    .commentIdx(commentLikeDto.getCommentIdx())         // 댓글 idx
                    .build();

            // 푸시 보내기
            likePushService.sendLikePush(token, pushDto);

        }
    }

    /**
     * 댓글 알림
     *
     * @param token
     * @param commentLikeDto
     */
    public void notiAction(String token, CommentLikeDto commentLikeDto) {
        // 댓글 등록시 알림 ON
        if (useCommentLikeNoti) {
            // 알림 등록
            NotiDto notiDto = NotiDto.builder()
                    .senderUuid(commentLikeDto.getMemberUuid())     // 댓글을 등록 한 회원idx
                    .contentsIdx(commentLikeDto.getContentsIdx())   // 컨텐츠 IDX
                    .commentIdx(commentLikeDto.getCommentIdx())     // 댓글 IDX
                    .subType("like_comment")                        // sub_type
                    .contents(commentLikeDto.getContents())         // contents
                    .build();
            commentNotiService.commentLikeNoti(token, notiDto);
        }
    }

    /**
     * 댓글 좋아요 취소
     *
     * @param token             access token
     * @param commentLikeDto    commentIdx
     */
    @Transactional
    public void commentLikeCancel(String token, CommentLikeDto commentLikeDto) {

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);
        commentLikeDto.setMemberUuid(memberDto.getUuid());

        // 좋아요 & 취소 공통 검증
        likeCommonValidate(commentLikeDto);

        // 좋아요 내역 가져오기
        CommentLikeDto oTargetInfo = getTargetInfo(commentLikeDto);

        // 좋아요 한 적 없는데 취소 요청한 경우
        if (oTargetInfo == null) {
            throw new CustomException(CustomError.LIKE_DATA_ERROR); // 잘못된 요청입니다.
        }

        // 좋아요 insert 된 적이 있고 상태값이 정상인 경우
        if (oTargetInfo.getState() == 1) {

            // 등록일 set
            commentLikeDto.setRegDate(dateLibrary.getDatetime());

            // idx set [sns_comment_like]
            commentLikeDto.setIdx(oTargetInfo.getIdx());

            // 좋아요 상태값 0으로 변경
            updateUnLikeState(commentLikeDto);

            // 좋아요 cnt -1
            updateCommentLikeCntDown(commentLikeDto);

        }
        // 이미 취소되어 있다면
        else {
            throw new CustomException(CustomError.LIKE_CANCEL_STATE); // 이미 좋아요 취소한 댓글입니다.
        }

    }


    /*****************************************************
     *  SubFunction - Select
     ****************************************************/
    /**
     * 좋아요 내역 가져오기
     *
     * @param commentLikeDto commentIdx, memberUuid
     * @return commentLikeDto
     */
    public CommentLikeDto getTargetInfo(CommentLikeDto commentLikeDto) {
        return commentLikeDaoSub.oGetTargetInfo(commentLikeDto);
    }

    /**
     * cnt 테이블에 해당 idx 있는지 확인
     *
     * @param commentLikeDto commentIdx
     * @return Boolean
     */
    public Boolean checkCntByIdx(CommentLikeDto commentLikeDto) {
        return commentLikeDaoSub.lCheckCntByIdx(commentLikeDto) != null;
    }

    /*****************************************************
     *  SubFunction - Insert
     ****************************************************/
    /**
     * 댓글 좋아요 등록
     *
     * @param commentLikeDto commentIdx, memberUuid, regDate
     */
    public Integer insertCommentLike(CommentLikeDto commentLikeDto) {
        Integer iResult = commentLikeDao.insertCommentLike(commentLikeDto);
        if (iResult < 1) {
            throw new CustomException(CustomError.LIKE_ERROR); // 좋아요 실패하였습니다.
        }

        return iResult;
    }

    /**
     * 댓글 좋아요 cnt 등록
     *
     * @param commentLikeDto commentIdx, regDate
     */
    public void insertCommentLikeCnt(CommentLikeDto commentLikeDto) {
        // 댓글 idx set
        commentLikeDao.insertCommentLikeCnt(commentLikeDto);
    }

    /*****************************************************
     *  SubFunction - Update
     ****************************************************/
    /**
     * 좋아요 상태값 변경 (state : 1)
     *
     * @param commentLikeDto commentIdx
     */
    public void updateLikeState(CommentLikeDto commentLikeDto) {
        commentLikeDto.setState(1);
        Integer iResult = commentLikeDao.updateCommentLike(commentLikeDto);
        if (iResult != 1) {
            throw new CustomException(CustomError.LIKE_ERROR);  // 좋아요 실패하였습니다.
        }
    }

    /**
     * 좋아요 취소 상태값 변경 (state : 0)
     *
     * @param commentLikeDto commentIdx
     */
    public void updateUnLikeState(CommentLikeDto commentLikeDto) {
        commentLikeDto.setState(0);
        Integer iResult = commentLikeDao.updateCommentLike(commentLikeDto);
        if (iResult != 1) {
            throw new CustomException(CustomError.LIKE_CANCEL_ERROR); // 좋아요 취소 실패하였습니다.
        }
    }

    /**
     * 좋아요 cnt +1
     *
     * @param commentLikeDto commentIdx
     */
    public void updateCommentLikeCntUp(CommentLikeDto commentLikeDto) {
        commentLikeDao.updateCommentLikeCntUp(commentLikeDto);
    }

    /**
     * 좋아요 cnt -1
     *
     * @param commentLikeDto commentIdx
     */
    public void updateCommentLikeCntDown(CommentLikeDto commentLikeDto) {
        commentLikeDao.updateCommentLikeCntDown(commentLikeDto);
    }

    /*****************************************************
     *  SubFunction - Delete
     ****************************************************/


    /*****************************************************
     *  Validation
     ****************************************************/

    /**
     * 좋아요 리스트 유효성 [로그인, 비 로그인]
     *
     * @param searchDto : memberUuid, commentIdx
     */
    private void likeListValidate(SearchDto searchDto) {

        Long idx = searchDto.getCommentIdx();           // 댓글 idx
        String memberUuid = searchDto.getMemberUuid(); // 회원 idx

        // 댓글 idx로 컨텐츠 idx 조회
        Long contentsIdx = contentsService.getContentsIdxByCommentIdx(idx);

        /** 댓글 공통 유효성 검사 **/
        commonLikeCommentValidate(idx, contentsIdx, memberUuid);

        // 컨텐츠 검증용 dto
        SearchDto searchContents = new SearchDto();
        searchContents.setLoginMemberUuid(memberUuid);
        searchContents.setContentsIdx(contentsIdx);

        /** 컨텐츠 공통 검증 [로그인, 비 로그인 나누어 검증] **/
        contentsService.commonContentsValidate(searchContents);
    }

    /**
     * 댓글 좋아요 유효성 [로그인]
     *
     * @param commentLikeDto : memberUuid, commentIdx
     */
    private void likeCommonValidate(CommentLikeDto commentLikeDto) {

        Long idx = commentLikeDto.getCommentIdx();      // 댓글 idx
        String loginMemberUuid = commentLikeDto.getMemberUuid(); // 회원 idx

        // 댓글 idx로 컨텐츠 idx 조회
        Long contentsIdx = contentsService.getContentsIdxByCommentIdx(idx);

        /** 댓글 공통 유효성 검사 **/
        commonLikeCommentValidate(idx, contentsIdx, loginMemberUuid);

        // 컨텐츠 검증용 dto
        SearchDto searchContents = new SearchDto();
        searchContents.setLoginMemberUuid(loginMemberUuid);
        searchContents.setContentsIdx(contentsIdx);

        /** 컨텐츠 공통 검증 [로그인, 비 로그인 나누어 검증] **/
        contentsService.commonContentsValidate(searchContents);
    }

    /**
     * 댓글 좋아요 관련 공통 유효성
     *
     * @param idx             : 댓글 idx
     * @param contentsIdx     : 컨텐츠 idx
     * @param loginMemberUuid : 로그인 회원 uuid
     */
    private void commonLikeCommentValidate(Long idx, Long contentsIdx, String loginMemberUuid) {

        /** 댓글 idx 기본 검증 **/
        if (idx == null || idx < 1) {
            throw new CustomException(CustomError.COMMENT_IDX_NULL); // 존재하지 않는 댓글입니다.
        }

        CommentDto searchCommentDto = CommentDto.builder()  // 조회용 dto
                .idx(idx)
                .contentsIdx(contentsIdx).build();

        /** 유효한 댓글인지 db 검증 **/
        commentService.normalCommentValidate(searchCommentDto);

        /** 댓글 작성한 회원과 차단한 관계인지 검증 **/
        String commentWriterUuid = commentService.getMemberUuidByIdx(idx); // 댓글 작성자 idx 조회
        blockMemberService.writerAndMemberBlockValidate(commentWriterUuid,loginMemberUuid);

        // 신고 조회용 dto
        SearchDto reportSearchDto = new SearchDto();
        reportSearchDto.setLoginMemberUuid(loginMemberUuid);    // 로그인 회원 idx
        reportSearchDto.setContentsIdx(contentsIdx);            // 컨텐츠 idx
        reportSearchDto.setCommentIdx(idx);                     // 댓글 idx

        /** 댓글 신고 검증 **/
        commentService.reportCommentValidate(reportSearchDto);
    }

}
