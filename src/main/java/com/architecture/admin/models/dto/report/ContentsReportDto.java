package com.architecture.admin.models.dto.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContentsReportDto {

    /**
     * sns_contents_report
     */
    private Long idx; // 일련번호
    private String memberUuid;  // puppycat_member.uuid
    private Long contentsIdx; // 콘텐츠 idx
    private String contents; // 신고된 내용
    private Integer reportCode; // 신고사유 코드
    private Integer state; // 신고상태 (0:취소, 1:신고)
    private String regDate; // 등록일
    private String regDateTz; // 등록일 타임존

    /**
     * sns_contents_report_reason
     */
    private String reason; // 신고 상세사유

    /**
     * sns_report_code
     */
    private Integer codeIdx; // 신고사유 번호
    private String name; // 신고사유

    // sql
    private Long insertedIdx;

}

