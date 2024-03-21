package com.architecture.admin.controllers.v1.contents;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.contents.ContentsLikeDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;
import com.architecture.admin.services.contents.ContentsLikeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/contents")
public class ContentsLikeV1Controller extends BaseController {

    private final ContentsLikeService contentsLikeService;

    /**
     * 해당 콘텐츠에 좋아요한 회원 list
     *
     * @param token       : access token
     * @param contentsIdx : 콘텐츠 Idx
     * @param searchDto   : page [현재 페이지], imgLimit [이미지 개수], imgOffSet [이미지 시작 위치]
     * @return ResponseEntity
     */
    @GetMapping("{contentsIdx}/like")
    public ResponseEntity contentsLikeList(@RequestHeader("Authorization") String token, @PathVariable("contentsIdx") Long contentsIdx,
                                           @ModelAttribute SearchDto searchDto) throws JsonProcessingException {

        // 콘텐츠 idx 세팅
        searchDto.setContentsIdx(contentsIdx);

        // list
        List<MemberInfoDto> list = contentsLikeService.getContentsLikeList(token, searchDto);

        String sMessage = super.langMessage("lang.contents.success.list");

        Map<String, Object> map = new HashMap<>();
        map.put("list", list);
        map.put("params", searchDto);

        JSONObject data = new JSONObject(map);
        return displayJson(true, "1000", sMessage, data);
    }

    @GetMapping("/like/check")
    public ResponseEntity<String> getContentsLikeCheck(@RequestParam(name = "memberUuid") String memberUuid,
                                                       @RequestParam(name = "contentsIdx") Long contentsIdx) {

        ContentsLikeDto contentsLikeDto = new ContentsLikeDto();
        contentsLikeDto.setMemberUuid(memberUuid);
        contentsLikeDto.setContentsIdx(contentsIdx);

        Boolean isContentsLike = contentsLikeService.getContentsLikeCheck(contentsLikeDto);

        JSONObject data = new JSONObject();
        data.put("isContentsLike", isContentsLike);

        return displayJson(true, "1000", "", data);
    }


    /**
     * 콘텐츠 좋아요 등록
     *
     * @param token       access token
     * @param contentsIdx 콘텐츠 idx
     * @return ResponseEntity
     */
    @PostMapping("{contentsIdx}/like")
    public ResponseEntity likeContents(@RequestHeader("Authorization") String token, @PathVariable("contentsIdx") Long contentsIdx) {

        // 콘텐츠 idx set
        ContentsLikeDto contentsLikeDto = new ContentsLikeDto();
        contentsLikeDto.setContentsIdx(contentsIdx);

        // 콘텐츠 좋아요
        contentsLikeService.contentsLike(token, contentsLikeDto);

        // response object
        String sMessage = super.langMessage("lang.contents.success.like");
        return displayJson(true, "1000", sMessage);
    }

    /**
     * 콘텐츠 좋아요 취소
     *
     * @param token       access token
     * @param contentsIdx 콘텐츠 idx
     * @return ResponseEntity
     */
    @DeleteMapping("{contentsIdx}/like")
    public ResponseEntity likeCancelContents(@RequestHeader("Authorization") String token, @PathVariable("contentsIdx") Long contentsIdx) throws ParseException {

        // 콘텐츠 idx set
        ContentsLikeDto contentsLikeDto = new ContentsLikeDto();
        contentsLikeDto.setContentsIdx(contentsIdx);

        // 콘텐츠 좋아요 취소
        contentsLikeService.contentsLikeCancel(token, contentsLikeDto);

        // response object
        String sMessage = super.langMessage("lang.contents.success.like.cancel");
        return displayJson(true, "1000", sMessage);
    }

}
