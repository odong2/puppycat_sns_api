package com.architecture.admin.services.member;

import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.daosub.contents.ContentsDaoSub;
import com.architecture.admin.models.daosub.member.MemberInfoDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;
import com.architecture.admin.services.BaseService;
import com.architecture.admin.services.contents.ContentsCurlService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;


@RequiredArgsConstructor
@Service
@Transactional
public class MemberInfoService extends BaseService {
    private final MemberInfoDaoSub memberInfoDaoSub;
    private final ContentsDaoSub contentsDaoSub;
    private final ContentsCurlService contentsCurlService; // 컨텐츠 curl 통신

    /*****************************************************
     *  Modules
     ****************************************************/

    /*****************************************************
     *  SubFunction - Select
     ****************************************************/
    /**
     * 유저 페이지 - 회원 info 가져오기
     *
     * @param memberDto : memberUuid[회원 uuid] , uuid [로그인 회원 uuid]
     * @return 회원 정보
     */
    public MemberInfoDto getMemberInfo(MemberDto memberDto) {

        // 타겟팅 회원 검증
        if (ObjectUtils.isEmpty(memberDto.getMemberUuid())) {
            throw new CustomException(CustomError.MEMBER_UUID_EMPTY); // 회원 UUID가 비었습니다.
        }

        // 로그인 회원 정보 조회
        MemberDto searchMemberDto = MemberDto.builder()
                .uuid(memberDto.getUuid())             // 로그인 회원 uuid
                .memberUuid(memberDto.getMemberUuid()) // 해당 회원 uuid
                .build();

        // 타게팅 회원 DB 검증 및 회원 정보 조회
        return getCommonMemberInfo(searchMemberDto);
    }

    /**
     * 내정보 info 가져오기
     *
     * @param loginMemberUuid :  로그인 회원 uuid
     * @return 회원 정보
     */
    public MemberInfoDto getMyInfoByUuid(String loginMemberUuid) {

        // 로그인 회원 정보 조회
        MemberDto searchMemberDto = MemberDto.builder()
                .memberUuid(loginMemberUuid) // 작성자 회원 uuid -> memberUuid에 setting
                .build();

        return getCommonMemberInfo(searchMemberDto);
    }

    /**
     * 컨텐츠 작성자 정보 info 가져오기
     *
     * @param searchDto : loginMemberUuid[로그인 회원 uuid], contentsIdx[컨텐츠 idx]
     * @return
     */
    public MemberInfoDto getMemberInfoByContentsIdx(SearchDto searchDto) {
        String loginMemberUuid = searchDto.getLoginMemberUuid();
        Long contentsIdx = searchDto.getContentsIdx();

        // 작성자 uuid 조회
        String writerUuid = contentsDaoSub.getMemberUuidByIdx(contentsIdx);

        // 작성자 정보 조회
        MemberDto searchMemberDto = MemberDto.builder()
                .uuid(loginMemberUuid)  // 로그인 회원 uuid
                .memberUuid(writerUuid) // 작성자 uuid
                .build();

        return getCommonMemberInfo(searchMemberDto);
    }

    /**
     * 회원 정보 가져오기 공통 사용
     *
     * @param memberDto : uuid[로그인 회원 (선택)], memberUuid[작성자 uuid]
     * @return
     */
    private MemberInfoDto getCommonMemberInfo(MemberDto memberDto) {

        // 정상 회원인지 조회
        String jsonString = memberCurlService.checkMemberUuid(memberDto.getMemberUuid());

        JSONObject jsonObject = new JSONObject(jsonString);
        JSONObject dataObject = (JSONObject) jsonObject.get("data");

        boolean isExist = (boolean) dataObject.get("isExist"); // 회원 존재 유무

        if (isExist == false) {
            throw new CustomException(CustomError.MEMBER_UUID_ERROR); // 존재하지 않는 회원입니다.
        }

        // uuid : 로그인 회원 uuid, memberUuid : 작성자 uuid
        MemberInfoDto memberInfoDto = memberInfoDaoSub.getMemberInfo(memberDto);

        if (memberInfoDto == null) {
            throw new CustomException(CustomError.MEMBER_UUID_ERROR); // 존재하지 않는 회원입니다.
        }

        // 컨텐츠 작성자 정보
        MemberInfoDto writerInfoDto = contentsCurlService.getWriterInfo(memberDto.getMemberUuid());

        memberInfoDto.setUuid(writerInfoDto.getUuid());                     // uuid
        memberInfoDto.setNick(writerInfoDto.getNick());                     // 닉네임
        memberInfoDto.setSimpleType(writerInfoDto.getSimpleType());         // 간편가입 유형
        memberInfoDto.setEmail(writerInfoDto.getEmail());                   // 이메일
        memberInfoDto.setProfileImgUrl(writerInfoDto.getProfileImgUrl());   // 프로필 이미지
        memberInfoDto.setIntro(writerInfoDto.getIntro());                   // 소개글

        return memberInfoDto;
    }

    /**
     * 유저 페이지
     *
     * @param token      access token
     * @param memberDto  memberUuid (타겟)
     * @return
     */
    public MemberInfoDto getMemberInfoByUuid(String token, MemberDto memberDto) {

        // 로그인 한 상태라면 회원 UUID 조회 & 세팅
        if (!ObjectUtils.isEmpty(token)) {
            MemberDto member = super.getMemberUuidByToken(token);
            memberDto.setUuid(member.getUuid());
        }

        // 대상 회원 Uuid validate
        Boolean bTargetMemberUuid = getCheckMemberByUuid(memberDto.getMemberUuid());
        if (Boolean.FALSE.equals(bTargetMemberUuid)) {
            throw new CustomException(CustomError.MEMBER_UUID_ERROR);
        }

        // Curl로 memberInfo 데이터 가져오기 (uuid : 로그인 회원 , memberUuid : 타겟 회원)
        MemberInfoDto getCurlMemberInfo = memberCurlService.getMemberInfoByUuid(memberDto.getMemberUuid());

        if (getCurlMemberInfo != null) {
            MemberInfoDto getSocialMemberInfo = memberInfoDaoSub.getSocialMemberInfo(memberDto);
            getCurlMemberInfo.setIsBadge(getSocialMemberInfo.getIsBadge());
            getCurlMemberInfo.setFollowerCnt(getSocialMemberInfo.getFollowerCnt());
            getCurlMemberInfo.setFollowCnt(getSocialMemberInfo.getFollowCnt());
            getCurlMemberInfo.setFollowState(getSocialMemberInfo.getFollowState());
            getCurlMemberInfo.setBlockedState(getSocialMemberInfo.getBlockedState());
            getCurlMemberInfo.setBlockedMeState(getSocialMemberInfo.getBlockedMeState());
        }

        return getCurlMemberInfo;
    }

    /*****************************************************
     *  SubFunction - Update
     ****************************************************/

    /*****************************************************
     *  SubFunction - ETC
     ****************************************************/

}
