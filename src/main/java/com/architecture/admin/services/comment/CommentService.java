package com.architecture.admin.services.comment;

import com.architecture.admin.libraries.PaginationLibray;
import com.architecture.admin.libraries.exception.CurlException;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.comment.CommentDao;
import com.architecture.admin.models.dao.comment.CommentLikeDao;
import com.architecture.admin.models.daosub.comment.CommentDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.comment.CommentDto;
import com.architecture.admin.models.dto.comment.CommentLikeDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;
import com.architecture.admin.models.dto.noti.NotiDto;
import com.architecture.admin.models.dto.push.PushDto;
import com.architecture.admin.models.dto.tag.HashTagDto;
import com.architecture.admin.models.dto.tag.MentionTagDto;
import com.architecture.admin.services.BaseService;
import com.architecture.admin.services.contents.ContentsCurlService;
import com.architecture.admin.services.contents.ContentsService;
import com.architecture.admin.services.noti.CommentNotiService;
import com.architecture.admin.services.push.CommentPushService;
import com.architecture.admin.services.restrain.RestrainCurlService;
import com.architecture.admin.services.tag.HashTagService;
import com.architecture.admin.services.tag.MentionTagService;
import com.architecture.admin.services.wordcheck.ContentsWordCheckService;
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

import java.text.BreakIterator;
import java.util.*;

/*****************************************************
 * 컨텐츠 댓글 모델러
 ****************************************************/
@Service
@RequiredArgsConstructor
@Transactional
public class CommentService extends BaseService {

    private final CommentDao commentDao;
    private final CommentDaoSub commentDaoSub;
    private final CommentLikeDao commentLikeDao;
    private final ContentsService contentsService;
    private final HashTagService hashTagService;                // 해시태그
    private final MentionTagService mentionTagService;          // 회원 멘션
    private final CommentNotiService commentNotiService;        // 알림
    private final CommentPushService commentPushService;        // 푸시
    private final ContentsWordCheckService contentsWordCheckService;    // 콘텐츠 금칙어
    private final String COMMENT = "comment";                   // 댓글 상수
    private final RestrainCurlService restrainCurlService;
    private final ContentsCurlService contentsCurlService;
    @Value("${word.check.comment.type}")
    private int commentWordChk;  // 댓글 금칙어 타입
    @Value("${use.push.comment.regist}")
    private boolean useCommentPush; // 푸시 알림 true/false
    @Value("${use.noti.comment.regist}")
    private boolean useCommentNoti; // 알림 true/false
    @Value("${contents.comment.text.max}")
    private int commentTextMax;     // 최대 입력 가능 글자수

    /*****************************************************
     *  Modules
     ****************************************************/

    /**
     * 댓글 등록
     *
     * @param commentDto : contentsIdx(컨텐츠 idx), memberIdx(회원 idx), contents(댓글 내용), parentIdx(부모댓글)
     */
    public Long registComment(String token, CommentDto commentDto) {

        // 등록 유효성 검사
        registerValidate(token, commentDto);

        // 컨텐츠 작성자 <-> 댓글 작성자 차단 체크 ( return : 컨텐츠 작성자 Idx )
        checkContentsWriterBlock(commentDto);

        // parents_idx값이 비어있지 않으면 ( 대댓글이면 )
        if (commentDto.getParentIdx() != null) {
            // 댓글 작성자  <-> 대댓글 작성자 차단 체크
            checkCommentWriterBlock(commentDto);
        }

        // 댓글 uuid 세팅 ( cmt + locale Language + 랜덤 UUID + Timestamp )
        String localeLang = super.getLocaleLang();
        String setUuid = dateLibrary.getTimestamp();
        String uuid = "sns_contents_comment" + localeLang + UUID.randomUUID().toString().concat(setUuid);
        uuid = uuid.replace("-", "");

        // 고유아이디 중복체크
        Boolean bDupleUuid = checkDupleUuid(uuid);

        // 고유아이디가 중복이면 5번 재시도
        int retry = 0;
        while (Boolean.TRUE.equals(bDupleUuid) && retry < 5) {
            retry++;
            pushAlarm("컨텐츠 댓글 고유아이디 중복 시도::" + retry + "번째");

            // 회원 uuid 세팅 ( locale Language + 랜덤 UUID + Timestamp )
            localeLang = super.getLocaleLang();
            setUuid = dateLibrary.getTimestamp();
            uuid = "sns_contents_comment" + localeLang + UUID.randomUUID().toString().concat(setUuid);
            uuid = uuid.replace("-", "");

            bDupleUuid = checkDupleUuid(uuid);

            if (retry == 5) {
                throw new CustomException(CustomError.COMMENT_UUID_DUPLE);
            }
        }
        // 고유아이디 set
        commentDto.setUuid(uuid);

        // 댓글 등록
        insert(commentDto);

        // cnt 테이블 정보
        boolean checkMemberCnt = selectContentsCommentCntCheck(commentDto);

        if (!checkMemberCnt) {
            // cnt 테이블에 0으로 인서트
            insertCommentCnt(commentDto);
        }

        // cnt + 1
        updateCommentCntUp(commentDto);

        // 해시태그 입력
        insertHashTag(commentDto);

        // 회원 멘션 입력 후 멘션 회원 리스트 가져오기
        insertMention(token, commentDto);

        // 댓글 좋아요 카운트 0으로 insert
        insertCommentLikeCnt(commentDto);

        // 댓글 내용 업데이트
        Long result = updateCommentContents(commentDto);

        // 댓글 등록시 알림 ON
        if (useCommentNoti) {
            // 알림 등록
            NotiDto notiDto = NotiDto.builder()
                    .senderUuid(commentDto.getMemberUuid())       // 댓글을 등록 한 회원idx
                    .contentsIdx(commentDto.getContentsIdx())   // 컨텐츠 IDX
                    .commentIdx(commentDto.getInsertedIdx())    // 댓글 IDX
                    .subType("new_comment")
                    .contents(commentDto.getContents())
                    .parentIdx(commentDto.getParentIdx())
                    .build();
            commentNotiService.commentSendNoti(token, notiDto, "register");
        }

        if (result > 0) {
            // 푸시 사용 여부
            if (useCommentPush) {
                // 푸시 등록
                PushDto pushDto = PushDto.builder()
                        .commentIdx(commentDto.getInsertedIdx()) // 컨텐츠 IDX
                        .build();
                commentPushService.commentRegistSendPush(pushDto);
            }
        }
        return result;
    }

