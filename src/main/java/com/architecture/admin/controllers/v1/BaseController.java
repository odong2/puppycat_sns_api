package com.architecture.admin.controllers.v1;

import com.architecture.admin.libraries.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.net.URI;
import java.util.HashMap;

/*****************************************************
 * 코어 컨트롤러
 ****************************************************/
public class BaseController {
    // 서버 환경 변수
    protected HashMap<String, Object> hmServer;
    protected String sSever;

    // 뷰에서 포함할 파일 데이터
    protected HashMap<String, String> hmImportFile;

    // 뷰에 전달할 데이터
    protected HashMap<String, Object> hmDataSet;

    /**
     * 메시지 가져오는 라이브러리
     */
    @Autowired
    protected MessageSource messageSource;

    /**
     * 텔레그램
     */
    @Autowired(required=false)
    protected TelegramLibrary telegramLibrary;

    /**
     * 암호화 라이브러리
     */
    @Autowired(required=false)
    protected SecurityLibrary securityLibrary;

    /**
     * 숫자 변환 관련 라이브러리 참조
     */
    @Autowired
    protected NumberFormatLibrary numberFormatLibrary;

    /**
     * 세션
     */
    @Autowired(required=false)
    protected HttpSession session;

    /**
     * 시간 라이브러리 참조
     */
    @Autowired(required=false)
    protected DateLibrary dateLibrary;

    /**
     * Redis 라이브러리 참조
     */
    @Autowired(required=false)
    protected RedisLibrary redisLibrary;

    // 텔레그램 푸시 알림 true/false
    @Value("${use.displayError.telegram}")
    private boolean useExceptionTelegram;

    /*****************************************************
     * 생성자
     ****************************************************/
    public BaseController() {
        // 뷰에서 포함할 파일 데이터
        hmImportFile = new HashMap<>();

        // 뷰에 전달할 데이터
        hmDataSet = new HashMap<>();

        // 서버 환경 변수
        hmServer = new HashMap<>();
        sSever = System.getProperty("spring.profiles.active");
        sSever = sSever == null ? "local" : sSever;
        hmServer.put("sSever", sSever);
    }

    /*****************************************************
     * 레디스
     ****************************************************/
    // 레디스 값 생성
    public void setRedis(String key, String value, Integer expiredSeconds) {
        redisLibrary.setData(key, value, expiredSeconds);
    }

    // 레디스 값 불러오기
    public String getRedis(String key) {
        return redisLibrary.getData(key);
    }

    // 레디스 값 삭제하기
    public void removeRedis(String key) {
        redisLibrary.deleteData(key);
    }

