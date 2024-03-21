package com.architecture.admin.models.daosub.member;

import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface MemberInfoDaoSub {

    /*****************************************************
     * Select
     *****************************************************

    /**
     * 해당 회원 정보
     *
     * @param memberDto : uuid(로그인 회원), memberUuid(해당 회원)
     * @return
     */
    MemberInfoDto getMemberInfo(MemberDto memberDto);

    /**
     * social에서 가져 올  회원 정보
     *
     * @param memberDto : uuid(로그인 회원), memberUuid(해당 회원)
     * @return
     */
    MemberInfoDto getSocialMemberInfo(MemberDto memberDto);

    /**
     * 회원 정보 리스트 조회
     *
     * @param memberUuidList
     * @return
     */
    List<MemberInfoDto> getMemberFollowInfoByUuidList(List<String> memberUuidList);

}
