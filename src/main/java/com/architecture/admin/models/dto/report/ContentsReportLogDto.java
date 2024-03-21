package com.architecture.admin.models.dto.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContentsReportLogDto {
    /**
     * sns_contents
     */
    private Long idx; // 일련번호
    private Long contentsReportIdx; // sns_contents.idx
    private Long contentsIdx; // sns_contents.idx
    private String uuid; // 컨텐츠 고유아이디
    private String title; // 제목
    private String memberUuid; // 회원 uuid
    private Integer menuIdx; // 컨텐츠 메뉴 idx
    private String contents; // 내용
    private Integer imageCnt; // 이미지 cnt
    private Integer state; // 상태값 [0:삭제 1:정상]
    private Integer isComment; // 댓글 활성화 상태값 [0:비활성화 1:활성화]
    private Integer isLike; // 좋아요 활성화 상태값 [0:비활성화 1:활성화]
    private Integer isView; // 공개상태 상태값 [1:전체 2:팔로우만]
    private Integer isKeep; // 보관상태 상태값 [0:보관해제 1:보관]
    private String contentsModiDate; // 컨텐츠 수정일
    private String contentsModiDateTz; // 컨텐츠 수정일 타임존
    private String contentsRegDate; // 컨텐츠 등록일
    private String contentsRegDateTz; // 컨텐츠 등록일 타임존
    private String regDate; // 등록일
    private String regDateTz; // 등록일 타임존

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
}
