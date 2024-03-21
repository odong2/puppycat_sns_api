package com.architecture.admin.services.search;

import com.architecture.admin.libraries.PaginationLibray;
import com.architecture.admin.libraries.exception.CurlException;
import com.architecture.admin.models.dao.search.SearchDao;
import com.architecture.admin.models.daosub.follow.FollowDaoSub;
import com.architecture.admin.models.daosub.search.SearchDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;
import com.architecture.admin.models.dto.search.SearchLogDto;
import com.architecture.admin.models.dto.tag.HashTagDto;
import com.architecture.admin.services.BaseService;
import com.architecture.admin.services.member.MemberService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/*****************************************************
 * 검색 모델러
 ****************************************************/
@Service
@RequiredArgsConstructor
@Transactional
public class SearchService extends BaseService {

    private final SearchDao searchDao;
    private final SearchDaoSub searchDaoSub;
    private final FollowDaoSub followDaoSub;
    private final SearchCurlService searchCurlService;
    private final MemberService memberService;
    @Value("${cloud.aws.s3.img.url}")
    private String imgDomain;

    /*****************************************************
     *  Modules
     ****************************************************/
    public List<MemberInfoDto> getSearchNickList(String token, SearchDto searchDto) {
        MemberDto memberDto;
        if (token != null && !token.equals("")) {
            // 회원 UUID 조회
            memberDto = super.getMemberUuidByToken(token);
            searchDto.setLoginMemberUuid(memberDto.getUuid());
        }
        List<MemberInfoDto> searchList = new ArrayList<>();

        // 검색어 LIKE 검색 UUID 리스트
        String getSearchNickUuid = searchCurlService.getSearchNickUuid(searchDto.getSearchWord());
        JSONObject getSearchNickUuidObject = new JSONObject(getSearchNickUuid);
        if (!((boolean) getSearchNickUuidObject.get("result"))) {
            throw new CurlException(getSearchNickUuidObject);
        }
        JSONObject getSearchNickUuidResult = (JSONObject) getSearchNickUuidObject.get("data");

        // 목록 전체 count
        JSONArray searchMemberUuidList = getSearchNickUuidResult.getJSONArray("searchMemberUuidList");
        List<String> memberUUidList = new ArrayList<>();
        Long memberUuidCnt = 0L;
        Long blockMemberUuidCnt = 0L;
        if (searchMemberUuidList != null) {
            for (int i = 0; i < searchMemberUuidList.length(); i++) {
                memberUUidList.add(searchMemberUuidList.getString(i));
                memberUuidCnt++;

                if (searchDto.getLoginMemberUuid() != null && !searchDto.getLoginMemberUuid().equals("")) {
                    boolean chekBlock = super.bChkBlock(searchDto.getLoginMemberUuid(), searchMemberUuidList.getString(i));
                    if (chekBlock) {
                        memberUUidList.remove(searchMemberUuidList.getString(i));
                        blockMemberUuidCnt++;
                    }
                }
            }
        }

        // 전체 카운트 - 차단 회원 카운트
        long totalCount = memberUuidCnt - blockMemberUuidCnt;

        if (totalCount > 0) {
            // 리스트 값 세팅
            List<MemberInfoDto> sameSearchList = new ArrayList<>();
            List<MemberInfoDto> followSearchList = new ArrayList<>();
            List<MemberInfoDto> memberSearchList = new ArrayList<>();
            MemberInfoDto getCurlMemberInfoDto = null;

            // 1. 검색어가 완전 일치하는 회원 리스트
            if (searchDto.getPage() == 1) {
                // 1. 검색어가 완전 일치한 회원 카운트

                // 닉네임이 완전이 일치하는 UUID
                String getSameNickUuid = searchCurlService.getSameNickUuid(searchDto.getSearchWord());
                JSONObject searchNickMemberUuidObject = new JSONObject(getSameNickUuid);
                if (!((boolean) searchNickMemberUuidObject.get("result"))) {
                    throw new CurlException(searchNickMemberUuidObject);
                }
                JSONObject searchNickMemberUuidResult = (JSONObject) searchNickMemberUuidObject.get("data");
                String searchSameNickMemberUuid = (String) searchNickMemberUuidResult.get("searchMemberUuid");

                if (!Objects.equals(searchSameNickMemberUuid, "")) {
                    if (searchDto.getLoginMemberUuid() != null && !searchDto.getLoginMemberUuid().equals("")) {
                        boolean chekBlock = super.bChkBlock(searchDto.getLoginMemberUuid(), searchSameNickMemberUuid);
                        if (!chekBlock) {
                            // 회원 정보 가져오기
                            getCurlMemberInfoDto = memberCurlService.getMemberInfoByUuid(searchSameNickMemberUuid);
                        }
                    } else {
                        // 회원 정보 가져오기
                        getCurlMemberInfoDto = memberCurlService.getMemberInfoByUuid(searchSameNickMemberUuid);
                    }

                    MemberDto badgeInfo = memberService.getMemberBadgeInfoByUuid(searchSameNickMemberUuid);
                    if (badgeInfo != null && badgeInfo.getIsBadge() != null) {
                        getCurlMemberInfoDto.setIsBadge(badgeInfo.getIsBadge());
                    }
                    sameSearchList.add(getCurlMemberInfoDto);
                    // 회원 리스트에서 검색어 같은 UUID 제거 해주기
                    memberUUidList.remove(searchSameNickMemberUuid);
                }
            }
            // 로그인 한 회원이라면
            if (searchDto.getLoginMemberUuid() != null) {
                searchDto.setMemberUuidList(memberUUidList);
                if (!memberUUidList.isEmpty()) {

                    // 2. 내 팔로우 중 검색어가 포함 된 회원 리스트
                    List<String> searchNickFollowUuid = searchDaoSub.getFollowSearchNickList(searchDto);
                    searchDto.setMemberUuidList(searchNickFollowUuid);

                    if (searchNickFollowUuid != null && !searchNickFollowUuid.isEmpty()) {
                        // uuid로 회원 MEMBER 정보 가져오기
                        String getSearchNickFollowMemberInfo = memberCurlService.getMemberInfoOrderByNick(searchDto);
                        JSONObject searchNickFollowMemberInfoObject = new JSONObject(getSearchNickFollowMemberInfo);
                        if (!((boolean) searchNickFollowMemberInfoObject.get("result"))) {
                            throw new CurlException(searchNickFollowMemberInfoObject);
                        }

                        JSONObject searchNickFollowMemberResult = (JSONObject) searchNickFollowMemberInfoObject.get("data");
                        // 목록 전체 count
                        JSONArray searchNickFollowMemberInfo = searchNickFollowMemberResult.getJSONArray("memberInfo");
                        if (searchNickFollowMemberInfo != null && !searchNickFollowMemberInfo.isEmpty()) {
                            searchNickFollowMemberInfo.forEach(memberInfo -> {
                                JSONObject member = (JSONObject) memberInfo;
                                MemberInfoDto memberInfoDto = new MemberInfoDto();
                                memberInfoDto.setUuid(member.getString("uuid"));
                                memberInfoDto.setNick(member.getString("nick"));
                                memberInfoDto.setIntro(member.getString("intro"));
                                memberInfoDto.setProfileImgUrl(member.getString("profileImgUrl"));

                                MemberDto badgeInfo = memberService.getMemberBadgeInfoByUuid(member.getString("uuid"));
                                if (badgeInfo != null && badgeInfo.getIsBadge() != null) {
                                    memberInfoDto.setIsBadge(badgeInfo.getIsBadge());
                                }
                                followSearchList.add(memberInfoDto);

                                // 회원 리스트에서 UUID 제거 해주기
                                memberUUidList.remove(member.getString("uuid"));
                            });
                        }
                    }
                }
            }

            // 3. 팔로우 한 회원이 아닌 검색어가 포함 된 회원 리스트
            if (!memberUUidList.isEmpty()) {
                if (searchDto.getLoginMemberUuid() != null && !searchDto.getLoginMemberUuid().equals("")) {
                    for (String uuid : memberUUidList) {
                        boolean chekBlock = super.bChkBlock(searchDto.getLoginMemberUuid(), uuid);
                        if (chekBlock) {
                            memberUUidList.remove(uuid);
                        }
                    }
                }

                searchDto.setMemberUuidList(memberUUidList);

                // uuid로 회원 MEMBER 정보 가져오기
                String getSearchNickMemberInfo = memberCurlService.getMemberInfoOrderByNick(searchDto);
                JSONObject searchNickMemberInfoObject = new JSONObject(getSearchNickMemberInfo);
                if (!((boolean) searchNickMemberInfoObject.get("result"))) {
                    throw new CurlException(searchNickMemberInfoObject);
                }

                JSONObject searchNickMemberResult = (JSONObject) searchNickMemberInfoObject.get("data");
                // 목록 전체 count
                JSONArray searchNickMemberInfo = searchNickMemberResult.getJSONArray("memberInfo");
                if (searchNickMemberInfo != null && !searchNickMemberInfo.isEmpty()) {
                    searchNickMemberInfo.forEach(memberInfo -> {
                        JSONObject member = (JSONObject) memberInfo;
                        MemberInfoDto memberInfoDto = new MemberInfoDto();
                        memberInfoDto.setUuid(member.getString("uuid"));
                        memberInfoDto.setNick(member.getString("nick"));
                        memberInfoDto.setIntro(member.getString("intro"));
                        memberInfoDto.setProfileImgUrl(member.getString("profileImgUrl"));

                        MemberDto badgeInfo = memberService.getMemberBadgeInfoByUuid(member.getString("uuid"));
                        if (badgeInfo != null && badgeInfo.getIsBadge() != null) {
                            memberInfoDto.setIsBadge(badgeInfo.getIsBadge());
                        }
                        memberSearchList.add(memberInfoDto);
                    });
                }
            }
            // UUID 초기화
            searchDto.setMemberUuidList(new ArrayList<>());
            // paging
            PaginationLibray pagination = new PaginationLibray((int) totalCount, searchDto);
            searchDto.setPagination(pagination);

            // 1, 2, 3 merge
            if (getCurlMemberInfoDto != null) {
                searchList.addAll(sameSearchList);
            }
            if (!followSearchList.isEmpty()) {
                searchList.addAll(followSearchList);
            }
            searchList.addAll(memberSearchList);
        }
        return searchList;
    }