    /*****************************************************
     * 뷰에 전달할 데이터 셋팅
     ****************************************************/
    // 서버 환경변수 가져오기
    @ModelAttribute
    public void init(Model model){
        HttpServletRequest request = ServerLibrary.getCurrReq();
        String scheme = request.getScheme(); // http / https
        String requestUrl = String.valueOf(request.getRequestURL());// 전체 경로
        String serverName = request.getServerName();// 도메인만
        Integer serverPort = request.getServerPort();// 포트
        String currentDomain = scheme + "://" + serverName + ":" + serverPort; // 전체 도메인
        String requestURI = request.getRequestURI();// 경로+파일
        // String segment = request.getContextPath();// 경로만
        // String filename = request.getServletPath();// 파일만
        String clientIp = getClientIP(request);

        hmServer.put("requestUrl", requestUrl);
        hmServer.put("serverName", serverName);
        hmServer.put("serverPort", String.valueOf(serverPort));
        hmServer.put("currentDomain", currentDomain);
        hmServer.put("requestUri", requestURI);
        hmServer.put("clientIp", clientIp);

        model.addAttribute("dataSet", hmDataSet);
        model.addAttribute("importFile", hmImportFile);
        model.addAttribute("SERVER", hmServer);


    }

    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-FORWARDED-FOR");
        if (ip == null) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }

    /*****************************************************
     * Language 값 가져오기
     ****************************************************/
    public String langMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    public String langMessage(String code, @Nullable Object[] args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    /*****************************************************
     * 암호화 처리
     ****************************************************/
    // 양방향 암호화 암호화
    public String encrypt(String str) throws Exception {
        return securityLibrary.aesEncrypt(str);
    }

    // 양방향 암호화 복호화
    public String decrypt(String str) throws Exception {
        return securityLibrary.aesDecrypt(str);
    }

    // 단방향 암호화
    public String md5encrypt(String str) {
        return securityLibrary.md5Encrypt(str);
    }

    /*****************************************************
     * 세션 값 가져오기
     ****************************************************/
    public String getSession(String id) {
        return (String) session.getAttribute(id);
    }

    /*****************************************************
     * 리다이렉트 처리
     ****************************************************/
    public ResponseEntity<Object> redirect(String sUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(sUrl));
        return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
    }

    /*****************************************************
     * 뷰 Json
     ****************************************************/
    public ResponseEntity displayJson(Boolean result, String code, String message) {
        final HttpHeaders httpHeaders= new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        JSONObject obj = new JSONObject();
        obj.put("result", result);
        obj.put("code", code);
        obj.put("message", message);

        return new ResponseEntity(obj.toString(), httpHeaders, HttpStatus.OK);
    }

    public ResponseEntity redirectDisplayJson(Boolean result, String code, String message) {
        final HttpHeaders httpHeaders= new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        JSONObject obj = new JSONObject();
        obj.put("result", result);
        obj.put("code", code);
        obj.put("message", message);

        return new ResponseEntity(obj.toString(), httpHeaders, HttpStatus.resolve(200));
    }

    public ResponseEntity displayJson(Boolean result, String code, String message, JSONObject data) {
        final HttpHeaders httpHeaders= new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        JSONObject obj = new JSONObject();
        obj.put("result", result);
        obj.put("code", code);
        obj.put("message", message);
        obj.put("data", data);

        return new ResponseEntity(obj.toString(),httpHeaders, HttpStatus.OK);
    }

    public ResponseEntity displayError(BindingResult result) {
        final HttpHeaders httpHeaders= new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        JSONObject obj = new JSONObject();
        obj.put("result", false);

        result.getAllErrors().forEach(objectError -> {
            System.out.println(objectError.getDefaultMessage());
            if (!obj.has("message") || obj.get("message").toString() == null) {
                obj.put("field", ((FieldError) objectError).getField());
                obj.put("message", objectError.getDefaultMessage());
            }
        });

        // 텔레그램 푸시알림
        if(useExceptionTelegram) {
            HttpServletRequest request = ServerLibrary.getCurrReq();
            String referrer = request.getHeader("Referer");
            String sendMessgae = "referrer :: "+referrer+"\n"+"JSONObject :: "+String.valueOf(obj);
            pushAlarm(sendMessgae);
        }

        return new ResponseEntity(obj.toString(), httpHeaders, HttpStatus.resolve(200));
    }

    /*****************************************************
     * 디버깅
     ****************************************************/
    public void d() {
        int iSeq = 2;
        System.out.println("======================================================================");
        System.out.println("클래스명 : " + Thread.currentThread().getStackTrace()[iSeq].getClassName());
        System.out.println("메소드명 : " + Thread.currentThread().getStackTrace()[iSeq].getMethodName());
        System.out.println("줄번호 : " + Thread.currentThread().getStackTrace()[iSeq].getLineNumber());
        System.out.println("파일명 : " + Thread.currentThread().getStackTrace()[iSeq].getFileName());
    }

    public void pushAlarm(String sendMessage) {
        telegramLibrary.sendMessage(sendMessage);
    }

    public void pushAlarm(String sendMessage, String sChatId) {
        telegramLibrary.sendMessage(sendMessage, sChatId);
    }

}