    /**
     * 댓글 수정
     *
     * @param token
     * @param commentDto : idx[댓글 idx], contentsIdx[컨텐츠 idx], memberUuid[로그인 회원 idx]
     */
    public void modifyComment(String token, CommentDto commentDto) {

        MemberDto loginUserInfo = super.getMemberUuidByToken(token);
        // 로그인 회원 uuid set
        commentDto.setMemberUuid(loginUserInfo.getUuid());
        // validate
        modifyValidate(token, commentDto);

        // 기존 멘션된 회원 uuid 리스트
        List<String> mentionMemberList = commentNotiService.selectMentionMemberFromSub(commentDto.getIdx());

        /** 멘션 수정 **/
        modifyMention(token, commentDto, mentionMemberList);

        /** 해시태그 수정 후 댓글 내용 반환 **/
        modifyHashTag(commentDto);

        /** 댓글 수정 **/
        commentDto.setModiDate(dateLibrary.getDatetime()); // 수정일 시간 set
        int result = commentDao.modifyComment(commentDto);

        if (result < 1) {
            throw new CustomException(CustomError.COMMENT_MODIFY_ERROR); // 댓글 수정에 실패하였습니다.
        }

        // 댓글 등록시 알림 ON
        if (useCommentNoti) {
            // 알림 등록
            NotiDto notiDto = NotiDto.builder()
                    .senderUuid(commentDto.getMemberUuid())       // 댓글을 등록 한 회원idx
                    .contentsIdx(commentDto.getContentsIdx())   // 컨텐츠 IDX
                    .commentIdx(commentDto.getIdx())            // 댓글 IDX
                    .subType("new_comment")
                    .contents(commentDto.getContents())
                    .parentIdx(commentDto.getParentIdx())
                    .mentionMemberUuidList(mentionMemberList)
                    .build();

            commentNotiService.commentSendNoti(token, notiDto, "modify");
        }

        // 푸시 사용 여부
        if (useCommentPush) {
            // 푸시 등록
            PushDto pushDto = PushDto.builder()
                    .commentIdx(commentDto.getIdx()) // 댓글 IDX
                    .build();

            commentPushService.commentModifySendPush(pushDto);
        }
    }


    /*****************************************************
     *  SubFunction - select
     ****************************************************/
    /**
     * sns_contents_comment_cnt 데이터 있는지 확인
     *
     * @param commentDto contents_idx
     * @return true/false
     */
    public Boolean selectContentsCommentCntCheck(CommentDto commentDto) {
        int iCount = commentDaoSub.getCommentCntCheck(commentDto);

        return iCount > 0;
    }

    /**
     * sns_contents_comment_cnt 데이터 있는지 확인
     *
     * @param commentDto contents_idx
     * @return true/false
     */
    public CommentDto getTotalSumCommentCount(CommentDto commentDto) {
        int iCount = commentDaoSub.getCommentCntCheck(commentDto);

        if (iCount < 1) {
            commentDto.setTotalCommentCnt(0L);
            return commentDto;
        }

        return (commentDaoSub.getTotalSumCommentCount(commentDto));
    }

    /**
     * 댓글 고유 아이디 중복 검색
     *
     * @param uuid 고유아이디
     * @return 중복여부 [중복 : true]
     */
    public Boolean checkDupleUuid(String uuid) {
        Integer iCount = commentDaoSub.getCountByUuid(uuid);

        return iCount > 0;
    }

    /**
     * 댓글 IDX로 댓글 작성자idx 값 가져오기
     *
     * @param commentIdx commentIdx
     * @return memberUuid
     */
    public String getMemberUuidByIdx(Long commentIdx) {
        return commentDaoSub.getMemberUuidByIdx(commentIdx);
    }

