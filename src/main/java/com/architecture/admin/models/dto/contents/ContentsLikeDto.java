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
public class ContentsLikeDto {
    /**
     * sns_contents_like
     */
    private Long idx;           // 일련번호
    private Long contentsIdx;   // 콘텐츠 idx
    private Integer state;      // 상태값
    private String memberUuid;  // 회원 uuid
    private String regDate;     // 등록일
    private String regDateTz;   // 등록일 타임존

    /**
     * sns_contents_like_cnt
     */
    private Long likeCnt;

    /**
     * sns_follow_contents_like_cnt
     */
    private String followUuid;     //sns_member_follow.member_uuid
    private Long followIdx;

    private String nick;
    private String intro;
    private String img;

    // 기타
    private String loginMemberUuid;

}
