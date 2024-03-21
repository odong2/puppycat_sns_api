package com.architecture.admin.services.push;

import com.architecture.admin.models.dto.push.PushDto;
import com.architecture.admin.services.BaseService;
import com.architecture.admin.services.aws.SNSService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;


/*****************************************************
 * 콘텐츠 등록시 푸시
 ****************************************************/
@Service
@RequiredArgsConstructor
public class ContentsPushService extends BaseService {

    private final SNSService snsService;             // sqs /sns
    @Value("${cloud.aws.sns.contents.push.topic.arn}")
    private String snsTopicARN;                     // 푸시sns
    @Value("${cloud.aws.sns.contents.push.topic.arn2}")
    private String snsTopicARN2;                     // 푸시sns2
    @Value("${cloud.aws.sns.contents.push.topic.arn3}")
    private String snsTopicARN3;                     // 푸시sns3

    /*****************************************************
     *  Modules
     ****************************************************/

    /**
     * 컨텐츠 푸시 sqs 쌓기
     *
     * @param pushDto InsertedIdx
     */
    public void contentsRegistSendPush(PushDto pushDto) {
        // 컨텐츠 IDX 를 3으로 나눠서 QUE에 분산 시킴
        long remain = pushDto.getContentsIdx() % 3;
        String arn;

        if (remain == 0) {
            arn = snsTopicARN;
        } else if (remain == 1) {
            arn = snsTopicARN2;
        } else {
            arn = snsTopicARN3;
        }

        // DeduplicationId 컨텐츠IDX + "contents_regist"
        String dpId = pushDto.getContentsIdx() + "contents_regist";
        // 데이터 세팅 action : regist, contents_idx : 등록된 컨텐츠idx
        JSONObject data = new JSONObject();
        data.put("action", "regist");
        data.put("contents_idx", pushDto.getContentsIdx());
        // 푸시 데이터 보내기
        snsService.publish(data.toString(), arn, "contentsPush", "contents", dpId);
    }

    /**
     * 컨텐츠 수정시 푸시
     * 
     * @param pushDto
     */
    public void contentsModifySendPush(PushDto pushDto) {
        // 컨텐츠 IDX 를 3으로 나눠서 QUE에 분산 시킴
        long remain = pushDto.getContentsIdx() % 3;
        String arn;

        if (remain == 0) {
            arn = snsTopicARN;
        } else if (remain == 1) {
            arn = snsTopicARN2;
        } else {
            arn = snsTopicARN3;
        }

        // DeduplicationId 컨텐츠IDX + "contents_modify"
        String dpId = pushDto.getContentsIdx() + "contents_modify"+ UUID.randomUUID();
        // 데이터 세팅 action : regist, contents_idx : 등록된 컨텐츠idx
        JSONObject data = new JSONObject();
        data.put("action", "modify");
        data.put("contents_idx", pushDto.getContentsIdx());
         // 푸시 데이터 보내기
        snsService.publish(data.toString(), arn, "contentsPush", "contents", dpId);
    }
}