    /**
     * 댓글 리스트 가져오기
     */
    public List<CommentDto> getCommentList(String token, SearchDto searchDto) throws JsonProcessingException {
        // 로그인 시
        if (!ObjectUtils.isEmpty(token)) {
            MemberDto loginUserInfo = super.getMemberUuidByToken(token);
            searchDto.setMemberUuid(loginUserInfo.getUuid()); // 회원 uuid setting
        }

        // data init
        List<CommentDto> parentCommentList = new ArrayList<>();
        List<String> uuidList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        // 부모 댓글 목록 전체 count by 그 콘텐츠 기준으로 댓글 총 갯수
        int iTotalCount = commentDaoSub.iGetParentTotalCommentCnt(searchDto);

        // 리스트가 비었다면
        if (iTotalCount > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(iTotalCount, searchDto);
            searchDto.setPagination(pagination);

            // 부모 댓글 list by contentsIdx
            parentCommentList = commentDaoSub.getParentCommentList(searchDto);

            // 빈 리스트 리턴 처리
            if (parentCommentList.size() == 0) {
                return new ArrayList<>(); // 리스트가 비었습니다
            }

            // list 에서 memberUuid 추출
            parentCommentList.forEach(item -> {
                uuidList.add(item.getMemberUuid());
            });

            // curl 회원 조회
            String jsonString = memberCurlService.getMemberInfoByUuidList(uuidList);

            JSONObject jsonObject = new JSONObject(jsonString);
            if (!(jsonObject.getBoolean("result"))) {
                throw new CurlException(jsonObject);
            }

            // data 안 list 파싱
            JsonObject listJson = (JsonObject) JsonParser.parseString(jsonObject.get("data").toString());
            List<MemberInfoDto> memberInfoList = mapper.readValue(listJson.get("memberInfoList").toString(), new TypeReference<>() {
            }); // 회원 정보 list

            // uuid 로 회원 정보 매핑
            for (MemberInfoDto memberInfo : memberInfoList) {
                for (CommentDto lCommentDto : parentCommentList) {
                    if (Objects.equals(lCommentDto.getMemberUuid(), memberInfo.getUuid())) {
                        lCommentDto.setNick(memberInfo.getNick());
                        lCommentDto.setProfileImgUrl(memberInfo.getProfileImgUrl());
                        lCommentDto.setIntro(memberInfo.getIntro());
                    }
                }
            }

            /** 금칙어 치환 및 멘션 리스트 조회 **/
            for (CommentDto commentDto : parentCommentList) {
                commentDto.setMentionList((new ArrayList<>())); // 멘션 리스트 생성

                // 금칙어가 포함되어있으면 치환
                String comment = contentsWordCheckService.contentsWordCheck(commentDto.getContents(), commentWordChk);
                commentDto.setContents(comment); // 금칙어 처리

                // 멘션 리스트 가져오기
                List<String> mentionTagDtoList = commentDaoSub.getCommentMentionTags(commentDto.getIdx());
                List<MentionTagDto> mentionUserInfoList = new ArrayList<>();

                if (!ObjectUtils.isEmpty(mentionTagDtoList)) {

                    String mentionInfoJsonString = contentsCurlService.getMentionInfoList(mentionTagDtoList);

                    JSONObject mentionInfoObject = new JSONObject(mentionInfoJsonString);
                    // curl 통신 중 에러
                    if (!mentionInfoObject.getBoolean("result")) {
                        throw new CurlException(mentionInfoObject);
                    }

                    JSONObject mentionInfoResult = (JSONObject) mentionInfoObject.get("data");
                    // 멘션 리스트
                    mentionUserInfoList = mapper.readValue(mentionInfoResult.get("list").toString(), new TypeReference<>() {
                    });
                }

                commentDto.setMentionList(mentionUserInfoList); // 멘션 리스트
            }

            // 리스트 메인 / 자식 댓글 2개가 보여야함
            if (!parentCommentList.isEmpty()) {
                // 자식 comment data 가져오기
                addChildComment(parentCommentList, searchDto);
            }
        }

        return parentCommentList;
    }

    /**
     * 대 댓글 리스트 가져오기
     */
    public List<CommentDto> getChildCommentList(String token, SearchDto searchDto) throws JsonProcessingException {
        // 로그인 시
        if (!ObjectUtils.isEmpty(token)) {
            MemberDto loginUserInfo = super.getMemberUuidByToken(token);
            searchDto.setMemberUuid(loginUserInfo.getUuid()); // 회원 uuid setting
        }

        List<CommentDto> childCommentList = new ArrayList<>();
        List<String> uuidList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        // 대 댓글 목록 전체 count
        int iTotalCount = commentDaoSub.iGetChildTotalCommentCnt(searchDto);

        // 리스트 비었는지 체크
        if (iTotalCount > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(iTotalCount, searchDto);
            searchDto.setPagination(pagination);

            // list
            childCommentList = commentDaoSub.getChildCommentList(searchDto);

            // 빈 리스트 리턴 처리
            if (childCommentList.size() == 0) {
                return new ArrayList<>(); // 리스트가 비었습니다
            }

            // list 에서 memberUuid 추출
            childCommentList.forEach(item -> {
                uuidList.add(item.getMemberUuid());
            });

            // curl 회원 조회
            String jsonString = memberCurlService.getMemberInfoByUuidList(uuidList);

            JSONObject jsonObject = new JSONObject(jsonString);
            if (!(jsonObject.getBoolean("result"))) {
                throw new CurlException(jsonObject);
            }

            // data 안 list 파싱
            JsonObject listJson = (JsonObject) JsonParser.parseString(jsonObject.get("data").toString());
            List<MemberInfoDto> memberInfoList = mapper.readValue(listJson.get("memberInfoList").toString(), new TypeReference<>() {
            }); // 회원 정보 list

            // uuid 로 회원 정보 매핑
            for (MemberInfoDto memberInfo : memberInfoList) {
                for (CommentDto childCommentDto : childCommentList) {
                    if (Objects.equals(childCommentDto.getMemberUuid(), memberInfo.getUuid())) {
                        childCommentDto.setNick(memberInfo.getNick());
                        childCommentDto.setProfileImgUrl(memberInfo.getProfileImgUrl());
                        childCommentDto.setIntro(memberInfo.getIntro());
                    }
                }
            }

            /** 금칙어 치환 및 멘션 리스트 조회 **/
            for (CommentDto commentDto : childCommentList) {
                commentDto.setMentionList((new ArrayList<>())); // 멘션 리스트 생성
                // 금칙어가 포함되어있으면 치환
                String comment = contentsWordCheckService.contentsWordCheck(commentDto.getContents(), commentWordChk);
                commentDto.setContents(comment); // 금칙어 처리

                // 멘션 리스트 가져오기
                List<String> mentionTagDtoList = commentDaoSub.getCommentMentionTags(commentDto.getIdx());
                List<MentionTagDto> mentionUserInfoList = new ArrayList<>();

                if (!ObjectUtils.isEmpty(mentionTagDtoList)) {

                    String mentionInfoJsonString = contentsCurlService.getMentionInfoList(mentionTagDtoList);

                    JSONObject mentionInfoObject = new JSONObject(mentionInfoJsonString);
                    // curl 통신 중 에러
                    if (!mentionInfoObject.getBoolean("result")) {
                        throw new CurlException(mentionInfoObject);
                    }

                    JSONObject mentionInfoResult = (JSONObject) mentionInfoObject.get("data");
                    // 멘션 리스트
                    mentionUserInfoList = mapper.readValue(mentionInfoResult.get("list").toString(), new TypeReference<>() {
                    });
                }

                commentDto.setMentionList(mentionUserInfoList); // 멘션 리스트
            }
        }

        return childCommentList;
    }

