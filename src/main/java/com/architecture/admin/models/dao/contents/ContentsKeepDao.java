package com.architecture.admin.models.dao.contents;

import com.architecture.admin.models.dto.contents.ContentsDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;


@Repository
@Mapper
public interface ContentsKeepDao {

    /*****************************************************
     * Update
     ****************************************************/
    /**
     * 콘텐츠 보관상태 변경
     * [sns_contents]
     *
     * @param contentsDto sns_contents.idx
     * @return 처리결과
     */
    Integer updateIsStore(ContentsDto contentsDto);

}