    /**
     * 해시태그 검색 리스트
     *
     * @param searchDto
     * @return
     */
    public List<HashTagDto> getSearchHashTagList(String token, SearchDto searchDto) {
        MemberDto memberDto;
        if (token != null && !token.equals("")) {
            // 회원 UUID 조회
            memberDto = super.getMemberUuidByToken(token);
            searchDto.setLoginMemberUuid(memberDto.getUuid());
        }

        List<HashTagDto> lResult = new ArrayList<>();

        // 목록 전체 count
        Long totalCount = searchDaoSub.getSearchHashTagCount(searchDto);

        if (totalCount > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(Math.toIntExact(totalCount), searchDto);
            searchDto.setPagination(pagination);
            lResult = searchDaoSub.getSearchHashTagList(searchDto);

            if (!lResult.isEmpty()) {
                // 문자 변환
                remakeInfo(lResult);
            }
        }

        // 태그리스트
        return lResult;
    }

    /**
     * @param token
     * @param searchDto
     * @return 교류많은 유저 List + 팔로우 리스트
     * 현재 교류많은 유저 조건이 팔로우 회원중 contents_like_cnt 가 2이상인 회원이라
     * follow리스트를 like_cnt로 oreder by 해줌
     * @ 만 눌렀을때 노출 될 리스트
     */
    @SneakyThrows
    public List<MemberDto> getMentionMemberList(String token, SearchDto searchDto) {

        // 로그인 회원 uuid curl 조회
        MemberDto loginUserInfo = super.getMemberUuidByToken(token);

        searchDto.setMemberUuid(loginUserInfo.getUuid());

        List<MemberDto> mentionMemberList = new ArrayList<>();
        List<MemberDto> resultMentionMemberList = new ArrayList<>();

        // 전체 팔로워 cnt
        Long totalCount = followDaoSub.getTotalFollowingCnt(searchDto.getMemberUuid());

        if (totalCount > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(Math.toIntExact(totalCount), searchDto);
            searchDto.setPagination(pagination);

            // 교류 많은 회원 리스트 조회(uuid, isBadge)
            mentionMemberList = searchDaoSub.getMentionMemberList(searchDto);

            // 교류 많은 유저 존재
            if (!mentionMemberList.isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                List<String> memberUuidList = new ArrayList<>(); // 멘션 회원 uuid 리스트 [조회용]

                mentionMemberList.forEach(memberDto -> {
                    memberUuidList.add(memberDto.getUuid()); // 회원 uuid 추가
                });

                // 회원 정보(닉네임, 소개글, 프로필) curl 통신으로 조회
                String memberInfoJsonString = memberCurlService.getMemberInfoByUuidList(memberUuidList);
                JSONObject memberInfoObject = new JSONObject(memberInfoJsonString);

                // 회원 서버 정상 통신
                if ((boolean) memberInfoObject.get("result") == true) {
                    JSONObject mentionInfoResult = (JSONObject) memberInfoObject.get("data");
                    List<MemberInfoDto> memberInfoList = mapper.readValue(mentionInfoResult.get("memberInfoList").toString(), new TypeReference<>() {
                    });

                    // uuid 해당하는 회원 정보 회원 서버에 존재
                    if (!ObjectUtils.isEmpty(memberInfoList)) {
                        for (MemberInfoDto memberInfo : memberInfoList) {
                            String uuid = memberInfo.getUuid();

                            mentionMemberList.forEach(memberDto -> {
                                // 컬 통신 통해 조회한 회원 uuid 와 같으면 set
                                if (uuid.equals(memberDto.getUuid())) {
                                    memberDto.setIntro(memberInfo.getIntro());                  // 소개글 set
                                    memberDto.setProfileImgUrl(memberInfo.getProfileImgUrl());  // 프로필 set
                                    memberDto.setNick(memberInfo.getNick());                    // 닉네임 set
                                }
                            });
                        }
                    }
                }
            }
        }

        mentionMemberList.forEach(memberDto-> {
            if (memberDto.getNick() != null && !memberDto.getNick().equals("")) {
                resultMentionMemberList.add(memberDto);
            }
        });

        // list
        return resultMentionMemberList;
    }

