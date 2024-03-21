package com.architecture.admin.models.dto.comment;

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
public class CommentLikeDto {
    /**
     * sns_contents_comment_like
     */
    private Long idx;
    private Long commentIdx;
    private Integer state;
    private String regDate;
    private String regDateTz;
    private String memberUuid;
    private String receiverUuid;

    /**
     * sns_contents_comment_like_cnt
     */
    private Long likeCnt;

    private String nick;
    private String intro;
    private String img;

    // common data
    private Long contentsIdx;
    private String contents;


    // sql
    private Long insertedIdx;

}
