package com.architecture.admin.models.daosub.push;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface PushDaoSub {

    /**
     * 타입 제목 가져오기
     *
     * @param idx 타입idx
     * @return 타입title
     */
    String getPushTypeTitle(int idx);

}
