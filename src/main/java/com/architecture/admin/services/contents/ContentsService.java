package com.architecture.admin.services.contents;

import com.architecture.admin.libraries.PaginationLibray;
import com.architecture.admin.libraries.S3Library;
import com.architecture.admin.libraries.exception.CurlException;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.comment.CommentDao;
import com.architecture.admin.models.dao.contents.ContentsDao;
import com.architecture.admin.models.dao.contents.ContentsLikeDao;
import com.architecture.admin.models.dao.contents.ContentsSaveDao;
import com.architecture.admin.models.daosub.comment.CommentDaoSub;
import com.architecture.admin.models.daosub.contents.ContentsDaoSub;
import com.architecture.admin.models.daosub.follow.FollowDaoSub;
import com.architecture.admin.models.daosub.member.MemberInfoDaoSub;
import com.architecture.admin.models.daosub.tag.HashTagDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.comment.CommentDto;
import com.architecture.admin.models.dto.contents.*;
import com.architecture.admin.models.dto.follow.FollowDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;
import com.architecture.admin.models.dto.noti.NotiDto;
import com.architecture.admin.models.dto.push.PushDto;
import com.architecture.admin.models.dto.tag.HashTagDto;
import com.architecture.admin.models.dto.tag.MentionTagDto;
import com.architecture.admin.models.dto.wordcheck.ContentsWordCheckDto;
import com.architecture.admin.services.BaseService;
import com.architecture.admin.services.block.BlockMemberService;
import com.architecture.admin.services.comment.CommentCurlService;
import com.architecture.admin.services.noti.ContentsNotiService;
import com.architecture.admin.services.push.ContentsPushService;
import com.architecture.admin.services.restrain.RestrainCurlService;
import com.architecture.admin.services.tag.HashTagService;
import com.architecture.admin.services.tag.MentionTagService;
import com.architecture.admin.services.wordcheck.ContentsWordCheckService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.text.BreakIterator;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/*****************************************************
 * 콘텐츠 모델러
 ****************************************************/
@Service
@RequiredArgsConstructor
public class ContentsService extends BaseService {

    private final ContentsDao contentsDao;                      // 컨텐츠
    private final ContentsDaoSub contentsDaoSub;                // 컨텐츠
    private final ContentsLikeDao contentsLikeDao;              // 컨텐츠 좋아요
    private final CommentDao commentDao;                        // 댓글
    private final MemberInfoDaoSub memberInfoDaoSub;            // 회원 정보
    private final ContentsSaveDao contentsSaveDao;              // 컨텐츠 저장
    private final CommentDaoSub commentDaoSub;                  // 댓글
    private final HashTagDaoSub hashTagDaoSub;                  // 해시태그
    private final FollowDaoSub followDaoSub;                    // 팔로우
    private final S3Library s3Library;
    private final BlockMemberService blockMemberService;        // 회원 차단
    private final HashTagService hashTagService;                // 해시태그
    private final MentionTagService mentionTagService;          // 회원 멘션
    private final ContentsWordCheckService contentsWordcheckService;    // 컨텐츠 금칙어
    private final ContentsNotiService contentsNotiService;      // 알림
    private final ContentsPushService contentsPushService;      // 푸시
    private final ContentsCurlService contentsCurlService;      // 컨텐츠 curl 통신
    private final CommentCurlService commentCurlService;        // 댓글 curl 통신
    private final RestrainCurlService restrainCurlService;      // 제재 curl

    private final int ONLY_FOLLOW_VIEW = 2;
    @Value("${contents.sns.type}")
    private int sns;  // sns 타입 (menu : 1)
    @Value("${contents.text.max}")
    private int textMax;  // 최대 입력 가능 글자수
    @Value("${contents.image.max}")
    private int imgMax;  // 최대 등록 가능 이미지 수
    @Value("${word.check.contents.type}")
    private int contentsWordChk;  // 금칙어 컨텐츠 타입
    @Value("${use.contents.register}")
    private boolean useContentsRegister;     // 소셜 콘텐츠 등록 true/false
    @Value("${use.contents.modify}")
    private boolean useContentsModify;       // 소셜 콘텐츠 수정 true/false
    @Value("${use.push.contents.regist}")
    private boolean useContentsPush;         // 푸시 알림 true/false
    @Value("${use.noti.contents.regist}")
    private boolean useContentsNoti;        // 알림 true/false
    @Value("${following.contents.time}")
    private Integer followingContentsTime;  // 팔로잉 게시물 주기
    @Value("${cloud.aws.s3.img.url}")
    private String imgDomain;

    /*****************************************************
     *  Modules
     ****************************************************/

    /**
     * 해당 컨텐츠 작성자 uuid 가져오기
     */

    public String getMemberUuidByContentsIdx(Long contentsIdx) {
        // contentsIdx validation
        contentsIdxValidate(contentsIdx);
        // return uuid
        return contentsDaoSub.getMemberUuidByIdx(contentsIdx);
    }

    /**
     * 컨텐츠 상세 [단일]
     *
     * @param searchDto loginMemberIdx [로그인 한 회원 idx], contentsIdx(컨텐츠 idx), imgLimit [이미지 개수]
     * @return
     */
    public ContentsDto getContentsDetail(String token, SearchDto searchDto) {
        // 로그인 시
        if (!ObjectUtils.isEmpty(token)) {
            MemberDto loginUserInfo = super.getMemberUuidByToken(token);
            searchDto.setLoginMemberUuid(loginUserInfo.getUuid()); // 회원 uuid setting
        }

        /** 유효성 검사 **/
        contentsDetailValidate(searchDto);

        // 로그인 중
        if (searchDto.getLoginMemberUuid() != null) {
            String writerUuid = searchDto.getMemberUuid();           // 작성자 uuid
            String loginMemberUuid = searchDto.getLoginMemberUuid(); // 로그인 uuid
            // 작성자가 본인이면 -> 보관한 것도 조회 하기 위해서
            if (writerUuid.equals(loginMemberUuid)) {
                searchDto.setSearchType("my");
            }
        }

        ContentsDto contentsDto = contentsDaoSub.getContentsDetail(searchDto);

        if (contentsDto != null) {
            /** 등록일 초,분,시간,일 단위로 변환 **/
            setRegDateToTime(contentsDto);

            /** 컨텐츠 상세 setting **/
            setContentsDetail(contentsDto, searchDto);

            /** 금칙어 치환 **/
            List<ContentsWordCheckDto> contentsBadWordList = contentsWordcheckService.getList(contentsWordChk); // 콘텐츠 금칙어 리스트 조회
            convertBadWord(contentsDto, contentsBadWordList);
        }

        return contentsDto;
    }

    /**
     * 내가 작성한 컨텐츠 상세 [일상글]
     *
     * @param searchDto
     * @return
     */
    public ContentsDto getMyNormalContentsDetail(String token, SearchDto searchDto) {
        // 로그인 회원 uuid curl 조회
        MemberDto loginUserInfo = super.getMemberUuidByToken(token);

        // 로그인 회원 uuid setting
        searchDto.setLoginMemberUuid(loginUserInfo.getUuid());
        /** 유효성 검사 **/
        myNormalContentsDetailValidate(searchDto);

        searchDto.setSearchType("normal");

        // 내가 작성한컨텐츠 상세 조회
        ContentsDto contentsDto = contentsDaoSub.getMyContentsDetail(searchDto);

        if (contentsDto != null) {
            /** 등록일 초,분,시간,일 단위로 변환 **/
            setRegDateToTime(contentsDto);

            /** 컨텐츠 상세 setting **/
            setContentsDetail(contentsDto, searchDto);

            /** 금칙어 치환 **/
            List<ContentsWordCheckDto> contentsBadWordList = contentsWordcheckService.getList(contentsWordChk); // 콘텐츠 금칙어 리스트 조회
            convertBadWord(contentsDto, contentsBadWordList);
        }

        return contentsDto;
    }

    /**
     * 내가 작성한 컨텐츠 상세 [보관글]
     *
     * @param searchDto
     * @return
     */
    public ContentsDto getMyKeepContentsDetail(String token, SearchDto searchDto) {

        MemberDto loginUserInfo = super.getMemberUuidByToken(token);

        // 로그인 회원 uuid set
        searchDto.setLoginMemberUuid(loginUserInfo.getUuid());

        /** 유효성 검사 **/
        myKeepContentsDetailValidate(searchDto);

        searchDto.setSearchType("keep");

        // 내가 작성한 컨텐츠 상세 조회
        ContentsDto contentsDto = contentsDaoSub.getMyContentsDetail(searchDto);

        if (contentsDto != null) {
            /** 등록일 초,분,시간,일 단위로 변환 **/
            setRegDateToTime(contentsDto);

            /** 컨텐츠 상세 setting **/
            setContentsDetail(contentsDto, searchDto);

            /** 금칙어 치환 **/
            List<ContentsWordCheckDto> contentsBadWordList = contentsWordcheckService.getList(contentsWordChk); // 콘텐츠 금칙어 리스트 조회
            convertBadWord(contentsDto, contentsBadWordList);
        }

        return contentsDto;
    }

    /**
     * 내가 작성한 컨텐츠 상세 리스트 [일상글]
     *
     * @param token
     * @param searchDto
     * @return
     */
    public List<ContentsDto> getWrittenByMeContentsList(String token, SearchDto searchDto) {

        // 로그인 회원 uuid curl 조회
        MemberDto loginUserInfo = super.getMemberUuidByToken(token);

        // 로그인 회원 uuid set
        searchDto.setLoginMemberUuid(loginUserInfo.getUuid());
        // 컨텐츠 공통 유효성 검사
        commonImgLimitValidate(searchDto);
        List<ContentsDto> contentsDtoList = new ArrayList<>();

        // 내가 작성한 컨텐츠 카운트
        int totalCnt = contentsDaoSub.getTotalWrittenByMeCnt(searchDto);

        if (totalCnt > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
            searchDto.setPagination(pagination);

            /** 내가 작성한 컨텐츠 리스트 조회 **/
            contentsDtoList = contentsDaoSub.getWrittenByMeList(searchDto);

            /** 등록일 초,분,시간,일 단위로 변환 **/
            setListRegDateToTime(contentsDtoList);

            /** 컨텐츠 상세 리스트 setting [해시 태그, 멘션, 이미지, 이미지 회원 태그, 좋아요 많은 댓글] **/
            setContentsDetailList(contentsDtoList, searchDto, false, true);
            /** 금칙어 치환 **/
            convertBadWordInList(contentsDtoList);
        }

        return contentsDtoList;
    }

    /**
     * 내가 저장한 게시물 상세 리스트
     *
     * @param token
     * @param searchDto
     * @return
     */
    public List<ContentsDto> getMySaveList(String token, SearchDto searchDto) {

        // 로그인 회원 uuid curl 조회
        MemberDto loginUserInfo = super.getMemberUuidByToken(token);
        // 로그인 회원 uuid set
        searchDto.setLoginMemberUuid(loginUserInfo.getUuid());

        // 컨텐츠 상세 공통 유효성
        commonImgLimitValidate(searchDto);
        List<ContentsDto> contentsDtoList = new ArrayList<>();

        // 저장한 컨텐츠 카운트
        int totalCnt = contentsDaoSub.getTotalSaveCnt(searchDto);

        if (totalCnt > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
            searchDto.setPagination(pagination);

            /** 저장한 컨텐츠 리스트 조회 **/
            contentsDtoList = contentsDaoSub.getSaveList(searchDto);

            /** 등록일 초,분,시간,일 단위로 변환 **/
            setListRegDateToTime(contentsDtoList);

            /** 컨텐츠 상세 리스트 setting [해시 태그, 멘션, 이미지, 이미지 회원 태그, 좋아요 많은 댓글] **/
            setContentsDetailList(contentsDtoList, searchDto, true, true);

            /** 금칙어 치환 **/
            convertBadWordInList(contentsDtoList);
        }

        // 닉네임이 비어있으면 제외
        List<ContentsDto> filteredList = new ArrayList<>();
        for (ContentsDto ContentsDto : contentsDtoList) {
            if (ContentsDto.getMemberInfo() != null) {
                if (ContentsDto.getMemberInfo().getNick() != null && !ContentsDto.getMemberInfo().getNick().isEmpty()) {
                    filteredList.add(ContentsDto);
                }
            }
        }

        return filteredList;
    }

    /**
     * 내가 좋아요한 게시물 상세 리스트
     *
     * @param token
     * @param searchDto
     * @return
     */
    public List<ContentsDto> getMyLikeList(String token, SearchDto searchDto) {
        // 로그인 회원 uuid curl 조회
        MemberDto loginUserInfo = super.getMemberUuidByToken(token);

        // 로그인 회원 uuid set
        searchDto.setLoginMemberUuid(loginUserInfo.getUuid());

        // 컨텐츠 상세 공통 유효성
        commonImgLimitValidate(searchDto);

        List<ContentsDto> contentsDtoList = new ArrayList<>();

        // 좋아요한 컨텐츠 카운트
        int totalCnt = contentsDaoSub.getTotalLikeCnt(searchDto);

        if (totalCnt > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
            searchDto.setPagination(pagination);

            /** 좋아요 한 컨텐츠 리스트 조회 **/
            contentsDtoList = contentsDaoSub.getLikeList(searchDto);

            /** 등록일 초,분,시간,일 단위로 변환 **/
            setListRegDateToTime(contentsDtoList);

            /** 컨텐츠 상세 리스트 setting [해시 태그, 멘션, 이미지, 이미지 회원 태그, 좋아요 많은 댓글] **/
            setContentsDetailList(contentsDtoList, searchDto, true, true);

            /** 금칙어 치환 **/
            convertBadWordInList(contentsDtoList);
        }

        // 닉네임이 비어있으면 제외
        List<ContentsDto> filteredList = new ArrayList<>();
        for (ContentsDto ContentsDto : contentsDtoList) {
            if (ContentsDto.getMemberInfo() != null) {
                if (ContentsDto.getMemberInfo().getNick() != null && !ContentsDto.getMemberInfo().getNick().isEmpty()) {
                    filteredList.add(ContentsDto);
                }
            }
        }

        return filteredList;
    }


