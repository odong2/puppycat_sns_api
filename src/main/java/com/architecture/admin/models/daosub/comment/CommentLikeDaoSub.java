package com.architecture.admin.models.daosub.comment;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.comment.CommentLikeDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface CommentLikeDaoSub {

    /*****************************************************
     * Select
     ****************************************************/
    /**
     * 좋아요 카운트
     *
     * @param searchDto 댓글 idx, 검색조건
     * @return count
     */
    Integer iGetTotalLikeCount(SearchDto searchDto);

    /**
     * 좋아요한 회원 리스트
     *
     * @param searchDto 댓글 idx, 검색조건
     * @return list
     */
    List<MemberInfoDto> lGetCommentLikeList(SearchDto searchDto);

    /**
     * 좋아요 내역 가져오기
     *
     * @param commentLikeDto commentIdx, memberIdx
     * @return ContentsLikeDto
     */
    CommentLikeDto oGetTargetInfo (CommentLikeDto commentLikeDto);

    /**
     * cnt 테이블에 해당 idx 있는지 확인
     *
     * @param commentLikeDto commentIdx
     * @return commentIdx
     */
    Long lCheckCntByIdx(CommentLikeDto commentLikeDto);

    /**
     * 댓글 Contents Idx 가져오기
     *
     * @return contentsIdx값
     */
    CommentLikeDto getCommentInfo(CommentLikeDto commentLikeDto);

}
