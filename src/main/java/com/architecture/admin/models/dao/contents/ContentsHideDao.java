package com.architecture.admin.models.dao.contents;

import com.architecture.admin.models.dto.contents.ContentsHideDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface ContentsHideDao {

    /*****************************************************
     * Insert
     ****************************************************/
    /**
     * 숨기기 등록
     *
     * @param contentsHideDto contentsIdx, memberUuid, regDate
     * @return 처리결과
     */
    Integer insertContentsHide(ContentsHideDto contentsHideDto);

    /*****************************************************
     * Update
     ****************************************************/
    /**
     * 숨기기 상태값 변경
     *
     * @param contentsHideDto contentsIdx, memberUuid, regDate
     * @return 처리결과
     */
    Integer updateContentsHide(ContentsHideDto contentsHideDto);

}
