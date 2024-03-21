package com.architecture.admin.libraries;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.architecture.admin.config.AWSConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

/*
    AWS 자격 증명 서비스
 */
@Service
@RequiredArgsConstructor
public class AWSLibrary {

    private final AWSConfig awsConfig;
    private final AmazonSQS amazonSQS;

    public AwsCredentialsProvider getAwsCredentials(String accessKeyID, String secretAccessKey) {
        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(accessKeyID, secretAccessKey);
        return () -> awsBasicCredentials;
    }

    // SNS Client info
    public SnsClient getSnsClient() {
        return SnsClient.builder()
                .credentialsProvider(getAwsCredentials(awsConfig.getAccessKey(), awsConfig.getSecretKey()))
                .region(Region.of(awsConfig.getRegion()))
                .build();
    }

    // Message send action function
    public SendMessageResult sendMessage(String message, String url, String groupId, String dpId) {
        // message send
        SendMessageRequest sendMessageRequest = new SendMessageRequest()
                .withQueueUrl(url)
                .withMessageGroupId(groupId)
                .withMessageBody(message)
                .withMessageDeduplicationId(dpId);

        return amazonSQS.sendMessage(sendMessageRequest);
    }

}
