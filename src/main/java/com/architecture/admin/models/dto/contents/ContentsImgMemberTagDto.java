package com.architecture.admin.models.dto.contents;

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
public class ContentsImgMemberTagDto {
    /**
     * sns_img_member_tag_mapping
     */
    private Long idx; // 일련번호
    private Long contentsIdx; // sns_contents.idx
    private Long imgIdx; // sns_contents_img.idx
    private String memberUuid; // sns_member.uuid
    private Double width; // 태그 x 좌표
    private Double height; // 태그 y 좌표
    private Integer state; // 상태값
    private String regDate; // 등록일(UTC)
    private String regDateTz; // 등록일 타임존

    private String status;  // 수정 시 상태값 ( new modi del )
    /**
     * sns_member
     */
    private String nick;

    /**
     * profile
     */
    private String profileImgUrl;
    private String intro;
    // etc
    private Integer followState; // 팔로우 상태
    private Integer isBadge;     // 배지 유무

}