    /**
     * 내가 태그된 게시물 상세 리스트
     *
     * @param token
     * @param searchDto
     * @return
     */
    public List<ContentsDto> getMyTagList(String token, SearchDto searchDto) {

        // 로그인 회원 uuid curl 조회
        MemberDto loginUserInfo = super.getMemberUuidByToken(token);
        // 로그인 회원 uuid set
        searchDto.setLoginMemberUuid(loginUserInfo.getUuid());

        // 컨텐츠 상세 공통 유효성
        commonImgLimitValidate(searchDto);

        List<ContentsDto> contentsDtoList = new ArrayList<>();

        // 내가 태그된 컨텐츠 카운트
        int totalCnt = contentsDaoSub.getTotalTagCnt(searchDto);

        if (totalCnt > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
            searchDto.setPagination(pagination);

            /** 태그된 컨텐츠 리스트 조회 **/
            contentsDtoList = contentsDaoSub.getTagList(searchDto);

            /** 등록일 초,분,시간,일 단위로 변환 **/
            setListRegDateToTime(contentsDtoList);

            /** 컨텐츠 상세 리스트 setting [해시 태그, 멘션, 이미지, 이미지 회원 태그, 좋아요 많은 댓글] **/
            setContentsDetailList(contentsDtoList, searchDto, true, true);

            /** 금칙어 치환 **/
            convertBadWordInList(contentsDtoList);
        }

        // 닉네임이 비어있으면 제외
        List<ContentsDto> filteredList = new ArrayList<>();
        for (ContentsDto ContentsDto : contentsDtoList) {
            if (ContentsDto.getMemberInfo() != null) {
                if (ContentsDto.getMemberInfo().getNick() != null && !ContentsDto.getMemberInfo().getNick().isEmpty()) {
                    filteredList.add(ContentsDto);
                }
            }
        }

        return filteredList;
    }

    /**
     * 최신 컨텐츠 상세 리스트
     *
     * @param token
     * @param searchDto
     * @return
     */
    public List<ContentsDto> getRecentList(String token, SearchDto searchDto) {

        // 로그인 시
        if (!ObjectUtils.isEmpty(token)) {
            MemberDto loginUserInfo = super.getMemberUuidByToken(token);
            searchDto.setLoginMemberUuid(loginUserInfo.getUuid()); // 회원 uuid setting
        }

        // 컨텐츠 상세 공통 유효성
        commonImgLimitValidate(searchDto);

        List<ContentsDto> contentsDtoList = new ArrayList<>();
        // 최신 컨텐츠 카운트
        int totalCnt = contentsDaoSub.getTotalRecentCnt(searchDto);

        if (totalCnt > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
            searchDto.setPagination(pagination);

            /** 최신 컨텐츠 리스트 조회 **/
            contentsDtoList = contentsDaoSub.getRecentList(searchDto);

            /** 등록일 초,분,시간,일 단위로 변환 **/
            setListRegDateToTime(contentsDtoList);

            /** 컨텐츠 상세 리스트 setting [해시 태그, 멘션, 이미지, 이미지 회원 태그, 좋아요 많은 댓글] **/
            setContentsDetailList(contentsDtoList, searchDto, true, true);

            /** 금칙어 치환 **/
            convertBadWordInList(contentsDtoList);
        }

        // 닉네임이 비어있으면 제외
        List<ContentsDto> filteredList = new ArrayList<>();
        for (ContentsDto ContentsDto : contentsDtoList) {
            if (ContentsDto.getMemberInfo() != null) {
                if (ContentsDto.getMemberInfo().getNick() != null && !ContentsDto.getMemberInfo().getNick().isEmpty()) {
                    filteredList.add(ContentsDto);
                }
            }
        }

        return filteredList;

    }

    /**
     * 회원 태그된 게시물 상세 리스트
     *
     * @param token
     * @param searchDto : memberUuid [태그된 회원 uuid], page [현재 페이지], imgLimit [이미지 개수]
     * @return
     */
    public List<ContentsDto> getMemberTagList(String token, SearchDto searchDto) {

        // 로그인 시
        if (!ObjectUtils.isEmpty(token)) {
            MemberDto loginUserInfo = super.getMemberUuidByToken(token);
            searchDto.setLoginMemberUuid(loginUserInfo.getUuid()); // 회원 uuid setting
            // 차단 여부 유효성 검사
            blockValidate(searchDto);
        }

        // 해당 회원 유효한지 체크
        Boolean isExist = super.getCheckMemberByUuid(searchDto.getMemberUuid());

        if (isExist == false) {
            // 회원 UUID가 유효하지 않습니다.
            throw new CustomException(CustomError.MEMBER_UUID_ERROR);
        }

        // 컨텐츠 상세 공통 유효성
        commonImgLimitValidate(searchDto);

        List<ContentsDto> contentsDtoList = new ArrayList<>();
        // 태그된 컨텐츠 카운트
        int totalCnt = contentsDaoSub.getTotalTagCnt(searchDto);

        if (totalCnt > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
            searchDto.setPagination(pagination);

            /** 태그된 컨텐츠 리스트 조회 **/
            contentsDtoList = contentsDaoSub.getTagList(searchDto);

            /** 등록일 초,분,시간,일 단위로 변환 **/
            setListRegDateToTime(contentsDtoList);

            /** 컨텐츠 상세 리스트 setting [해시 태그, 멘션, 이미지, 이미지 회원 태그, 좋아요 많은 댓글] **/
            setContentsDetailList(contentsDtoList, searchDto, true, true);

            /** 금칙어 치환 **/
            convertBadWordInList(contentsDtoList);
        }

        // 닉네임이 비어있으면 제외
        List<ContentsDto> filteredList = new ArrayList<>();
        for (ContentsDto ContentsDto : contentsDtoList) {
            if (ContentsDto.getMemberInfo() != null) {
                if (ContentsDto.getMemberInfo().getNick() != null && !ContentsDto.getMemberInfo().getNick().isEmpty()) {
                    filteredList.add(ContentsDto);
                }
            }
        }

        return filteredList;
    }

    /**
     * 인기 게시글 상세 리스트
     *
     * @param token
     * @param searchDto
     * @return
     */
    public List<ContentsDto> getWeekPopularList(String token, SearchDto searchDto) {
        // 로그인 시
        if (!ObjectUtils.isEmpty(token)) {
            MemberDto loginUserInfo = super.getMemberUuidByToken(token);
            searchDto.setLoginMemberUuid(loginUserInfo.getUuid()); // 회원 uuid setting
        }

        // 컨텐츠 공통 유효성 검사
        commonImgLimitValidate(searchDto);

        List<ContentsDto> contentsDtoList = new ArrayList<>();

        // 인기 게시글 카운트
        int totalCnt = contentsDaoSub.getTotalWeekPopularCnt(searchDto);

        if (totalCnt > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
            searchDto.setPagination(pagination);

            /** 컨텐츠 리스트 조회 **/
            contentsDtoList = contentsDaoSub.getWeekPopularList(searchDto);

            /** 등록일 초,분,시간,일 단위로 변환 **/
            setListRegDateToTime(contentsDtoList);

            /** 컨텐츠 상세 리스트 setting [해시 태그, 멘션, 이미지, 이미지 회원 태그, 좋아요 많은 댓글] **/
            setContentsDetailList(contentsDtoList, searchDto, true, true);

            /** 금칙어 치환 **/
            convertBadWordInList(contentsDtoList);
        }

        // 닉네임이 비어있으면 제외
        List<ContentsDto> filteredList = new ArrayList<>();
        for (ContentsDto ContentsDto : contentsDtoList) {
            if (ContentsDto.getMemberInfo() != null) {
                if (ContentsDto.getMemberInfo().getNick() != null && !ContentsDto.getMemberInfo().getNick().isEmpty()) {
                    filteredList.add(ContentsDto);
                }
            }
        }

        return filteredList;
    }

    /**
     * 급상승 인기 게시글 상세 리스트
     *
     * @param token
     * @param searchDto
     * @return
     */
    public List<ContentsDto> getHourPopularList(String token, SearchDto searchDto) {

        // 로그인 시
        if (!ObjectUtils.isEmpty(token)) {
            MemberDto loginUserInfo = super.getMemberUuidByToken(token);
            searchDto.setLoginMemberUuid(loginUserInfo.getUuid()); // 회원 uuid setting
        }

        // 컨텐츠 상세 공통 유효성
        commonImgLimitValidate(searchDto);

        List<ContentsDto> contentsDtoList = new ArrayList<>();

        // 급상승 인기 게시글 카운트
        int totalCnt = contentsDaoSub.getTotalHourPopularCnt(searchDto);

        if (totalCnt > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
            searchDto.setPagination(pagination);

            /** 컨텐츠 리스트 조회 **/
            contentsDtoList = contentsDaoSub.getHourPopularList(searchDto);

            /** 등록일 초,분,시간,일 단위로 변환 **/
            setListRegDateToTime(contentsDtoList);

            /** 컨텐츠 상세 리스트 setting [해시 태그, 멘션, 이미지, 이미지 회원 태그, 좋아요 많은 댓글] **/
            setContentsDetailList(contentsDtoList, searchDto, true, true);

            /** 금칙어 치환 **/
            convertBadWordInList(contentsDtoList);
        }

        // 닉네임이 비어있으면 제외
        List<ContentsDto> filteredList = new ArrayList<>();
        for (ContentsDto ContentsDto : contentsDtoList) {
            if (ContentsDto.getMemberInfo() != null) {
                if (ContentsDto.getMemberInfo().getNick() != null && !ContentsDto.getMemberInfo().getNick().isEmpty()) {
                    filteredList.add(ContentsDto);
                }
            }
        }

        return filteredList;
    }

    /**
     * 회원 컨텐츠 상세 리스트 조회
     *
     * @param searchDto : memberUuid, page, limit
     * @return
     */
    public List<ContentsDto> getContentsDetailList(String token, SearchDto searchDto) {

        // 로그인 시
        if (!ObjectUtils.isEmpty(token)) {
            MemberDto loginUserInfo = super.getMemberUuidByToken(token);
            searchDto.setLoginMemberUuid(loginUserInfo.getUuid()); // 회원 uuid setting
        }

        // 해당 회원 유효한지 체크
        Boolean isExist = super.getCheckMemberByUuid(searchDto.getMemberUuid());

        if (isExist == false) {
            // 회원 UUID가 유효하지 않습니다.
            throw new CustomException(CustomError.MEMBER_UUID_ERROR);
        }

        /** 유효성 검사 **/
        detailListValidate(searchDto);

        List<ContentsDto> contentsDtoList = new ArrayList<>();

        // 컨텐츠 상세 카운트
        int totalCnt = contentsDaoSub.getTotalContentsDetailCnt(searchDto);

        if (totalCnt > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
            searchDto.setPagination(pagination);

            /** 컨텐츠 리스트 조회 **/
            contentsDtoList = contentsDaoSub.getContentsDetailList(searchDto);
            /** 등록일 초,분,시간,일 단위로 변환 **/
            setListRegDateToTime(contentsDtoList);
            /** 컨텐츠 상세 리스트 setting [해시 태그, 멘션, 이미지, 이미지 회원 태그, 좋아요 많은 댓글] **/
            setContentsDetailList(contentsDtoList, searchDto, false, true);
            /** 금칙어 치환 **/
            convertBadWordInList(contentsDtoList);

        }

        // 닉네임이 비어있으면 제외
//        List<ContentsDto> filteredList = new ArrayList<>();
//        for (ContentsDto ContentsDto : contentsDtoList) {
//            if (ContentsDto.getMemberInfo() != null) {
//                if (ContentsDto.getMemberInfo().getNick() != null && !ContentsDto.getMemberInfo().getNick().isEmpty()) {
//                    filteredList.add(ContentsDto);
//                }
//            }
//        }

        return contentsDtoList;
    }

    /**
     * 해시 태그 게시물 상세 리스트1
     *
     * @param token
     * @param searchDto
     * @return
     */
    public List<ContentsDto> getHashTagList(String token, SearchDto searchDto) {

        // 로그인 시
        if (!ObjectUtils.isEmpty(token)) {
            MemberDto loginUserInfo = super.getMemberUuidByToken(token);
            searchDto.setLoginMemberUuid(loginUserInfo.getUuid()); // 회원 uuid setting
        }
        // 해시 태그
        String hashTag = searchDto.getSearchWord();

        if (hashTag == null || hashTag.isEmpty()) {
            throw new CustomException(CustomError.CONTENTS_HASH_TAG_EMPTY); // 해시태그를 입력해주세요.
        }

        // 유효한 해시 태그인지 조회
        int hashTagCnt = hashTagDaoSub.getHashTagCntByHashTag(hashTag);

        if (hashTagCnt < 1) {
            throw new CustomException(CustomError.CONTENTS_HASH_TAG_ERROR); // 존재하지않는 해시태그입니다.
        }

        // 컨텐츠 상세 공통 유효성
        commonImgLimitValidate(searchDto);

        List<ContentsDto> contentsDtoList = new ArrayList<>();

        // 태그된 컨텐츠 카운트
        int totalCnt = contentsDaoSub.getTotalHashTagCnt(searchDto);

        if (totalCnt > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(totalCnt, searchDto);
            searchDto.setPagination(pagination);

            /** 해시태그 컨텐츠 리스트 조회 **/
            contentsDtoList = contentsDaoSub.getHashTagContentsList(searchDto);

            /** 등록일 초,분,시간,일 단위로 변환 **/
            setListRegDateToTime(contentsDtoList);

            /** 컨텐츠 상세 리스트 setting [해시 태그, 멘션, 이미지, 이미지 회원 태그, 좋아요 많은 댓글] **/
            setContentsDetailList(contentsDtoList, searchDto, true, true);

            /** 금칙어 치환 **/
            convertBadWordInList(contentsDtoList);
        }

        // 닉네임이 비어있으면 제외
        List<ContentsDto> filteredList = new ArrayList<>();
        for (ContentsDto ContentsDto : contentsDtoList) {
            if (ContentsDto.getMemberInfo() != null) {
                if (ContentsDto.getMemberInfo().getNick() != null && !ContentsDto.getMemberInfo().getNick().isEmpty()) {
                    filteredList.add(ContentsDto);
                }
            }
        }

        return filteredList;
    }


