package com.architecture.admin.libraries;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/*****************************************************
 * Curl 라이브러리
 ****************************************************/
@Component
public class CurlLibrary {
    static WebClient client = WebClient.create();

    /**
     * GET
     */
    public static String get(String url,String header) {
        return client.get()
                .uri(url)
                .header("Authorization",header)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /**
     * POST
     */
    public static String post(String url, Map<String, String> dataSet) {
        return client.post()
                .uri(url)
                .body(BodyInserters.fromValue(dataSet))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
