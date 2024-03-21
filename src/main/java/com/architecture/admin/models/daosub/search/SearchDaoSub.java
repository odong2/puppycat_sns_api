package com.architecture.admin.models.daosub.search;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.search.SearchLogDto;
import com.architecture.admin.models.dto.tag.HashTagDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface SearchDaoSub {

    /**
     * 최근 태그 한 회원 카운트
     *
     * @param searchDto
     * @return
     */
    Long getLatelyImgTagMemberCount(SearchDto searchDto);

    /**
     * 해시태그 검색 결과 카운트
     *
     * @param searchDto searchWord
     * @return
     */
    Long getSearchHashTagCount(SearchDto searchDto);

    /**
     * 팔로우 회원 중 검색어가 포함된 결과 리스트
     *
     * @param searchDto searchWord
     * @return
     */
    List<String> getFollowSearchNickList(SearchDto searchDto);

    /**
     * 최근 이미지내 태그한 회원 리스트
     *
     * @param searchDto searchWord
     * @return
     */
    List<MemberDto> getLatelyImgTagMemberList(SearchDto searchDto);


    /**
     * @param searchDto
     * @return
     * @시 추천회원 리스트
     */
    List<MemberDto> getMentionMemberList(SearchDto searchDto);


    /**
     * 검색시 해시태그 리스트
     *
     * @param searchDto searchWord
     * @return
     */
    List<HashTagDto> getSearchHashTagList(SearchDto searchDto);

    /**
     * 검색시 해시태그 컨텐츠 리스트
     *
     * @param searchDto searchWord
     * @return
     */
    List<HashTagDto> getSearchHashTagConList(SearchDto searchDto);


    /**
     * 해시태그 검색 컨텐츠 결과 카운트
     *
     * @param searchDto searchWord
     * @return
     */
    Long getSearchHashTagConCount(SearchDto searchDto);


    /**
     * 해시태그가 사용된 컨텐츠 카운트
     *
     * @param hashTagIdx
     * @return
     */
    Long getSearchHashTagContentsCount(Long hashTagIdx);

    /**
     * 인기 검색어 리스트
     *
     * @return
     */
    List<SearchLogDto> getSearchLogList();
}