    /**
     * 콘텐츠 상세 이미지
     *
     * @param token
     * @param searchDto : contentsIdx [컨텐츠 idx], imgLimit [이미지 개수], imgOffSet [이미지 시작 위치]
     * @return
     */
    @SneakyThrows
    public List<ContentsImgDto> getContentsImgList(String token, SearchDto searchDto) {

        // 로그인 시
        if (!ObjectUtils.isEmpty(token)) {
            MemberDto loginUserInfo = super.getMemberUuidByToken(token);
            searchDto.setLoginMemberUuid(loginUserInfo.getUuid()); // 회원 uuid setting
        }

        // 컨텐츠 이미지 유효성 검사
        commonImgLimitValidate(searchDto);
        // 컨텐츠 공통 유효성 검사
        commonContentsValidate(searchDto);

        // 이미지 조회
        searchDto.setImgDomain(imgDomain); // 이미지 도메인 set
        List<ContentsImgDto> imgList = contentsDaoSub.getContentsImgList(searchDto);

        // 이미지가 존재
        if (!ObjectUtils.isEmpty(imgList)) {
            // 이미지 idx 리스트 생성
            List<Long> imgIdxList = new ArrayList<>();

            for (ContentsImgDto contentsImgDto : imgList) {
                imgIdxList.add(contentsImgDto.getIdx()); // 이미지 idx 리스트에 추가
            }

            // 이미지 해시 태그 리스트 조회
            searchDto.setIdxList(imgIdxList);
            /** 이미지 내 회원 태그 리스트 조회 **/
            List<ContentsImgMemberTagDto> imgMemberTagList = contentsDaoSub.getImgMemberTagList(searchDto);
            searchDto.setIdxList(null);

            /** 이미지 내 회원 태그 리스트 SET **/
            if (!ObjectUtils.isEmpty(imgMemberTagList)) {              // 이미지 내 회원 태그 존재
                ObjectMapper mapper = new ObjectMapper();
                List<String> imgTagMemberUuidList = new ArrayList<>(); // 이미지 내 태그된 회원 uuid 리스트

                for (ContentsImgMemberTagDto memberTagDto : imgMemberTagList) {
                    imgTagMemberUuidList.add(memberTagDto.getMemberUuid());
                }

                List<String> imgTagOutMemberUuidList = new ArrayList<>(imgTagMemberUuidList); // 유효하지 않은 회원 삭제할 리스트

                // 이미지 태그 내 회원 정보 curl 통신
                String memberInfoJsonString = contentsCurlService.getImgMemberTagInfoList(imgTagMemberUuidList);

                JSONObject memberInfoObject = new JSONObject(memberInfoJsonString);

                if (!(boolean) memberInfoObject.get("result")) {
                    throw new CurlException(memberInfoObject);
                }
                JSONObject mentionInfoResult = (JSONObject) memberInfoObject.get("data");
                // 이미지 태그 내 회원 리스트
                List<MemberDto> memberInfoList = mapper.readValue(mentionInfoResult.get("list").toString(), new TypeReference<>() {
                });

                if (!ObjectUtils.isEmpty(memberInfoList)) {
                    for (MemberDto memberDto : memberInfoList) {
                        String uuid = memberDto.getUuid();
                        imgTagOutMemberUuidList.removeIf(memberUuid -> memberUuid.equals(uuid)); // 정상 회원 삭제
                    }

                    // 탈퇴한 회원 존재
                    if (!ObjectUtils.isEmpty(imgTagOutMemberUuidList)) {
                        for (String outUuid : imgTagOutMemberUuidList) {
                            imgMemberTagList.removeIf(memberTagDto -> memberTagDto.getMemberUuid().equals(outUuid)); // 탈퇴한 회원 제거(원본)
                        }
                    }
                }
                // 이미지 내 태그된 정상 회원 존재
                if (!ObjectUtils.isEmpty(imgMemberTagList)) {
                    for (ContentsImgMemberTagDto memberTagDto : imgMemberTagList) {
                        String memberUuid = memberTagDto.getMemberUuid();

                        memberInfoList.forEach(memberDto -> {
                            if (memberUuid.equals(memberDto.getUuid())) {
                                memberTagDto.setNick(memberDto.getNick());                   // 태그된 회원 닉네임
                                memberTagDto.setProfileImgUrl(memberDto.getProfileImgUrl()); // 태그된 회원 프로필
                                memberTagDto.setIntro(memberTagDto.getIntro());              // 태그된 회원 소개글
                            }
                        });
                    }

                    for (ContentsImgDto imgDto : imgList) {
                        Long imgIdx = imgDto.getIdx();
                        imgDto.setImgMemberTagList(new ArrayList<>()); // 이미지 회원 태그 리스트 생성

                        // 하나의 컨텐츠 이미지에 이미지 회원 태그 리스트 SET
                        for (int i = 0; i < imgMemberTagList.size(); i++) {
                            ContentsImgMemberTagDto memberTagDto = imgMemberTagList.get(i);

                            // 컨텐츠 idx가 같으면 추가 후 해당 인덱스 삭제
                            if (imgIdx.equals(memberTagDto.getImgIdx())) {
                                imgDto.getImgMemberTagList().add(memberTagDto);
                                imgMemberTagList.remove(i--);
                            }
                        } // end of for
                    } // end of for

                }
            } else { // 이미지 내 회원 태그 리스트 없을 경우
                // 빈 리스트 삽입
                for (ContentsImgDto contentsImgDto : imgList) {
                    imgMemberTagList = new ArrayList<>();
                    contentsImgDto.setImgMemberTagList(imgMemberTagList);
                }
            }
        }

        return imgList;
    }

    /**
     * 팔로잉 콘텐츠 상세 리스트
     *
     * @param token
     * @param searchDto : memberUuid
     * @return
     */
    public List<ContentsDto> getFollowContentsList(String token, SearchDto searchDto) {

        MemberDto loginUserInfo = super.getMemberUuidByToken(token);
        // 회원 로그인 uuid set
        searchDto.setLoginMemberUuid(loginUserInfo.getUuid());

        // 30일 전 세팅
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -followingContentsTime);
        String date = formatDatetime.format(calendar.getTime());
        searchDto.setDate(date);

        List<ContentsDto> contentsDtoList = new ArrayList<>();

        // 목록 전체 count
        int iTotalCount = contentsDaoSub.iGetTotalFollowContentsCount(searchDto);

        if (iTotalCount > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(iTotalCount, searchDto);
            searchDto.setPagination(pagination);

            // list
            contentsDtoList = contentsDaoSub.lGetFollowContentsList(searchDto);

            /** 등록일 초,분,시간,일 단위로 변환 **/
            setListRegDateToTime(contentsDtoList);

            /** 컨텐츠 상세 리스트 setting [회원 정보, 해시 태그, 멘션, 이미지, 이미지 회원 태그] **/
            setContentsDetailList(contentsDtoList, searchDto, true, false);

            /** 금칙어 치환 **/
            convertBadWordInList(contentsDtoList);
        }

        // 닉네임이 비어있으면 제외
        List<ContentsDto> filteredList = new ArrayList<>();
        for (ContentsDto ContentsDto : contentsDtoList) {
            if (ContentsDto.getMemberInfo() != null) {
                if (ContentsDto.getMemberInfo().getNick() != null && !ContentsDto.getMemberInfo().getNick().isEmpty()) {
                    filteredList.add(ContentsDto);
                }
            }
        }

