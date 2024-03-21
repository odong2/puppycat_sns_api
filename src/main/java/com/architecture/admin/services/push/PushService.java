package com.architecture.admin.services.push;

import com.architecture.admin.config.NotiConfig;
import com.architecture.admin.models.dao.push.PushDao;
import com.architecture.admin.models.daosub.push.PushDaoSub;
import com.architecture.admin.models.dto.push.PushDto;
import com.architecture.admin.services.BaseService;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/*****************************************************
 * 푸시 공통 모델러
 ****************************************************/
@Service
@RequiredArgsConstructor
public class PushService extends BaseService {
    private static final Logger logger = LoggerFactory.getLogger(PushService.class);
    private final PushDao pushDao;
    private final PushDaoSub pushDaoSub;
    private final FirebaseMessaging firebaseMessaging;
    private final PushCurlService pushCurlService;
    @Value("${chat.push.idx}")
    private int chatPushTypeIdx; // 채팅 type idx

    /*****************************************************
     *  Modules
     ****************************************************/

    /**
     * 1명의 회원 토큰 조회 후 푸시 보내기
     *
     * @param pushDto senderNick:바디에 보낼 회원 닉네임 typeTitle:푸시타입 contentsIdx commentIdx Img
     */
    public void oneToOnePush(String token, PushDto pushDto) {

        boolean checkBlock = false;

        if (pushDto.getTypeIdx() != chatPushTypeIdx) {
            checkBlock = super.bChkBlock(pushDto.getSenderUuid(), pushDto.getReceiverUuid());
        }

        // 서로 차단 내역이 없으면
        if (!checkBlock) {
            // 푸시 보내는 사람과 받는 사람이 같지 않을 때 ( 나에게 보내는 푸시가 아닌경우 )
            if (!Objects.equals(pushDto.getSenderUuid(), pushDto.getReceiverUuid())) {
                List<SendResponse> responses;

                // 푸시 받을 회원 토큰 리스트(fcm 토큰이 정상이고,알람 설정한 회원)
                List<String> pushTokenlist = pushCurlService.getPushTokenList(token, pushDto.getReceiverUuid(), pushDto.getTypeIdx());

                // 토큰 세팅
                pushDto.setTokenList(pushTokenlist);
                // 유효한 토큰이 있다면
                if (!pushTokenlist.isEmpty()) {
                    // 푸시 보내기
                    BatchResponse response = sendFcmMessage(pushDto);
                    // 결과 처리
                    if (response != null) {
                        responses = response.getResponses();
                        for (int i = 0; i < responses.size(); i++) {
                            // 토큰 세팅
                            pushDto.setFcmToken(pushTokenlist.get(i));
                            if (!responses.get(i).isSuccessful()) {
                                // 실패 토큰 로그 쌓기
                                insertFailLog(pushDto);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 1명의 회원 토큰 조회 후 푸시 보내기
     *
     * @param pushDto senderNick:바디에 보낼 회원 닉네임 typeTitle:푸시타입 contentsIdx commentIdx Img
     */
    public void notTokenOneToOnePush(PushDto pushDto) {
        boolean checkBlock = false;
        if (pushDto.getTypeIdx() != chatPushTypeIdx) {
            checkBlock = super.bChkBlock(pushDto.getSenderUuid(), pushDto.getReceiverUuid());
        }
        // 서로 차단 내역이 없으면
        if (!checkBlock) {
            // 푸시 보내는 사람과 받는 사람이 같지 않을 때 ( 나에게 보내는 푸시가 아닌경우 )
            if (!Objects.equals(pushDto.getSenderUuid(), pushDto.getReceiverUuid())) {
                List<SendResponse> responses;
                // 푸시 받을 회원 토큰 리스트(fcm 토큰이 정상이고,알람 설정한 회원)
                List<String> pushTokenlist = pushCurlService.getNotJwtTokenPushTokenList(pushDto.getReceiverUuid(), pushDto.getTypeIdx());
                // 토큰 세팅
                pushDto.setTokenList(pushTokenlist);
                // 유효한 토큰이 있다면
                if (!pushTokenlist.isEmpty()) {
                    // 푸시 보내기
                    BatchResponse response = sendFcmMessage(pushDto);
                    // 결과 처리
                    if (response != null) {
                        responses = response.getResponses();
                        for (int i = 0; i < responses.size(); i++) {
                            // 토큰 세팅
                            pushDto.setFcmToken(pushTokenlist.get(i));
                            if (!responses.get(i).isSuccessful()) {
                                // 실패 토큰 로그 쌓기
                                insertFailLog(pushDto);
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * fcm 푸시 보내기
     *
     * @param pushDto senderNick:바디에 보낼 회원 닉네임 typeTitle:푸시타입 contentsIdx commentIdx Img TokenList
     * @return fcm 응답 값
     */
    public BatchResponse sendFcmMessage(PushDto pushDto) {
        Map<String, String> msg = new HashMap<>();

        // title 가져오기
        String titleText = NotiConfig.getNotiTitle().get(pushDto.getTypeTitle());
        String bodyText = pushDto.getBody() + NotiConfig.getNotiBody().get(pushDto.getTypeTitle());

        // title이 null 이라면 nick 적용 [채팅용]
        if (titleText == null) {
            titleText = pushDto.getSenderNick();

            // 방이 개설 되었습니다 문구
            if (pushDto.getNewChatState() != null && pushDto.getNewChatState()) {
                bodyText = NotiConfig.getNotiBody().get("new_chatting");
            }
        }

        // notification builder
        Notification notification = Notification.builder()
                .setTitle(titleText)
                .setBody(bodyText)
                .setImage(pushDto.getImg())
                .build();

        msg.put("body", pushDto.getBody());       // 메세지
        // chatBody가 null이 아니라면 채팅용 정보 전달
        if (pushDto.getChatBody() != null) {
            msg.put("chat", pushDto.getChatBody());       // 채팅용 정보 [다른 push null 처리]
        }
        msg.put("type", pushDto.getTypeTitle());        // 푸시 타입
        msg.put("contents_idx", String.valueOf(pushDto.getContentsIdx()));   // 이동될 컨텐츠 idx
        msg.put("comment_idx", String.valueOf(pushDto.getCommentIdx()));     // 이동될 댓글 idx
        msg.put("image", pushDto.getImg());

        //apnsConfig Set 하기
        ApnsConfig apnsConfig = ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setContentAvailable(true)
                        .build()).build();

        // 푸시 세팅
        MulticastMessage.Builder builder = MulticastMessage.builder();
        MulticastMessage message = builder
                .addAllTokens(pushDto.getTokenList())
                .setNotification(notification)
                .setApnsConfig(apnsConfig)
                .putAllData(msg)
                .build();

        BatchResponse response = null;
        try {
            // 푸시 보내기
            response = firebaseMessaging.sendMulticast(message);
        } catch (FirebaseMessagingException e) {
            pushDto.setErrorCode(String.valueOf(e.getMessagingErrorCode()));
            // 실패 토큰 로그 쌓기
            insertFailLog(pushDto);
        }
        return response;
    }


    /*****************************************************
     *  SubFunction - select
     ****************************************************/
    /**
     * 타입 제목 가져오기
     *
     * @param idx 타입idx
     * @return 타입title
     */
    public String selectPushTypeTitle(int idx) {
        return pushDaoSub.getPushTypeTitle(idx);
    }

    /*****************************************************
     *  SubFunction - insert
     ****************************************************/
    /**
     * 실패 로그 입력
     *
     * @param pushDto `sender_uuid` `receiver_uuid` `type_idx` `contents_idx` `comment_idx` `body` `img` `fcm_token`
     */
    public void insertFailLog(PushDto pushDto) {
        logger.error("fail_token :: " + pushDto.getFcmToken());
    }
    /*****************************************************
     *  SubFunction - Update
     ****************************************************/
    /*****************************************************
     *  SubFunction - Delete
     ****************************************************/
}
