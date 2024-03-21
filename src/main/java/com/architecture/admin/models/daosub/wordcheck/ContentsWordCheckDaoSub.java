package com.architecture.admin.models.daosub.wordcheck;

import com.architecture.admin.models.dto.wordcheck.ContentsWordCheckDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface ContentsWordCheckDaoSub {
    /*****************************************************
     * Select
     ****************************************************/
    /**
     * 금칙어 목록
     *
     * @return list
     */
    List<ContentsWordCheckDto> getList(int type);

}
