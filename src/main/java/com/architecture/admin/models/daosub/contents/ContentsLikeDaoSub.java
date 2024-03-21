package com.architecture.admin.models.daosub.contents;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.contents.ContentsDto;
import com.architecture.admin.models.dto.contents.ContentsLikeDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface ContentsLikeDaoSub {

    /*****************************************************
     * Select
     ****************************************************/
    /**
     * 좋아요 카운트
     *
     * @param searchDto 콘텐츠 idx, 검색조건
     * @return count
     */
    Integer iGetTotalLikeCount(SearchDto searchDto);

    /**
     * 컨텐츠 좋아요 여부
     *
     * @param contentsLikeDto
     * @return
     */
    Integer getContentsLikeCheck(ContentsLikeDto contentsLikeDto);

    /**
     * 내가 좋아요 한 콘텐츠 카운트
     *
     * @param searchDto loginMemberUuid
     * @return count
     */
    Integer iGetTotalMyLikeContentsCount(SearchDto searchDto);

    /**
     * 좋아요한 회원 리스트
     *
     * @param searchDto 콘텐츠 idx, 검색조건
     * @return list
     */
    List<MemberInfoDto> lGetContentsLikeList(SearchDto searchDto);

    /**
     * 내가 좋아요 한 콘텐츠 리스트
     *
     * @param searchDto loginMemberUuid
     * @return list
     */
    List<ContentsDto> lGetMyLikeContentsList(SearchDto searchDto);

    /**
     * 좋아요 내역 가져오기
     *
     * @param contentsLikeDto contentsIdx, loginMemberUuid
     * @return ContentsLikeDto
     */
    ContentsLikeDto oGetTargetInfo(ContentsLikeDto contentsLikeDto);

    /**
     * cnt 테이블에 해당 idx 있는지 확인
     *
     * @param contentsLikeDto contentsIdx
     * @return contentsIdx
     */
    Long lCheckCntByIdx(ContentsLikeDto contentsLikeDto);

    /**
     * sns_follow_contents_like_cnt 테이블에 해당 idx 있는지 확인
     *
     * @param contentsLikeDto followIdx
     * @return
     */
    Integer lCheckFollowCntByIdx(ContentsLikeDto contentsLikeDto);

}
