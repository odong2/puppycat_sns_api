package com.architecture.admin.services.auth;

import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



/*****************************************************
 * 카카오 로그인
 ****************************************************/
@RequiredArgsConstructor
@Service
@Transactional
public class KakaoAuthService extends BaseService {

    @Value("${auth.kakao.clientId}")
    private String kakaoClientId;
    
    @Value("${auth.kakao.callbackUrl}")
    private String kakaoCallbackUrl;

    @Value("${auth.kakao.authUrl}")
    private String kakaoAuthUrl;

    /*****************************************************
     *  Function
     ***************************************************/
    /**
     * 카카오 연동 URL 생성
     *
     * @return 카카오 연동 URL
     */
    public String getUrl() {
        // 도메인 받아오기
        String currentDomain = super.getCurrentDomain();

        return kakaoAuthUrl + "?response_type=code&client_id=" + kakaoClientId + "&redirect_uri=" + currentDomain+kakaoCallbackUrl + "&response_type=code";
    }

}
