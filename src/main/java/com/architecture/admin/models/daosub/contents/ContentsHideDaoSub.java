package com.architecture.admin.models.daosub.contents;

import com.architecture.admin.models.dto.contents.ContentsHideDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface ContentsHideDaoSub {

    /*****************************************************
     * Select
     ****************************************************/
    /**
     * 숨기기 내역 가져오기
     *
     * @param contentsHideDto contentsIdx, memberIdx
     * @return ContentsHideDto
     */
    ContentsHideDto oGetTargetInfo(ContentsHideDto contentsHideDto);

}
