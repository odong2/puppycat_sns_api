package com.architecture.admin.controllers.v1.login;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.services.login.JoinService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/join")
public class JoinV1Controller extends BaseController {
    private final JoinService joinService;

    /**
     * 소셜 회원가입
     *
     * @param memberUuid
     * @return
     * @throws Exception
     */
    @PostMapping("/social")
    public ResponseEntity socialMemberJoin(@RequestBody String memberUuid) {

        // 회원가입 처리
        boolean result = joinService.regist(memberUuid);

        String sMessage = super.langMessage("lang.common.success.insert"); // 등록 성공
        JSONObject data = new JSONObject();
        data.put("result", result);

        return displayJson(true, "1000", sMessage, data);
    }
}

