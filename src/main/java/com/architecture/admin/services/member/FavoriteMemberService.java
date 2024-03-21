package com.architecture.admin.services.member;

import com.architecture.admin.libraries.PaginationLibray;
import com.architecture.admin.libraries.exception.CurlException;
import com.architecture.admin.models.daosub.member.FavoriteMemberDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;
import com.architecture.admin.services.BaseService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/*****************************************************
 * 교류많은 회원
 * 20230713 기준 : 팔로우 한 회원 중 컨텐츠 좋아요 2개 이상
 ****************************************************/
@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteMemberService extends BaseService {

    private final FavoriteMemberDaoSub favoriteMemberDaoSub;

    /*****************************************************
     *  Modules
     ****************************************************/
    /**
     * 교류많은 유저 리스트 가져오기
     *
     * @param token     access token
     * @param searchDto page 페이지 limit 페이지에 노출 될 개수
     * @return 교류많은 유저 List
     */
    public List<MemberInfoDto> getFavoriteMemberList(String token, SearchDto searchDto) throws JsonProcessingException {
        List<MemberInfoDto> favoriteList = new ArrayList<>();
        List<String> uuidList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);
        searchDto.setMemberUuid(memberDto.getUuid());

        // 교류 많은 유저 목록 전체 count
        Long totalCount = favoriteMemberDaoSub.getFavoriteMemberCount(searchDto);

        if (totalCount > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(Math.toIntExact(totalCount), searchDto);
            searchDto.setPagination(pagination);

            // 교류 많은 유저 리스트 가져오기
            favoriteList = favoriteMemberDaoSub.getFavoriteMemberList(searchDto);

            // list 에서 memberUuid 추출
            favoriteList.forEach(item -> {
                uuidList.add(item.getUuid());
            });

            // curl 회원 조회
            String jsonString = memberCurlService.getMemberInfoByUuidList(uuidList);

            JSONObject jsonObject = new JSONObject(jsonString);
            if (!(jsonObject.getBoolean("result"))) {
                throw new CurlException(jsonObject);
            }

            JsonObject listJson = (JsonObject) JsonParser.parseString(jsonObject.get("data").toString()); // data 안 list 파싱
            List<MemberInfoDto> memberInfoList = mapper.readValue(listJson.get("memberInfoList").toString(), new TypeReference<>() {
            }); // 회원 정보 list

            // uuid 로 회원 정보 매핑
            for (MemberInfoDto memberInfo : memberInfoList) {
                for (MemberInfoDto memberInfoDto : favoriteList) {
                    if (Objects.equals(memberInfoDto.getUuid(), memberInfo.getUuid())) {
                        memberInfoDto.setNick(memberInfo.getNick());
                        memberInfoDto.setProfileImgUrl(memberInfo.getProfileImgUrl());
                        memberInfoDto.setIntro(memberInfo.getIntro());
                    }
                }
            }
        }

        // 닉네임이 비어있으면 제외
        List<MemberInfoDto> filteredList = new ArrayList<>();
        for (MemberInfoDto memberInfoDto : favoriteList) {
            if (memberInfoDto.getNick() != null && !memberInfoDto.getNick().isEmpty()) {
                filteredList.add(memberInfoDto);
            }
        }

        return filteredList;
    }

    /**
     * 회사 계정 리스트 가져오기
     *
     * @param searchDto   page 페이지 limit 페이지에 노출 될 개수
     * @param httpRequest httpRequest
     * @return 회사 계정 리스트
     */
    public List<MemberInfoDto> getOfficialAccountList(SearchDto searchDto, HttpServletRequest httpRequest) throws ParseException, JsonProcessingException {
        List<MemberInfoDto> list = new ArrayList<>();
        List<String> uuidList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        // list
        list = favoriteMemberDaoSub.getOfficialAccountList(searchDto);

        String sRegDate = "";
        Date dRegDate = null;

        if (!ObjectUtils.isEmpty(list)) {

            // 오늘 날짜 UTC 기준으로 가져오기
            String today = dateLibrary.getDatetime();
            String nowYMD = today.substring(0, 10);
            String sStartDate = nowYMD + " 00:00:00";

            // 포맷터
            SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            for (MemberInfoDto member : list) {

                /* 레드닷 여부 추가 */
                // 오늘 등록한 게시물 중 최근 등록일 가져오기
                member.setRegDate(sStartDate);
                sRegDate = getLastRegDateByOfficial(member);
                member.setRegDate(null);

                // 오늘 등록한 게시물 X
                if (ObjectUtils.isEmpty(sRegDate)) {
                    // 레드닷 비활성화
                    member.setRedDotState(0);
                }
                // 오늘 등록한 게시물 O
                else {
                    // 날짜 비교 위해 date 타입으로 변환
                    dRegDate = formatDatetime.parse(sRegDate);

                    // 쿠키 가져오기 -> 회사 계정 게시물을 들어갔거나, 유저 페이지 방문한 경우 쿠키 생성해줌
                    Cookie[] cookies = httpRequest.getCookies(); // 모든 쿠키 가져오기
                    String decodedValue = "";

                    if (cookies != null) {
                        for (Cookie cookie : cookies) {
                            String name = cookie.getName(); // 쿠키 이름 가져오기

                            // 회사 계정 레드닷 관련 쿠키가 있으면 value 값 가져오기
                            if (name.equals("officialRedDot")) {
                                String cookieValue = cookie.getValue(); // 쿠키 값 가져오기
                                decodedValue = URLDecoder.decode(cookieValue, StandardCharsets.UTF_8);
                            }
                        }
                    }

                    // 쿠키가 있는 경우
                    if (!decodedValue.equals("")) {
                        Date dRedDotDate = formatDatetime.parse(decodedValue);

                        // 게시물 등록일이 레드닷 생성된 시간 보다 이후인지 체크
                        if (dRegDate.after(dRedDotDate)) { // 이후라면
                            member.setRedDotState(1); // 레드닷 활성화
                        } else {
                            member.setRedDotState(0); // 레드닷 비활성화
                        }

                    }
                    // 쿠키가 없는 경우 (아직 안 본 상태)
                    else {
                        member.setRedDotState(1); // 레드닷 활성화
                    }

                }

                uuidList.add(member.getUuid());

            }

            // curl 회원 조회
            if (!ObjectUtils.isEmpty(uuidList)) {

                String jsonString = memberCurlService.getMemberInfoByUuidList(uuidList);

                JSONObject jsonObject = new JSONObject(jsonString);
                if (!(jsonObject.getBoolean("result"))) {
                    throw new CurlException(jsonObject);
                }

                JsonObject listJson = (JsonObject) JsonParser.parseString(jsonObject.get("data").toString()); // data 안 list 파싱
                List<MemberInfoDto> memberInfoList = mapper.readValue(listJson.get("memberInfoList").toString(), new TypeReference<>() {
                }); // 회원 정보 list

                // uuid 로 회원 정보 매핑
                for (MemberInfoDto memberInfo : memberInfoList) {
                    for (MemberInfoDto memberInfoDto : list) {
                        if (Objects.equals(memberInfoDto.getUuid(), memberInfo.getUuid())) {
                            memberInfoDto.setNick(memberInfo.getNick());
                            memberInfoDto.setProfileImgUrl(memberInfo.getProfileImgUrl());
                            memberInfoDto.setIntro(memberInfo.getIntro());
                        }
                    }
                }
            }
        }

        // 닉네임이 비어있으면 제외
        List<MemberInfoDto> filteredList = new ArrayList<>();
        for (MemberInfoDto memberInfoDto : list) {
            if (memberInfoDto.getNick() != null && !memberInfoDto.getNick().isEmpty()) {
                filteredList.add(memberInfoDto);
            }
        }

        return filteredList;
    }

    /**
     * 오늘 등록한 게시물 중 가장 최근 등록일 가져오기
     *
     * @param memberInfoDto uuid, regDate(오늘)
     * @return 등록일
     */
    public String getLastRegDateByOfficial(MemberInfoDto memberInfoDto) {
        return favoriteMemberDaoSub.getLastRegDateByOfficial(memberInfoDto);
    }

    /*****************************************************
     *  SubFunction - Etc
     ****************************************************/

}
