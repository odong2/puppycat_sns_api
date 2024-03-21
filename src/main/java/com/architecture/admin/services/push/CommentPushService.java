package com.architecture.admin.services.push;

import com.architecture.admin.models.dto.push.PushDto;
import com.architecture.admin.services.BaseService;
import com.architecture.admin.services.aws.SNSService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


/*****************************************************
 * 팔로우시 푸시
 ****************************************************/
@Service
@RequiredArgsConstructor
public class CommentPushService extends BaseService {

    private final SNSService snsService;             // sqs /sns
    @Value("${cloud.aws.sns.comment.push.topic.arn}")
    private String snsTopicARN;                      // 푸시sns
    @Value("${cloud.aws.sns.comment.push.topic.arn2}")
    private String snsTopicARN2;                     // 푸시sns2
    @Value("${cloud.aws.sns.comment.push.topic.arn3}")
    private String snsTopicARN3;                     // 푸시sns3
    /*****************************************************
     *  Modules
     ****************************************************/

    /**
     * 댓글 푸시 sqs 쌓기
     *
     * @param pushDto InsertedIdx
     */
    public void commentRegistSendPush(PushDto pushDto) {

        // 컨텐츠 IDX 를 3으로 나눠서 QUE에 분산 시킴
        Long remain = pushDto.getCommentIdx() % 3;
        String arn;

        if (remain == 0) {
            arn = snsTopicARN;
        } else if (remain == 1) {
            arn = snsTopicARN2;
        } else {
            arn = snsTopicARN3;
        }

        // DeduplicationId 댓글IDX + "comment_regist"
        String dpId = pushDto.getCommentIdx() + "comment_regist";

        // 데이터 세팅 action : regist, comment_idx : 등록된 댓글 idx
        JSONObject data = new JSONObject();
        data.put("action", "regist");
        data.put("comment_idx", pushDto.getCommentIdx());

        // 푸시 데이터 보내기
        snsService.publish(data.toString(), arn, "commentPush", "comment", dpId);
    }

    /**
     * 댓글 수정 sqs 쌓기
     *
     * @param pushDto
     */
    public void commentModifySendPush(PushDto pushDto) {
        // 댓글 IDX 를 3으로 나눠서 QUE에 분산 시킴
        Long remain = pushDto.getCommentIdx() % 3;
        String arn;

        if (remain == 0) {
            arn = snsTopicARN;
        } else if (remain == 1) {
            arn = snsTopicARN2;
        } else {
            arn = snsTopicARN3;
        }

        // DeduplicationId 댓글IDX + "comment_regist"
        String dpId = pushDto.getCommentIdx() + "comment_modify";

        // 데이터 세팅 action : regist, comment_idx : 등록된 댓글 idx
        JSONObject data = new JSONObject();
        data.put("action", "modify");
        data.put("comment_idx", pushDto.getCommentIdx());

        // 푸시 데이터 보내기
        snsService.publish(data.toString(), arn, "commentPush", "comment", dpId);
    }
}
