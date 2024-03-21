package com.architecture.admin.services.push;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "pushCurlService", url = "${member.domain}/v1/push")
public interface PushCurlService {

    @GetMapping("/token")
    List<String> getPushTokenList(@RequestHeader("Authorization") String token,
                                  @RequestParam(value = "receiverUuid", required = false, defaultValue = "") String receiverUuid,
                                  @RequestParam(value = "typeIdx", required = false, defaultValue = "") Integer typeIdx);

    @GetMapping("/none/token")
    List<String> getNotJwtTokenPushTokenList(@RequestParam(value = "receiverUuid", required = false, defaultValue = "") String receiverUuid,
                                  @RequestParam(value = "typeIdx", required = false, defaultValue = "") Integer typeIdx);
}
