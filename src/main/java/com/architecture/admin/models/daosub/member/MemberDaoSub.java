package com.architecture.admin.models.daosub.member;

import com.architecture.admin.models.dto.member.MemberDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface MemberDaoSub {

    /*****************************************************
     * Select
     ****************************************************/
    /**
     * 뱃지 정보 가져오기 by memberUuid
     *
     * @param memberUuid
     * @return
     */
    MemberDto getMemberBadgeInfoByUuid(String memberUuid);

}
