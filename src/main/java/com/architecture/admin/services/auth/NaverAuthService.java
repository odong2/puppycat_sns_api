package com.architecture.admin.services.auth;

import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.security.SecureRandom;


/*****************************************************
 * 네이버 로그인
 ****************************************************/
@RequiredArgsConstructor
@Service
@Transactional
public class NaverAuthService extends BaseService {

    @Value("${auth.naver.clientId}")
    private String naverClientId;
    
    @Value("${auth.naver.callbackUrl}")
    private String naverCallbackUrl;

    @Value("${auth.naver.authUrl}")
    private String naverAuthUrl;

    /*****************************************************
     *  Function
     ***************************************************/
    /**
     * 네이버 연동 URL 생성
     *
     * @return 네이버 연동 URL
     */
    public String getUrl() {
        // 도메인 받아오기
        String currentDomain = super.getCurrentDomain();

        // state용 난수 생성
        SecureRandom random = new SecureRandom();
        String naverState = new BigInteger(130, random).toString(32);

        return naverAuthUrl + "?response_type=code&client_id=" + naverClientId + "&redirect_uri=" + currentDomain+naverCallbackUrl + "&state=" + naverState;
    }

}
