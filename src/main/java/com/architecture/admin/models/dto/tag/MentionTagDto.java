package com.architecture.admin.models.dto.tag;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MentionTagDto {
    /**
     * sns_member_mention
     */
    private Long idx;           // 일련번호
    private String memberUuid;     // 멘션된 회원 idx
    private Long mentionCnt;    // 멘션 사용 cnt

    /**
     * sns_contents_mention_mapping
     */
    private Long contentsIdx;    // 컨텐츠idx
    private Long mentionIdx;     // 멘션 idx
    private Integer state;       // 상태값
    private String regDate;      // 등록일
    private String regDateTz;    // 등록일 타임존

    /**
     * sns_contents_comment_mention_mapping
     */
    private Long commentIdx;    // 댓글idx

    // etc
    private String type;                    // 컨텐츠인지 댓글인지
    private String contents;                // 컨텐츠,댓글 내용
    private List<Long> mentionMemberList;   // 멘션 회원 리스트
    private String uuid;                    // 멘션된 회원 uuid
    private String nick;                    // 멘션된 회원 닉네임
    private Long outMemberIdx;              // 멘션된 탈퇴 회원 idx
    private String outUuid;                 // 멘션된 탈퇴 회원 uuid
    private String outNick;                 // 멘션된 탈퇴 회원 닉네임
    private Integer memberState;            // 멘션된 회원 상태값

    // sql
    private Long insertedIdx;
    private Integer affectedRow;
}
