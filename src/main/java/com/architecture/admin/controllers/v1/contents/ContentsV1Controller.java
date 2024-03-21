package com.architecture.admin.controllers.v1.contents;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.contents.ContentsDto;
import com.architecture.admin.models.dto.contents.ContentsImgDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;
import com.architecture.admin.services.contents.ContentsService;
import com.architecture.admin.services.member.MemberInfoService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/contents")
public class ContentsV1Controller extends BaseController {

    private final ContentsService contentsService;
    private final MemberInfoService memberInfoService;
    @Value("${contents.sns.type}")
    private int sns;  // sns 타입 (menu : 1)
    @Value("${use.contents.register}")
    private boolean useContentsRegister; // 소셜 콘텐츠 등록 true/false
    @Value("${use.contents.modify}")
    private boolean useContentsModify; // 소셜 콘텐츠 수정 true/false
    @Value("${official.uuid}")
    private String officialUuid;        // 회사 계정 uuid

    /**
     * 회원 콘텐츠 상세 리스트
     *
     * @param token         : access token
     * @param searchDto     : memberUuid [콘텐츠 작성자 uuid], page [현재 페이지], imgLimit [이미지 개수]
     * @param response      : response
     * @return
     */
    @GetMapping("/member/detail")
    public ResponseEntity getMemberContentsList(@RequestHeader(value = "Authorization", defaultValue = "") String token,
                                                @ModelAttribute SearchDto searchDto,
                                                HttpServletResponse response) {

        // 공식 계정에 들어온 경우 [교류 많은 유저] 레드닷 지워지도록 쿠키 생성
        if (Objects.equals(searchDto.getMemberUuid(), officialUuid)) {

            String datetime = dateLibrary.getDatetime();
            // 쿠키 value 공백 입력 불가하여 URL encode 사용
            String encodedNow = URLEncoder.encode(datetime, StandardCharsets.UTF_8);

            // 쿠키 생성
            Cookie officialRedDot = new Cookie("officialRedDot", encodedNow); // 쿠키 이름 지정하여 생성( key, value 개념)
            officialRedDot.setMaxAge(60 * 60 * 24); // 쿠키 유효 기간: 하루로 설정(60초 * 60분 * 24시간)
            officialRedDot.setPath("/"); // 모든 경로에서 접근 가능하도록 설정

            response.addCookie(officialRedDot); // 응답 헤더에 추가
        }

        // 이미지 offset setting
        searchDto.setImgOffSet(0);

        // 회원 콘텐츠 리스트 조회
        List<ContentsDto> list = contentsService.getContentsDetailList(token, searchDto);
        // 조회용 dto
        MemberDto memberDto = MemberDto.builder()
                .uuid(searchDto.getLoginMemberUuid())    // 로그인 회원 uuid
                .memberUuid(searchDto.getMemberUuid())   // 해당 회원 uuid
                .build();

        // 작성자 정보 조회
        MemberInfoDto memberInfo = memberInfoService.getMemberInfo(memberDto);

        String sMessage = super.langMessage("lang.common.success.search");

        JSONObject data = new JSONObject();
        data.put("list", list);
        data.put("memberInfo", new JSONObject(memberInfo));
        data.put("params", new JSONObject(searchDto));

        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 최신 콘텐츠 상세 리스트
     *
     * @param token     : access token
     * @param searchDto : memberUuid [콘텐츠 작성자 uuid], page [현재 페이지], imgLimit [이미지 개수]
     * @param response  : response
     * @return
     */
    @GetMapping("/recent/detail")
    public ResponseEntity getRecentList(@RequestHeader(value = "Authorization", defaultValue = "") String token,
                                        @ModelAttribute SearchDto searchDto,
                                        HttpServletResponse response) {

        // 공식 계정에 들어온 경우 [교류 많은 유저] 레드닷 지워지도록 쿠키 생성
        if (Objects.equals(searchDto.getMemberUuid(), officialUuid)) {

            String datetime = dateLibrary.getDatetime();
            // 쿠키 value 공백 입력 불가하여 URL encode 사용
            String encodedNow = URLEncoder.encode(datetime, StandardCharsets.UTF_8);

            // 쿠키 생성
            Cookie officialRedDot = new Cookie("officialRedDot", encodedNow); // 쿠키 이름 지정하여 생성( key, value 개념)
            officialRedDot.setMaxAge(60 * 60 * 24); // 쿠키 유효 기간: 하루로 설정(60초 * 60분 * 24시간)
            officialRedDot.setPath("/"); // 모든 경로에서 접근 가능하도록 설정

            response.addCookie(officialRedDot); // 응답 헤더에 추가
        }

        // 이미지 offset setting
        searchDto.setImgOffSet(0);

        // 최신 콘텐츠 리스트 조회
        List<ContentsDto> list = contentsService.getRecentList(token, searchDto);

        String sMessage = super.langMessage("lang.common.success.search");

        JSONObject data = new JSONObject();
        data.put("list", list);
        data.put("params", new JSONObject(searchDto));

        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 회원 태그된 콘텐츠 상세 리스트
     *
     * @param token     : access token
     * @param searchDto : memberUuid [태그된 회원 uuid], , page [현재 페이지], imgLimit [이미지 개수]
     * @return
     */
    @GetMapping("/tag/detail")
    public ResponseEntity getMemberTagList(@RequestHeader(value = "Authorization", defaultValue = "") String token,
                                           @ModelAttribute SearchDto searchDto,
                                           HttpServletResponse response) {

        // 공식 계정에 들어온 경우 [교류 많은 유저] 레드닷 지워지도록 쿠키 생성
        if (Objects.equals(searchDto.getMemberUuid(), officialUuid)) {

            String datetime = dateLibrary.getDatetime();
            // 쿠키 value 공백 입력 불가하여 URL encode 사용
            String encodedNow = URLEncoder.encode(datetime, StandardCharsets.UTF_8);

            // 쿠키 생성
            Cookie officialRedDot = new Cookie("officialRedDot", encodedNow); // 쿠키 이름 지정하여 생성( key, value 개념)
            officialRedDot.setMaxAge(60 * 60 * 24); // 쿠키 유효 기간: 하루로 설정(60초 * 60분 * 24시간)
            officialRedDot.setPath("/"); // 모든 경로에서 접근 가능하도록 설정

            response.addCookie(officialRedDot); // 응답 헤더에 추가
        }

        // 이미지 offset setting
        searchDto.setImgOffSet(0);

        // 회원 콘텐츠 리스트 조회
        List<ContentsDto> list = contentsService.getMemberTagList(token, searchDto);

        String sMessage = super.langMessage("lang.common.success.search");

        JSONObject data = new JSONObject();
        data.put("list", list);
        data.put("params", new JSONObject(searchDto));

        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 해시태그 상세 리스트
     *
     * @param token     : access token
     * @param searchDto : page [현재 페이지], imgLimit [이미지 개수]
     * @return
     */
    @GetMapping("/hashtag/detail")
    public ResponseEntity getHashTagDetail(@RequestHeader(value = "Authorization", defaultValue = "") String token,
                                           @ModelAttribute SearchDto searchDto) {

        // 이미지 offset setting
        searchDto.setImgOffSet(0);

        // 해시 태그 리스트 조회
        List<ContentsDto> list = contentsService.getHashTagList(token, searchDto);

        String sMessage = super.langMessage("lang.common.success.search");

        JSONObject data = new JSONObject();
        data.put("list", list);
        data.put("params", new JSONObject(searchDto));

        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 인기 콘텐츠 상세 리스트
     *
     * @param token     : access token
     * @param searchDto : page [현재 페이지], imgLimit [이미지 개수]
     * @return
     */
    @GetMapping("/week/popular/detail")
    public ResponseEntity getWeekPopularList(@RequestHeader(value = "Authorization", defaultValue = "") String token,
                                             SearchDto searchDto) {

        // 이미지 offset setting
        searchDto.setImgOffSet(0);

        // 인기 콘텐츠 리스트 조회
        List<ContentsDto> list = contentsService.getWeekPopularList(token, searchDto);

        String sMessage = super.langMessage("lang.common.success.search");

        JSONObject data = new JSONObject();
        data.put("list", list);
        data.put("params", new JSONObject(searchDto));

        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 급상승 인기 콘텐츠 상세 리스트
     *
     * @param token     : access token
     * @param searchDto :  page [현재 페이지], imgLimit [이미지 개수]
     * @return
     */
    @GetMapping("/hour/popular/detail")
    public ResponseEntity getHourPopularList(@RequestHeader(value = "Authorization", defaultValue = "") String token,
                                             @ModelAttribute SearchDto searchDto) {

        // 이미지 offset setting
        searchDto.setImgOffSet(0);

        // 급상승 인기 콘텐츠 리스트 조회
        List<ContentsDto> list = contentsService.getHourPopularList(token, searchDto);

        String sMessage = super.langMessage("lang.common.success.search");

        JSONObject data = new JSONObject();
        data.put("list", list);
        data.put("params", new JSONObject(searchDto));

        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 콘텐츠 상세 단일
     *
     * @param token       : access token
     * @param contentsIdx : 콘텐츠 idx
     * @param searchDto   : imgLimit [이미지 개수]
     * @return
     */
    @GetMapping("/{contentsIdx}")
    public ResponseEntity getContentsDetail(@RequestHeader(value = "Authorization", defaultValue = "") String token,
                                            @PathVariable Long contentsIdx,
                                            @ModelAttribute SearchDto searchDto,
                                            HttpServletResponse response) {

        searchDto.setContentsIdx(contentsIdx);
        // 이미지 offset setting
        searchDto.setImgOffSet(0);

        MemberInfoDto memberInfo = null; // 작성자 정보

        // 콘텐츠 상세 조회
        ContentsDto contentsDto = contentsService.getContentsDetail(token, searchDto);

        // 작성자 정보 조회
        if (contentsDto != null) {
            memberInfo = memberInfoService.getMemberInfoByContentsIdx(searchDto);
            if (memberInfo.getUuid().equals(officialUuid)) {
                String datetime = dateLibrary.getDatetime();
                // 쿠키 value 공백 입력 불가하여 URL encode 사용
                String encodedNow = URLEncoder.encode(datetime, StandardCharsets.UTF_8);

                // 쿠키 생성
                Cookie officialRedDot = new Cookie("officialRedDot", encodedNow); // 쿠키 이름 지정하여 생성( key, value 개념)
                officialRedDot.setMaxAge(60 * 60 * 24); // 쿠키 유효 기간: 하루로 설정(60초 * 60분 * 24시간)
                officialRedDot.setPath("/"); // 모든 경로에서 접근 가능하도록 설정

                response.addCookie(officialRedDot); // 응답 헤더에 추가
            }
        }
        String sMessage = super.langMessage("lang.common.success.search");

        // 결과값 없을 경우 빈 데이터 생성
        if (contentsDto == null) {
            contentsDto = new ContentsDto();
            memberInfo = new MemberInfoDto();
        }

        JSONObject data = new JSONObject();
        data.put("list", new JSONObject(contentsDto));
        data.put("memberInfo", new JSONObject(memberInfo));
        data.put("params", new JSONObject(searchDto));

        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 콘텐츠 상세 이미지
     *
     * @param token       : access token
     * @param contentsIdx : 콘텐츠 idx
     * @param searchDto   : imgLimit [이미지 개수], imgOffSet [이미지 시작 위치]
     * @return
     */
    @GetMapping("/{contentsIdx}/images")
    public ResponseEntity contentsImgList(@RequestHeader(value = "Authorization", defaultValue = "") String token,
                                          @PathVariable Long contentsIdx,
                                          @ModelAttribute SearchDto searchDto) {

        searchDto.setContentsIdx(contentsIdx);
        // 이미지 리스트 조회
        List<ContentsImgDto> list = contentsService.getContentsImgList(token, searchDto);

        // 검색 성공
        String sMessage = super.langMessage("lang.common.success.search");

        JSONObject data = new JSONObject();
        data.put("list", list);
        data.put("params", new JSONObject(searchDto));

        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 소셜 콘텐츠 등록
     *
     * @param contentsDto menuIdx, contents, uploadFile, location, isView, imgTagList
     * @return ResponseEntity
     */
    @PostMapping("")
    public ResponseEntity registContents(@RequestHeader("Authorization") String token,
                                         @ModelAttribute ContentsDto contentsDto) {

        if (useContentsRegister) {
            // 콘텐츠 등록
            contentsService.registContents(token, contentsDto, sns);
        } else {
            throw new CustomException(CustomError.SWITCH_FALSE_ERROR); // 이용 불가한 기능입니다.
        }
        // 등록 완료
        String sMessage = super.langMessage("lang.contents.success.regist");
        // response object
        return displayJson(true, "1000", sMessage);
    }

    /**
     * 소셜 콘텐츠 수정
     *
     * @param token         access token
     * @param contentsIdx   콘텐츠 idx
     * @param contentsDto   contents, uploadFile, location, isView, imgTagList
     * @return
     */
    @PutMapping(("{contentsIdx}"))
    public ResponseEntity modifyContents(@RequestHeader("Authorization") String token,
                                         @PathVariable("contentsIdx") Long contentsIdx,
                                         @RequestBody ContentsDto contentsDto) {
        Long result;
        if (useContentsModify) {
            contentsDto.setIdx(contentsIdx);
            // 콘텐츠 수정
            result = contentsService.modifyContents(token, contentsDto, sns);
        } else {
            throw new CustomException(CustomError.SWITCH_FALSE_ERROR); // 이용 불가한 기능입니다.
        }

        // 수정 완료
        String sMessage = super.langMessage("lang.contents.success.modify");

        // response object
        JSONObject data = new JSONObject(result);

        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 콘텐츠 삭제
     *
     * @param token   access token
     * @param idxList 콘텐츠 idx
     * @return ResponseEntity
     */
    @DeleteMapping("")
    public ResponseEntity deleteContentsList(@RequestHeader("Authorization") String token,
                                             @RequestParam("idx") List<Long> idxList) {

        Long result;

        ContentsDto contentsDto = new ContentsDto();
        contentsDto.setIdxList(idxList);

        // 콘텐츠 삭제
        result = contentsService.deleteContents(token, contentsDto);

        String sMessage;
        // 삭제 완료
        if (result > 0) {
            sMessage = super.langMessage("lang.contents.success.delete");
        } else {
            sMessage = super.langMessage("lang.contents.exception.delete");
        }
        return displayJson(true, "1000", sMessage);
    }

}
