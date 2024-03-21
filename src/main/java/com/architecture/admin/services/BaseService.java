package com.architecture.admin.services;

import com.architecture.admin.libraries.*;
import com.architecture.admin.libraries.exception.CurlException;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.daosub.block.BlockMemberDaoSub;
import com.architecture.admin.models.daosub.member.MemberDaoSub;
import com.architecture.admin.models.dto.block.BlockMemberDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.services.member.MemberCurlService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

/*****************************************************
 * 코어 서비스
 ****************************************************/
@Service
public class BaseService {

    // 시간 라이브러리 참조
    @Autowired
    protected DateLibrary dateLibrary;

    // 숫자 변환 관련 라이브러리 참조
    @Autowired
    protected NumberFormatLibrary numberFormatLibrary;

    // 암호화 라이브러리
    @Autowired
    protected SecurityLibrary securityLibrary;

    // 세션
    @Autowired
    protected HttpSession session;

    // 텔레그램
    @Autowired
    protected TelegramLibrary telegramLibrary;

    // Redis 라이브러리
    @Autowired
    protected RedisLibrary redisLibrary;

    // Curl 라이브러리
    @Autowired
    protected CurlLibrary curlLibrary;

    @Autowired
    protected MemberCurlService memberCurlService;

    /**
     * 메시지 가져오는 라이브러리
     */
    @Autowired
    protected MessageSource messageSource;

    @Autowired
    protected MemberDaoSub memberDaoSub;

    @Autowired
    private BlockMemberDaoSub blockMemberDaoSub;

    @Value("${member.badge.cnt}")
    private Integer badgeCnt;

    /*****************************************************
     * 세션 값 가져오기
     ****************************************************/
    public String getSession(String id) {
        return (String) session.getAttribute(id);
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
     * Curl
     ****************************************************/
    // get
    public String getCurl(String url, String header) {
        return curlLibrary.get(url, header);
    }

    // post
    public String postCurl(String url, Map dataset) {
        return curlLibrary.post(url, dataset);
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
     * get locale Language 현재 언어 값
     ****************************************************/
    public String getLocaleLang() {
        String localLang = LocaleContextHolder.getLocale().toString().toLowerCase();

        switch (localLang) {
            case "ko_kr", "ko", "kr":
                return "ko";
            case "en":
                return "en";
            default:
                return "en";
        }
    }

    /*****************************************************
     * ip 값 가져오기
     * private => public 으로 변환
     ****************************************************/
    public String getClientIP(HttpServletRequest request) {
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
     * 현재 도메인
     ****************************************************/
    public String getCurrentDomain() {
        // 도메인 받아오기
        String scheme = "";
        String currentDomain = "";
        HttpServletRequest request = ServerLibrary.getCurrReq();
        if (request.getServerName().equals("localhost")) {
            scheme = "http";
        } else {
            scheme = "https";
        } // http / https
        String serverName = request.getServerName();// 도메인만
        Integer serverPort = request.getServerPort();// 포트
        if (serverPort.equals(80) || serverPort.equals(443)) {
            currentDomain = scheme + "://" + serverName; // 전체 도메인
        } else {
            currentDomain = scheme + "://" + serverName + ":" + serverPort; // 전체 도메인
        }
        return currentDomain; // 전체 도메인
    }

    /*****************************************************
     * 한명이라도 차단 한/된 상태인지 체크
     ****************************************************/
    public boolean bChkBlock(String memberUuid, String blockUuid) {
        // 차단 관련 데이터 set
        BlockMemberDto blockMemberDto = new BlockMemberDto();
        blockMemberDto.setMemberUuid(memberUuid);
        blockMemberDto.setBlockUuid(blockUuid);
        Integer check1 = blockMemberDaoSub.getBlockByUuid(blockMemberDto);

        blockMemberDto.setMemberUuid(blockUuid);
        blockMemberDto.setBlockUuid(memberUuid);
        Integer check2 = blockMemberDaoSub.getBlockByUuid(blockMemberDto);

        boolean result = false;
        if (check1 > 0 || check2 > 0) {
            result = true;
        }
        return result;
    }

    /*****************************************************
     * JWT Token
     ****************************************************/
    /**
     * 토큰으로 회원 UUID 가져오기
     *
     * @param token
     * @return
     */
    public MemberDto getMemberUuidByToken(String token) {

        MemberDto memberDto = memberCurlService.getMemberUuid(token);
        if (memberDto.getUuid() == null || memberDto.getUuid().equals("")) {
            JSONObject obj = new JSONObject();
            obj.put("result", memberDto.getResult());
            obj.put("message", memberDto.getMessage());
            obj.put("code", memberDto.getCode());
            throw new CurlException(obj);
        }

        return memberDto;
    }

    /**
     * 토큰으로 회원 UUID 유효한지 체크
     *
     * @param uuid
     * @return
     */
    public Boolean getCheckMemberByUuid(String uuid) {

        if (ObjectUtils.isEmpty(uuid)) {
            throw new CustomException(CustomError.MEMBER_UUID_EMPTY);
        }

        String jsonString = memberCurlService.checkMemberUuid(uuid);

        JSONObject jsonObject = new JSONObject(jsonString);
        JSONObject dataObject = (JSONObject) jsonObject.get("data");

        if (dataObject.get("isExist") == null || dataObject.get("isExist").equals("")) {
            // 회원 UUID가 유효하지 않습니다.
            throw new CustomException(CustomError.MEMBER_UUID_ERROR);
        }

        return (Boolean) dataObject.get("isExist");
    }
}
