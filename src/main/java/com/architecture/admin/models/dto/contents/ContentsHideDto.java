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
public class ContentsHideDto {
    /**
     * sns_contents_hide
     */
    private Long idx;           // 일련번호
    private String memberUuid;  // puppycat_member.uuid
    private Long contentsIdx;   // sns_contents.idx
    private Integer state;      // 상태값
    private String regDate;     // 등록일
    private String regDateTz;   // 등록일 타임존
}
