package com.architecture.admin.controllers.v1.follow;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.follow.FollowDto;
import com.architecture.admin.services.follow.FollowerService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/follower")
public class FollowerV1Controller extends BaseController {

    private final FollowerService followerService;

    /**
     * 팔로워 리스트 전체
     *
     * @param memberUuid 조회 할 회원의 uuid
     * @param searchDto  page 페이지 searchType 검색항목 searchWord 검색내용 limit 페이지에 노출 될 개수
     * @return 리스트 가져오기 성공
     */
    @GetMapping("{memberUuid}")
    public ResponseEntity totalFollowerLists(@RequestHeader(value = "Authorization", defaultValue = "") String token,
                                             @PathVariable(name = "memberUuid") String memberUuid,
                                             @ModelAttribute SearchDto searchDto) {
        
        // 조회 할 회원 idx 세팅
        searchDto.setMemberUuid(memberUuid);

        // list
        List<FollowDto> list = followerService.getTotalFollowerList(token, searchDto);

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
     * 팔로워 리스트 검색시
     *
     * @param memberUuid 조회 할 회원의 uuid
     * @param searchDto  page 페이지 searchType 검색항목 searchWord 검색내용 limit 페이지에 노출 될 개수
     * @return 리스트 가져오기 성공
     */
    @GetMapping("{memberUuid}/search")
    public ResponseEntity searchFollowerLists(@RequestHeader(value = "Authorization", defaultValue = "") String token,
                                              @PathVariable(name = "memberUuid") String memberUuid,
                                              @ModelAttribute SearchDto searchDto) {

        // 조회 할 회원 idx 세팅
        searchDto.setMemberUuid(memberUuid);

        // list
        List<FollowDto> list = followerService.getSearchFollowerList(token, searchDto);
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
     * 팔로워 해제
     *
     * @param token
     * @param followerUuid
     * @return
     */
    @DeleteMapping("{followerUuid}")
    public ResponseEntity removeFollower(@RequestHeader("Authorization") String token,
                                         @PathVariable(name = "followerUuid") String followerUuid) {
        FollowDto followDto = new FollowDto();
        // 팔로워 회원 uuid 세팅
        followDto.setFollowUuid(followerUuid);

        // 팔로잉 삭제
        int result = followerService.removeFollower(token, followDto);

        String sMessage;

        // 팔로워 삭제 성공
        sMessage = super.langMessage("lang.follow.success.follower.remove");

        // response object
        JSONObject data = new JSONObject(result);

        return displayJson(true, "1000", sMessage, data);
    }
}
