package com.architecture.admin.controllers.v1.member;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.contents.ContentsDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;
import com.architecture.admin.services.contents.ContentsService;
import com.architecture.admin.services.contents.ContentsTagService;
import com.architecture.admin.services.member.MemberInfoService;
import com.architecture.admin.services.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/member")
public class MemberV1Controller extends BaseController {

    private final MemberService memberService;
    private final MemberInfoService memberInfoService;
    private final ContentsService contentsService;
    private final ContentsTagService contentsTagService;

    @Value("${official.uuid}")
    private String officialUuid;        // 회사 계정 Uuid

    /**
     * 회원 활동 정보
     *
     * @param token
     * @return
     */
    @GetMapping("/activity/info")
    public ResponseEntity getMemberActivityInfo(@RequestHeader(value = "Authorization") String token) {

        String sMessage = super.langMessage("lang.common.success.search");

        // 회원 활동 조회
        MemberDto memberInfo = memberService.getMemberActivityInfo(token);

        JSONObject data = new JSONObject();
        data.put("memberInfo", new JSONObject(memberInfo));

        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 유저 페이지
     *
     * @param token                 access token
     * @param sTargetMemberUuid     타겟 uuid
     * @param response              response
     * @return
     */
    @GetMapping("/info/{targetMemberUuid}")
    public ResponseEntity getMemberInfoList(@RequestHeader(value = "Authorization", defaultValue = "") String token,
                                            @PathVariable("targetMemberUuid") String sTargetMemberUuid,
                                            HttpServletResponse response) {

        // data set
        Map<String, Object> map = new HashMap<>();

        MemberDto memberDto = MemberDto.builder()
                .memberUuid(sTargetMemberUuid)
                .build();

        // 회원 Info
        MemberInfoDto info = memberInfoService.getMemberInfoByUuid(token, memberDto);

        String sMessage = super.langMessage("lang.member.info.success.list"); // 리스트 가져오기 성공

        // 공식 계정에 들어온 경우 [교류 많은 유저] 레드닷 지워지도록 쿠키 생성
        if (Objects.equals(sTargetMemberUuid, officialUuid)) {
            String datetime = dateLibrary.getDatetime();
            // 쿠키 value에 공백 입력 불가하여 URL encode 사용
            String encodedNow = URLEncoder.encode(datetime, StandardCharsets.UTF_8);

            // 쿠키 생성
            Cookie officialRedDot = new Cookie("officialRedDot", encodedNow); // 쿠키 이름 지정하여 생성( key, value 개념)
            officialRedDot.setMaxAge(60 * 60 * 24); // 쿠키 유효 기간: 하루로 설정(60초 * 60분 * 24시간)
            officialRedDot.setPath("/"); // 모든 경로에서 접근 가능하도록 설정

            response.addCookie(officialRedDot); // 응답 헤더에 추가
        }

        //data set
        map.put("info", info);

        JSONObject data = new JSONObject(map);

        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 타인이 작성 한 콘텐츠 리스트
     *
     * @param memberUuid 회원 uuid
     * @param searchDto
     * @return ResponseEntity
     */
    @GetMapping("/{memberUuid}/contents")
    public ResponseEntity getTargetMemberContentsList(@RequestHeader(value = "Authorization", defaultValue = "") String token,
                                                      @PathVariable("memberUuid") String memberUuid,
                                                      @ModelAttribute SearchDto searchDto) {

        // data set
        searchDto.setMemberUuid(memberUuid);

        // list
        List<ContentsDto> lContentsList = contentsService.getMemberContentsList(token, searchDto);

        //Text Set
        String sMessage = super.langMessage("lang.contents.success.list"); // 리스트 가져오기 성공

        // data set
        Map<String, Object> mMap = new HashMap<>();
        mMap.put("list", lContentsList);
        mMap.put("params", searchDto);

        JSONObject oJsonData = new JSONObject(mMap);
        return displayJson(true, "1000", sMessage, oJsonData);
    }

    /**
     * 타인이 태그 된 컨텐츠 리스트
     *
     * @param memberUuid :회원 uuid
     * @param searchDto  : page, limit
     * @return ResponseEntity
     */
    @GetMapping("/{memberUuid}/tag/contents")
    public ResponseEntity getMemberTagContentsList(@RequestHeader(value = "Authorization", defaultValue = "") String token,
                                                   @PathVariable("memberUuid") String memberUuid,
                                                   @ModelAttribute SearchDto searchDto) {

        // data set
        searchDto.setMemberUuid(memberUuid);

        // list
        List<ContentsDto> lTagContentsList = contentsTagService.getMemberTagContentsList(token, searchDto);

        // Text Set
        String sMessage = super.langMessage("lang.contents.success.list"); // 리스트 가져오기 성공

        Map<String, Object> mMap = new HashMap<>();
        mMap.put("list", lTagContentsList);
        mMap.put("params", searchDto);

        JSONObject oJsonData = new JSONObject(mMap);
        return displayJson(true, "1000", sMessage, oJsonData);
    }

    /**
     * 회원 뱃지 여부
     *
     * @param memberUuid
     * @return
     */
    @GetMapping("/badge")
    public ResponseEntity<String> getMemberBadge(String memberUuid) {

        MemberDto badgeInfo = memberService.getMemberBadgeInfoByUuid(memberUuid);

        boolean isBadge = false;

        if (badgeInfo != null && badgeInfo.getIsBadge() != null && badgeInfo.getIsBadge() == 1) {
            isBadge = true;
        }

        JSONObject data = new JSONObject();
        data.put("isBadge", isBadge);

        return displayJson(true, "1000", "", data);
    }
}
