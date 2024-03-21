package com.architecture.admin.controllers.v1.main;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.contents.ContentsDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;
import com.architecture.admin.services.contents.ContentsService;
import com.architecture.admin.services.member.FavoriteMemberService;
import com.architecture.admin.services.member.PopularMemberService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/main")
public class MainV1Controller extends BaseController {

    private final ContentsService contentsService;
    private final FavoriteMemberService favoriteMemberService;
    private final PopularMemberService popularMemberService;
    @Value("${official.uuid}")
    private String officialUuid;        // 회사 계정 uuid

    /**
     * 팔로잉 컨텐츠
     *
     * @param token
     * @param searchDto  page 페이지 limit 페이지에 노출 될 개수
     * @return ResponseEntity
     */
    @GetMapping("/follow")
    public ResponseEntity contentsLikeList(@RequestHeader(value = "Authorization") String token, @ModelAttribute SearchDto searchDto) {

        // list
        List<ContentsDto> followContentsList = contentsService.getFollowContentsList(token, searchDto);

        String sMessage;

        if (!followContentsList.isEmpty()) {
            // 리스트 가져오기 성공
            sMessage = super.langMessage("lang.contents.success.list");
        } else {
            // 등록된 게시물이 없습니다.
            sMessage = super.langMessage("lang.contents.list.empty");
        }

        Map<String, Object> map = new HashMap<>();
        map.put("list", followContentsList);
        map.put("params", searchDto);

        JSONObject data = new JSONObject(map);
        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 교류많은 유저 리스트
     *
     * @param token     access token
     * @param searchDto page 페이지 limit 페이지에 노출 될 개수
     * @return ResponseEntity
     */
    @GetMapping("/favorite")
    public ResponseEntity favoriteUserList(@RequestHeader(value = "Authorization") String token,
                                           @ModelAttribute SearchDto searchDto, HttpServletRequest httpRequest) throws ParseException, JsonProcessingException {


        // 교류 많은 유저 list
        List<MemberInfoDto> favotieMemberList = favoriteMemberService.getFavoriteMemberList(token, searchDto);

        // 회사 계정 list
        List<MemberInfoDto> officialList = favoriteMemberService.getOfficialAccountList(searchDto, httpRequest);

        // 교류 많은 유저 중 회사 계정이 있다면 제거
        int index = -1;
        for (int i = 0; i < favotieMemberList.size(); i++) {
            if (Objects.equals(favotieMemberList.get(i).getUuid(), officialUuid)) {
                index = i;
                break;
            }
        }
        if (index > -1) {
            favotieMemberList.remove(index);
        }

        favotieMemberList.addAll(0, officialList);

        String sMessage;

        if (!favotieMemberList.isEmpty()) {
            // 리스트 가져오기 성공
            sMessage = super.langMessage("lang.contents.success.list");
        } else {
            // 리스트가 비어있습니다.
            sMessage = super.langMessage("lang.contents.user.list.empty");
        }

        Map<String, Object> map = new HashMap<>();
        map.put("memberList", favotieMemberList);

        JSONObject data = new JSONObject(map);
        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 인기 유저 리스트
     *
     * @param token     access token
     * @param searchDto page 페이지 limit 페이지에 노출 될 개수
     * @return ResponseEntity
     */
    @GetMapping("/popular")
    public ResponseEntity popularUserList(@RequestHeader(value = "Authorization", defaultValue = "") String token, @ModelAttribute SearchDto searchDto) {

        // 인기 유저 list
        List<MemberInfoDto> popularMemberList = popularMemberService.getPopularMemberList(token, searchDto);

        String sMessage;

        if (!popularMemberList.isEmpty()) {
            // 리스트 가져오기 성공
            sMessage = super.langMessage("lang.contents.success.list");
        } else {
            // 리스트가 비어있습니다.
            sMessage = super.langMessage("lang.contents.user.list.empty");
        }

        Map<String, Object> map = new HashMap<>();
        map.put("list", popularMemberList);
        map.put("params", searchDto);

        JSONObject data = new JSONObject(map);
        return displayJson(true, "1000", sMessage, data);
    }

}