    /**
     * 최근 이미지 내 태그된 회원 리스트
     *
     * @param token     access token
     * @param searchDto
     * @return
     */
    public List<MemberDto> getLatelyImgTagMemberList(String token, SearchDto searchDto) throws
            JsonProcessingException {
        List<MemberDto> tagMemberList = new ArrayList<>();
        List<String> uuidList = new ArrayList<>();
        List<MemberDto> resultTagMemberList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);
        searchDto.setLoginMemberUuid(memberDto.getUuid());

        // 최근 태그한 회원  count
        Long totalCount = searchDaoSub.getLatelyImgTagMemberCount(searchDto);

        if (totalCount > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(Math.toIntExact(totalCount), searchDto);
            searchDto.setPagination(pagination);

            tagMemberList = searchDaoSub.getLatelyImgTagMemberList(searchDto);

            // list 에서 memberUuid 추출
            tagMemberList.forEach(item -> {
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
                for (MemberDto member : tagMemberList) {
                    if (Objects.equals(member.getUuid(), memberInfo.getUuid())) {
                        member.setNick(memberInfo.getNick());
                        member.setProfileImgUrl(memberInfo.getProfileImgUrl());
                        member.setIntro(memberInfo.getIntro());
                    }
                }
            }
        }

        // tagMemberList에서 nick 없는 데이터 제거
        tagMemberList.forEach(getMemberDto-> {
            if (getMemberDto.getNick() != null && !getMemberDto.getNick().equals("")) {
                resultTagMemberList.add(getMemberDto);
            }
        });

        // list
        return resultTagMemberList;

    }

