package com.architecture.admin.models.dto.member;

import com.architecture.admin.models.dto.contents.ContentsDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Email;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberInfoDto {
    /**
     * puppycat_member
     **/
    private Long memberIdx;     // 회원번호
    @Email
    private String email;       // email[아이디]
    private String uuid;        // 고유아이디
    private String nick;        // 닉네임
    private Integer isDel;      // 탈퇴상태 (0:정상/1:탈퇴)
    private String ci;          // ci
    private String di;          // di
    private String name;        // 이름
    private String phone;       // 전화번호
    private String gender;      // 성별(M: male, F: female)
    private String genderText;  // 성별 문자변환
    private Integer blockedMeState;    //나를 차단
    private Integer blockedState;   //너를 차단
    private String birth;       // 생년월일
    private String simpleType;  // 간편가입 타입 (ex kakao google)
    private String profileImgUrl;         // 프로필 이미지 Url
    private String regDate;         // 가입일
    private String modiDate;        // 연락처 수정일

    // sql
    private Long insertedIdx;
    private Integer affectedRow;

    private Long followerCnt;
    private Long followCnt;

    // profile
    private List<MultipartFile> uploadFile;    // 프로필 업로드 이미지
    private String intro;          // 회원 소개글

    private Integer isBadge; // 뱃지 조건 달성 [0:비활성화 1:활성화]
    private Integer followState; // 팔로우 여부 [0:안함 1:팔로우]
    private Integer redDotState; // 레드닷 여부 [0:비활성화 1:활성화]

    // 기타
    private List<ContentsDto> contentsList; // 대표 게시물 리스트
    private Integer resetState;             // 프로필 이미지 초기화 상태 값
    private Boolean result;
    private String sMessage;
    private String code;
}