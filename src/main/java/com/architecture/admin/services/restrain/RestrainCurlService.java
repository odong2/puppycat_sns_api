package com.architecture.admin.services.restrain;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "restrainCurlService", url = "${member.domain}/v1/restrain")
public interface RestrainCurlService {

    @GetMapping("/check")
    String getRestrainCheck(@RequestHeader("Authorization") String token,
                            @RequestParam("restrainType") Integer restrainType);
}
