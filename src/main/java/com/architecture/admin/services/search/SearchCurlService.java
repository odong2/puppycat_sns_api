package com.architecture.admin.services.search;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "searchCurlService", url = "${member.domain}/v1/search")
public interface SearchCurlService {

    /*****************************************************
     *  SubFunction - select
     ****************************************************/
    /**
     * 닉네임이 완전히 일치하는 회원 UUID리스트 가져오기
     *
     * @param searchWord 검색어
     * @return
     */
    @GetMapping("/nick/same")
    String getSameNickUuid(@RequestParam(name = "searchWord") String searchWord);

    /**
     * 닉네임이 완전히 일치하는 회원 UUID리스트 가져오기
     *
     * @param searchWord 검색어
     * @return
     */
    @GetMapping("/nick")
    String getSearchNickUuid(@RequestParam(name = "searchWord") String searchWord);
}