    /**
     * 해시태그 검색 컨텐츠 리스트
     *
     * @param token     access token
     * @param searchDto searchWord
     * @return
     */
    public List<HashTagDto> getSearchHashTagContentsList(String token, SearchDto searchDto) {
        List<HashTagDto> searchHashTagConList = new ArrayList<>();

        if (token != null && !token.equals("")) {
            // 회원 UUID 조회 & 세팅
            MemberDto memberDto = super.getMemberUuidByToken(token);
            searchDto.setLoginMemberUuid(memberDto.getUuid());
        }

        // 목록 전체 count
        Long totalCount = searchDaoSub.getSearchHashTagConCount(searchDto);

        if (totalCount > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(Math.toIntExact(totalCount), searchDto);
            searchDto.setPagination(pagination);

            // 이미지 도메인 set
            searchDto.setImgDomain(imgDomain);

            // list
            searchHashTagConList = searchDaoSub.getSearchHashTagConList(searchDto);
        }

        // 태그 리스트
        return searchHashTagConList;
    }

    /**
     * 인기 검색어 리스트
     *
     * @return
     */
    public List<SearchLogDto> getSeachLogList() {
        // 인기 검색어 리스트
        return searchDaoSub.getSearchLogList();
    }

    /*****************************************************
     *  SubFunction - select
     ****************************************************/

    /*****************************************************
     *  SubFunction - insert
     ****************************************************/
    public void inserSearchLog(SearchDto searchDto) {
        if (!Objects.equals(searchDto.getSearchWord(), "")) {
            // 등록일
            SearchLogDto searchLogDto = new SearchLogDto();
            searchLogDto.setRegDate(dateLibrary.getDatetime());
            searchLogDto.setSearchWord(searchDto.getSearchWord());

            searchDao.insertSearchLog(searchLogDto);
        }
    }
    /*****************************************************
     *  SubFunction - Update
     ****************************************************/

    /*****************************************************
     *  SubFunction - Delete
     ****************************************************/
    /*****************************************************
     *  SubFunction - Etc
     ****************************************************/
    /**
     * 정보 변경 list
     *
     * @param list
     */
    public void remakeInfo(List<HashTagDto> list) {
        for (HashTagDto l : list) {
            remakeInfo(l);
        }
    }

    /**
     * 정보 변경 dto
     *
     * @param dto
     */
    public void remakeInfo(HashTagDto dto) {
        if (dto.getIdx() != null) {
            Long lHashTagContentsCnt = searchDaoSub.getSearchHashTagContentsCount(dto.getIdx());
            String sHashTagContentsCnt = "0";
            if (lHashTagContentsCnt != null && lHashTagContentsCnt > 0) {
                sHashTagContentsCnt = numberFormatLibrary.krFormatNumber(lHashTagContentsCnt);
            }
            dto.setHashTagContentsCnt(sHashTagContentsCnt);
        }
    }

}
