package com.architecture.admin.models.dto.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatDto {
    private Long idx; // 일련번호
    private String uuid; // uuid
    private String memberUuid; // puppycat_member.uuid
    private String targetMemberUuid; // puppycat_member.uuid
    private String roomId;  // room id
    private String roomUuid;  // room uuid
    private String roomName; //room name
    private String message;  // 채팅 메세지
    private int maxUser; //채팅 최대 인원
    private int type;   // 타입 : 0 : DB / 1 : multi
    private int state;  // 상태값 (0: 삭제, 1: 정상);
    private int fixState;  // 고정 상태값 (0: 미사용, 1: 사용);
    private int favoriteState;  // 즐겨 찾기 상태값 (0: 미사용, 1: 사용);
    private int sort;   // 순서
    private String modiDate;        // 등록일
    private String modiDateTz;      // 등록일 타임존
    private String regDate;        // 등록일
    private String regDateTz;      // 등록일 타임존


    private int result_cd;
    private String result_msg;
    private String gTransStat;
    private String roomType;

    // sql
    private Integer insertedIdx;
    private Integer affectedRow;

    // memberInfo Dto
    private String nick;
    private String profileImgUrl;
}
