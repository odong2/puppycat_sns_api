package com.architecture.admin.controllers.v1.push;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.models.dto.chat.ChatDto;
import com.architecture.admin.services.push.ChatPushService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/push")
public class PushV1Controller extends BaseController {

    private final ChatPushService chatPushService;

    @PostMapping("/chat")
    public ResponseEntity registCreateChatRoomPush(@RequestHeader(value = "Authorization") String token,
                                                   @RequestBody ChatDto chatDto) {

        chatPushService.createRoomPush(token, chatDto);

        // 등록 완료
        String sMessage = super.langMessage("lang.chat.push.success.regist");

        return displayJson(true, "1000", sMessage);
    }

    @PostMapping("/chat/message")
    public ResponseEntity chatMessagePush(@RequestBody ChatDto chatDto) {
        // data set
        chatPushService.chatMessagePush(chatDto);
        // 등록 완료
        String sMessage = super.langMessage("lang.chat.push.success.regist");

        return displayJson(true, "1000", sMessage);
    }
}
