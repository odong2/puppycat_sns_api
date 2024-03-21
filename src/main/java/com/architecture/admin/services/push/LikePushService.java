package com.architecture.admin.services.push;

import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.push.PushDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/*****************************************************
 * 좋아요시 푸시
 ****************************************************/
@Service
@RequiredArgsConstructor
public class LikePushService extends BaseService {

    private final PushService pushService;

    /*****************************************************
     *  Modules
     ****************************************************/
    /**
     * 좋아요 푸시
     *
     * @param pushDto senderUuid receiverUuid typeIdx contentsIdx img
     */
    public void sendLikePush(String token, PushDto pushDto) {
        // 이미지 없으면 빈값으로 초기화
        if (pushDto.getImg() == null) {
            pushDto.setImg("");
        }

        // 보낼 닉네임
        MemberDto senderNick = memberCurlService.getNickByUuid(token, pushDto.getSenderUuid());
        pushDto.setBody(senderNick.getNick());

        // 타입 타이틀
        String typeTitle = pushService.selectPushTypeTitle(pushDto.getTypeIdx());
        pushDto.setTypeTitle(typeTitle);

        pushService.oneToOnePush(token,pushDto);
    }

}
