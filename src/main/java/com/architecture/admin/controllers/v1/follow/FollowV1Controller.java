package com.architecture.admin.controllers.v1.follow;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.follow.FollowDto;
import com.architecture.admin.services.follow.FollowService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/follow")
public class FollowV1Controller extends BaseController {

    private final FollowService followService;

    /**
     * 팔로우 리스트 전체
     *
     * @param memberUuid 조회 할 회원의 uuid
     * @param searchDto  page 페이지 searchType 검색항목 searchWord 검색내용 limit 페이지에 노출 될 개수
     * @return 리스트 가져오기 성공
     * @throws ParseException
     */
    @GetMapping("{memberUuid}")
    public ResponseEntity totalFollowLists(@RequestHeader(value = "Authorization", defaultValue = "") String token,
                                           @PathVariable(name = "memberUuid") String memberUuid,
                                           @ModelAttribute SearchDto searchDto) throws ParseException {

        // 조회 할 회원 idx 세팅
        searchDto.setMemberUuid(memberUuid);

        // list
        List<FollowDto> list = followService.getTotalFollowList(token, searchDto);
        Map<String, Object> map = new HashMap<>();
        map.put("list", list);
        map.put("params", searchDto);

        String sMessage;

        if (!list.isEmpty()) {
            // 리스트 가져오기 성공
            sMessage = super.langMessage("lang.follow.success.list");
        } else {
            // 친구가 없습니다.
            sMessage = super.langMessage("lang.follow.exception.list.null");
        }

        JSONObject data = new JSONObject(map);

        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 팔로잉 리스트 검색시
     *
     * @param memberUuid 조회 할 회원의 uuid
     * @param searchDto  page 페이지 searchType 검색항목 searchWord 검색내용 limit 페이지에 노출 될 개수
     * @return 리스트 가져오기 성공
     * @throws ParseException
     */
    @GetMapping("{memberUuid}/search")
    public ResponseEntity searchFollowingLists(@RequestHeader(value = "Authorization", defaultValue = "") String token,
                                               @PathVariable(name = "memberUuid") String memberUuid,
                                               @ModelAttribute SearchDto searchDto) throws ParseException {

        // 조회 할 회원 idx 세팅
        searchDto.setMemberUuid(memberUuid);

        // list
        List<FollowDto> list = followService.getSearchFollowingList(token, searchDto);
        Map<String, Object> map = new HashMap<>();
        map.put("list", list);
        map.put("params", searchDto);

        String sMessage;

        if (!list.isEmpty()) {
            // 리스트 가져오기 성공
            sMessage = super.langMessage("lang.follow.success.list");
        } else {
            // 유저를 찾을 수 없습니다.
            sMessage = super.langMessage("lang.follow.exception.member.null");
        }

        JSONObject data = new JSONObject(map);
        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 팔로우 여부 조회
     *
     * @param memberUuid
     * @param followUuid
     * @return
     */
    @GetMapping("/check")
    public ResponseEntity<String> getFollowCheck(@RequestParam(name = "memberUuid") String memberUuid,
                                                 @RequestParam(name = "followUuid") String followUuid) {

        FollowDto followDto = new FollowDto();
        followDto.setMemberUuid(memberUuid);
        followDto.setFollowUuid(followUuid);

        Boolean isFollow = followService.checkFollow(followDto);

        JSONObject data = new JSONObject();
        data.put("isFollow", isFollow);

        return displayJson(true, "1000", "", data);
    }

    /**
     * 팔로우 등록
     *
     * @param token      access token
     * @param followUuid 팔로우 할 회원 uuid
     * @return
     */
    @PostMapping("{followUuid}")
    public ResponseEntity regist(@RequestHeader("Authorization") String token,
                                 @PathVariable(name = "followUuid") String followUuid) {
        // follow uuid 세팅
        FollowDto followDto = new FollowDto();
        followDto.setFollowUuid(followUuid);

        // 팔로우
        int result = followService.registFollow(token, followDto);

        // 팔로우 성공
        String sMessage = super.langMessage("lang.follow.success.follow.regist");
        // response object
        JSONObject data = new JSONObject(result);
        return displayJson(true, "1000", sMessage, data);
    }


    /**
     * 팔로우 취소
     *
     * @param followUuid 언팔로우 할 회원의 idx
     * @return 언팔로우 성공
     */
    @DeleteMapping("{followUuid}")
    public ResponseEntity remove(@RequestHeader("Authorization") String token,
                                 @PathVariable(name = "followUuid") String followUuid) {

        // follow uuid 세팅
        FollowDto followDto = new FollowDto();
        followDto.setFollowUuid(followUuid);

        // 팔로잉 삭제
        int result = followService.removeFollow(token, followDto);

        //언팔로우 성공
        String sMessage = super.langMessage("lang.follow.success.follow.remove");
        // response object
        JSONObject data = new JSONObject(result);
        return displayJson(true, "1000", sMessage, data);
    }
}
