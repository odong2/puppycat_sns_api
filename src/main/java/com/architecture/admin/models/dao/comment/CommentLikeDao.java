package com.architecture.admin.models.dao.comment;

import com.architecture.admin.models.dto.comment.CommentLikeDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface CommentLikeDao {

    /*****************************************************
     * Insert
     ****************************************************/
    /**
     * 댓글 좋아요수
     * [sns_contents_comment_like_cnt]
     * @param commentLikeDto commentIdx
     */
    void insertCommentLikeCnt(CommentLikeDto commentLikeDto);

    /**
     * 좋아요 등록
     *
     * @param commentLikeDto commentIdx, memberUuid, regDate
     * @return 처리결과
     */
    Integer insertCommentLike(CommentLikeDto commentLikeDto);

    /*****************************************************
     * Update
     ****************************************************/
    /**
     * 좋아요 상태값 변경
     *
     * @param commentLikeDto commentIdx, memberUuid, regDate
     * @return 처리결과
     */
    Integer updateCommentLike(CommentLikeDto commentLikeDto);

    /**
     * 좋아요 cnt +1
     *
     * @param commentLikeDto commentIdx, regDate
     */
    void updateCommentLikeCntUp(CommentLikeDto commentLikeDto);

    /**
     * 좋아요 cnt -1
     *
     * @param commentLikeDto commentIdx, regDate
     */
    void updateCommentLikeCntDown(CommentLikeDto commentLikeDto);

}
