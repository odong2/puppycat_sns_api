package com.architecture.admin.models.daosub.member;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
@Mapper
public interface FavoriteMemberDaoSub {

    /*****************************************************
     * Select
     ****************************************************/
    /**
     * 교류많은 회원 카운트
     *
     * @param searchDto memberIdx
     * @return count
     */
    Long getFavoriteMemberCount(SearchDto searchDto);

    /**
     * 교류 많은 유저 리스트
     *
     * @param searchDto memberIdx
     * @return List
     */
    List<MemberInfoDto> getFavoriteMemberList(SearchDto searchDto);

    /**
     * 회사 계정 리스트
     *
     * @param searchDto memberIdx
     * @return List
     */
    List<MemberInfoDto> getOfficialAccountList(SearchDto searchDto);

    /**
     * 오늘 등록한 게시물 중 가장 최근 등록일 가져오기
     *
     * @param memberInfoDto memberIdx, regDate(오늘)
     * @return 등록일
     */
    String getLastRegDateByOfficial(MemberInfoDto memberInfoDto);

}
