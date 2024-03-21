package com.architecture.admin.models.dto.follow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FollowDto {

    private Long idx;               // 고유번호
    private String memberUuid;      // 회원uuid
    private String followUuid;      // 팔로우uuid
    private String followId;        // 팔로우id
    private String followNick;      // 팔로우nick
    private String followerUuid;    // 팔로워uuid
    private String followerId;      // 팔로워id
    private String followerNick;    // 팔로워nick
    private Integer state;          // 상태값
    private Integer newState;       // 오늘 팔로잉한 회원 상태값 (NEW)
    private String regDate;         // 등록일
    private String regDateTz;       // 등록일 타임존

    private String nick;            // 닉네임
    private String intro;            // 닉네임
    private Integer isFollow;       // 팔로우 여부

    private Integer followCnt;      // 팔로우cnt
    private Integer followerCnt;    // 팔로워cnt
    private Integer isBadge;        // 뱃지여부
    private String profileImgUrl;   // 프로필 이미지

    // sql
    private Long insertedIdx;
    private Long affectedRow;
}
