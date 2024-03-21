package com.architecture.admin.models.dao.contents;

import com.architecture.admin.models.dto.contents.ContentsLikeDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface ContentsLikeDao {

    /*****************************************************
     * Insert
     ****************************************************/

    /**
     * 콘텐츠 좋아요수 등록
     * [sns_contents_like_cnt]
     * @param contentsLikeDto contentsIdx
     */
    void insertContentsLikeCnt(ContentsLikeDto contentsLikeDto);

    /**
     * 팔로우의 콘텐츠 좋아요수 등록
     * [sns_follow_contents_like_cnt]
     * @param contentsLikeDto followIdx
     */
    void insertFollowContentsLikeCnt(ContentsLikeDto contentsLikeDto);

    /**
     * 좋아요 등록
     *
     * @param contentsLikeDto contentsIdx, memberUuid, regDate
     * @return 처리결과
     */
    Integer insertContentsLike(ContentsLikeDto contentsLikeDto);

    /*****************************************************
     * Update
     ****************************************************/
    /**
     * 좋아요 상태값 변경
     *
     * @param contentsLikeDto contentsIdx, memberUuid, regDate
     * @return 처리결과
     */
    Integer updateContentsLike(ContentsLikeDto contentsLikeDto);

    /**
     * 좋아요 cnt +1
     *
     * @param contentsLikeDto contentsIdx, regDate
     */
    void updateContentsLikeCntUp(ContentsLikeDto contentsLikeDto);

    /**
     * sns_follow_contents_like_cnt 좋아요 cnt +1
     *
     * @param contentsLikeDto followIdx
     */
    void updateFollowContentsLikeCntUp(ContentsLikeDto contentsLikeDto);

    /**
     * 좋아요 cnt -1
     *
     * @param contentsLikeDto contentsIdx, regDate
     */
    void updateContentsLikeCntDown(ContentsLikeDto contentsLikeDto);

    /**
     * sns_follow_contents_like_cnt 좋아요 cnt -1
     *
     * @param contentsLikeDto followIdx
     */
    void updateFollowContentsLikeCntDown(ContentsLikeDto contentsLikeDto);

    /**
     * 언팔로우시 팔로우 컨텐츠 좋아요 0으로 리셋
     *
     * @param contentsLikeDto
     */
    void updateFollowContentsLikeReset(ContentsLikeDto contentsLikeDto);
}
