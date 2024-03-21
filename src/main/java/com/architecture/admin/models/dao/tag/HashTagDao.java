package com.architecture.admin.models.dao.tag;

import com.architecture.admin.models.dto.tag.HashTagDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface HashTagDao {

    /*****************************************************
     * Select
     ****************************************************/

    /*****************************************************
     * Insert
     ****************************************************/
    /**
     * 해시태그 등록
     * [sns_hash_tag]
     *
     * @param hashTagDto hashtag
     * @return 처리결과
     */
    Integer insertHashTag(HashTagDto hashTagDto);

    /**
     * 컨텐츠 해시태그 매핑 등록
     * [sns_contents_hash_tag_mapping]
     *
     * @param hashTagDto
     * @return
     */
    Integer insertContentsHashTagMapping(HashTagDto hashTagDto);

    /**
     * 댓글 해시태그 매핑 등록
     * [sns_comment_hash_tag_mapping]
     *
     * @param hashTagDto
     * @return
     */
    Integer insertCommentHashTagMapping(HashTagDto hashTagDto);

    /*****************************************************
     * Update
     ****************************************************/
    /**
     * 해시태그 cnt + 1
     * [sns_hash_tag]
     *
     * @param hashTagDto
     * @return 처리결과
     */
    Integer updateHashTagCnt(HashTagDto hashTagDto);

    /*****************************************************
     * Delete
     ****************************************************/
    /**
     * 컨텐츠 해시태그 매핑 제거
     * 
     * @param idx 매핑테이블 idx
     */
    Integer removeContentsHashTagMapping(Long idx);

    /**
     * 댓글 해시태그 매핑 제거
     * 
     * @param idx 매핑테이블 idx
     */
    Integer removeCommentHashTagMapping(Long idx);
}
