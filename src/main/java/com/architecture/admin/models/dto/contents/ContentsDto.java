package com.architecture.admin.models.dto.contents;

import com.architecture.admin.models.dto.comment.CommentDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;
import com.architecture.admin.models.dto.tag.MentionTagDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Size;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContentsDto {
    /**
     * sns_contents
     */
    private Long idx; // 일련번호
    private String uuid; // 컨텐츠 고유아이디
    private Integer menuIdx; // 컨텐츠 메뉴 idx
    private String contents; // 내용
    private Integer imageCnt; // 이미지 cnt
    private Integer state; // 상태값 [0:삭제 1:정상]
    private Integer isComment; // 댓글 활성화 상태값 [0:비활성화 1:활성화]
    private Integer isLike; // 좋아요 활성화 상태값 [0:비활성화 1:활성화]
    private Integer isView; // 공개상태 상태값 [1:전체 2:팔로우만]
    private Integer isKeep; // 보관상태 상태값 [0:보관해제 1:보관]
    private String modiDate; // 수정일
    private String modiDateTz; // 수정일 타임존
    private String regDate; // 등록일
    private String regDateTz; // 등록일 타임존

    private Integer selfLike; // 본인 게시글 좋아요 상태값 [0:안 누름 1:누름]

    // sql
    private Long insertedIdx;  // insert idx
    private Integer affectedRow; // 처리 row수

    @Size(min = 1,max = 12)
    private List<MultipartFile> uploadFile;    // 업로드 이미지
    private List<ContentsImgMemberTagDto> imgTagList;     // 이미지 내 태그된 회원 List
    private Long imgIdx; // sns_contents_img.idx
    private String imgUrl; // url (도메인 제외)
    private List<Long> idxList; // 콘텐츠 idx 리스트

    /**
     * sns_hash_tag
     */
    private Long hashTagIdx; // sns_hash_tag.idx
    private String hashTag; // 해시태그

    /**
     * sns_contents_location
     */
    private Long locationIdx; // sns_contents_location.idx
    private String location; // 위치정보

    /**
     * sns_contents_like_cnt
     */
    private Long likeCnt;   // 좋아요 개수

    /**
     * sns_contents_comment_cnt
     */
    private Long commentCnt;    // 댓글 개수

    /**
     * sns_contents_save_cnt
     */
    private Long saveCnt;       // 저장 개수

    /**
     * sns_member
     */
    private String memberUuid;

    /**
     * 기타
     */
    private Integer followState; // 팔로우 상태
    private Integer likeState;   // 좋아요 상태
    private Integer saveState;   // 저장 상태
    private Integer modifyState; // 수정 상태
    private Integer keepState;    // 보관 상태
    private CommentDto comment;  // 댓글 정보
    private MemberInfoDto memberInfo;            // 회원정보
    private List<CommentDto>  commentList;       // 댓글 리스트
    private List<MemberInfoDto> memberInfoList;  // 회원 정보 리스트
    private List<ContentsImgDto> imgList;        // 이미지 리스트
    private List<MentionTagDto> mentionList;     // 멘션 리스트
}
