package com.architecture.admin.models.dto.wordcheck;

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
public class ContentsWordCheckDto {

    private String word;        // 금칙어
    private String changeWord;  // 금칙어 변환값
}