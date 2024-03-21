package com.architecture.admin.libraries.exception;


import com.architecture.admin.libraries.ServerLibrary;
import com.architecture.admin.libraries.TelegramLibrary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;

/*****************************************************
 * 예외 처리 - 기본 제공
 ****************************************************/
@RestControllerAdvice
@ResponseStatus(HttpStatus.OK)
@Slf4j
public class CommonExceptionLibrary {

    private final TelegramLibrary telegramLibrary;
    private final ObjectMapper objectMapper;

    @Value("${use.exceptionError.telegram}")
    private boolean useExceptionTelegram;    // 텔레그램 푸시 알림 true/false

    @Value("${env.server}")
    private String server;  // 서버

    public CommonExceptionLibrary(TelegramLibrary telegramLibrary, ObjectMapper objectMapper) {
        this.telegramLibrary = telegramLibrary;
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity except(Exception e, Model model) {
        model.addAttribute("exception", e);
        return displayError(e.getMessage(), "9500", null);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity runtimeException(Exception e) {
        CustomError customError = CustomError.BAD_REQUEST;
        log.error("ErrorClass : {}", e.getClass());
        log.error("ErrorMessage : {}", e.getMessage());
        return displayError(customError.getMessage(), customError.getCode(), null);
    }

    // 파라미터 타입 미스매치(bad request)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity methodArgumentTypeMismatchException(Exception e) {
        CustomError customError = CustomError.BAD_REQUEST_PARAMETER_TYPE_MISMATCH;
        // 에러 로그 개발단계에서만 사용
        log.error("ErrorClass : {} ", e.getClass());
        log.error("ErrorMessage : {} ", e.getMessage());
        return displayError(customError.getMessage(), customError.getCode(), null);
    }

    // 사용자 필수 파라미터값 미입력(bad request)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity missingServletRequestParameterException(Exception e) {
        CustomError error = CustomError.BAD_REQUEST_REQUIRED_VALUE;
        log.error("ErrorClass : {} ", e.getClass());
        log.error("ErrorMessage : {} ", e.getMessage());
        return displayError(error.getMessage(), error.getCode(), null);
    }

    // SQL 관련 오류 (서버 오류)
    @ExceptionHandler(SQLException.class)
    public ResponseEntity sqlException(Exception e) {
        CustomError error = CustomError.SERVER_SQL_ERROR;
        log.error("ErrorClass : {} ", e.getClass());
        log.error("ErrorMessage : {} ", e.getMessage());
        return displayError(error.getMessage(), error.getCode(), null);
    }

    // DB 관련 오류 (서버 오류) ex.duplicatedKeyException
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity dataAccessException(Exception e) {
        CustomError error = CustomError.SERVER_DATABASE_ERROR;
        log.error("ErrorClass : {} ", e.getClass());
        log.error("ErrorMessage : {} ", e.getMessage());
        return displayError(error.getMessage(), error.getCode(), null);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity exceptBind(BindException e) {
        final String sMESSAGE = "message";
        final String[] message = new String[1];
        JSONObject obj = new JSONObject();
        obj.put("result", false);
        e.getAllErrors().forEach(objectError -> {
            if (!obj.has(sMESSAGE) || obj.getString(sMESSAGE) == null) {
                message[0] = objectError.getDefaultMessage();
            }
        });

        return displayError(message[0], "9400", null);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity handle404(NoHandlerFoundException e) {
        return displayError(e.getMessage(), "9404", null);
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity customExceptionHandle(CustomException e) {
        return displayError(e.getCustomError().getMessage(), e.getCustomError().getCode(), e.getData());
    }

    @ExceptionHandler(CurlException.class)
    public ResponseEntity handleCustomException(CurlException ex) throws JsonProcessingException {
        JsonNode jsonResponseNode = objectMapper.readTree(objectMapper.writeValueAsString(ex.getJsonResponse()));

        String code = jsonResponseNode.get("code").asText();
        String message = jsonResponseNode.get("message").asText();

        return displayError(message, code, null);
    }

    private ResponseEntity displayError(String sMessage, String sCode, Object data) {
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        JSONObject obj = new JSONObject();
        obj.put("result", false);
        obj.put("code", sCode);
        obj.put("message", sMessage);

        if (data != null) {
            obj.put("data", data);
        }

        // 텔레그램 푸시알림
        if (useExceptionTelegram && !sCode.equals("EMLI-0000")) {
            pushException(obj, 200);
        }

        return new ResponseEntity(obj.toString(), httpHeaders, HttpStatus.resolve(200));
    }

    // 텔레그램 푸시
    private void pushException(JSONObject obj, int httpCode) {
        HttpServletRequest request = ServerLibrary.getCurrReq();
        String referrer = request.getHeader("Referer");
        String url = request.getRequestURI();
        String query = request.getQueryString();
        String method = request.getMethod();
        String sendMessgae = "=== [API] ===" + "\n"
                + "referrer :: " + referrer + "\n"
                + "server :: " + server + "\n"
                + "url :: " + url + "\n"
                + "query :: " + query + "\n"
                + "method :: " + method + "\n"
                + "httpCode :: " + httpCode + "\n"
                + "JSONObject :: " + obj;

        telegramLibrary.sendMessage(sendMessgae);
    }
}
