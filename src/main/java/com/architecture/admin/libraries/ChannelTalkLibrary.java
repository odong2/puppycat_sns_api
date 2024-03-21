package com.architecture.admin.libraries;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

/*****************************************************
 * 채널톡 라이브러리
 ****************************************************/
@Service
@RequiredArgsConstructor
public class ChannelTalkLibrary {

    private static final String secretKey = "1fea822518e7ad5391f59ee0664f87c4cb393c2cf47d5aac55de857b4e4d6ce7";

    private static final String algorithms = "HmacSHA256";

    // 채널톡 연동 문서에 나온 멤버 해시
    public static String encode(String memberId) throws RuntimeException {
        try {

            Mac mac = Mac.getInstance(algorithms);
            mac.init(new SecretKeySpec(hexify(secretKey), algorithms));

            byte[] hash = mac.doFinal(memberId.getBytes());

            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] hexify(String string) {
        return DatatypeConverter.parseHexBinary(string);
    }

}
