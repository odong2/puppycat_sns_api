package com.architecture.admin.services.push;

import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dto.chat.ChatDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.push.PushDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/*****************************************************
 * 채팅 푸시
 ****************************************************/
@Service
@RequiredArgsConstructor
public class ChatPushService extends BaseService {

    private final PushService pushService;
    @Value("${chat.push.idx}")
    private int chatPushTypeIdx; // 채팅 type idx
    /*****************************************************
     *  Modules
     ****************************************************/
    /**
     * 방 생성 푸시
     *
     */
    public void createRoomPush(String token, ChatDto chatDto) {
        // pushDto init
        PushDto pushDto = new PushDto();

        // 해당 getMemberUuid 존재 하는지 체크
        if (chatDto.getMemberUuid() == null || chatDto.getMemberUuid().equals("")) {
            throw new CustomException(CustomError.MEMBER_UUID_EMPTY);
        }

        // 해당 roomId가 존재 하는지 체크
        if (chatDto.getRoomUuid() == null || chatDto.getRoomUuid().equals("")) {
            throw new CustomException(CustomError.CHAT_ROOM_ID_EMPTY);
        }

        // 대상 유저가 uuid validation
        if (chatDto.getTargetMemberUuid() == null || chatDto.getTargetMemberUuid().equals("")) {
            throw new CustomException(CustomError.MEMBER_UUID_ERROR);
        }

        // 차단할 회원 uuid 정상인지 curl 통신
        Boolean isExist = super.getCheckMemberByUuid(chatDto.getTargetMemberUuid());

        if (!isExist) {
            // 회원 UUID가 유효하지 않습니다.
            throw new CustomException(CustomError.MEMBER_UUID_ERROR);
        }

        JSONObject obj = new JSONObject();
        obj.put("roomUuid", chatDto.getRoomUuid());
        obj.put("senderMemberUuid", chatDto.getMemberUuid());
        obj.put("targetMemberUuid", chatDto.getTargetMemberUuid());
        obj.put("senderNick", chatDto.getNick());
        obj.put("senderMemberProfileImg", chatDto.getProfileImgUrl());
        obj.put("message", "채팅방이 개설 되었습니다.");
        obj.put("regDate", chatDto.getRegDate());

        // body date set
        pushDto.setBody("채팅방이 개설 되었습니다.");
        // body date set[total data]
        pushDto.setChatBody(String.valueOf(obj));
        // 타입 타이틀
        pushDto.setTypeTitle(pushService.selectPushTypeTitle(chatPushTypeIdx));
        // 타입 타이틀 IDX
        pushDto.setTypeIdx(chatPushTypeIdx);
        // 보내는 사람 uuid
        pushDto.setSenderUuid(chatDto.getMemberUuid());
        // 받는 사람 uuid
        pushDto.setReceiverUuid(chatDto.getTargetMemberUuid());
        // 닉네임
        pushDto.setSenderNick(chatDto.getNick());
        // 이미지
//        pushDto.setImg(chatDto.getProfileImgUrl());
        pushDto.setImg("");
        // 방 최초 생성
        pushDto.setNewChatState(true);
        // push 발송
        pushService.oneToOnePush(token, pushDto);
    }

    /**
     * 방 메세지 푸시
     *
     */
    public void chatMessagePush(ChatDto chatDto) {
        // pushDto init
        PushDto pushDto = new PushDto();

        // 해당 getMemberUuid 존재 하는지 체크
        if (chatDto.getMemberUuid() == null || chatDto.getMemberUuid().equals("")) {
            throw new CustomException(CustomError.MEMBER_UUID_EMPTY);
        }

        // 해당 roomId가 존재 하는지 체크
        if (chatDto.getRoomUuid() == null || chatDto.getRoomUuid().equals("")) {
            throw new CustomException(CustomError.CHAT_ROOM_ID_EMPTY);
        }

        // 대상 유저가 uuid validation
        if (chatDto.getTargetMemberUuid() == null || chatDto.getTargetMemberUuid().equals("")) {
            throw new CustomException(CustomError.MEMBER_UUID_ERROR);
        }

        // 채팅 메세지 validation
        if (chatDto.getMessage() == null || chatDto.getMessage().equals("")) {
            throw new CustomException(CustomError.MEMBER_UUID_ERROR);
        }

        // 프로필 이미지
        if (chatDto.getProfileImgUrl() == null || chatDto.getProfileImgUrl().equals("")) {
            chatDto.setProfileImgUrl("");
        }

        // 차단할 회원 uuid 정상인지 curl 통신
        Boolean isExist = super.getCheckMemberByUuid(chatDto.getTargetMemberUuid());

        if (!isExist) {
            // 회원 UUID가 유효하지 않습니다.
            throw new CustomException(CustomError.MEMBER_UUID_ERROR);
        }

        JSONObject obj = new JSONObject();
        obj.put("roomUuid", chatDto.getRoomUuid());
        obj.put("senderMemberUuid", chatDto.getMemberUuid());
        obj.put("targetMemberUuid", chatDto.getTargetMemberUuid());
        obj.put("senderNick", chatDto.getNick());
        obj.put("senderMemberProfileImg", chatDto.getProfileImgUrl());
        obj.put("message", chatDto.getMessage());
        obj.put("regDate", chatDto.getRegDate());

        // body date set[message]
        pushDto.setBody(chatDto.getMessage());
        // body date set[total data]
        pushDto.setChatBody(String.valueOf(obj));
        // 타입 타이틀
        pushDto.setTypeTitle(pushService.selectPushTypeTitle(chatPushTypeIdx));
        // 타입 IDX
        pushDto.setTypeIdx(chatPushTypeIdx);
        // 보내는 사람 uuid
        pushDto.setSenderUuid(chatDto.getMemberUuid());
        // 받는 사람 uuid
        pushDto.setReceiverUuid(chatDto.getTargetMemberUuid());
        // 닉네임
        pushDto.setSenderNick(chatDto.getNick());
        //이미지
//        pushDto.setImg(chatDto.getProfileImgUrl());
        pushDto.setImg("");
        // push 발송
        pushService.notTokenOneToOnePush(pushDto);
    }
}
