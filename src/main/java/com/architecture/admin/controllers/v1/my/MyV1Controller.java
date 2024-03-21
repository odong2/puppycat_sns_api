package com.architecture.admin.controllers.v1.my;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.contents.ContentsDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;
import com.architecture.admin.services.contents.*;
import com.architecture.admin.services.member.MemberInfoService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/my")
public class MyV1Controller extends BaseController {

    private final ContentsService contentsService;
    private final ContentsLikeService contentsLikeService;
    private final ContentsSaveService contentsSaveService;
    private final ContentsKeepService contentsKeepService;
    private final ContentsTagService contentsTagService;
    private final MemberInfoService memberInfoService;

    /**
     * 내가 작성한 컨텐츠 리스트
     *
     * @param token
     * @param searchDto : page, limit
     * @return
     */
    @GetMapping("/contents")
    public ResponseEntity getMyContentsList(@RequestHeader(value = "Authorization") String token, @ModelAttribute SearchDto searchDto) {

        // list
        List<ContentsDto> lContentsList = contentsService.getMyContentsList(token, searchDto);

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
     * 내가 좋아요 한 컨텐츠 리스트
     *
     * @param token
     * @param searchDto
     * @return
     */
    @GetMapping("/like/contents")
    public ResponseEntity getMyLikeContentsList(@RequestHeader(value = "Authorization") String token,
                                                @ModelAttribute SearchDto searchDto) {

        // list
        List<ContentsDto> lLikeContentsList = contentsLikeService.getMyLikeContentsList(token, searchDto);

        //Text Set
        String sMessage = super.langMessage("lang.contents.success.list"); // 리스트 가져오기 성공

        Map<String, Object> mMap = new HashMap<>();
        mMap.put("list", lLikeContentsList);
        mMap.put("params", searchDto);

        JSONObject oJsonData = new JSONObject(mMap);
        return displayJson(true, "1000", sMessage, oJsonData);
    }

    /**
     * 내가 저장 한 컨텐츠 리스트
     *
     * @param token
     * @param searchDto : page, limit
     * @return
     */
    @GetMapping("/save/contents")
    public ResponseEntity getMySaveContentsList(@RequestHeader(value = "Authorization") String token,
                                                @ModelAttribute SearchDto searchDto) {

        // list
        List<ContentsDto> lSaveContentsList = contentsSaveService.getMySaveContentsList(token, searchDto);

        //Text Set
        String sMessage = super.langMessage("lang.contents.success.list"); // 리스트 가져오기 성공

        Map<String, Object> mMap = new HashMap<>();
        mMap.put("list", lSaveContentsList);
        mMap.put("params", searchDto);

        JSONObject oJsonData = new JSONObject(mMap);
        return displayJson(true, "1000", sMessage, oJsonData);
    }

    /**
     * 내가 보관 한 컨텐츠 리스트
     *
     * @param token
     * @param searchDto : page, limit
     * @return
     */
    @GetMapping("/keep/contents")
    public ResponseEntity getMyKeepContentsList(@RequestHeader(value = "Authorization") String token,
                                                @ModelAttribute SearchDto searchDto) {

        // list
        List<ContentsDto> lKeepContentsList = contentsKeepService.getMyKeepContentsList(token, searchDto);

        //Text Set
        String sMessage = super.langMessage("lang.contents.success.list"); // 리스트 가져오기 성공

        Map<String, Object> mMap = new HashMap<>();
        mMap.put("list", lKeepContentsList);
        mMap.put("params", searchDto);

        JSONObject oJsonData = new JSONObject(mMap);
        return displayJson(true, "1000", sMessage, oJsonData);
    }

    /**
     * 내가 태그 된 컨텐츠 리스트
     *
     * @param token
     * @param searchDto : page, limit
     * @return
     */
    @GetMapping("/tag/contents")
    public ResponseEntity getMyTagContentsList(@RequestHeader(value = "Authorization") String token,
                                               @ModelAttribute SearchDto searchDto) {

        // list
        List<ContentsDto> lTagContentsList = contentsTagService.getMyTagContentsList(token, searchDto);

        //Text Set
        String sMessage = super.langMessage("lang.contents.success.list"); // 리스트 가져오기 성공

        Map<String, Object> mMap = new HashMap<>();

        mMap.put("list", lTagContentsList);
        mMap.put("params", searchDto);

        JSONObject oJsonData = new JSONObject(mMap);

        return displayJson(true, "1000", sMessage, oJsonData);
    }

    /**************************************************************
     * 컨텐츠 상세 단건 조회
     **************************************************************/

    /**
     * 내가 작성한 컨텐츠 상세 [일상글]
     *
     * @param token
     * @param contentsIdx
     * @param searchDto
     * @return
     */
    @GetMapping("/normal/contents/{contentsIdx}")
    public ResponseEntity getMyNormalContents(@RequestHeader(value = "Authorization") String token,
                                              @PathVariable Long contentsIdx,
                                              @ModelAttribute SearchDto searchDto) {

        searchDto.setContentsIdx(contentsIdx);
        // 이미지 offset setting
        searchDto.setImgOffSet(0);

        // 내가 작성한 컨텐츠 상세 조회
        ContentsDto contents = contentsService.getMyNormalContentsDetail(token, searchDto);

        // 내 정보 조회
        MemberInfoDto memberInfo = memberInfoService.getMyInfoByUuid(searchDto.getLoginMemberUuid());

        // 검색 성공
        String sMessage = super.langMessage("lang.common.success.search");

        JSONObject data = new JSONObject();
        data.put("list", new JSONObject(contents));
        data.put("memberInfo", new JSONObject(memberInfo));
        data.put("params", new JSONObject(searchDto));

        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 내가 작성한 컨텐츠 상세 [보관글]
     *
     * @param token
     * @param contentsIdx
     * @param searchDto
     * @return
     */
    @GetMapping("/keep/contents/{contentsIdx}")
    public ResponseEntity getMyKeepContents(@RequestHeader(value = "Authorization") String token,
                                            @PathVariable Long contentsIdx,
                                            @ModelAttribute SearchDto searchDto) {

        searchDto.setContentsIdx(contentsIdx);
        // 이미지 offset setting
        searchDto.setImgOffSet(0);

        // 내가 작성한 컨텐츠 상세 조회
        ContentsDto contents = contentsService.getMyKeepContentsDetail(token, searchDto);

        // 내 정보 조회
        MemberInfoDto memberInfo = memberInfoService.getMyInfoByUuid(searchDto.getLoginMemberUuid());

        // 검색 성공
        String sMessage = super.langMessage("lang.common.success.search");

        JSONObject data = new JSONObject();
        data.put("list", new JSONObject(contents));
        data.put("memberInfo", new JSONObject(memberInfo));
        data.put("params", new JSONObject(searchDto));

        return displayJson(true, "1000", sMessage, data);
    }

    /**************************************************************
     * 컨텐츠 상세 리스트
     **************************************************************/

    /**
     * 내가 작성한 컨텐츠 상세 리스트
     *
     * @param searchDto
     * @return
     */
    @GetMapping("/normal/contents/detail")
    public ResponseEntity getContentsList(@RequestHeader(value = "Authorization") String token,
                                          @ModelAttribute SearchDto searchDto) {

        // 이미지 offset setting
        searchDto.setImgOffSet(0);

        // 내가 작성한 컨텐츠 상세 조회
        List<ContentsDto> contentsList = contentsService.getWrittenByMeContentsList(token, searchDto);

        // 내 정보 조회
        MemberInfoDto memberInfo = memberInfoService.getMyInfoByUuid(searchDto.getLoginMemberUuid());

        // 검색 성공
        String sMessage = super.langMessage("lang.common.success.search");

        JSONObject data = new JSONObject();
        data.put("list", contentsList);
        data.put("memberInfo", new JSONObject(memberInfo));
        data.put("params", new JSONObject(searchDto));

        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 내가 좋아요한 컨텐츠 상세 리스트
     *
     * @param searchDto
     * @return
     */
    @GetMapping("/like/contents/detail")
    public ResponseEntity getLikeContentsDetail(@RequestHeader(value = "Authorization") String token,
                                                @ModelAttribute SearchDto searchDto) {

        // 이미지 offset setting
        searchDto.setImgOffSet(0);

        // 내가 좋아요한 상세 리스트 조회
        List<ContentsDto> contentsList = contentsService.getMyLikeList(token, searchDto);

        // 검색 성공
        String sMessage = super.langMessage("lang.common.success.search");

        JSONObject data = new JSONObject();
        data.put("list", contentsList);
        data.put("params", new JSONObject(searchDto));

        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 내가 태그된 컨텐츠 상세 리스트
     *
     * @param searchDto
     * @return
     */
    @GetMapping("/tag/contents/detail")
    public ResponseEntity getTagContentsDetail(@RequestHeader(value = "Authorization") String token,
                                               @ModelAttribute SearchDto searchDto) {

        // 이미지 offset setting
        searchDto.setImgOffSet(0);

        // 내가 태그된 컨텐츠 상세 조회
        List<ContentsDto> contentsList = contentsService.getMyTagList(token, searchDto);

        // 검색 성공
        String sMessage = super.langMessage("lang.common.success.search");

        JSONObject data = new JSONObject();
        data.put("list", contentsList);
        data.put("params", new JSONObject(searchDto));

        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 내가 저장한 컨텐츠 상세 리스트
     *
     * @param token
     * @param searchDto
     * @return
     */
    @GetMapping("/save/contents/detail")
    public ResponseEntity getSaveContentsDetail(@RequestHeader(value = "Authorization") String token,
                                                @ModelAttribute SearchDto searchDto) {

        // 이미지 offset setting
        searchDto.setImgOffSet(0);

        // 내가 저장한 컨텐츠 상세 조회
        List<ContentsDto> contentsList = contentsService.getMySaveList(token, searchDto);

        // 검색 성공
        String sMessage = super.langMessage("lang.common.success.search");

        JSONObject data = new JSONObject();
        data.put("list", contentsList);
        data.put("params", new JSONObject(searchDto));

        return displayJson(true, "1000", sMessage, data);
    }

}
