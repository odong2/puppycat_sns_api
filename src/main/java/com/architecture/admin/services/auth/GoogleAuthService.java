package com.architecture.admin.services.auth;

import com.architecture.admin.libraries.ServerLibrary;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;


/*****************************************************
 * 구글 로그인
 ****************************************************/
@RequiredArgsConstructor
@Service
@Transactional
public class GoogleAuthService extends BaseService {

    @Value("${auth.google.clientId}")
    private String googleClientId;

    @Value("${auth.google.callbackUrl}")
    private String googleCallbackUrl;

    @Value("${auth.google.authUrl}")
    private String googleAuthUrl;

    @Value("${auth.google.scope}")
    private String googleScope;

    /*****************************************************
     *  Function
     ***************************************************/
    /**
     * 구글 연동 URL 생성
     *
     * @return 구글 연동 URL
     */
    public String getUrl() {
        // 도메인 받아오기
        HttpServletRequest request = ServerLibrary.getCurrReq();
        String scheme = request.getScheme(); // http / https
        String serverName = request.getServerName();// 도메인만
        Integer serverPort = request.getServerPort();// 포트
        String currentDomain = scheme + "://" + serverName + ":" + serverPort; // 전체 도메인

        return googleAuthUrl + "?response_type=code&client_id=" + googleClientId + "&scope=" + googleScope + "&redirect_uri=" + currentDomain + googleCallbackUrl + "&access_type=offline" + "&prompt=consent";
    }

}
