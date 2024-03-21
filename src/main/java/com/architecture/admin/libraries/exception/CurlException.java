package com.architecture.admin.libraries.exception;

import org.json.JSONObject;

import java.util.Map;

/*****************************************************
 * 예외 처리 - 사용자 예외처리
 ****************************************************/
public class CurlException extends RuntimeException {
    private final Map<String, Object> jsonResponse;


    public CurlException(JSONObject object) {
        boolean result = (boolean) object.get("result");
        String code = object.getString("code");
        String message = object.getString("message");

        this.jsonResponse = Map.of(
                "result", result,
                "code", code,
                "message", message
        );
    }

    public Map<String, Object> getJsonResponse() {
        return jsonResponse;
    }
}
