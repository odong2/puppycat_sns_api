package com.architecture.admin.models.daosub.member;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.contents.ContentsDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;

import java.util.List;

public interface PopularMemberDaoSub {

    /*****************************************************
     * Select
     ****************************************************/
    /**
     * 인기 유저 리스트
     *
     * @param searchDto loginMemberUuid
     * @return 인기 유저 30명
     */
    List<MemberInfoDto> getPopularMemberList(SearchDto searchDto);

    /**
     * 인기 유저 컨텐츠 리스트
     *
     * @param searchDto loginMemberUuid , memberUuid (인기 유저 idx)
     * @return 인기 유저 콘텐츠 3개
     */
    List<ContentsDto> getPopularMemberContentsList(SearchDto searchDto);

}
