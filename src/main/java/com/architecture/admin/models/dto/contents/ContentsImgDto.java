package com.architecture.admin.models.dto.contents;

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
public class ContentsImgDto {
    /**
     * sns_contents_img
     */
    private Long idx; // 일련 번호
    private String uuid; // 고유아이디
    private Long contentsIdx; // 컨텐츠 idx
    private String url; // url (도메인 제외)
    private String uploadPath;     // 이미지 경로
    private String uploadName;     // 이미지 파일명
    private Integer imgWidth;     // 이미지 가로 사이즈
    private Integer imgHeight;     // 이미지 세로 사이즈
    private Integer sort;          // 정렬순서
    private Integer state;         // 상태값
    private String regDate;        // 등록일
    private String regDateTz;      // 등록일 타임존

    // etc
    private List<ContentsImgMemberTagDto> imgMemberTagList; // 이미지 내 태그된 회원 리스트

    /**
     * sns_img_hash_tag_mapping
     */
    private Integer tagWidth;       // 태그 가로 위치
    private Integer tagHeight;      // 태그 세로 위치

    // sql
    private Long insertedIdx;
    private Integer affectedRow;
}