    /**
     * 포커스 커멘트 리스트
     */
    public List<CommentDto> getFocusCommentList(String token, SearchDto searchDto) throws JsonProcessingException {
        // 초기화
        int iTotalCnt = 0;
        int iRowNumPaging = 0;
        List<String> uuidList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);

        // set uuid
        searchDto.setMemberUuid(memberDto.getUuid());

        // 부모 댓글이냐 체크
        Long parentCheckValue = commentDaoSub.getCheckParent(searchDto);

        // 빈 리스트 리턴 처리
        if (parentCheckValue == null) {
            return new ArrayList<>(); // 리스트가 비었습니다
        }

        // set parent Idx
        searchDto.setParentIdx(parentCheckValue);

        // 부모 댓글이면 해당 컨텐츠에서 몇번째 인덱스인지
        if (parentCheckValue == 0) {
            //total 가져오기
            iTotalCnt = commentDaoSub.iGetParentTotalCommentCnt(searchDto);
        } else {
            //total 가져오기
            iTotalCnt = commentDaoSub.iGetChildTotalCommentCnt(searchDto);
        }

        if (iTotalCnt <= 0) {
            return new ArrayList<>(); // 리스트가 비었습니다
        }

        // 해당 rowNum 가져오기
        List<CommentDto> getRowNumList = commentDaoSub.getRowNum(searchDto);

        /** 해당 위치 알아오기 **/
        for (CommentDto commentDto : getRowNumList) {
            if (commentDto.getIdx().equals(searchDto.getCommentIdx())) {
                // 해당 페이지
                iRowNumPaging = (int) Math.ceil((double) commentDto.getRowNum() / (double) searchDto.getLimit());
                searchDto.setPage(iRowNumPaging);
            }
        }

        // paging
        PaginationLibray pagination = new PaginationLibray(iTotalCnt, searchDto);

        // 포커스 당하는 얘 위치 setPaging 처리
        searchDto.setPagination(pagination);

        // 리스트 표출
        List<CommentDto> getFocusList = commentDaoSub.getFocusList(searchDto);

        // 빈 리스트 리턴 처리
        if (getFocusList.size() == 0) {
            return new ArrayList<>(); // 리스트가 비었습니다
        }

        // list 에서 memberUuid 추출
        getFocusList.forEach(item -> {
            uuidList.add(item.getMemberUuid());
        });

        // curl 회원 조회
        String jsonString = memberCurlService.getMemberInfoByUuidList(uuidList);

        JSONObject jsonObject = new JSONObject(jsonString);
        if (!(jsonObject.getBoolean("result"))) {
            throw new CurlException(jsonObject);
        }

        // data 안 list 파싱
        JsonObject listJson = (JsonObject) JsonParser.parseString(jsonObject.get("data").toString());
        List<MemberInfoDto> memberInfoList = mapper.readValue(listJson.get("memberInfoList").toString(), new TypeReference<>() {
        }); // 회원 정보 list

        // uuid 로 회원 정보 매핑
        for (MemberInfoDto memberInfo : memberInfoList) {
            for (CommentDto focusCommentDto : getFocusList) {
                if (Objects.equals(focusCommentDto.getMemberUuid(), memberInfo.getUuid())) {
                    focusCommentDto.setNick(memberInfo.getNick());
                    focusCommentDto.setProfileImgUrl(memberInfo.getProfileImgUrl());
                    focusCommentDto.setIntro(memberInfo.getIntro());
                }
            }
        }

        /** 금칙어 치환 및 멘션 리스트 조회 **/
        for (CommentDto commentDto : getFocusList) {
            commentDto.setMentionList((new ArrayList<>())); // 멘션 리스트 생성
            // 금칙어가 포함되어있으면 치환
            String comment = contentsWordCheckService.contentsWordCheck(commentDto.getContents(), commentWordChk);
            commentDto.setContents(comment); // 금칙어 처리

            // 멘션 리스트 가져오기
            List<String> mentionTagDtoList = commentDaoSub.getCommentMentionTags(commentDto.getIdx());
            List<MentionTagDto> mentionUserInfoList = new ArrayList<>();

            if (!ObjectUtils.isEmpty(mentionTagDtoList)) {

                String mentionInfoJsonString = contentsCurlService.getMentionInfoList(mentionTagDtoList);

                JSONObject mentionInfoObject = new JSONObject(mentionInfoJsonString);
                // curl 통신 중 에러
                if (!mentionInfoObject.getBoolean("result")) {
                    throw new CurlException(mentionInfoObject);
                }

                JSONObject mentionInfoResult = (JSONObject) mentionInfoObject.get("data");
                // 멘션 리스트
                mentionUserInfoList = mapper.readValue(mentionInfoResult.get("list").toString(), new TypeReference<>() {
                });
            }

            commentDto.setMentionList(mentionUserInfoList); // 멘션 리스트
        }

