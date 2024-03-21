package com.architecture.admin.services.member;

import com.architecture.admin.libraries.exception.CurlException;
import com.architecture.admin.models.daosub.member.PopularMemberDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.contents.ContentsDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;
import com.architecture.admin.services.BaseService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/*****************************************************
 * 인기 유저 회원
 * 20230713 기준 : 팔로우가 높은 순 100명 중 콘텐츠가 한 개 이상 있는 유저 랜덤 6명
 ****************************************************/
@Service
@RequiredArgsConstructor
@Transactional
public class PopularMemberService extends BaseService {

    private final PopularMemberDaoSub popularMemberDaoSub;
    @Value("${cloud.aws.s3.img.url}")
    private String imgDomain;

    /*****************************************************
     *  Modules
     ****************************************************/
    /**
     * 인기 유저 리스트 가져오기
     *
     * @param token     access token
     * @param searchDto page 페이지 limit 페이지에 노출 될 개수
     * @return 인기 유저 List
     */
    @SneakyThrows
    public List<MemberInfoDto> getPopularMemberList(String token, SearchDto searchDto) {
        List<MemberInfoDto> list = new ArrayList<>();
        List<String> uuidList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        // 회원 UUID 조회 & 세팅
        if (!ObjectUtils.isEmpty(token)) {
            MemberDto memberDto = super.getMemberUuidByToken(token);
            searchDto.setLoginMemberUuid(memberDto.getUuid());
        }

        // 인기 유저 리스트
        List<MemberInfoDto> popularMemberList = popularMemberDaoSub.getPopularMemberList(searchDto);

        // 랜덤으로 뽑은 memberUuid 저장할 List
        List<String> randomMemberUuidList = new ArrayList<>();

        // 인기 유저 리스트에 값이 존재한다면 반복문 실행
        if (!popularMemberList.isEmpty()) {
            Random random = new Random();

            // 랜덤 6명 뽑기 & 대표 게시물 가져오기
            while (randomMemberUuidList.size() < 6) {

                // 0~n 미만의 난수 반환
                int randomIndex = random.nextInt(popularMemberList.size());
                // 추출된 랜덤 인덱스로 인기유저 memberUuid 가져오기
                String randomMemberUuid = popularMemberList.get(randomIndex).getUuid();

                // 콘텐츠 가져오기 (최신순 1~3개)
                SearchDto tmpSearchDto = new SearchDto();
                tmpSearchDto.setLoginMemberUuid(searchDto.getLoginMemberUuid()); // 로그인 회원 uuid
                tmpSearchDto.setMemberUuid(randomMemberUuid); // 랜덤 회원 uuid
                tmpSearchDto.setImgDomain(imgDomain);         // 이미지 도메인 설정
                List<ContentsDto> popularMemberContentsList = popularMemberDaoSub.getPopularMemberContentsList(tmpSearchDto);

                // 게시물이 존재하면 리턴할 리스트에 추가
                if (!ObjectUtils.isEmpty(popularMemberContentsList)) {

                    // 게시물 정보 set
                    MemberInfoDto memberInfoDto = popularMemberList.get(randomIndex);
                    memberInfoDto.setContentsList(new ArrayList<>());
                    memberInfoDto.getContentsList().addAll(popularMemberContentsList);

                    // 리턴할 리스트에 추가
                    list.add(memberInfoDto);

                    // 반복문 종료 위해 추가 (n명 채워지면 종료)
                    randomMemberUuidList.add(randomMemberUuid);

                }

                // 다음 반복시 난수 중복 방지위해 리스트에서 해당 인덱스 제거
                popularMemberList.remove(randomIndex);

                // 인기 유저 리스트에 값이 없는 경우 종료
                if (popularMemberList.isEmpty()) {
                    break;
                }
            }
        }

        // 회원 정보 세팅
        if (!ObjectUtils.isEmpty(list)) {

            // list 에서 memberUuid 추출
            list.forEach(item -> {
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
                for (MemberInfoDto memberInfoDto : list) {
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
        for (MemberInfoDto memberInfoDto : list) {
            if (memberInfoDto.getNick() != null && !memberInfoDto.getNick().isEmpty()) {
                filteredList.add(memberInfoDto);
            }
        }

        return filteredList;
    }

    /*****************************************************
     *  SubFunction - Etc
     ****************************************************/

}
