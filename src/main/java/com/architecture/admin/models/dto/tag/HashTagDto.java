package com.architecture.admin.models.dto.tag;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HashTagDto {
    /**
     * sns_hash_tag
     */
    private Long idx;           // 일련번호
    private String hashTagContentsCnt;    // 해시태그 사용 cnt
    private String hashTag;     // 해시태그

    /**
     * sns_contents_hash_tag_mapping
     */
    private Long contentsIdx;    // 컨텐츠idx
    private Long hashTagIdx;     // 해시태그 idx
    private Integer state;       // 상태값
    private String regDate;      // 등록일
    private String regDateTz;    // 등록일 타임존

    /**
     * sns_contents_comment_hash_tag_mapping
     */
    private Long commentIdx;    // 댓글idx

    /**
     * sns_img_member_tag_mapping
     */
    private Double width;      // 가로 위치
    private Double height;     // 세로 위치
    private Long imgIdx;        // 이미지 idx

    // etc
    private String type;        // 컨텐츠인지 댓글인지
    private String contents;    // 컨텐츠,댓글 내용
    private Integer followState;
    private String url;
    private String imgUrl;
    private Integer imageCnt;

    // sql
    private Integer insertedIdx;
    private Integer affectedRow;

}
