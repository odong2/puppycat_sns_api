package com.architecture.admin.models.dto.block;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlockMemberDto {
    /**
     * sns_member_block
     */
    private Long idx; // IDX
    private String memberUuid;  // 회원 uuid
    private String blockUuid;   // 차단 회원 uuid
    private Integer state; // 상태값
    private String regDate; // 등록일
    private String regDateTz; // 등록일 타임존

    // sql
    private Integer insertedIdx;  // insert idx
    private Integer affectedRow; // 처리 row

}
