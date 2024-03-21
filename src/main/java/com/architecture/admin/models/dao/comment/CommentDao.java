package com.architecture.admin.models.dao.comment;

import com.architecture.admin.models.dto.comment.CommentDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface CommentDao {


    /*****************************************************
     * Insert
     ****************************************************/
    /**
     * 댓글 등록
     * [sns_contents_comment]
     *
     * @param commentDto insertedIdx
     */
    Long insert(CommentDto commentDto);

    /**
     * sns_contents_comment_cnt 신규 인서트
     *
     * @param commentDto contentsIdx
     */
    void insertCommentCnt(CommentDto commentDto);
    /*****************************************************
     * Update
     ****************************************************/
    /**
     * sns_contents_comment_cnt.comment_cnt + 1
     *
     * @param commentDto contentsIdx
     */
    void updateCommentCntUp(CommentDto commentDto);

    /**
     * sns_contents_comment_cnt.comment_cnt - 1
     *
     * @param commentDto contentsIdx
     */
    void updateCommentCntDown(CommentDto commentDto);

    /**
     * 댓글 내용 업데이트
     *
     * @param commentDto contents idx
     */
    Long updateCommentContents(CommentDto commentDto);

    /**
     * 댓글 수정
     *
     * @param commentDto
     * @return
     */
    int modifyComment(CommentDto commentDto);

    /**
     * 부모 댓글 삭제
     *
     * @param commentDto contents idx
     */
    Integer removeParentComment(CommentDto commentDto);

    /**
     * 자식 댓글 삭제
     *
     * @param commentDto contents idx
     */
    Integer removeChildComment(CommentDto commentDto);

    /*****************************************************
     * Delete
     ****************************************************/

}
