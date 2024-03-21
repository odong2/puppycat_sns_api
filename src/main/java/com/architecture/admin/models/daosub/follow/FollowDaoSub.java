package com.architecture.admin.models.daosub.follow;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.follow.FollowDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface FollowDaoSub {
    /**
     * 팔로우 회원 SNS 정보
     * 
     * @param searchDto
     * @return
     */
    FollowDto getFollowSnsInfo(SearchDto searchDto);
    FollowDto getFollowerSnsInfo(SearchDto searchDto);

    /**
     * 팔로우 회원 UUID 리스트
     *
     * @param searchDto
     * @return
     */
    List<String> getFollowUuidList(SearchDto searchDto);

    /**
     * 팔로워 회원 UUID 리스트
     * 
     * @param searchDto
     * @return
     */
    List<String> getFollowerUuidList(SearchDto searchDto);

    /**
     * 팔로잉 카운트 가져오기 [ member_follow_cnt ]
     *
     * @param memberUuid 회원uuid
     * @return count
     */
    Long getTotalFollowingCnt(String memberUuid);

    /**
     * 조회 한 회원의 팔로잉 중 로그인 한 회원이 차단한 회원 카운트
     *
     * @param searchDto
     * @return
     */
    Long getBlockFollowCnt(SearchDto searchDto);
    /**
     * 팔로워 카운트 가져오기 [ member_follow_cnt ]
     *
     * @param memberUuid 회원uuid
     * @return count
     */
    Long getTotalFollowerCnt(String memberUuid);

    /**
     * 조회 한 회원의 팔로워 중 로그인 한 회원이 차단한 회원 카운트
     *
     * @param searchDto
     * @return
     */
    Long getBlockFollowerCnt(SearchDto searchDto);

    /**
     * follow 회원 데이터 가져오기
     * 회원을 팔로우했던 기록이 있는지 확인용
     *
     * @param followDto memberidx followidx state
     * @return count
     */
    Integer getCntCheck(FollowDto followDto);

    /**
     * 정상적인 sns_member_follow 테이블 조회
     *
     * @param followDto memberidx followidx state
     * @return
     */
    FollowDto getFollowInfo(FollowDto followDto);

    /**
     * follow_cnt 회원 데이터 가져오기
     *
     * @param followDto memberIdx
     * @return count
     */
    Integer getMemberCntCheck(FollowDto followDto);

    /**
     * follow_cnt 팔로우 회원 데이터 가져오기
     *
     * @param followDto followIdx
     * @return count
     */
    Integer getFollowerCntCheck(FollowDto followDto);


}
