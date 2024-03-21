package com.architecture.admin.models.dto.noti;

import com.architecture.admin.models.dto.member.MemberDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotiDto {

    // sns_member_notification
    private long idx;           // 고유 번호
    private String memberUuid;  // 회원번호
    private Integer type;       // 알림 타입
    private String subType;     // 상세 타입
    private String senderUuid;    // 알림 보내는 회원
    private long allNotiIdx;    // 전체 공지 알림 idx
    private long contentsIdx;   // 컨텐츠idx
    private long commentIdx;    // 댓글idx
    private String title;       // 제목
    private String body;        // 내용
    private String img;         // 이미지
    private String contents;    // 상세 내용
    private Integer state;      // 상태값
    private Integer isShow;     // 확인 상태값
    private String delDate;     // 삭제일
    private String delDateTz;   // 삭제일 타임존
    private String regDate;     // 등록일
    private String regDateTz;   // 등록일 타임존

    // sns_member_notification_show
    private String showDate;     // 마지막 공지 확인일
    private String showDateTz;   // 마지막 공지 확인일 타임존


    private String modiDate;    // 수정일
    private List<MemberDto> senderInfo;                             // 글 작성자 정보
    List<HashMap<String, List<MemberDto>>> mentionMemberInfo;       // 글에 멘션된 회원 정보
    private Long parentIdx;                 // 부모댓글idx
    private String checkNotiDate;           // 공지 중복 체크할 데이트
    private String joinDate;                // 회원 가입 날짜

    // 기타
    private List<String> mentionMemberUuidList; // 멘션 회원 리스트
    private Integer contentsLikeState;          // 컨텐츠 좋아요 여부
    private Integer followState;                // 팔로우 여부
    private List<String> memberUuidList;      // 회원 uuid 리스트 (공통 사용)
    // sql
    private Long insertedIdx;
    private Integer affectedRow;

}
