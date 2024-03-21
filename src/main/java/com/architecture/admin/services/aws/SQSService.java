package com.architecture.admin.services.aws;

import com.amazonaws.services.sqs.model.SendMessageResult;
import com.architecture.admin.libraries.AWSLibrary;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class SQSService extends BaseService {

    private final AWSLibrary awsLibrary;

    /**
     * SQS 메시지 publish
     *
     * @param message 보낼 data
     * @param arn 보낼 topic
     * @param groupId Group ID
     * @param dpId Deduplication ID
     * @return 처리결과
     */
    public String publish(String message, String arn, String groupId, String dpId) {

        SendMessageResult sendMessageResult = awsLibrary.sendMessage(message, arn, groupId, dpId);

        // SQS 통신 성공 유무
        return getResultSendMsg(sendMessageResult);
    }

    /**
     * SQS 메세지 발신 성공 유무
     *
     * @param sendMessageResult
     * @return String
     */
    private String getResultSendMsg(SendMessageResult sendMessageResult) {
        String sqsId = sendMessageResult.getMessageId();
        String sMessage = "";

        if ("".equals(sqsId) || sqsId == null) {
            sMessage = super.langMessage("lang.admin.exception.sqs.send_fail");
        } else {
            sMessage = super.langMessage("lang.admin.success.sqs.send");
        }
        return sMessage;
    }

}