        return filteredList;
    }

    /**
     * 내가 작성한 콘텐츠 리스트
     *
     * @param token
     * @param searchDto : page, limit
     * @return
     */
    public List<ContentsDto> getMyContentsList(String token, SearchDto searchDto) {

        MemberDto loginUserInfo = super.getMemberUuidByToken(token);

        // 로그인 회원 uuid set
        searchDto.setLoginMemberUuid(loginUserInfo.getUuid());

        List<ContentsDto> contentsList = new ArrayList<>();
        // 목록 전체 count
        int iTotalCount = contentsDaoSub.iGetTotalMyContentsCount(searchDto);

        if (iTotalCount > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(iTotalCount, searchDto);
            searchDto.setPagination(pagination);

            // list
            contentsList = contentsDaoSub.lGetMyContentsList(searchDto);
            // 첫 번째 이미지 url setting
            setFirstImgUrl(contentsList);
        }

        return contentsList;
    }

    /**
     * 해당 유저가 작성한 콘텐츠 리스트
     *
     * @param searchDto memberIdx
     * @return 내가 좋아요 한 콘텐츠 리스트
     */
    public List<ContentsDto> getMemberContentsList(String token, SearchDto searchDto) {

        // 로그인 중
        if (!ObjectUtils.isEmpty(token)) {
            MemberDto loginUserInfo = super.getMemberUuidByToken(token);
            // 회원 로그인 uuid set
            searchDto.setLoginMemberUuid(loginUserInfo.getUuid());
        }

        Boolean isExist = super.getCheckMemberByUuid(searchDto.getMemberUuid());

        if (isExist == false) {
            // 존재하지 않는 회원입니다
            throw new CustomException(CustomError.MEMBER_UUID_ERROR);
        }

        List<ContentsDto> contentsList = new ArrayList<>();

        // 목록 전체 count
        int iTotalCount = contentsDaoSub.iGetTotalMemberContentsCount(searchDto);

        if (iTotalCount > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(iTotalCount, searchDto);
            searchDto.setPagination(pagination);

            // list
            contentsList = contentsDaoSub.lGetMemberContentsList(searchDto);
            // 첫 번째 이미지 url setting
            setFirstImgUrl(contentsList);
        }

        return contentsList;
    }

    /**
     * 콘텐츠 등록
     *
     * @param contentsDto menuIdx, contents, uploadFile, location, isView, imgTagList, memberUuid
     * @param type        타입 (1: sns)
     * @return Long
     */
    @Transactional
    public Long registContents(String token, ContentsDto contentsDto, int type) {

        // menuIdx 검사
        Integer menuIdx = contentsDto.getMenuIdx();
        if (menuIdx == null || type != menuIdx) { // menuIdx 가 요청한 타입과 불일치
            throw new CustomException(CustomError.CONTENTS_MENUIDX_ERROR); // 카테고리 idx 오류
        }

        // switch 검사
        if (type == sns && !useContentsRegister) {
            throw new CustomException(CustomError.SWITCH_FALSE_ERROR); // 이용 불가한 기능입니다.
        }

        // 회원 UUID 조회
        MemberDto memberDto = super.getMemberUuidByToken(token);

        String memberUuid = memberDto.getUuid();
        contentsDto.setMemberUuid(memberUuid); // 회원 uuid set

        // 등록일 set
        contentsDto.setRegDate(dateLibrary.getDatetime());

        // 회원 글작성 제재 체크 :: 2 -> 글작성 제재
        String getWriteRestrainMemberInfo = restrainCurlService.getRestrainCheck(token, 2);
        JSONObject writeRestrainMemberInfoObject = new JSONObject(getWriteRestrainMemberInfo);
        if (!((boolean) writeRestrainMemberInfoObject.get("result"))) {
            throw new CurlException(writeRestrainMemberInfoObject);
        }

        // 회원 글작성 제재 체크 :: 3 -> 컨텐츠 작성 제재
        String getContentsRestrainMemberInfo = restrainCurlService.getRestrainCheck(token, 3);
        JSONObject cntentsRestrainMemberInfoObject = new JSONObject(getContentsRestrainMemberInfo);
        if (!((boolean) cntentsRestrainMemberInfoObject.get("result"))) {
            throw new CurlException(cntentsRestrainMemberInfoObject);
        }

        // 유효성 검사
        registValidate(contentsDto);

        // 이미지 유효성 검사
        List<MultipartFile> uploadFile = contentsDto.getUploadFile(); // 이미지
        s3Library.checkUploadFiles(uploadFile);

        // 콘텐츠 uuid 세팅 ( locale Language + 랜덤 UUID + Timestamp )
        String localeLang = super.getLocaleLang();
        String setUuid = dateLibrary.getTimestamp();
        String uuid = "sns_con_" + localeLang + UUID.randomUUID().toString().concat(setUuid);
        uuid = uuid.replace("-", "");

        // 고유아이디 중복체크
        Boolean bDupleUuid = checkDupleUuid(uuid);

        // 고유아이디가 중복이면 5번 재시도
        int retry = 0;
        while (Boolean.TRUE.equals(bDupleUuid) && retry < 5) {
            retry++;
            pushAlarm("콘텐츠 고유아이디 중복 시도::" + retry + "번째");

            // 콘텐츠 uuid 세팅 ( locale Language + 랜덤 UUID + Timestamp )
            localeLang = super.getLocaleLang();
            setUuid = dateLibrary.getTimestamp();
            uuid = "sns_con_" + localeLang + UUID.randomUUID().toString().concat(setUuid);
            uuid = uuid.replace("-", "");

            bDupleUuid = checkDupleUuid(uuid);

            if (retry == 5) {
                throw new CustomException(CustomError.CONTENTS_UID_DUPLE);
            }
        }

        // 고유아이디 set
        contentsDto.setUuid(uuid);

        // 이미지 개수 set
        contentsDto.setImageCnt(uploadFile.size());

        // 공개범위 set
        if (contentsDto.getIsView() != null && contentsDto.getIsView() == 0) {
            contentsDto.setIsKeep(1);
        } else {
            contentsDto.setIsKeep(0);
        }

        // 콘텐츠 insert [sns_contents]
        insertContents(contentsDto);
        Long insertedIdx = contentsDto.getInsertedIdx();

        // 이미지 s3 upload (원본)
        String s3Path = "contents/" + insertedIdx;
        List<HashMap<String, Object>> uploadResponse = s3Library.uploadFileNew(uploadFile, s3Path);

        // 콘텐츠 이미지 insert [sns_contents_img]
        List<Long> imgInsertedIdxList = registerImage(uploadResponse, insertedIdx, s3Path);

        // 소셜 콘텐츠인 경우 (menu : 1)
        if (type == sns) {

            // 이미지 내 태그 insert
            insertContentsImgTags(contentsDto, imgInsertedIdxList);

            // 위치 정보 insert
            String location = contentsDto.getLocation();

            if (location != null && !location.trim().equals("")) {

                // location 테이블에 기존에 등록된 위치가 있는지 확인
                Long locationIdx = getIdxByLocation(location);
                contentsDto.setLocationIdx(locationIdx);

                // 등록되지 않은 위치정보 > insert
                if (locationIdx == null) {
                    insertLocation(contentsDto);
                }

                // 위치 매핑 insert [sns_contents_location_mapping]
                insertLocationMapping(contentsDto);

            }

        }

        // 좋아요 cnt 0으로 insert
        insertContentsLikeCnt(contentsDto);

        // 댓글 cnt 0으로 insert
        insertContentsCommentCnt(contentsDto);

        // 저장 cnt 0으로 insert
        insertContentsSaveCnt(contentsDto);

        // 해시태그 입력
        insertHashTag(contentsDto);

        // 회원 멘션 입력
        insertMention(token, contentsDto);

        // 컨텐츠 내용 업데이트
        contentsDto.setIdx(insertedIdx);
        Long result = updateContentsContents(contentsDto);

        if (result > 0) {

            // 알림 ON
            if (useContentsNoti) {
                // 알림 등록
                NotiDto notiDto = NotiDto.builder()
                        .senderUuid(contentsDto.getMemberUuid())  // 컨텐츠를 등록 한 회원idx
                        .contentsIdx(insertedIdx)               // 컨텐츠 IDX
                        .subType("new_contents")
                        .contents(contentsDto.getContents())
                        .build();
                contentsNotiService.contentsRegistSendNoti(token, notiDto);
            }

            // 푸시 ON
            if (useContentsPush) {
                // 푸시 등록
                PushDto pushDto = PushDto.builder()
                        .contentsIdx(insertedIdx) // 컨텐츠 IDX
                        .build();
                contentsPushService.contentsRegistSendPush(pushDto);
            }

        }

        return result;
    }

    /**
     * 콘텐츠 수정
     *
     * @param contentsDto contents.idx, memberIdx, contents, location, isView, imgTagList
     * @param type        타입 (1: sns)
     * @return 처리결과
     */
    @Transactional
    public Long modifyContents(String token, ContentsDto contentsDto, int type) {

        // menuIdx 검사
        Integer menuIdx = contentsDto.getMenuIdx();
        if (menuIdx == null || type != menuIdx) { // menuIdx 가 요청한 타입과 불일치
            throw new CustomException(CustomError.CONTENTS_MENUIDX_ERROR); // 카테고리 idx 오류
        }

        // switch 검사
        if (type == sns && !useContentsModify) { // type : 소셜
            throw new CustomException(CustomError.SWITCH_FALSE_ERROR); // 이용 불가한 기능입니다.
        }

        // 회원 UUID 조회
        MemberDto memberDto = super.getMemberUuidByToken(token);

        String memberUuid = memberDto.getUuid();
        contentsDto.setMemberUuid(memberUuid);

        // 유효성 검사
        modifyValidate(contentsDto);

        // 회원 글작성 제재 체크 :: 2 -> 글작성 제재
        String getWriteRestrainMemberInfo = restrainCurlService.getRestrainCheck(token, 2);
        JSONObject writeRestrainMemberInfoObject = new JSONObject(getWriteRestrainMemberInfo);
        if (!((boolean) writeRestrainMemberInfoObject.get("result"))) {
            throw new CurlException(writeRestrainMemberInfoObject);
        }

        // 회원 글작성 제재 체크 :: 3 -> 컨텐츠 작성 제재
        String getContentsRestrainMemberInfo = restrainCurlService.getRestrainCheck(token, 3);
        JSONObject cntentsRestrainMemberInfoObject = new JSONObject(getContentsRestrainMemberInfo);
        if (!((boolean) cntentsRestrainMemberInfoObject.get("result"))) {
            throw new CurlException(cntentsRestrainMemberInfoObject);
        }

        // 수정일 set
        contentsDto.setModiDate(dateLibrary.getDatetime());

        // 공개범위 set
        if (contentsDto.getIsView() != null && contentsDto.getIsView() == 0) {
            contentsDto.setIsKeep(1);
        } else {
            contentsDto.setIsKeep(0);
        }

        // 컨텐츠 업데이트
        Long result = updateContentsContents(contentsDto);

        if (result <= 0) {
            // 내용 업데이트 실패하였습니다.
            throw new CustomException(CustomError.CONTENTS_UPDATE_CONTENTS_ERROR);
        }

        // 소셜 콘텐츠인 경우 (menu : 1)
        if (type == sns) {
            // 이미지 내 태그
            if (contentsDto.getImgTagList() != null) {

                // 컨텐츠에 기존에 등록된 IMG IDX가져오기
                List<Long> lContentsImgList = getContentsImgIdxList(contentsDto);

                // 이미지 태그
                for (ContentsImgMemberTagDto tagList : contentsDto.getImgTagList()) {
                    Map<String, Object> map = new HashMap<>();
                    List<Map<String, Object>> lImgTagList = new ArrayList<>();

                    // 수정 시 넘어온 imgIdx값이 해당 컨텐츠의 이미지가 맞는지 체크
                    boolean bIsImgIdx = lContentsImgList.contains(tagList.getImgIdx());
                    if (!bIsImgIdx) {
                        // 컨텐츠에 등록된 IMG가 아닌 이미지 태그가 넘어왔습니다
                        throw new CustomException(CustomError.CONTENTS_NOT_IMG_TAG_IDX_ERROR);
                    }

                    // 이미지 내 태그 회원idx 정상적인 회원인지 체크
                    memberDto.setUuid(tagList.getMemberUuid());

                    // 회원 uuid 정상인지 curl 통신
                    String jsonString = memberCurlService.checkMemberUuid(tagList.getMemberUuid());

                    JSONObject jsonObject = new JSONObject(jsonString);
                    JSONObject dataObject = (JSONObject) jsonObject.get("data");
                    boolean isExist = (boolean) dataObject.get("isExist"); // 이미지 내 태그 존재 유무

                    // exception
                    if (!isExist) {
                        tagList.setStatus("del");
//                        throw new CustomException(CustomError.CONTENTS_REGISTER_IMG_TAG_MEMBER_ERROR); // 이미지내 태그 uuid 오류
                    }

                    // 컨텐츠 idx & 등록일 set
                    map.put("contentsIdx", contentsDto.getIdx());
                    map.put("regDate", contentsDto.getModiDate());
                    // width & height
                    map.put("width", tagList.getWidth());
                    map.put("height", tagList.getHeight());
                    // memberIdx
                    map.put("memberUuid", tagList.getMemberUuid());
                    // 이미지Idx
                    map.put("imgIdx", tagList.getImgIdx());
                    lImgTagList.add(map);

                    // 새로운 이미지 태그이면
                    if (Objects.equals(tagList.getStatus(), "new")) {
                        insertImgTag(lImgTagList);
                    }
                    // 이미지 태그 WIDTH HEIGHT 값 수정이면
                    else if (Objects.equals(tagList.getStatus(), "modi")) {
                        modifyImgTag(lImgTagList);
                    }
                    // 삭제된 이미지 태그이면
                    else if (Objects.equals(tagList.getStatus(), "del")) {
                        deleteImgTag(lImgTagList);
                    }
                }
            }

            // 위치정보
            String location = contentsDto.getLocation();
            // 기존에 등록 된 위치 가져오기
            String sContentsLocation = getContentsLocation(contentsDto);
            // 기존 등록 된 위치와 같지 않으면
            if (sContentsLocation != null) {
                if (!Objects.equals(location, sContentsLocation)) {
                    //기존 위치 지우고
                    deleteLocation(contentsDto);

                    // 새로운 위치로 인서트
                    if (location != null && !location.trim().equals("")) {

                        // location 테이블에 기존에 등록된 위치가 있는지 확인
                        Long locationIdx = getIdxByLocation(location);
                        contentsDto.setLocationIdx(locationIdx);
                        contentsDto.setRegDate(contentsDto.getModiDate());
                        contentsDto.setInsertedIdx(contentsDto.getIdx());

                        // 등록되지 않은 위치정보 > insert
                        if (locationIdx == null) {
                            insertLocation(contentsDto);
                        }
                        // 위치 매핑 insert [sns_contents_location_mapping]
                        insertLocationMapping(contentsDto);
                    }
                }
            } else {
                // 새로운 위치로 인서트
                if (location != null && !location.trim().equals("")) {

                    // location 테이블에 기존에 등록된 위치가 있는지 확인
                    Long locationIdx = getIdxByLocation(location);
                    contentsDto.setLocationIdx(locationIdx);
                    contentsDto.setRegDate(contentsDto.getModiDate());
                    contentsDto.setInsertedIdx(contentsDto.getIdx());

                    // 등록되지 않은 위치정보 > insert
                    if (locationIdx == null) {
                        insertLocation(contentsDto);
                    }
                    // 위치 매핑 insert [sns_contents_location_mapping]
                    insertLocationMapping(contentsDto);
                }
            }
        }

        // 해시 태그 수정
        modifyHashTag(contentsDto);

        // 회원 멘션 수정
        modifyMention(token, contentsDto);

        // 컨텐츠 내용 업데이트
        result = updateContentsContents(contentsDto);

        if (result > 0) {

            // 알림 ON
            if (useContentsNoti) {
                // 알림 등록
                NotiDto notiDto = NotiDto.builder()
                        .senderUuid(contentsDto.getMemberUuid())  // 컨텐츠를 등록 한 회원idx
                        .contentsIdx(contentsDto.getIdx())      // 컨텐츠 IDX
                        .subType("mention_contents")
                        .contents(contentsDto.getContents())
                        .modiDate(contentsDto.getModiDate())
                        .build();

                contentsNotiService.contentsModifySendNoti(token, notiDto);
            }

            // 푸시 ON
            if (useContentsPush) {
                // 푸시 등록
                PushDto pushDto = PushDto.builder()
                        .contentsIdx(contentsDto.getIdx()) // 컨텐츠 IDX
                        .build();
                contentsPushService.contentsModifySendPush(pushDto);
            }

        }

        return result;
    }

    /**
     * 해시태그 테이블 입력
     *
     * @param contentsDto insertedIdx contents
     */
    public void insertHashTag(ContentsDto contentsDto) {
        // 해시태그 관련 데이터 set
        HashTagDto hashTagDto = new HashTagDto();
        // 타입을 contents로 정의
        hashTagDto.setType("contents");
        // 컨텐츠 idx
        hashTagDto.setContentsIdx(contentsDto.getInsertedIdx());
        // 컨텐츠 내용
        hashTagDto.setContents(contentsDto.getContents());
        // 해시태그 치환 ex) #해시태그 -> [#[해시태그]] 및 해시태그/cnt 업데이트
        String hashTagContents = hashTagService.reigstHashTag(hashTagDto);
        // 컨텐츠 세팅
        contentsDto.setContents(hashTagContents);
    }


    /**
     * 해시태그 수정
     *
     * @param contentsDto idx contents
     */
    public void modifyHashTag(ContentsDto contentsDto) {
        // 해시태그 관련 데이터 set
        HashTagDto hashTagDto = new HashTagDto();
        // 타입을 contents로 정의
        hashTagDto.setType("contents");
        // 컨텐츠 idx
        hashTagDto.setContentsIdx(contentsDto.getIdx());
        // 컨텐츠 내용
        hashTagDto.setContents(contentsDto.getContents());
        // 해시태그 치환 ex) #해시태그 -> [#[해시태그]] 및 해시태그/cnt 업데이트
        String hashTagContents = hashTagService.modifyHashTag(hashTagDto);
        // 컨텐츠 세팅
        contentsDto.setContents(hashTagContents);
    }

    /**
     * 멘션 테이블 입력
     *
     * @param contentsDto insertedIdx contents
     */
    public void insertMention(String token, ContentsDto contentsDto) {
        // 멘션 관련 데이터 set
        MentionTagDto mentionTagDto = new MentionTagDto();
        // 타입을 contents로 정의
        mentionTagDto.setType("contents");
        // 컨텐츠 idx
        mentionTagDto.setContentsIdx(contentsDto.getInsertedIdx());
        // 컨텐츠 내용
        mentionTagDto.setContents(contentsDto.getContents());
        // 멤버멘션 치환
        String mentionTagContents = mentionTagService.reigstMention(token, mentionTagDto);

        // 컨텐츠 세팅
        contentsDto.setContents(mentionTagContents);
    }


    /**
     * 멘션 테이블 수정
     *
     * @param contentsDto idx contents
     */
    public void modifyMention(String token, ContentsDto contentsDto) {
        // 멘션 관련 데이터 set
        MentionTagDto mentionTagDto = new MentionTagDto();
        // 타입을 contents로 정의
        mentionTagDto.setType("contents");
        // 컨텐츠 idx
        mentionTagDto.setContentsIdx(contentsDto.getIdx());
        // 컨텐츠 내용
        mentionTagDto.setContents(contentsDto.getContents());
        // 멤버멘션 치환
        String mentionTagContents = mentionTagService.modifyMention(token, mentionTagDto);

        // 컨텐츠 세팅
        contentsDto.setContents(mentionTagContents);
    }

    /**
     * 콘텐츠 삭제
     *
     * @param token       access token
     * @param contentsDto idxList (삭제할 콘텐츠 idx 리스트)
     * @return 처리결과
     */
    @Transactional
    public Long deleteContents(String token, ContentsDto contentsDto) {

        Long result = 0L;

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);
        contentsDto.setMemberUuid(memberDto.getUuid());

        List<Long> idxList = contentsDto.getIdxList();

        // 콘텐츠 idx 리스트 비어 있는지 검증
        if (idxList.isEmpty()) {
            throw new CustomException(CustomError.DELETE_IDX_EMPTY_ERROR); // 삭제할 콘텐츠를 선택해주세요.
        }

        for (Long idx : idxList) { // idx 돌면서 state 0 으로 변경

            // 콘텐츠 idx 검증
            if (idx == null || idx < 1) {
                throw new CustomException(CustomError.CONTENTS_IDX_NULL); // 존재하지 않는 콘텐츠입니다.
            }

            contentsDto.setIdx(idx);

            // 내가 작성한 콘텐츠 인지 검증
            Boolean bResult = checkMyContents(contentsDto);

            if (Boolean.TRUE.equals(bResult)) {
                result = deleteContent(contentsDto);
            } else {
                throw new CustomException(CustomError.DELETE_NOT_MY_CONTENTS_ERROR); // 내가 작성한 콘텐츠가 아닙니다.
            }

        }

        return result;
    }

    /*****************************************************
     *  SubFunction - Select
     ****************************************************/
    /**
     * 콘텐츠 고유 아이디 중복 검색
     *
     * @param uuid 고유아이디
     * @return 중복여부 [중복 : true]
     */
    public Boolean checkDupleUuid(String uuid) {
        Integer iCount = contentsDaoSub.getCountByUuid(uuid);

        return iCount > 0;
    }

    /**
     * 콘텐츠 고유 아이디 중복 검색
     *
     * @param uuid 고유아이디
     * @return 중복여부 [중복 : true]
     */
    public Boolean checkDupleImgUuid(String uuid) {
        Integer iCount = contentsDaoSub.getCountByImgUuid(uuid);

        return iCount > 0;
    }

    /**
     * 콘텐츠 idx 조회 by 댓글 idx
     *
     * @param commentIdx commentIdx
     * @return contentsIdx
     */
    public Long getContentsIdxByCommentIdx(Long commentIdx) {
        return contentsDaoSub.getContentsIdxByCommentIdx(commentIdx);
    }

    /**
     * 위치정보 idx 검색
     *
     * @param location 위치정보
     * @return 위치정보 idx
     */
    public Long getIdxByLocation(String location) {
        return contentsDaoSub.getIdxByLocation(location);
    }

    /**
     * 내가 쓴 콘텐츠인지 확인
     *
     * @param contentsDto memberUuid, 콘텐츠 idx
     * @return true / false
     */
    public Boolean checkMyContents(ContentsDto contentsDto) {
        Integer iCount = contentsDaoSub.getCountByMyContentsIdx(contentsDto);

        return iCount > 0;
    }

    /**
     * 소셜/산책 콘텐츠 타입 확인
     *
     * @param contentsDto menuIdx, 콘텐츠 idx
     * @return true / false
     */
    public Boolean checkContentsMenu(ContentsDto contentsDto) {
        Integer iCount = contentsDaoSub.getCountByContentsMenu(contentsDto);

        return iCount > 0;
    }

    /**
     * 컨텐츠에 사용된 IMG IDX 목록
     *
     * @param contentsDto idx
     * @return img idx 리스트
     */
    public List<Long> getContentsImgIdxList(ContentsDto contentsDto) {
        return contentsDaoSub.getContentsImgIdxList(contentsDto);
    }

    /**
     * 컨텐츠에 사용된 위치정보 가져오기
     *
     * @param contentsDto idx
     * @return string 위치
     */
    public String getContentsLocation(ContentsDto contentsDto) {
        return contentsDaoSub.getContentsLocation(contentsDto);
    }

    /**
     * 컨텐츠 첫번째 이미지 리스트 조회
     *
     * @param idx : 컨텐츠 idx
     * @return
     */
    public ContentsImgDto getContentsFirstImg(Long idx) {
        return contentsDaoSub.getContentsFirstImg(idx);
    }

    /**
     * 댓글 멘션 리스트 조회 후 set
     *
     * @param commentDto
     */
    @SneakyThrows
    private void setCommentMentionList(CommentDto commentDto) {

        if (!ObjectUtils.isEmpty(commentDto)) {

            MentionTagDto mentionTagDto = MentionTagDto.builder()
                    .commentIdx(commentDto.getIdx()).build();

            /** 댓글 멘션 리스트 **/
            commentDto.setMentionList(new ArrayList<>());
            List<MentionTagDto> commentMentionTagList = mentionTagService.getCommentMentionTagList(mentionTagDto);
            List<String> commentUuidList = new ArrayList<>();

            for (MentionTagDto dto : commentMentionTagList) {
                commentUuidList.add(dto.getMemberUuid());
            }

            if (!ObjectUtils.isEmpty(commentUuidList)) {
                // 멘션된 회원 정보 curl 통신
                String mentionInfoJsonString = contentsCurlService.getMentionInfoList(commentUuidList); // memberUuid, nick, outMemberUuid, outNick, state
                JSONObject mentionInfoObject = new JSONObject(mentionInfoJsonString);

                if (!(boolean) mentionInfoObject.get("result")) {
                    throw new CurlException(mentionInfoObject);
                }

                ObjectMapper mapper = new ObjectMapper();
                JSONObject mentionInfoResult = (JSONObject) mentionInfoObject.get("data");
                // 멘션 리스트
                List<MentionTagDto> mentionInfoList = mapper.readValue(mentionInfoResult.get("list").toString(), new TypeReference<>() {
                });

                commentDto.setMentionList(mentionInfoList); // 멘션 리스트 추가
            }
        }
    }

    /**
     * 컨텐츠 리스트에 첫 번째 이미지 url setting
     * [상세 아닌 일반 리스트 공통 사용]
     *
     * @param contentsList
     */
    public void setFirstImgUrl(List<ContentsDto> contentsList) {
        List<ContentsImgDto> contentsImgList = new ArrayList<>();

        // 이미지 contentsIdx, url 조회
        for (ContentsDto contentsDto : contentsList) {
            ContentsImgDto contentsImgDto = getContentsFirstImg(contentsDto.getIdx());
            contentsImgList.add(contentsImgDto);
        }

        // imgUrl setting
        for (ContentsDto contentsDto : contentsList) {
            long contentsIdx = contentsDto.getIdx();

            for (int index = 0; index < contentsImgList.size(); index++) {
                ContentsImgDto contentsImgDto = contentsImgList.get(index);

                if (contentsIdx == contentsImgDto.getContentsIdx()) {
                    contentsDto.setImgUrl(imgDomain + contentsImgDto.getUrl());
                    contentsImgList.remove(index);
                    break;
                }
            }
        }

    }

    /**
     * (공통) 컨텐츠 상세 리스트 setting
     * [회원정보, 해시 태그, 멘션, 이미지, 이미지 회원 태그, 좋아요 많은 댓글]
     *
     * @param contentsDtoList
     * @param searchDto
     */
    @SneakyThrows
    private void setContentsDetailList(List<ContentsDto> contentsDtoList, SearchDto searchDto, boolean isIncludeMemberInfo, boolean isIncludeComment) {

        ObjectMapper mapper = new ObjectMapper();
        searchDto.setImgDomain(imgDomain); // 이미지 도메인 설정

        // 데이터가 없어도 리스트는 존재
        for (ContentsDto contentsDto : contentsDtoList) {
            contentsDto.setMentionList((new ArrayList<>()));  // 멘션 리스트 생성
            contentsDto.setImgList(new ArrayList<>());        // 이미지 리스트 생성
        }

        List<String> memberUuidList = new ArrayList<>();    // [조회용] 회원 idx 리스트
        List<Long> contentsIdxList = new ArrayList<>();  // [조회용] 컨텐츠 idx 리스트

        // 조회할 memberUuid 추출
        for (ContentsDto contentsDto : contentsDtoList) {
            memberUuidList.add(contentsDto.getMemberUuid()); // 회원 리스트 추가
        }
        // memberUuid 중복 제거
        memberUuidList = memberUuidList.stream().distinct().collect(Collectors.toList());

        // 컨텐츠 idx 리스트 추가
        for (ContentsDto contentsDto : contentsDtoList) {
            contentsIdxList.add(contentsDto.getIdx());
        }

        // 작성자 포함 여부 [회원 상세 리스트는 작성자 같으므로 false]
        if (isIncludeMemberInfo) {
            // 작성자 팔로우 관련 정보 리스트 조회
            List<MemberInfoDto> memberInfoDtoList = memberInfoDaoSub.getMemberFollowInfoByUuidList(memberUuidList);

            /** 작성자 리스트 SET **/
            if (memberInfoDtoList != null) {

                // 작성자 정보 curl 조회
                String writerInfoJsonString = contentsCurlService.getWriterInfoList(memberUuidList);
                JSONObject writerInfoObject = new JSONObject(writerInfoJsonString);

                if (!(boolean) writerInfoObject.get("result")) {
                    throw new CurlException(writerInfoObject);
                }
                JSONObject writerInfoResult = (JSONObject) writerInfoObject.get("data");
                // 컨텐츠 작성자 정보 리스트
                List<MemberInfoDto> writerInfoList = mapper.readValue(writerInfoResult.get("list").toString(), new TypeReference<>() {
                });

                if (!ObjectUtils.isEmpty(writerInfoList)) {
                    // 작성자 정보 setting(닉네임, 간편 가입, 이메일...)
                    for (MemberInfoDto memberInfoDto : memberInfoDtoList) {
                        String uuid = memberInfoDto.getUuid();

                        for (MemberInfoDto writerInfoDto : writerInfoList) {

                            if (writerInfoDto.getUuid().equals(uuid)) {
                                memberInfoDto.setNick(writerInfoDto.getNick());                     // 닉네임
                                memberInfoDto.setSimpleType(writerInfoDto.getSimpleType());         // 간편 가입 유형
                                memberInfoDto.setEmail(writerInfoDto.getEmail());                   // 이메일
                                memberInfoDto.setProfileImgUrl(writerInfoDto.getProfileImgUrl());   // 프로필 이미지
                                memberInfoDto.setIntro(writerInfoDto.getIntro());                   // 소개글
                                break;
                            }
                        }
                    }
                }

                // contentsDto 별로 작성자 정보 set
                for (ContentsDto contentsDto : contentsDtoList) {
                    String memberUuid = contentsDto.getMemberUuid();

                    for (MemberInfoDto writerInfo : memberInfoDtoList) {

                        if (writerInfo.getUuid().equals(memberUuid)) {
                            contentsDto.setMemberInfo(writerInfo);
                            break;
                        }
                    }
                }
            } // end of if
        } // end of if

        // 멘션 리스트 조회 [contentsIdx, uuid(멘션된 회원 uuid)] -> uuid, nick, outUuid, outNick
        List<MentionTagDto> mentionTagDtoList = contentsDaoSub.getMentionTagList(contentsIdxList); // curl 조회 위한 uuid 조회

        /** 멘션 리스트 SET **/
        if (!ObjectUtils.isEmpty(mentionTagDtoList)) {
            List<String> mentionUuidList = new ArrayList<>(); // 멘션된 회원 uuid 리스트

            for (MentionTagDto mentionTagDto : mentionTagDtoList) {
                String uuid = mentionTagDto.getUuid(); // 멘션된 회원 uuid
                mentionUuidList.add(uuid); // uuid 리스트 추가
            }
            // 멘션 회원 중복 제거
            mentionUuidList = mentionUuidList.stream().distinct().collect(Collectors.toList());

            // 멘션된 회원 정보 curl 통신
            String mentionInfoJsonString = contentsCurlService.getMentionInfoList(mentionUuidList); // memberUuid, nick, outMemberUuid, outNick, state

            JSONObject mentionInfoObject = new JSONObject(mentionInfoJsonString);
            // curl 통신 중 에러
            if (!(boolean) mentionInfoObject.get("result")) {
                throw new CurlException(mentionInfoObject);
            }

            JSONObject mentionInfoResult = (JSONObject) mentionInfoObject.get("data");
            // 멘션 리스트
            List<MentionTagDto> mentionUserInfoList = mapper.readValue(mentionInfoResult.get("list").toString(), new TypeReference<>() {
            });

            // 멘션된 회원 정보 리스트에 contentsIdx setting [컨텐츠 리스트의 컨텐츠 별로 비교하여 담기 위해]
            for (MentionTagDto mentionTagDto : mentionTagDtoList) {
                String uuid = mentionTagDto.getUuid();

                for (MentionTagDto userInfo : mentionUserInfoList) {
                    if (userInfo.getUuid().equals(uuid)) { // curl 조회한 회원 uuid와 메션 리스트의 회원 uuid 같으면
                        mentionTagDto.setNick(userInfo.getNick());
                        mentionTagDto.setState(userInfo.getState());
                        mentionTagDto.setNick(userInfo.getNick());
                    }
                }
            }

            // 같은 contentsIdx 끼리 묶는 작업
            for (ContentsDto contentsDto : contentsDtoList) {
                long contentsIdx = contentsDto.getIdx();

                for (MentionTagDto mentionTagDto : mentionTagDtoList) {
                    if (contentsIdx == mentionTagDto.getContentsIdx()) {
                        contentsDto.getMentionList().add(mentionTagDto); // 멘션 리스트에 추가
                    } // end of if
                } // end of for
            } // end of for
        } // end of 멘션

        // 이미지 idx 리스트 생성
        List<Long> imgIdxList = new ArrayList<>();

        // 이미지 리스트 조회 [이미지 limit을 걸어 놨으므로 in 조건 불가능]
        for (ContentsDto contentsDto : contentsDtoList) {
            Long contentsIdx = contentsDto.getIdx();
            searchDto.setContentsIdx(contentsIdx);
            // 컨텐츠에 해당하는 이미지 리스트 조회
            List<ContentsImgDto> imgList = contentsDaoSub.getContentsImgList(searchDto);
            // 이미지 리스트 추가
            contentsDto.setImgList(imgList);

            if (imgList != null && !imgList.isEmpty()) {

                for (ContentsImgDto contentsImgDto : imgList) {
                    contentsImgDto.setContentsIdx(null);
                    imgIdxList.add(contentsImgDto.getIdx());               // 이미지 idx 리스트 추가
                    contentsImgDto.setImgMemberTagList(new ArrayList<>()); // 회원 태그 리스트 생성 [데이터는 없으도 리스트는 존재]
                }
            }
        } // end of for contentsDtoList

        // 이미지 idx 리스트 set
        searchDto.setIdxList(imgIdxList);

        /** 이미지 내 회원 태그 리스트 조회 **/
        List<ContentsImgMemberTagDto> imgMemberTagList = contentsDaoSub.getImgMemberTagList(searchDto);

        /** 이미지 내 회원 태그 리스트 SET **/
        if (!ObjectUtils.isEmpty(imgMemberTagList)) {              // 이미지 내 회원 태그 존재
            List<String> imgTagMemberUuidList = new ArrayList<>(); // 이미지 내 태그된 회원 uuid 리스트

            for (ContentsImgMemberTagDto memberTagDto : imgMemberTagList) {
                imgTagMemberUuidList.add(memberTagDto.getMemberUuid()); // 태그된 회원 uuid 리스트 추가
            }
            // 이미지 내 태그된 회원 중복 제거
            imgTagMemberUuidList = imgTagMemberUuidList.stream().distinct().collect(Collectors.toList());

            // 이미지 태그 내 회원 정보 curl 통신
            String memberInfoJsonString = contentsCurlService.getImgMemberTagInfoList(imgTagMemberUuidList);
            JSONObject memberInfoObject = new JSONObject(memberInfoJsonString);

            if (!(boolean) memberInfoObject.get("result")) {
                throw new CurlException(memberInfoObject);
            }

            JSONObject memberInfoResult = (JSONObject) memberInfoObject.get("data");

            // 이미지 태그 내 회원 리스트
            List<MemberDto> memberInfoList = mapper.readValue(memberInfoResult.get("list").toString(), new TypeReference<>() {
            });

            // 이미지 내 태그된 회원 존재
            if (!ObjectUtils.isEmpty(memberInfoList)) {

                for (ContentsImgMemberTagDto memberTagDto : imgMemberTagList) { // 이미지 태그된 회원 정보 set
                    String memberUuid = memberTagDto.getMemberUuid();

                    memberInfoList.forEach(memberDto -> {
                        // curl 통신 통해 조회한 uuid와 태그된 회원 uuid 같으면 회원 정보 set
                        if (memberUuid.equals(memberDto.getUuid())) {
                            memberTagDto.setNick(memberDto.getNick());                   // 태그된 회원 닉네임
                            memberTagDto.setProfileImgUrl(memberDto.getProfileImgUrl()); // 태그된 회원 프로필
                            memberTagDto.setIntro(memberDto.getIntro());                 // 태그된 회원 소개글
                            memberTagDto.setState(memberDto.getState());                 // 태그된 회원 상태(0: 탈퇴, 1: 정상)
                        }
                    });
                } // 태그된 회원 정보 관련 setting 완료

                for (ContentsDto contentsDto : contentsDtoList) {
                    List<ContentsImgDto> imgList = contentsDto.getImgList(); // 컨텐츠 별 이미지 리스트

                    // 해당 이미지 리스트에 이미지 회원 태그 리스트 생성
                    for (ContentsImgDto imgDto : imgList) {
                        Long imgIdx = imgDto.getIdx();                 // 이미지 idx

                        // 하나의 컨텐츠 이미지에 이미지 회원 태그 리스트 SET
                        for (ContentsImgMemberTagDto memberTagDto : imgMemberTagList) {
                            // 컨텐츠 idx가 같으면 추가 후 해당 인덱스 삭제
                            if (imgIdx.equals(memberTagDto.getImgIdx())) {
                                imgDto.getImgMemberTagList().add(memberTagDto);
                            }
                        } // end of for
                    } // end of for
                }
            }
        }

        // 댓글 포함 여부
        if (isIncludeComment) {
            /** 좋아요 많은 댓글 (1순위 : 좋아요 , 2순위 : 등록일)**/
            for (ContentsDto contentsDto : contentsDtoList) {
                Long contentsIdx = contentsDto.getIdx();

                // 댓글 조회
                searchDto.setContentsIdx(contentsIdx);
                CommentDto commentDto = commentDaoSub.getLikeManyComment(searchDto); // 좋아요 많은 댓글 한건 조회

                if (commentDto != null) {
                    // 댓글 작성자 정보 curl 통신
                    MemberDto commentWriterInfo = commentCurlService.getLikeManyCommentMemberInfo(commentDto.getMemberUuid());
                    if (!ObjectUtils.isEmpty(commentWriterInfo)) {
                        commentDto.setProfileImgUrl(commentWriterInfo.getProfileImgUrl());
                        commentDto.setIntro(commentWriterInfo.getIntro());
                        commentDto.setNick(commentWriterInfo.getNick());

                        /** 댓글 멘션 리스트 set **/
                        setCommentMentionList(commentDto);
                        contentsDto.setComment(commentDto);   // 댓글 추가
                    }
                }
            }
        } // end of 댓글 포함 여부
        searchDto.setIdxList(null);

    }

    /**
     * (공통) 컨텐츠 상세 setting
     * [해시 태그, 멘션, 이미지, 이미지 회원 태그, 좋아요 많은 댓글]
     *
     * @param contentsDto
     * @param searchDto
     */
    @SneakyThrows
    private void setContentsDetail(ContentsDto contentsDto, SearchDto searchDto) {

        ObjectMapper mapper = new ObjectMapper();
        Long contentsIdx = contentsDto.getIdx();

        // 데이터가 없어도 리스트는 존재
        contentsDto.setMentionList((new ArrayList<>())); // 멘션 리스트 생성
        contentsDto.setImgList(new ArrayList<>());       // 이미지 리스트 생성

        /** 멘션 리스트 조회 **/
        List<String> mentionUuidList = contentsDaoSub.getMentionMemberUuid(contentsIdx);
        List<MentionTagDto> mentionInfoList = new ArrayList<>();

        if (!ObjectUtils.isEmpty(mentionUuidList)) {
            // 멘션된 회원 정보 curl 통신
            String mentionInfoJsonString = contentsCurlService.getMentionInfoList(mentionUuidList); // memberUuid, nick, outMemberUuid, outNick, state
            JSONObject mentionInfoObject = new JSONObject(mentionInfoJsonString);

            // curl 통신 중 에러
            if (!(boolean) mentionInfoObject.get("result")) {
                throw new CurlException(mentionInfoObject);
            }

            JSONObject mentionInfoResult = (JSONObject) mentionInfoObject.get("data");
            // 멘션 리스트
            mentionInfoList = mapper.readValue(mentionInfoResult.get("list").toString(), new TypeReference<>() {
            });
        }

        /** 이미지 리스트 조회 **/
        searchDto.setImgDomain(imgDomain);
        List<ContentsImgDto> imgList = contentsDaoSub.getContentsImgList(searchDto);

        // 이미지 idx 리스트 생성
        List<Long> imgIdxList = new ArrayList<>();

        if (imgList != null && !imgList.isEmpty()) {

            for (ContentsImgDto contentsImgDto : imgList) {
                contentsImgDto.setContentsIdx(null);
                imgIdxList.add(contentsImgDto.getIdx()); // 이미지 idx 추가
            }

            searchDto.setIdxList(imgIdxList); // 이미지 idx 리스트 set

            /** 이미지 내 회원 태그 리스트 조회 **/
            List<ContentsImgMemberTagDto> imgMemberTagList = contentsDaoSub.getImgMemberTagList(searchDto);

            /** 이미지 내 회원 태그 리스트 SET **/
            if (!ObjectUtils.isEmpty(imgMemberTagList)) {              // 이미지 내 회원 태그 존재
                List<String> imgTagMemberUuidList = new ArrayList<>(); // 이미지 내 태그된 회원 uuid 리스트

                for (ContentsImgMemberTagDto memberTagDto : imgMemberTagList) {
                    imgTagMemberUuidList.add(memberTagDto.getMemberUuid());
                }

                // 이미지 태그 내 회원 정보 curl 통신
                String memberInfoJsonString = contentsCurlService.getImgMemberTagInfoList(imgTagMemberUuidList);

                JSONObject memberInfoObject = new JSONObject(memberInfoJsonString);

                if (!(boolean) memberInfoObject.get("result")) {
                    throw new CurlException(memberInfoObject);
                }
                JSONObject mentionInfoResult = (JSONObject) memberInfoObject.get("data");
                // 이미지 태그 내 회원 리스트
                List<MemberDto> memberInfoList = mapper.readValue(mentionInfoResult.get("list").toString(), new TypeReference<>() {
                });

                // 이미지 내 태그된 정상 회원 존재
                if (!ObjectUtils.isEmpty(memberInfoList)) {
                    for (ContentsImgMemberTagDto memberTagDto : imgMemberTagList) {
                        String memberUuid = memberTagDto.getMemberUuid();

                        memberInfoList.forEach(memberDto -> {
                            if (memberUuid.equals(memberDto.getUuid())) {
                                memberTagDto.setNick(memberDto.getNick());                   // 태그된 회원 닉네임
                                memberTagDto.setProfileImgUrl(memberDto.getProfileImgUrl()); // 태그된 회원 프로필
                                memberTagDto.setIntro(memberDto.getIntro());                 // 태그된 회원 소개글
                                memberTagDto.setState(memberDto.getState());                 // 태그된 회원 상태(0: 탈퇴, 1: 정상)
                            }
                        });
                    }

                    for (ContentsImgDto imgDto : imgList) {
                        Long imgIdx = imgDto.getIdx();
                        imgDto.setImgMemberTagList(new ArrayList<>()); // 이미지 회원 태그 리스트 생성

                        // 하나의 컨텐츠 이미지에 이미지 회원 태그 리스트 SET
                        for (int i = 0; i < imgMemberTagList.size(); i++) {
                            ContentsImgMemberTagDto memberTagDto = imgMemberTagList.get(i);

                            // 컨텐츠 idx가 같으면 추가 후 해당 인덱스 삭제
                            if (imgIdx.equals(memberTagDto.getImgIdx())) {
                                imgDto.getImgMemberTagList().add(memberTagDto);
                                imgMemberTagList.remove(i--);
                            }
                        } // end of for
                    } // end of for

                }
            } else { // 이미지 내 회원 태그 리스트 없을 경우
                // 빈 리스트 삽입
                for (ContentsImgDto contentsImgDto : imgList) {
                    imgMemberTagList = new ArrayList<>();
                    contentsImgDto.setImgMemberTagList(imgMemberTagList);
                }

            }
        } // end of if imgList not null

        // 좋아요 많은 댓글 (1순위 : 좋아요 , 2순위 : 등록일)
        CommentDto commentDto = commentDaoSub.getLikeManyComment(searchDto);
        if (commentDto != null) {
            // 댓글 작성자 정보 curl 통신
            MemberDto commentWriterInfo = commentCurlService.getLikeManyCommentMemberInfo(commentDto.getMemberUuid());

            if (!ObjectUtils.isEmpty(commentWriterInfo)) { // 댓글 작성자 존재
                commentDto.setProfileImgUrl(commentWriterInfo.getProfileImgUrl());
                commentDto.setIntro(commentWriterInfo.getIntro());
                commentDto.setNick(commentWriterInfo.getNick());

                /** 댓글 멘션 리스트 set **/
                setCommentMentionList(commentDto);
                contentsDto.setComment(commentDto);   // 댓글 리스트에 댓글 추가
            }
        }

        /** return value setting **/
        contentsDto.setMentionList(mentionInfoList);     // 컨텐츠 멘션 리스트 추가
        contentsDto.setImgList(imgList);                // 컨텐츠 이미지 리스트 추가
        searchDto.setIdxList(null);
    }

    /*****************************************************
     *  SubFunction - Insert
     ****************************************************/
    /**
     * 게시물 등록
     *
     * @param contentsDto uuid, memberIdx, menuIdx, contents, imageCnt, isView, regDate
     */
    public void insertContents(ContentsDto contentsDto) {
        Integer iResult = contentsDao.insert(contentsDto);
        if (iResult < 1) {
            throw new CustomException(CustomError.CONTENTS_REGISTER_ERROR); // 게시물 등록에 실패하였습니다.
        }
    }

    /**
     * 이미지 등록
     *
     * @param uploadResponse 업로드파일
     * @param idx            콘텐츠 idx
     * @param s3Path         S3경로
     * @return 이미지 idx List
     */
    public List<Long> registerImage(List<HashMap<String, Object>> uploadResponse, Long idx, String s3Path) {

        int sort = 1;
        for (HashMap<String, Object> map : uploadResponse) {
            map.put("idx", idx);
            map.put("path", s3Path);
            map.put("sort", sort);
            map.put("regDate", dateLibrary.getDatetime());

            // 콘텐츠 이미지 uuid 세팅 ( locale Language + 랜덤 UUID + Timestamp )
            String localeLang = super.getLocaleLang();
            String setUuid = dateLibrary.getTimestamp();
            String uuid = "sns_con_img_" + localeLang + UUID.randomUUID().toString().concat(setUuid);
            uuid = uuid.replace("-", "");

            // 고유아이디 중복체크
            Boolean bDupleImgUuid = checkDupleImgUuid(uuid);

            // 고유아이디가 중복이면 5번 재시도
            int retry = 0;
            while (Boolean.TRUE.equals(bDupleImgUuid) && retry < 5) {
                retry++;
                pushAlarm("콘텐츠 이미지 고유아이디 중복 시도::" + retry + "번째");

                // 콘텐츠 이미지 uuid 세팅 ( locale Language + 랜덤 UUID + Timestamp )
                localeLang = super.getLocaleLang();
                setUuid = dateLibrary.getTimestamp();
                uuid = "sns_con_img_" + localeLang + UUID.randomUUID().toString().concat(setUuid);
                uuid = uuid.replace("-", "");

                bDupleImgUuid = checkDupleImgUuid(uuid);

                if (retry == 5) {
                    throw new CustomException(CustomError.CONTENTS_IMAGE_UID_DUPLE);
                }
            }

            // 고유아이디 set
            map.put("uuid", uuid);

            sort++;
        }

        contentsDao.insertImg(uploadResponse);

        // 원본 insert idx list
        List<Long> parentList = new ArrayList<>();
        for (HashMap<String, Object> map : uploadResponse) {
            map.forEach((key, value) -> {
                if (key.equals("imgIdx")) {
                    parentList.add(Long.parseLong(value.toString()));
                }
            });
        }

        return parentList;
    }

    /**
     * 이미지 내 태그 등록
     *
     * @param contentsDto imgTagList : 이미지 내 태그
     */
    public void insertContentsImgTags(ContentsDto contentsDto, List<Long> imgInsertedIdxList) {
        List<Map<String, Object>> imgTagList = new ArrayList<>();

        if (contentsDto.getImgTagList() != null) {
            List<String> memberUuidList = new ArrayList<>();

            for (ContentsImgMemberTagDto tagList : contentsDto.getImgTagList()) {
                memberUuidList.add(tagList.getMemberUuid());
            }
            // 회원 uuid 정상인지 curl 통신
            String jsonString = memberCurlService.checkMemberUuidList(memberUuidList);

            JSONObject jsonObject = new JSONObject(jsonString);
            JSONObject dataObject = (JSONObject) jsonObject.get("data");
            boolean isExist = (boolean) dataObject.get("isExist"); // 이미지 내 태그 존재 유무

            // exception
            if (!isExist) {
                throw new CustomException(CustomError.CONTENTS_REGISTER_IMG_TAG_MEMBER_ERROR); // 이미지내 태그 uuid 오류
            }

            for (ContentsImgMemberTagDto tagList : contentsDto.getImgTagList()) {
                Map<String, Object> map = new HashMap<>();

                // 이미지 갯수랑 이미지 태그된 IDX 비교
                if (contentsDto.getImageCnt() <= tagList.getImgIdx()) {
                    // 이미지내 태그 idx 오류.
                    throw new CustomException(CustomError.CONTENTS_REGISTER_IMG_TAG_ERROR);
                }

                // 컨텐츠 idx & 등록일 set
                map.put("contentsIdx", contentsDto.getInsertedIdx());
                map.put("regDate", dateLibrary.getDatetime());

                // width & height
                map.put("width", tagList.getWidth());
                map.put("height", tagList.getHeight());

                // memberIdx
                map.put("memberUuid", tagList.getMemberUuid());

                // 해당 태그가 몇번째 사진에 있는 태그인지 가져오기
                Long imgIdx = tagList.getImgIdx();

                // 이미지 insertedIdx set
                map.put("imgIdx", imgInsertedIdxList.get(Math.toIntExact(imgIdx)));
                imgTagList.add(map);
            }

            /** 이미지 회원 태그 매핑 insert [sns_img_member_tag_mapping] */
            insertImgTag(imgTagList);
        }
    }

    /**
     * 이미지 내 회원태그 등록하기
     *
     * @param imgTagList imgIdx, memberIdx, width, height
     */
    public void insertImgTag(List<Map<String, Object>> imgTagList) {
        Integer iResult = contentsDao.insertImgTag(imgTagList);
        if (iResult < 1) {
            throw new CustomException(CustomError.CONTENTS_REGISTER_MEMBER_TAG_ERROR); // 회원 태그 등록에 실패하였습니다.
        }
    }

    /**
     * 위치정보 등록하기
     *
     * @param contentsDto location
     */
    public void insertLocation(ContentsDto contentsDto) {
        Integer iResult = contentsDao.insertLocation(contentsDto);
        if (iResult < 1) {
            throw new CustomException(CustomError.CONTENTS_REGISTER_LOCATION_ERROR); // 위치 등록에 실패하였습니다.
        }
    }

    /**
     * 위치정보 매핑 등록하기
     *
     * @param contentsDto insertedIdx, locationIdx, regDate
     */
    public void insertLocationMapping(ContentsDto contentsDto) {
        Integer iResult = contentsDao.insertLocationMapping(contentsDto);
        if (iResult < 1) {
            throw new CustomException(CustomError.CONTENTS_REGISTER_LOCATION_MAPPING_ERROR); // 위치 매핑 등록에 실패하였습니다.
        }
    }

    /**
     * 콘텐츠 좋아요 cnt 등록
     *
     * @param contentsDto insertedIdx, regDate
     */
    public void insertContentsLikeCnt(ContentsDto contentsDto) {
        // 콘텐츠 idx set
        ContentsLikeDto contentsLikeDto = new ContentsLikeDto();
        contentsLikeDto.setContentsIdx(contentsDto.getInsertedIdx());
        contentsLikeDao.insertContentsLikeCnt(contentsLikeDto);
    }

    /**
     * 콘텐츠 댓글 cnt 등록
     *
     * @param contentsDto insertedIdx, regDate
     */
    public void insertContentsCommentCnt(ContentsDto contentsDto) {
        // 콘텐츠 idx set
        CommentDto commentDto = new CommentDto();
        commentDto.setContentsIdx(contentsDto.getInsertedIdx());
        commentDao.insertCommentCnt(commentDto);
    }

    /**
     * 콘텐츠 저장 cnt 등록
     *
     * @param contentsDto insertedIdx, regDate
     */
    public void insertContentsSaveCnt(ContentsDto contentsDto) {
        // 콘텐츠 idx set
        ContentsSaveDto contentsSaveDto = new ContentsSaveDto();
        contentsSaveDto.setContentsIdx(contentsDto.getInsertedIdx());
        contentsSaveDao.insertContentsSaveCnt(contentsSaveDto);
    }

    /*****************************************************
     *  SubFunction - Update
     ****************************************************/
    /**
     * 내용 업데이트
     *
     * @param contentsDto insertedIdx 업데이트 할 컨텐츠 idx
     * @return
     */
    public Long updateContentsContents(ContentsDto contentsDto) {
        if (contentsDto.getIdx() == null || contentsDto.getIdx() < 0) {
            // 업데이트 IDX 오류
            throw new CustomException(CustomError.CONTENTS_UPDATE_CONTENTS_IDX_ERROR);
        }
        Long iResult = contentsDao.updateContentsContents(contentsDto);
        if (iResult < 1) {
            // 내용 업데이트 실패하였습니다.
            throw new CustomException(CustomError.CONTENTS_UPDATE_CONTENTS_ERROR);
        }
        return iResult;
    }

    /**
     * 이미지 내 회원태그 수정하기
     *
     * @param imgTagList imgIdx, memberIdx, width, height
     */
    public void modifyImgTag(List<Map<String, Object>> imgTagList) {
        Integer iResult = contentsDao.modifyImgTag(imgTagList);
        if (iResult < 1) {
            // 회원 이미지 태그 수정에 실패하였습니다.
            throw new CustomException(CustomError.CONTENTS_MODI_IMG_TAG_MEMBER_ERROR);
        }
    }

    /*****************************************************
     *  SubFunction - Delete
     ****************************************************/
    /**
     * 콘텐츠 삭제
     *
     * @param contentsDto 콘텐츠 idx
     */
    public Long deleteContent(ContentsDto contentsDto) {
        return contentsDao.delete(contentsDto);
    }

    /**
     * 이미지 내 회원태그 삭제
     *
     * @param imgTagList imgIdx, memberIdx, width, height
     */
    public void deleteImgTag(List<Map<String, Object>> imgTagList) {
        Integer iResult = contentsDao.deleteImgTag(imgTagList);
        if (iResult < 1) {
            // 회원 이미지 태그 삭제에 실패하였습니다.
            throw new CustomException(CustomError.CONTENTS_DELETE_IMG_TAG_MEMBER_ERROR);
        }
    }

    /**
     * 컨텐츠 위치 정보 삭제
     *
     * @param contentsDto idx
     */
    public void deleteLocation(ContentsDto contentsDto) {
        Integer iResult = contentsDao.deleteLocation(contentsDto);
        if (iResult < 1) {
            // 위치정보 삭제 실패
            throw new CustomException(CustomError.CONTENTS_DELETE_LOCATION_ERROR);
        }
    }

    /*****************************************************
     *  SubFunction - Etc
     ****************************************************/

    /**
     * 컨텐츠 작성일 시간 변환
     *
     * @param contentsDtoList
     */
    private void setListRegDateToTime(List<ContentsDto> contentsDtoList) {
        // 작성일 시간 변환
        for (ContentsDto contentsDto : contentsDtoList) {
            String regDate = contentsDto.getRegDate();
            String regDateTz = contentsDto.getRegDateTz();
            // 등록일 초,분,시간 으로 변경
            String convertedTime = dateLibrary.getConvertDateToTime(regDate, regDateTz);

            contentsDto.setRegDate(convertedTime); // 변환된 타임 set
            contentsDto.setRegDateTz(null);
        }
    }

    /**
     * 컨텐츠 작성일 시간 변환
     *
     * @param contentsDto
     */
    private void setRegDateToTime(ContentsDto contentsDto) {

        // 작성일 시간 변환
        String regDate = contentsDto.getRegDate();
        String regDateTz = contentsDto.getRegDateTz();
        // 등록일 초,분,시간 으로 변경
        String convertedTime = dateLibrary.getConvertDateToTime(regDate, regDateTz);

        contentsDto.setRegDate(convertedTime); // 변환된 타임 set
        contentsDto.setRegDateTz(null);
    }

    /**
     * 컨텐츠 금칙어 치환
     * [컨텐츠 내용, 댓글 내용, 이미지 내 회원 인트로]
     *
     * @param contentsDto
     */
    private void convertBadWord(ContentsDto contentsDto, List<ContentsWordCheckDto> contentsBadWordList) {

        if (!ObjectUtils.isEmpty(contentsDto)) {

            /** 컨텐츠 내용 금칙어 치환 **/
            String contents = contentsWordcheckService.contentsWordCheck(contentsDto.getContents(), contentsBadWordList);
            contentsDto.setContents(contents);

            /** 댓글 내용 금칙어 치환 [좋아요 많은 댓글 하나] **/
            CommentDto commentDto = contentsDto.getComment();

            if (!ObjectUtils.isEmpty(commentDto)) { // 댓글 리스트 존재
                // 댓글 내용 금칙어 변환
                String commentContents = contentsWordcheckService.contentsWordCheck(commentDto.getContents(), contentsBadWordList);
                commentDto.setContents(commentContents);
            } // end of if (댓글)
        }
    }

    /**
     * 컨텐츠 리스트 금칙어 치환
     *
     * @param contentsDtoList 콘텐츠 리스트
     */
    private void convertBadWordInList(List<ContentsDto> contentsDtoList) {
        List<ContentsWordCheckDto> contentsBadWordList = contentsWordcheckService.getList(contentsWordChk); // 콘텐츠 금칙어 리스트 조회

        if (!ObjectUtils.isEmpty(contentsDtoList)) {
            for (ContentsDto contentsDto : contentsDtoList) {
                // 금칙어 치환
                convertBadWord(contentsDto, contentsBadWordList);
            }
        }
    }

    /*****************************************************************
     * Validation
     *****************************************************************/
    /**
     * 콘텐츠 등록 시 유효성 검사
     *
     * @param contentsDto menuIdx, isView, contents, uploadFile, imgTagList
     */
    public void registValidate(ContentsDto contentsDto) {
        // data
        Integer menuIdx = contentsDto.getMenuIdx();
        Integer isView = contentsDto.getIsView();
        String contents = contentsDto.getContents();
        List<MultipartFile> uploadFileList = contentsDto.getUploadFile();
        List<ContentsImgMemberTagDto> imgTagList = contentsDto.getImgTagList();

        /** menuIdx */
        if (menuIdx == null || menuIdx < 1 || menuIdx > 2) { // 1: 소셜 , 2: 산책
            throw new CustomException(CustomError.CONTENTS_MENUIDX_EMPTY);
        }

        /** isView */
        if (isView == null || isView < 0 || isView > 2) { // 0: 비공개 , 1: 전체공개, 2: 팔로우만 공개
            throw new CustomException(CustomError.CONTENTS_ISVIEW_EMPTY);
        }

        /** contents */
        BreakIterator it = BreakIterator.getCharacterInstance();
        it.setText(contents);
        int contentsLength = 0;

        while (it.next() != BreakIterator.DONE) {
            contentsLength++;
        }
        // 내용이 500자 이상일때
        if (contents != null && contentsLength > textMax) {
            throw new CustomException(CustomError.CONTENTS_TEXT_LIMIT_ERROR);
        }

        /** uploadFile */
        // 이미지가 없을때
        if (uploadFileList == null) {
            throw new CustomException(CustomError.CONTENTS_IMAGE_EMPTY);
        }
        // 이미지 개수가 12개 이상일때
        else if (uploadFileList.size() > imgMax) {
            throw new CustomException(CustomError.CONTENTS_IMAGE_LIMIT_ERROR);
        }
        // null은 아니지만 사진이 없을때 > 에러
        else {
            int isNotEmptyCnt = 0;
            for (MultipartFile img : uploadFileList) {
                if (!img.isEmpty()) {
                    isNotEmptyCnt += 1;
                }
            }
            if (isNotEmptyCnt < 1) {
                throw new CustomException(CustomError.CONTENTS_IMAGE_EMPTY);
            }
        }

        if (menuIdx == sns) { // 소셜 콘텐츠인 경우 (menu : 1)
            /** imgTagList */
            // 한 사진에 태그를 10명 이상 했는지 검증
            List<Long> indexList = new ArrayList<>();

            if (imgTagList != null) {
                for (ContentsImgMemberTagDto tagList : imgTagList) {
                    // 해당 태그의 이미지의 번호 List에 저장
                    indexList.add(tagList.getImgIdx());
                }

                // List에 있는 값 중복 제거하기 위해 set 으로 저장
                Set<Long> set = new HashSet<>(indexList);
                for (Long index : set) {
                    // 중복된 횟수가 10번 이상이라면
                    if (Collections.frequency(indexList, index) > 10) {
                        throw new CustomException(CustomError.CONTENTS_IMAGE_MEMBER_TAG_LIMIT_ERROR);
                    }
                }
            }
        }

    }

    /**
     * 콘텐츠 수정 시 유효성 검사
     *
     * @param contentsDto menuIdx, isView, contents, uploadFile, imgTagList
     */
    public void modifyValidate(ContentsDto contentsDto) {
        // data
        Integer menuIdx = contentsDto.getMenuIdx();
        Integer isView = contentsDto.getIsView();
        String contents = contentsDto.getContents();
        List<ContentsImgMemberTagDto> imgTagList = contentsDto.getImgTagList();

        // 내가 작성한 콘텐츠 인지 검증
        Boolean bMyContents = checkMyContents(contentsDto);
        // 내가 작성한 콘텐츠가 아니라면
        if (Boolean.FALSE.equals(bMyContents)) {
            // 내 작성글이 아닙니다.
            throw new CustomException(CustomError.CONTENTS_NOT_MY_POST_ERROR);
        }

        // 소셜/산책글이 맞는지 검증
        Boolean bTypeResult = checkContentsMenu(contentsDto);
        if (Boolean.FALSE.equals(bTypeResult)) {
            if (menuIdx == sns) {
                // sns 글이 아닙니다.
                throw new CustomException(CustomError.CONTENTS_SNS_TYPE_ERROR);
            }
        }

        /** menuIdx */
        if (menuIdx == null || menuIdx < 1 || menuIdx > 2) { // 1: 소셜 , 2: 산책
            throw new CustomException(CustomError.CONTENTS_MENUIDX_EMPTY);
        }

        /** isView */
        if (isView == null || isView < 0 || isView > 2) { // 0: 비공개, 1: 전체공개, 2: 팔로우만 공개
            throw new CustomException(CustomError.CONTENTS_ISVIEW_EMPTY);
        }

        /** contents */
        // 내용이 500자 이상일때
        BreakIterator it = BreakIterator.getCharacterInstance();
        it.setText(contents);
        int contentsLength = 0;

        while (it.next() != BreakIterator.DONE) {
            contentsLength++;
        }
        if (contentsLength > textMax) {
            throw new CustomException(CustomError.CONTENTS_TEXT_LIMIT_ERROR);
        }

        if (menuIdx == sns) { // 소셜 콘텐츠인 경우 (menu : 1)
            /** imgTagList */
            // 한 사진에 태그를 10명 이상 했는지 검증
            List<Long> indexList = new ArrayList<>();

            if (imgTagList != null) {
                for (ContentsImgMemberTagDto tagList : imgTagList) {
                    // 해당 태그의 이미지의 번호 List에 저장
                    indexList.add(tagList.getImgIdx());
                }

                // List에 있는 값 중복 제거하기 위해 set 으로 저장
                Set<Long> set = new HashSet<>(indexList);
                for (Long index : set) {
                    // 중복된 횟수가 10번 이상이라면
                    if (Collections.frequency(indexList, index) > 10) {
                        throw new CustomException(CustomError.CONTENTS_IMAGE_MEMBER_TAG_LIMIT_ERROR);
                    }
                }
            }
        }

    }

    /**
     * 회원 컨텐츠 상세 리스트 유효성
     *
     * @param searchDto
     */
    private void detailListValidate(SearchDto searchDto) {

        // 로그인 중일 경우만 체크
        if (searchDto.getLoginMemberUuid() != null) {

            String loginMemberUuid = searchDto.getLoginMemberUuid();
            String memberUuid = searchDto.getMemberUuid();

            // 상대방 페이지 조회 시
            if (!loginMemberUuid.equals(memberUuid)) {
                // 차단했는지 체크
                blockValidate(searchDto);
            }
        }
        // 컨텐츠 상세 공통 유효성
        commonImgLimitValidate(searchDto);

    }

    /**
     * 컨텐츠 상세 유효성
     *
     * @param searchDto : loginMemberIdx[로그인 회원 idx], contentsIdx[컨텐츠 idx]
     */
    private void contentsDetailValidate(SearchDto searchDto) {
        Long contentsIdx = searchDto.getContentsIdx();
        String loginMemberUuid = searchDto.getLoginMemberUuid(); // 로그인 회원 uuid

        // 상세 공통 유효성
        commonImgLimitValidate(searchDto);
        // 컨텐츠 idx 유효성
        contentsIdxValidate(contentsIdx);
        // 컨텐츠 작성자 uuid 조회
        String writerUuid = contentsDaoSub.getMemberUuidByIdx(contentsIdx);
        searchDto.setMemberUuid(writerUuid); // 작성자 uuid set

        // 보관한 게시물인지 체크, 로그인 관계 없이 조회
        keepValidate(searchDto.getContentsIdx());

        // 로그인 중일 경우만 체크
        if (loginMemberUuid != null) {
            // 작성자가 내가 아닌 다른 사람이라면
            if (!writerUuid.equals(loginMemberUuid)) {
                searchDto.setMemberUuid(writerUuid); // 작성자 idx set
                // 차단했는지 체크
                blockValidate(searchDto);
                // 숨긴 컨텐츠인지 체크
                hideValidate(contentsIdx, loginMemberUuid);
                // 신고한 컨텐츠인지 체크
                reportContentsValidate(searchDto);
                // 팔로우 게시물인지 체크
                followContentsValidate(searchDto);
            } // end of if
            // 비로그인 상태
        } else {
            // 팔로우 게시물인지 체크
            int isView = contentsDaoSub.getIsViewByIdx(searchDto.getContentsIdx());

            if (isView == ONLY_FOLLOW_VIEW) {
                throw new CustomException(CustomError.CONTENTS_NEED_LOGIN_ONLY_FOLLOW); // 팔로우 공개 게시물이므로 로그인 후 이용해주세요.
            }
        }

    }

    /**
     * 내가 작성한 컨텐츠 상세 일상글 유효성
     *
     * @param searchDto
     */
    private void myNormalContentsDetailValidate(SearchDto searchDto) {

        // 컨텐츠 idx 유효성 검사
        contentsIdxValidate(searchDto.getContentsIdx());

        // keep 여부 조회
        int isKeep = contentsDaoSub.getIsKeepByIdx(searchDto.getContentsIdx());

        if (isKeep == 1) {
            throw new CustomException(CustomError.CONTENTS_KEEP_ERROR); // 보관한 게시물입니다.
        }
        // 작성자 idx
        String writerUuid = contentsDaoSub.getMemberUuidByIdx(searchDto.getContentsIdx());

        // 작성자가 다른 경우
        if (!Objects.equals(writerUuid, searchDto.getLoginMemberUuid())) {
            throw new CustomException(CustomError.CONTENTS_NOT_MY_POST_ERROR); // 내 작성글이 아닙니다.
        }
    }

    /**
     * 내가 작성한 컨텐츠 상세 보관글 유효성
     *
     * @param searchDto
     */
    private void myKeepContentsDetailValidate(SearchDto searchDto) {

        // 컨텐츠 idx 유효성 검사
        contentsIdxValidate(searchDto.getContentsIdx());

        // keep 여부 조회
        int isKeep = contentsDaoSub.getIsKeepByIdx(searchDto.getContentsIdx());

        if (isKeep != 1) {
            throw new CustomException(CustomError.CONTENTS_NOT_KEEP_ERROR); // 보관한 게시물이 아닙니다.
        }

        // 작성자 idx
        String writerUuid = contentsDaoSub.getMemberUuidByIdx(searchDto.getContentsIdx());

        // 작성자가 다른 경우
        if (!Objects.equals(writerUuid, searchDto.getLoginMemberUuid())) {
            throw new CustomException(CustomError.CONTENTS_NOT_MY_POST_ERROR); // 내 작성글이 아닙니다.
        }
    }

    /**
     * 팔로우 공개 게시물 유효성
     *
     * @param contentsIdx : contentsIdx[컨텐츠 idx]
     */
    public void followContentsValidate(Long contentsIdx) {

        // 컨텐츠 is_view 조회
        int isView = contentsDaoSub.getIsViewByIdx(contentsIdx);

        if (isView == ONLY_FOLLOW_VIEW) {
            // contents get memberUuid
            ContentsDto contentsDto = contentsDaoSub.getContentsInfo(contentsIdx);
            // 초기화
            Map<String, Object> map = new HashMap<>();
            // null이 아니라면 로직 실행
            if (contentsDto != null) {
                // 컨텐츠 작성자 정보
                MemberInfoDto writerInfoDto = contentsCurlService.getWriterInfo(contentsDto.getMemberUuid());

                map.put("memberUuid", contentsDto.getMemberUuid());
                map.put("nick", writerInfoDto.getNick());
            }

            JSONObject data = new JSONObject(map);
            throw new CustomException(CustomError.CONTENTS_ONLY_FOLLOW_VIEW_ERROR, data); // 팔로우 후 게시물 확인이 가능합니다.
        }
    }

    /**
     * 팔로우 공개 게시물 유효성
     *
     * @param searchDto : loginMemberUuid[로그인 회원 uuid], memberUuid[작성자 uuid], contentsIdx[컨텐츠 idx]
     */
    public void followContentsValidate(SearchDto searchDto) {

        // 컨텐츠 is_view 조회
        int isView = contentsDaoSub.getIsViewByIdx(searchDto.getContentsIdx());

        // 팔로우만 공개 게시물
        if (isView == ONLY_FOLLOW_VIEW) {

            FollowDto followDto = FollowDto.builder()
                    .memberUuid(searchDto.getLoginMemberUuid())
                    .followUuid(searchDto.getMemberUuid())
                    .state(1).build();

            // 팔로우 여부
            int followCnt = followDaoSub.getCntCheck(followDto);

            followDto.setMemberUuid(searchDto.getMemberUuid());
            followDto.setFollowUuid(searchDto.getLoginMemberUuid());

            // 팔로워 여부
            int followerCnt = followDaoSub.getCntCheck(followDto);

            if (followCnt != 1 && followerCnt != 1) {

                // contents get memberUuid
                ContentsDto contentsDto = contentsDaoSub.getContentsInfo(searchDto.getContentsIdx());
                // 초기화
                Map<String, Object> map = new HashMap<>();
                // null이 아니라면 로직 실행
                if (contentsDto != null) {
                    // 컨텐츠 작성자 정보
                    MemberInfoDto writerInfoDto = contentsCurlService.getWriterInfo(contentsDto.getMemberUuid());

                    map.put("memberUuid", contentsDto.getMemberUuid());
                    map.put("nick", writerInfoDto.getNick());
                }

                JSONObject data = new JSONObject(map);
                throw new CustomException(CustomError.CONTENTS_ONLY_FOLLOW_VIEW_ERROR, data); // 팔로우 후 게시물 확인이 가능합니다.
            }
        }
    }

    /**
     * 이미지 리밋 관련 유효성 검사
     *
     * @param searchDto
     */
    private void commonImgLimitValidate(SearchDto searchDto) {

        // 이미지 Limit 체크
        if (searchDto.getImgLimit() == null) {
            throw new CustomException(CustomError.CONTENTS_IMAGE_LIMIT_EMPTY); // imgLimit을 입력해주세요.
        }

        // 이미지 offSet 체크
        if (searchDto.getImgOffSet() == null) {
            throw new CustomException(CustomError.CONTENTS_IMAGE_OFFSET_EMPTY); // imgOffset을 입력해주세요.
        }
    }

    /**
     * 컨텐츠 idx 유효성
     *
     * @param idx
     */
    public void contentsIdxValidate(Long idx) {

        // 컨텐츠 idx 검증
        if (idx == null || idx < 1) {
            throw new CustomException(CustomError.CONTENTS_IDX_NULL); // 컨텐츠 IDX가 비었습니다.
        }

        int contentsCnt = contentsDaoSub.getContentsCntByIdx(idx);

        if (contentsCnt == 0) {
            throw new CustomException(CustomError.CONTENTS_IDX_ERROR); // 존재하지 않는 콘텐츠입니다.
        }
    }

    /**
     * 차단 했는지 유효성
     *
     * @param searchDto
     */
    private void blockValidate(SearchDto searchDto) {

        Boolean isBlock = super.bChkBlock(searchDto.getLoginMemberUuid(), searchDto.getMemberUuid());

        // 차단 내역이 존재
        if (Boolean.TRUE.equals(isBlock)) {
            throw new CustomException(CustomError.BLOCK_MEMBER_EXIST); // 차단 내역이 존재합니다.
        }
    }

    /**
     * 숨김 게시물 유효성
     *
     * @param idx
     * @param loginMemberUuid
     */
    public void hideValidate(Long idx, String loginMemberUuid) {

        SearchDto searchDto = new SearchDto();
        searchDto.setContentsIdx(idx);               // 컨텐츠 idx
        searchDto.setLoginMemberUuid(loginMemberUuid); // 로그인 회원 idx

        int hideCnt = contentsDaoSub.getContentsHideCnt(searchDto);

        if (hideCnt > 0) {
            throw new CustomException(CustomError.CONTENTS_HIDE_ERROR); // 숨김 처리된 게시물입니다.
        }
    }

    /**
     * 보관 여부 유효성
     *
     * @param idx
     */
    public void keepValidate(Long idx) {

        // keep 여부 조회
        int isKeep = contentsDaoSub.getIsKeepByIdx(idx);

        if (isKeep == 1) {
            throw new CustomException(CustomError.CONTENTS_KEEP_ERROR); // 보관한 게시물입니다.
        }
    }

    /**
     * 보관 여부 유효성
     *
     * @param searchDto : loginMemberUuid[로그인 회원 idx], memberIdx[작성자 idx], contentsIdx[컨텐츠 idx]
     */
    public void keepValidate(SearchDto searchDto) {

        String loginMemberUuid = searchDto.getLoginMemberUuid(); // 로그인 회원 idx
        String writerUuid = searchDto.getMemberUuid();           // 작성자 idx
        Long idx = searchDto.getContentsIdx();

        if (!loginMemberUuid.equals(writerUuid)) { // 작성자가 본이이 아닌 경우
            // keep 여부 조회
            int isKeep = contentsDaoSub.getIsKeepByIdx(idx);

            if (isKeep == 1) {
                throw new CustomException(CustomError.CONTENTS_KEEP_ERROR); // 보관한 게시물입니다.
            }
        }
    }

    /**
     * 컨텐츠 신고했는지 유효성
     *
     * @param searchDto : loginMemberUuid , contentsIdx
     */
    public void reportContentsValidate(SearchDto searchDto) {
        int reportCnt = contentsDaoSub.getContentsReportCnt(searchDto);

        if (reportCnt > 0) {
            throw new CustomException(CustomError.CONTENTS_REPORT_ERROR); // 신고한 게시물입니다.
        }
    }

    /**
     * 컨텐츠 공통 유효성 검사
     * [ idx, 숨김, 신고, 작성자와의 차단 관계, 팔로우 공개, 보관 게시물 검증]
     *
     * @param searchDto : loginMemberUuid [로그인 회원 uuid], contentsIdx [컨텐츠 idx]
     */
    public void commonContentsValidate(SearchDto searchDto) {

        String loginMemberUuid = searchDto.getLoginMemberUuid();
        Long contentsIdx = searchDto.getContentsIdx();

        /** 컨텐츠 idx 검증 **/
        contentsIdxValidate(contentsIdx);

        // 로그인 중
        if (loginMemberUuid != null) {
            /** 컨텐츠 작성한 회원과 차단한 관계인지 검증 **/
            String writerUuid = contentsDaoSub.getMemberUuidByIdx(contentsIdx); // 컨텐츠 작성자 idx 조회
            blockMemberService.writerAndMemberBlockValidate(writerUuid, loginMemberUuid);

            // 로그인 회원과 컨텐츠 작성자 회원이 같지 않으면 [내가 작성한 게시물이 아닌 경우만 검증]
            if (!Objects.equals(writerUuid, loginMemberUuid)) {
                // 신고 조회용 dto
                SearchDto reportSearchDto = new SearchDto();
                reportSearchDto.setLoginMemberUuid(loginMemberUuid);  // 로그인 회원 idx
                reportSearchDto.setContentsIdx(contentsIdx);          // 컨텐츠 idx

                // 팔로우 & 보관 조회용 dto
                SearchDto searchFollowAndKeep = new SearchDto();
                searchFollowAndKeep.setLoginMemberUuid(loginMemberUuid);  // 로그인 회원 idx
                searchFollowAndKeep.setMemberUuid(writerUuid);            // 컨텐츠 작성자 idx
                searchFollowAndKeep.setContentsIdx(contentsIdx);          // 컨텐츠 idx

                /** 숨김 처리한 게시물 검증 **/
                hideValidate(contentsIdx, loginMemberUuid);
                /** 게시물 신고 검증 **/
                reportContentsValidate(reportSearchDto);
                /** 팔로우 공개 게시물 검증 **/
                followContentsValidate(searchFollowAndKeep);
                /** 보관 게시물인지 검증 **/
                keepValidate(searchFollowAndKeep);
            }
        } else { // 비 로그인
            /** 팔로우 공개 게시물 검증 **/
            followContentsValidate(contentsIdx);
            /** 보관 게시물인지 검증 **/
            keepValidate(contentsIdx);
        }
    }

}
