package com.architecture.admin.services.contents;


import com.architecture.admin.models.dto.member.MemberInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "contentsCurlService", url = "${member.domain}/v1/contents")
public interface ContentsCurlService {

    /**
     * 멘션 시 uuid 조회 by nick
     *
     * @param nickList nick List
     * @return
     */
    @PostMapping("/mention/uuid")
    String getUuidByNick(@RequestHeader("Authorization") String token, @RequestParam(name = "nickList") List<String> nickList);

    /**
     * 멘션된 회원 정보 리스트
     *
     * @param memberUuidList : memberUuid, nick, outMemberUuid, outNick
     * @return
     */
    @PostMapping("/mention/member/info")
    String getMentionInfoList(@RequestBody List<String> memberUuidList);

    /**
     * 이미지 내 태그된 회원 정보 리스트
     *
     * @param imgTagMemberUuidList
     * @return
     */
    @PostMapping("/img/tag/member/info")
    String getImgMemberTagInfoList(List<String> imgTagMemberUuidList);

    /**
     * 컨텐츠 작성자 정보
     *
     * @param memberUuid
     */
    @GetMapping("/writer/info")
    MemberInfoDto getWriterInfo(@RequestParam(name = "memberUuid") String memberUuid);

    /**
     * 컨텐츠 작성자 정보 리스트
     *
     * @param uuidList
     */
    @PostMapping("/writer/info/list")
    String getWriterInfoList(@RequestBody List<String> uuidList);
}
