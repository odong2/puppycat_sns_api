package com.architecture.admin.controllers.v1.search;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;
import com.architecture.admin.models.dto.search.SearchLogDto;
import com.architecture.admin.models.dto.tag.HashTagDto;
import com.architecture.admin.services.member.FavoriteMemberService;
import com.architecture.admin.services.search.SearchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/search")
public class SearchV1Controller extends BaseController {

    private final SearchService searchService;
    private final FavoriteMemberService favoriteMemberService;

    /**
     * 전체 검색
     *
     * @param searchDto 검색 조건
     * @param result
     * @return
     */
    @GetMapping("")
    public ResponseEntity searchLists(@RequestHeader(value = "Authorization", defaultValue = "") String token,
                                      @ModelAttribute SearchDto searchDto,
                                      BindingResult result) {
        // recodeSize 유효성 체크
        if (result.hasErrors()) {
            return super.displayError(result);
        }

        int nickCount = 0;
        int tagCount = 0;

        //검색어 공백제거
        searchDto.setSearchWord(searchDto.getSearchWord().trim());

        // 회원 검색
        List<MemberInfoDto> nickList = searchService.getSearchNickList(token, searchDto);

        if (nickList != null) {
            nickCount = nickList.size();
        }
        // 해시태그 검색
        List<HashTagDto> tagList = searchService.getSearchHashTagList(token, searchDto);
        if (tagList != null) {
            tagCount = tagList.size();
        }
        // 로그 등록
        searchService.inserSearchLog(searchDto);

        Map<String, Object> map = new HashMap<>();
        map.put("nick_list", nickList);
        map.put("tag_list", tagList);
        map.put("nick_count", nickCount);
        map.put("tag_count", tagCount);

        String sMessage;

        if (tagCount != 0 || nickCount != 0) {
            // 검색 성공
            sMessage = super.langMessage("lang.common.success.search");
        } else {
            // 검색 내용이 없습니다.
            sMessage = super.langMessage("lang.common.exception.search");
            // 추천 검색어 가져오기
            List<SearchLogDto> bestList = searchService.getSeachLogList();
            map.put("best_list", bestList);
        }

        JSONObject data = new JSONObject(map);
        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * '@' 멘션 검색 / 프로필 검색
     *
     * @param searchDto searchWord
     * @param result
     * @return
     */
    @GetMapping("nick")
    public ResponseEntity nickSearchLists(@RequestHeader(value = "Authorization", defaultValue = "") String token,
                                          @ModelAttribute SearchDto searchDto,
                                          BindingResult result) {
        // recodeSize 유효성 체크
        if (result.hasErrors()) {
            return super.displayError(result);
        }

        // 검색어 공백제거
        searchDto.setSearchWord(searchDto.getSearchWord().trim());
        List<MemberInfoDto> list = new ArrayList<>();

        // 검색어가 존재할때만 조회
        if (!Objects.equals(searchDto.getSearchWord(), "")) {
            // list
            list = searchService.getSearchNickList(token, searchDto);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("list", list);
        map.put("params", searchDto);

        String sMessage;

        if (!list.isEmpty()) {
            // 검색 성공
            sMessage = super.langMessage("lang.common.success.search");
        } else {
            // 검색 내용이 없습니다.
            sMessage = super.langMessage("lang.common.exception.search");
            // 추천 검색어 가져오기
            List<SearchLogDto> bestList = searchService.getSeachLogList();
            map.put("best_list", bestList);
        }

        JSONObject data = new JSONObject(map);
        return displayJson(true, "1000", sMessage, data);
    }

    /**
     *  '@'만 입력시 교류많은 회원 리스트
     *
     * @param token
     * @param searchDto
     * @return
     */
    @GetMapping("nick/mention")
    public ResponseEntity nickMentionSearchLists(@RequestHeader(value = "Authorization") String token,
                                                 @ModelAttribute SearchDto searchDto) {

        // list
        List<MemberDto> list = searchService.getMentionMemberList(token, searchDto);
        Map<String, Object> map = new HashMap<>();
        map.put("list", list);
        map.put("params", searchDto);

        String sMessage;

        if (!list.isEmpty()) {
            // 검색 성공
            sMessage = super.langMessage("lang.common.success.search");
        } else {
            // 검색 내용이 없습니다.
            sMessage = super.langMessage("lang.common.exception.search");
        }

        JSONObject data = new JSONObject(map);
        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 이미지 내 태그시 최근 태그한 회원 리스트 및 교류많은 유저
     *
     * @param token     access token
     * @param searchDto
     * @return
     */
    @GetMapping("nick/img")
    public ResponseEntity imgSearchLists(@RequestHeader(value = "Authorization") String token,
                                         @ModelAttribute SearchDto searchDto) throws JsonProcessingException {

        // list
        List<MemberDto> list = searchService.getLatelyImgTagMemberList(token, searchDto);
        List<MemberInfoDto> lists = null;
        String sMessage;

        // 최근 이미지 태그한 회원이 존재한다면
        if (!list.isEmpty()) {
            // 검색 성공
            sMessage = super.langMessage("lang.common.success.search");
        } else {
            lists = favoriteMemberService.getFavoriteMemberList(token, searchDto);
            if (!lists.isEmpty()) {
                // 검색 성공
                sMessage = super.langMessage("lang.common.success.search");
            } else {
                // 리스트가 비었습니다.
                sMessage = super.langMessage("lang.common.exception.search");
            }
        }

        Map<String, Object> map = new HashMap<>();
        if (!list.isEmpty()) {
            map.put("list", list);
        } else if (lists != null) {
            map.put("list", lists);
        }
        map.put("params", searchDto);

        JSONObject data = new JSONObject(map);
        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 해시태그 검색
     *
     * @param searchDto
     * @param result
     * @return
     */
    @GetMapping("tag")
    public ResponseEntity tagSearchLists(@RequestHeader(value = "Authorization", defaultValue = "") String token,
                                         @ModelAttribute SearchDto searchDto,
                                         BindingResult result) {
        // recodeSize 유효성 체크
        if (result.hasErrors()) {
            return super.displayError(result);
        }

        //검색어 공백제거
        searchDto.setSearchWord(searchDto.getSearchWord().trim());

        // list
        List<HashTagDto> list = searchService.getSearchHashTagList(token, searchDto);

        Map<String, Object> map = new HashMap<>();
        map.put("list", list);
        map.put("params", searchDto);

        String sMessage;

        if (!list.isEmpty()) {
            // 검색 성공
            sMessage = super.langMessage("lang.common.success.search");
        } else {
            // 검색 내용이 없습니다.
            sMessage = super.langMessage("lang.common.exception.search");
            // 추천 검색어 가져오기
            List<SearchLogDto> bestList = searchService.getSeachLogList();
            map.put("best_list", bestList);
        }

        JSONObject data = new JSONObject(map);
        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 해시태그 검색 컨텐츠
     *
     * @param searchDto
     * @param result
     * @return
     */
    @GetMapping("tag/contents")
    public ResponseEntity tagSearchContentsLists(@RequestHeader(value = "Authorization", defaultValue = "") String token,
                                                 @ModelAttribute SearchDto searchDto,
                                                 BindingResult result) {
        // recodeSize 유효성 체크
        if (result.hasErrors()) {
            return super.displayError(result);
        }

        // list
        List<HashTagDto> list = searchService.getSearchHashTagContentsList(token, searchDto);

        Map<String, Object> map = new HashMap<>();
        map.put("list", list);
        map.put("params", searchDto);

        String sMessage;

        if (!list.isEmpty()) {
            // 검색 성공
            sMessage = super.langMessage("lang.common.success.search");
            String sHashTagContentsCnt = numberFormatLibrary.krFormatNumber((long) searchDto.getPagination().getTotalRecordCount());

            map.put("totalCnt", sHashTagContentsCnt);
        } else {
            // 검색 내용이 없습니다.
            sMessage = super.langMessage("lang.common.exception.search");
            // 추천 검색어 가져오기
            List<SearchLogDto> bestList = searchService.getSeachLogList();
            map.put("best_list", bestList);
        }

        JSONObject data = new JSONObject(map);
        return displayJson(true, "1000", sMessage, data);
    }
}
