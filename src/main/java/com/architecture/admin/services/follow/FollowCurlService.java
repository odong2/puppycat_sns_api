package com.architecture.admin.services.follow;

import com.architecture.admin.models.dto.SearchDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "followCurlService", url = "${member.domain}/v1/follow")
public interface FollowCurlService {

    /*****************************************************
     *  SubFunction - select
     ****************************************************/
    /**
     * 팔로우/팔로워 카운트 조회
     *
     * @param searchDto
     * @return
     */
    @PostMapping("/search/cnt")
    String getFollowSearchCnt(@ModelAttribute SearchDto searchDto);

}
