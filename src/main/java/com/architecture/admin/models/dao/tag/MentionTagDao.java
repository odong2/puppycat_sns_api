package com.architecture.admin.models.dao.tag;

import com.architecture.admin.models.dto.tag.MentionTagDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface MentionTagDao {

    /*****************************************************
     * Select
     ****************************************************/
    /**
     * 멘션 등록된게 있는지 확인
     * [sns_member_mention]
     *
     * @param mentionTagDto 회원IDx
     * @return mention.idx
     */
    Long getIdxByMentionTag(MentionTagDto mentionTagDto);

    /**
     * 컨텐츠에 등록된 멘션태그
     *
     * @param mentionTagDto
     * @return
     */
    List<MentionTagDto>getContentsMentionTagList(MentionTagDto mentionTagDto);

    /**
     * 댓글에 등록된 멘션태그
     *
     * @param mentionTagDto
     * @return
     */
    List<MentionTagDto> getCommentMentionTagList(MentionTagDto mentionTagDto);

    /**
     * 컨텐츠에 이미 사용된 멘션태그인지 체크
     *
     * @param mentionTagDto contentsIdx mentionTagIdx
     * @return
     */
    Integer getContentsMentionTag(MentionTagDto mentionTagDto);

    /**
     * 댓글에 이미 사용된 멘션태그인지 체크
     *
     * @param mentionTagDto commentIdx mentionTagIdx
     * @return
     */
    Integer getCommentMentionTag(MentionTagDto mentionTagDto);

    /*****************************************************
     * Insert
     ****************************************************/
    /**
     * 멘션 등록
     * [sns_member_mention]
     *
     * @param mentionTagDto memberIdx
     * @return 처리결과
     */
    Long insertMentionTag(MentionTagDto mentionTagDto);

    /**
     * 컨텐츠 회원 멘션 매핑 등록
     * [sns_contents_mention_mapping]
     *
     * @param mentionTagDto
     * @return
     */
    Integer insertContentsMentionTagMapping(MentionTagDto mentionTagDto);

    /**
     * 댓글 맨션 매핑 등록
     * [sns_contents_comment_mention_mapping]
     *
     * @param mentionTagDto
     * @return
     */
    Integer insertCommentMentionTagMapping(MentionTagDto mentionTagDto);

    /*****************************************************
     * Update
     ****************************************************/
    /**
     * 맨션 cnt + 1
     * [sns_member_mention]
     *
     * @param mentionTagDto
     * @return 처리결과
     */
    Integer updateMentionTagCnt(MentionTagDto mentionTagDto);

    /*****************************************************
     * Delete
     ****************************************************/
    /**
     * 컨텐츠 멘션태그 매핑 제거
     *
     * @param idx 매핑테이블 idx
     */
    Integer removeContentsMentionTagMapping(Long idx);

    /**
     * 댓글 멘션태그 매핑 제거
     *
     * @param idx 매핑테이블 idx
     */
    Integer removeCommentMentionTagMapping(Long idx);

}
