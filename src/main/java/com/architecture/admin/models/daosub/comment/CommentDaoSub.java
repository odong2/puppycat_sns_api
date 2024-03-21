package com.architecture.admin.models.daosub.comment;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.comment.CommentDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface CommentDaoSub {
    /**
     * sns_contents_comment_cnt 데이터 가져오기
     *
     * @param commentDto contentsIdx
     * @return count
     */
    Integer getCommentCntCheck(CommentDto commentDto);

    /**
     * 댓글 고유아이디 중복체크
     * [sns_contents_comment]
     *
     * @param uuid 고유아이디
     * @return count
     */
    Integer getCountByUuid(String uuid);

    /**
     * 댓글 신고했는지 조회
     *
     * @param searchDto : loginMemberIdx[로그인한 회원 idx], commentIdx[댓글 idx]
     * @return
     */
    int getCommentReportCnt(SearchDto searchDto);

    /**
     * 댓글 작성자 uuid 가져오기
     *
     * @param commentIdx 댓글 idx
     * @return memberUuid
     */
    String getMemberUuidByIdx(Long commentIdx);

    /**
     * 부모 댓글 CNT 가져오기
     *
     * @param searchDto 댓글 idx
     * @return Long
     */
    Integer iGetParentTotalCommentCnt(SearchDto searchDto);

    /**
     * 자식 댓글 CNT 가져오기
     *
     * @param searchDto 댓글 idx
     * @return Long
     */
    Integer iGetChildTotalCommentCnt(SearchDto searchDto);

    /**
     * 댓글 List 가져오기
     *
     * @param searchDto 댓글 by contentsIdx
     * @return List
     */
    List<CommentDto> getParentCommentList(SearchDto searchDto);

    /**
     * 대 댓글 List 가져오기
     *
     * @param searchDto 댓글 idx
     * @return List
     */
    List<CommentDto> getChildCommentList(SearchDto searchDto);


    Integer checkRemoveAuth(CommentDto commentDto);

    /**
     * 좋아요 많은 댓글 리스트
     *
     * @param searchDto
     * @return
     */
    CommentDto getLikeManyComment(SearchDto searchDto);

    /**
     * 댓글 + 대댓글 총 합
     *
     * @param commentDto
     * @return
     */
    CommentDto getTotalSumCommentCount(CommentDto commentDto);

    /**
     * 댓글 정보 조회
     *
     * @param idx : 댓글 idx
     * @return
     */
    CommentDto getCommentInfoByIdx(Long idx);

    /**
     * 부모 댓글인지 체크
     *
     * @param searchDto
     * @return
     */
    Long getCheckParent(SearchDto searchDto);

    /**
     * 해당 댓글 위치 Get
     *
     * @param searchDto
     * @return
     */
    List<CommentDto> getRowNum(SearchDto searchDto);

    /**
     * 해당 댓글 위치 포함 리스트
     *
     * @param searchDto
     * @return
     */
    List<CommentDto> getFocusList(SearchDto searchDto);

    /**
     * 멘션 리스트 조회
     *
     * @param commentIdx
     * @return
     */
    List<String> getCommentMentionTags(Long commentIdx);

    /**
     * 상태값에 따른 댓글 여부 조회
     *
     * @param commentDto
     * @return
     */
    int getCommentCnt(CommentDto commentDto);

    /**
     * 부모 idx 조회
     *
     * @param idx
     * @return
     */
    Long getParentIdxByIdx(Long idx);

    /**
     * 유효한 댓글인지 조회
     *
     * @param idx 댓글 idx
     * @return
     */
    int getCommentCntByIdx(Long idx);

}


