package com.architecture.admin.models.dto.comment;

import com.architecture.admin.models.dto.tag.MentionTagDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentDto {
    /**
     * sns_contents_comment
     */
    private Long idx;           // 일련번호
    private Long contentsIdx;   // sns_contents.idx
    private Long parentIdx;     // 부모 idx
    private String uuid;        // 고유아이디
    private String memberUuid;  // 회원 고유아이디
    private String nick;        // 닉네임
    private Integer isBadge;    // 뱃지여부
    private String contents;    // 내용
    private Integer state;      // 상태값
    private String modiDate;    // 수정일
    private String modiDateTz;  // 수정일 타임존
    private String regDate;     // 등록일
    private String regDateTz;   // 등록일 타임존
    private int likeState;      // 내가 좋아요 눌렀는지 여부

    /**
     * memberInfo
     */
    private String profileImgUrl; // 프로필 이미지 url
    private String intro;         // 프로필 인트로

    // sql
    private Long insertedIdx;    // insert idx
    private Integer affectedRow; // 처리 row수
    private Long childCommentCnt; //대댓글 수
    private Long contentsLikeCnt; //컨텐츠 좋아요 수
    private Long commentLikeCnt; //댓글 좋아요 수
    private LinkedHashMap childCommentData; // 대댓글
    private String url; // 프로필 이미지
    private Long totalCommentCnt;
    private Long rowNum;

    private List<MentionTagDto> mentionList;

}
