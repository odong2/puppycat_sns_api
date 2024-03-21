package com.architecture.admin.models.dto.push;

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
public class PushDto {

    // sns_member_push_success_log / sns_member_push_fail_log
    private Long idx;               // 고유 번호
    private Integer typeIdx;        // 푸시타입
    private long contentsIdx;       // 컨텐츠idx
    private long commentIdx;        // 댓글idx
    private String senderUuid;      // 푸시 보내는 회원
    private String receiverUuid;    // 푸시 받는 회원
    private String body;            // 내용
    private String chatBody;        // 채팅 내용
    private String img;             // 이미지
    private String regDate;         // 등록일
    private String regDateTz;       // 등록일 타임존

    // sns_push_type
    private String typeTitle;    // 타입 제목
    private Integer notiType;    // 공지 타입
    private Integer subType;     // 공지 상세 타입

    // sns_member_app
    private String fcmToken;     // fcm 토큰

    // etc
    private String senderNick;      // 푸시 보내는 회원 닉네임
    private List<String> tokenList; // 푸시 보낼 토큰 리스트
    private String errorCode;       // 에러코드

    // push 보낼 리스트
    private List<Long> mentionMemberList;  // 멘션 푸시 리스트


    // sql
    private Long insertedIdx;
    private Long affectedRow;

    //etc
    private Boolean newChatState;
}