        return getFocusList;
    }

    public Boolean checkRemoveAuth(CommentDto commentDto) {
        int iCount = commentDaoSub.checkRemoveAuth(commentDto);

        return iCount > 0;
    }


    /*****************************************************
     *  SubFunction - insert
     ****************************************************/
    /**
     * sns_contents_comment 테이블 등록
     *
     * @param commentDto
     * @return insertedIdx
     */
    public Long insert(CommentDto commentDto) {
        // 등록일
        commentDto.setRegDate(dateLibrary.getDatetime());

        return commentDao.insert(commentDto);
    }

    /**
     * cnt 테이블에 follower_cnt 신규 인서트
     *
     * @param commentDto contentsIdx
     */
    public void insertCommentCnt(CommentDto commentDto) {
        commentDao.insertCommentCnt(commentDto);
    }

    /**
     * 해시태그 테이블 입력
     *
     * @param commentDto insertedIdx contents
     */
    public void insertHashTag(CommentDto commentDto) {
        // 해시태그 관련 데이터 set
        HashTagDto hashTagDto = new HashTagDto();
        // 댓글 타입을 comment로 정의
        hashTagDto.setType("comment");
        // 댓글 idx
        hashTagDto.setCommentIdx(commentDto.getInsertedIdx());
        // 댓글 내용
        hashTagDto.setContents(commentDto.getContents());
        // 해시태그 치환 ex) #해시태그 -> [#[해시태그]] 및 해시태그/cnt 업데이트
        String hashTagContents = hashTagService.reigstHashTag(hashTagDto);
        // 컨텐츠 세팅
        commentDto.setContents(hashTagContents);
    }

    /**
     * 멘션 테이블 입력
     *
     * @param commentDto insertedIdx contents
     */
    public void insertMention(String token, CommentDto commentDto) {
        // 멘션 관련 데이터 set
        MentionTagDto mentionTagDto = new MentionTagDto();
        // 댓글 타입을 comment로 정의
        mentionTagDto.setType("comment");
        // 댓글 idx
        mentionTagDto.setCommentIdx(commentDto.getInsertedIdx());
        // 댓글 내용
        mentionTagDto.setContents(commentDto.getContents());
        // 멤버멘션 치환
        String mentionTagContents = mentionTagService.reigstMention(token, mentionTagDto);
        // 컨텐츠 세팅
        commentDto.setContents(mentionTagContents);
    }

    /**
     * 댓글 좋아요 cnt 등록
     *
     * @param commentDto commentIdx
     */
    public void insertCommentLikeCnt(CommentDto commentDto) {
        // 컨텐츠 idx set
        CommentLikeDto commentLikeDto = new CommentLikeDto();
        commentLikeDto.setCommentIdx(commentDto.getInsertedIdx());
        // 댓글 idx set
        commentLikeDao.insertCommentLikeCnt(commentLikeDto);
    }

    /*****************************************************
     *  SubFunction - Update
     ****************************************************/
    /**
     * sns_contents_comment_cnt.comment_cnt + 1
     *
     * @param commentDto contentsIdx
     */
    public void updateCommentCntUp(CommentDto commentDto) {
        commentDao.updateCommentCntUp(commentDto);
    }

    /**
     * sns_contents_comment_cnt.comment_cnt + 1
     *
     * @param commentDto contentsIdx
     */
    public void updateCommentCntDown(CommentDto commentDto) {
        commentDao.updateCommentCntDown(commentDto);
    }

    /**
     * 댓글 내용 업데이트 하기
     *
     * @param commentDto insertedIdx 업데이트 할 댓글 idx
     * @return
     */
    public Long updateCommentContents(CommentDto commentDto) {
        if (commentDto.getInsertedIdx() == null || commentDto.getInsertedIdx() < 0) {
            // 업데이트 IDX 오류
            throw new CustomException(CustomError.COMMENT_UPDATE_CONTENTS_IDX_ERROR);
        }

        Long iResult = commentDao.updateCommentContents(commentDto);

        if (iResult < 1) {
            // 댓글 내용 업데이트 실패하였습니다.
            throw new CustomException(CustomError.COMMENT_UPDATE_CONTENTS_ERROR);
        }
        return iResult;
    }

    /**
     * 멘션 테이블 수정
     *
     * @param commentDto
     * @param mentionMemberList : 기존 멘션 회원 idx 리스트
     */
    public void modifyMention(String token, CommentDto commentDto, List<String> mentionMemberList) {
        // 멘션 관련 데이터 set
        MentionTagDto mentionTagDto = new MentionTagDto();
        // 타입 지정
        mentionTagDto.setType(COMMENT);
        // 댓글 idx
        mentionTagDto.setCommentIdx(commentDto.getIdx());
        // 댓글 내용
        mentionTagDto.setContents(commentDto.getContents());

        if (mentionMemberList != null && !mentionMemberList.isEmpty()) { // 기존 회원 멘션이 존재하면
            String contents = mentionTagService.modifyMention(token, mentionTagDto);
            commentDto.setContents(contents);

        } else { // 기존 멘션이 존재하지 않으면
            String mentionTagContents = mentionTagService.reigstMention(token, mentionTagDto);
            commentDto.setContents(mentionTagContents);
        }
    }

    /**
     * 해시태그 테이블 수정
     *
     * @param commentDto
     * @return contents -> 댓글 내용
     */
    private void modifyHashTag(CommentDto commentDto) {

        HashTagDto hashTagDto = new HashTagDto();
        // 타입 지정
        hashTagDto.setType(COMMENT);
        // 댓글 idx
        hashTagDto.setCommentIdx(commentDto.getIdx());
        // 댓글 내용
        hashTagDto.setContents(commentDto.getContents());
        // 해시태그 치환 후 댓글 내용 리턴
        String contents = hashTagService.modifyHashTag(hashTagDto);
        commentDto.setContents(contents);
    }

    /*****************************************************
     *  SubFunction - Delete
     ****************************************************/
    /**
     * 댓글 /대댓글 삭제
     *
     * @param commentDto 삭제 할 댓글 idx, memberIdx, state
     * @return
     */
    @Transactional
    public Integer removeComment(String token, CommentDto commentDto) {
        Integer iResult = 0;

        // curl get uuid
        MemberDto loginUserInfo = super.getMemberUuidByToken(token);

        // 로그인 회원 uuid set
        commentDto.setMemberUuid(loginUserInfo.getUuid());

        // 댓글 삭제 유효성 검사
        deleteValidate(commentDto);

        if (commentDto.getParentIdx() == 0) {
            // 부모 댓글 삭제 처리
            iResult = commentDao.removeParentComment(commentDto);
            commentDto.setAffectedRow(iResult);
            // 코멘트 count 수정
            commentDao.updateCommentCntDown(commentDto);
        } else {
            // 자식 삭제 처리
            iResult = commentDao.removeChildComment(commentDto);
            commentDto.setAffectedRow(iResult);
            // 코멘트 count 수정
            commentDao.updateCommentCntDown(commentDto);
        }


        return iResult;
    }

    /*****************************************************
     *  SubFunction - Etc
     ****************************************************/
    /**
     * 정보 add list
     *
     * @param list
     */
    public void addChildComment(List<CommentDto> list, SearchDto searchParentDto) throws JsonProcessingException {
        List<String> uuidList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        for (CommentDto dto : list) {
            //idx 체크
            if (dto.getIdx() != null) {
                // 페이지에 노출 될 개수 2개로 처리
                SearchDto childSearchDto = new SearchDto();
                childSearchDto.setRecordSize(2);
                childSearchDto.setMemberUuid(searchParentDto.getLoginMemberUuid());
                childSearchDto.setCommentIdx(dto.getIdx());
                childSearchDto.setParentIdx(dto.getIdx());

                String contents = dto.getContents();
                contents = contentsWordCheckService.contentsWordCheck(contents, commentWordChk);
                dto.setContents(contents);

                // 해당 부모 댓글에서 자식 댓글 목록 전체 count
                int iTotalCount = commentDaoSub.iGetChildTotalCommentCnt(childSearchDto);

                // paging
                PaginationLibray childPagination = new PaginationLibray(iTotalCount, childSearchDto);
                childSearchDto.setPagination(childPagination);

                List<CommentDto> childCommentList = commentDaoSub.getChildCommentList(childSearchDto);

                // 빈 리스트 리턴 처리
                if (childCommentList.size() != 0) {
                    // list 에서 memberUuid 추출
                    childCommentList.forEach(item -> {
                        uuidList.add(item.getMemberUuid());
                    });

                    // curl 회원 조회
                    String jsonString = memberCurlService.getMemberInfoByUuidList(uuidList);

                    JSONObject jsonObject = new JSONObject(jsonString);
                    if (!(jsonObject.getBoolean("result"))) {
                        throw new CurlException(jsonObject);
                    }

                    // data 안 list 파싱
                    JsonObject listJson = (JsonObject) JsonParser.parseString(jsonObject.get("data").toString());
                    List<MemberInfoDto> memberInfoList = mapper.readValue(listJson.get("memberInfoList").toString(), new TypeReference<>() {
                    }); // 회원 정보 list

                    // uuid 로 회원 정보 매핑
                    for (MemberInfoDto memberInfo : memberInfoList) {
                        for (CommentDto childCommentDto : childCommentList) {
                            if (Objects.equals(childCommentDto.getMemberUuid(), memberInfo.getUuid())) {
                                childCommentDto.setNick(memberInfo.getNick());
                                childCommentDto.setProfileImgUrl(memberInfo.getProfileImgUrl());
                                childCommentDto.setIntro(memberInfo.getIntro());
                            }
                        }
                    }

                    /** 금칙어 치환 및 멘션 리스트 조회 **/
                    for (CommentDto commentDto : childCommentList) {
                        commentDto.setMentionList((new ArrayList<>())); // 멘션 리스트 생성
                        // 금칙어가 포함되어있으면 치환
                        String comment = contentsWordCheckService.contentsWordCheck(commentDto.getContents(), commentWordChk);
                        commentDto.setContents(comment); //금칙어 처리

                        // 멘션 리스트 가져오기
                        List<String> mentionTagDtoList = commentDaoSub.getCommentMentionTags(commentDto.getIdx());
                        List<MentionTagDto> mentionUserInfoList = new ArrayList<>();

                        if (!ObjectUtils.isEmpty(mentionTagDtoList)) {

                            String mentionInfoJsonString = contentsCurlService.getMentionInfoList(mentionTagDtoList);

                            JSONObject mentionInfoObject = new JSONObject(mentionInfoJsonString);
                            // curl 통신 중 에러
                            if (!mentionInfoObject.getBoolean("result")) {
                                throw new CurlException(mentionInfoObject);
                            }

                            JSONObject mentionInfoResult = (JSONObject) mentionInfoObject.get("data");
                            // 멘션 리스트
                            mentionUserInfoList = mapper.readValue(mentionInfoResult.get("list").toString(), new TypeReference<>() {
                            });
                        }

                        commentDto.setMentionList(mentionUserInfoList); // 멘션 리스트
                    }
                }

                // data set
                LinkedHashMap<String, Object> map = new LinkedHashMap<>();
                map.put("list", childCommentList);
                map.put("params", childSearchDto);
                dto.setChildCommentData(map);
            }
        }
    }

    /*****************************************************
     *  Validation
     ****************************************************/
    /**
     * 등록 유효성 검사
     *
     * @param commentDto : contentsIdx(컨텐츠 idx), memberIdx(회원 idx), contents(댓글 내용), parentIdx(부모댓글)
     */
    public void registerValidate(String token, CommentDto commentDto) {

        // 로그인 시
        if (!ObjectUtils.isEmpty(token)) {
            MemberDto loginUserInfo = super.getMemberUuidByToken(token);
            commentDto.setMemberUuid(loginUserInfo.getUuid()); // 회원 uuid setting
        }

        Long parentIdx = commentDto.getParentIdx();
        Long contentsIdx = commentDto.getContentsIdx();

        // 댓글 형식 유효성 체크
        contentsFormatValidate(commentDto);

        /** ================== 해당 구분선 아래부터 DB 조회 필요 ================ **/

        // 회원 글작성 제재 체크 :: 2 -> 글 작성/수정 제재
        String getWriteRestrainMemberInfo = restrainCurlService.getRestrainCheck(token, 2);
        JSONObject writeRestrainMemberInfoObject = new JSONObject(getWriteRestrainMemberInfo);
        if (!((boolean) writeRestrainMemberInfoObject.get("result"))) {
            throw new CurlException(writeRestrainMemberInfoObject);
        }

        // 회원 글작성 제재 체크 :: 4 -> 댓글 작성/수정 제재
        String getCommentWriteRestrainMemberInfo = restrainCurlService.getRestrainCheck(token, 4);
        JSONObject commentWriteRestrainMemberInfoObject = new JSONObject(getCommentWriteRestrainMemberInfo);
        if (!((boolean) commentWriteRestrainMemberInfoObject.get("result"))) {
            throw new CurlException(commentWriteRestrainMemberInfoObject);
        }

        /** 컨텐츠 공통 유효성 체크**/
        SearchDto searchContents = new SearchDto();
        searchContents.setLoginMemberUuid(commentDto.getMemberUuid());
        searchContents.setContentsIdx(contentsIdx);
        contentsService.commonContentsValidate(searchContents);

        // 대댓글일 경우
        if (parentIdx != null && parentIdx > 0) {
            // 조회용 dto
            CommentDto parentCommentDto = CommentDto.builder()
                    .idx(parentIdx)
                    .contentsIdx(contentsIdx)
                    .build();

            /** 정상인 부모 댓글인지 체크 **/
            normalCommentValidate(parentCommentDto);

            /** 댓글 작성자  <-> 대댓글 작성자 차단 체크 **/
            checkCommentWriterBlock(commentDto);
        }
    }

    /**
     * 댓글 수정 유효성
     *
     * @param commentDto : idx[댓글 idx], contentsIdx[컨텐츠 idx], memberUuid[로그인 회원 uuid]
     */
    private void modifyValidate(String token, CommentDto commentDto) {

        String memberUuid = commentDto.getMemberUuid(); // 로그인 회원 uuid
        Long contentsIdx = commentDto.getContentsIdx(); // 컨텐츠 idx
        Long commentIdx = commentDto.getIdx();          // 댓글 idx

        /** 수정하려는 댓글이 정상인지 체크 **/
        normalCommentValidate(commentDto);

        // 회원 uuid 빈값 체크
        if (ObjectUtils.isEmpty(memberUuid)) {
            throw new CustomException(CustomError.MEMBER_UUID_EMPTY);   // 회원 uuid 비어있습니다
        }
        // 댓글 idx 빈값 체크
        if (commentIdx == null || commentIdx < 1) {
            throw new CustomException(CustomError.COMMENT_IDX_NULL);    // 존재하지 않는 댓글입니다.
        }

        /** 댓글 형식 유효성 체크 **/
        contentsFormatValidate(commentDto);

        // 회원 글작성 제재 체크 :: 2 -> 글 작성/수정 제재
        String getWriteRestrainMemberInfo = restrainCurlService.getRestrainCheck(token, 2);
        JSONObject writeRestrainMemberInfoObject = new JSONObject(getWriteRestrainMemberInfo);
        if (!((boolean) writeRestrainMemberInfoObject.get("result"))) {
            throw new CurlException(writeRestrainMemberInfoObject);
        }

        // 회원 글작성 제재 체크 :: 4 -> 댓글 작성/수정 제재
        String getCommentWriteRestrainMemberInfo = restrainCurlService.getRestrainCheck(token, 4);
        JSONObject commentWriteRestrainMemberInfoObject = new JSONObject(getCommentWriteRestrainMemberInfo);
        if (!((boolean) commentWriteRestrainMemberInfoObject.get("result"))) {
            throw new CurlException(commentWriteRestrainMemberInfoObject);
        }

        /** 컨텐츠 공통 유효성 체크**/
        SearchDto searchContents = new SearchDto();
        searchContents.setLoginMemberUuid(memberUuid);
        searchContents.setContentsIdx(contentsIdx);
        contentsService.commonContentsValidate(searchContents);

        // 댓글 정보 조회 [contents_idx, parent_idx, member_uuid]
        CommentDto commentInfo = commentDaoSub.getCommentInfoByIdx(commentIdx);
        String writerUuid = commentInfo.getMemberUuid();

        if (!writerUuid.equals(memberUuid)) {
            throw new CustomException(CustomError.COMMENT_NOT_AUTH); // 해당 글의 작성자가 아닙니다
        }

        long parentIdx = commentInfo.getParentIdx(); // 부모 댓글 번호
        commentDto.setParentIdx(parentIdx); // 부모 댓글 번호 set

        // 대댓글일 경우
        if (parentIdx > 0) {
            // 조회용 dto
            CommentDto parentCommentDto = CommentDto.builder()
                    .idx(parentIdx)
                    .contentsIdx(contentsIdx)
                    .build();

            /** 정상인 부모 댓글인지 체크 **/
            normalCommentValidate(parentCommentDto);

            /** 댓글 작성자  <-> 대댓글 작성자 차단 체크 **/
            checkCommentWriterBlock(commentDto);
        }
    }

    /**
     * 댓글 삭제 유효성
     *
     * @param commentDto : contentsIdx, idx, memberIdx, parentIdx
     */
    private void deleteValidate(CommentDto commentDto) {

        Long parentIdx = commentDto.getParentIdx();
        Long commentIdx = commentDto.getIdx();
        Long contentsIdx = commentDto.getContentsIdx();
        String memberUuid = commentDto.getMemberUuid();

        // 회원 uuid 체크
        if (memberUuid == null || memberUuid.equals("")) {
            throw new CustomException(CustomError.MEMBER_UUID_ERROR); // 회원 UUID가 유효하지 않습니다.
        }

        // 댓글 idx 체크
        if (commentIdx == null || commentIdx < 1) {
            throw new CustomException(CustomError.COMMENT_IDX_NULL); // 존재하지 않는 댓글입니다.
        }

        // parentIdx 체크 [앞단에서 필수로 넘겨 받음]
        if (parentIdx == null) {
            throw new CustomException(CustomError.COMMENT_PARENT_IDX_EMPTY); // 부모 컨텐츠 IDX가 비었습니다.
        }

        /** ================== 해당 구분선 아래부터 DB 조회  ================ **/

        // 유효한 댓글인지 체크
        normalCommentValidate(commentDto);

        // 부모 댓글 번호 유효성
        parentIdxValidate(commentDto);

        // 해당 댓글 / 대댓글 작성자 인지 체크
        boolean checkRemoveAuth = checkRemoveAuth(commentDto);

        // 작성자 인지 체크
        if (!checkRemoveAuth) {
            throw new CustomException(CustomError.COMMENT_NOT_AUTH);
        }

        /** 컨텐츠 공통 유효성 체크**/
        SearchDto searchContents = new SearchDto();
        searchContents.setLoginMemberUuid(memberUuid);
        searchContents.setContentsIdx(contentsIdx);
        contentsService.commonContentsValidate(searchContents);
    }

    /**
     * 부모 댓글 유효성 검사
     *
     * @param commentDto
     */
    private void parentIdxValidate(CommentDto commentDto) {
        Long parentIdx = commentDto.getParentIdx();
        Long commentIdx = commentDto.getIdx();

        // 부모 댓글 조회
        Long searchParentIdx = commentDaoSub.getParentIdxByIdx(commentIdx);

        // 부모댓글 유효성
        if (!Objects.equals(parentIdx, searchParentIdx)) {
            throw new CustomException(CustomError.COMMENT_PARENT_IDX_DIFFERENT); // 부모 댓글 번호가 일치하지 않습니다.
        }
    }

    /**
     * 컨텐츠 작성자 , 댓글 작성자 차단 체크
     *
     * @param commentDto contentsIdx memberUuid
     */
    public void checkContentsWriterBlock(CommentDto commentDto) {
        // 컨텐츠 작성자 아이디 가져오기
        String contentsWriteUuid = contentsService.getMemberUuidByContentsIdx(commentDto.getContentsIdx());

        if (contentsWriteUuid != null && !contentsWriteUuid.equals("")) {
            boolean chekBlock = super.bChkBlock(contentsWriteUuid, commentDto.getMemberUuid());
            // 차단 내역이 있으면
            if (chekBlock) {
                // 댓글을 등록할 수 없습니다.
                throw new CustomException(CustomError.COMMENT_ERROR);
            }
        }
    }

    /**
     * 댓글 작성자, 대댓글 작성자 차단 확인
     *
     * @param commentDto parentIdx, memberIdx
     */
    public void checkCommentWriterBlock(CommentDto commentDto) {
        // 댓글 작성자 아이디 가져오기
        String commentWriteUuid = getMemberUuidByIdx(commentDto.getParentIdx());

        if (!ObjectUtils.isEmpty(commentWriteUuid)) {
            boolean chekBlock = super.bChkBlock(commentWriteUuid, commentDto.getMemberUuid());
            // 차단 내역이 있으면
            if (chekBlock) {
                // 답글을 등록할 수 없습니다.
                throw new CustomException(CustomError.COMMENT_REPLY_ERROR);
            }
        }
    }

    /**
     * 댓글 내용 형식 유효성
     *
     * @param commentDto
     */
    private void contentsFormatValidate(CommentDto commentDto) {
        // 내용이 빈 경우
        if (commentDto.getContents() == null || commentDto.getContents().equals("")) {
            throw new CustomException(CustomError.COMMENT_CONTENTS_EMPTY); // 댓글을 입력해주세요
        }

        // 댓글 최대 길이 초과시
        String comment = commentDto.getContents();

        BreakIterator it = BreakIterator.getCharacterInstance();
        it.setText(comment);
        int commentLength = 0;

        while (it.next() != BreakIterator.DONE) {
            commentLength++;
        }

        if (commentLength > commentTextMax) {
            throw new CustomException(CustomError.COMMENT_TEXT_LIMIT_ERROR); // 최대 입력 가능 글자수를 초과하였습니다.
        }
    }

    /**
     * 정상 댓글 조회
     *
     * @param commentDto : idx, contentsIdx, memberIdx
     */
    public void normalCommentValidate(CommentDto commentDto) {

        CommentDto searchCommentDto = CommentDto.builder()
                .idx(commentDto.getIdx())
                .contentsIdx(commentDto.getContentsIdx())
                .state(1).build();

        int commentCnt = commentDaoSub.getCommentCnt(searchCommentDto);

        if (commentCnt < 1) {
            throw new CustomException(CustomError.COMMENT_IDX_ERROR); // 존재하지 않는 댓글입니다.
        }
    }

    /**
     * 댓글 신고했는지 검증
     *
     * @param searchDto : loginMemberUuid , commentIdx
     */
    public void reportCommentValidate(SearchDto searchDto) {
        int reportCnt = commentDaoSub.getCommentReportCnt(searchDto);

        if (reportCnt > 0) {
            throw new CustomException(CustomError.COMMENT_REPORT_ERROR); // 신고한 댓글입니다.
        }
    }


}
