package com.architecture.admin.services.member;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "memberCurlService", url = "${member.domain}/v1/member")
public interface MemberCurlService {

    /**
     * 회원 uuid 조회
     *
     * @param token access token
     * @return
     */
    @GetMapping("/uuid")
    MemberDto getMemberUuid(@RequestHeader(value = "Authorization") String token);

    /**
     * 회원 uuid 체크
     *
     * @param uuid 회원 UUID
     * @return
     */
    @GetMapping("/uuid/check")
    String checkMemberUuid(@RequestParam(name = "uuid") String uuid);

    /**
     * 회원 정보 가져오기 - 닉네임 ㄱㄴㄷ 정렬
     *
     * @param searchDto
     * @return
     */
    @PostMapping("/info/nick")
    String getMemberInfoOrderByNick(@ModelAttribute SearchDto searchDto);

    /**
     * 회원 uuid 리스트 체크
     *
     * @param uuidList UUID 리스트
     * @return
     */
    @GetMapping("/uuid/list/check")
    String checkMemberUuidList(@RequestParam(name = "uuidList") List<String> uuidList);

    /**
     * 회원 검색
     *
     * @param searchWord 검색어
     * @return
     */
    @GetMapping("/search")
    String getMemberUuidBySearchDto(@RequestParam(name = "searchWord") String searchWord);

    /**
     * 회원 정보 가져오기 by uuidList
     *
     * @param uuidList memberUuidList
     * @return
     */
    @GetMapping("/info/list")
    String getMemberInfoByUuidList(@RequestParam(name = "uuidList") List<String> uuidList);

    /**
     * 회원 닉네임 가져오기 by uuid
     *
     * @param uuid memberUuid
     * @return
     */
    @GetMapping("/nick/{uuid}")
    MemberDto getMemberNickInfoByUuid(@PathVariable(name = "uuid") String uuid);

    /**
     * 회원 닉네임 가져오기 By uuid
     *
     * @param token
     * @param uuid
     * @return
     */
    @GetMapping("/nick")
    MemberDto getNickByUuid(@RequestHeader("Authorization") String token,
                            @RequestParam(name = "uuid") String uuid);

    /**
     * 회원 uuid 리스트 체크
     *
     * @return
     */
    @GetMapping("/info/{memberUuid}")
    MemberInfoDto getMemberInfoByUuid(@PathVariable("memberUuid") String memberUuid);

    /**
     * 회원 가입 날짜 조회
     *
     * @param token
     * @param uuid
     * @return
     */
    @GetMapping("regdate")
    MemberDto getMemberRegdate(@RequestHeader("Authorization") String token,
                               @RequestParam(name = "uuid") String uuid);

}
