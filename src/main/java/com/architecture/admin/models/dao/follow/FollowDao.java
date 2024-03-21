package com.architecture.admin.models.dao.follow;

import com.architecture.admin.models.dto.follow.FollowDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface FollowDao {

    /**
     * 팔로우 인서트
     *
     * @param followDto memberUuid followUuid
     * @return insertedIdx
     */
    int insert(FollowDto followDto);

    /**
     * cnt테이블에 follow_cnt 신규 인서트
     *
     * @param followDto memberUuid
     */
    void insertFollowCnt(FollowDto followDto);

    /**
     * cnt테이블에 follower_cnt 신규 인서트
     *
     * @param followDto followUuid
     */
    void insertFollowerCnt(FollowDto followDto);

    /**
     * 팔로우 업데이트
     *
     * @param followDto followUuid memberUuid
     * @return affectedRow
     */
    int update(FollowDto followDto);

    /**
     * sns_member_follow_cnt.follow_cnt + 1
     *
     * @param followDto memberUuid
     */
    void updateFollowCntUp(FollowDto followDto);

    /**
     * sns_member_follow_cnt.follow_cnt - 1
     *
     * @param followDto memberUuid
     */
    void updateFollowCntDown(FollowDto followDto);

    /**
     * sns_member_follow_cnt.follower_cnt + 1
     *
     * @param followDto followUuid
     */
    void updateFollowerCntUp(FollowDto followDto);

    /**
     * sns_member_follow_cnt.follower_cnt - 1
     *
     * @param followDto followUuid
     */
    void updateFollowerCntDown(FollowDto followDto);

}
