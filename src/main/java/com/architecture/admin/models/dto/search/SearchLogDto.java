package com.architecture.admin.models.dto.search;

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
public class SearchLogDto {

    // sns_search_log
    private String searchWord;    // 검색어
    private String regDate;     // 등록일
    private String regDateTz;   // 등록일 타임존


    private String searchCnt;    // 검색어 카운트

    // sql
    private Long insertedIdx;
    private Long affectedRow;

}