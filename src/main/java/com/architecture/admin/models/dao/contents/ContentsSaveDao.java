package com.architecture.admin.models.dao.contents;

import com.architecture.admin.models.dto.contents.ContentsSaveDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface ContentsSaveDao {

    /*****************************************************
     * Insert
     ****************************************************/
    /**
     * 콘텐츠 저장수 등록
     * [sns_contents_save_cnt]
     * @param contentsSaveDto contentsIdx
     */
    void insertContentsSaveCnt(ContentsSaveDto contentsSaveDto);

    /**
     * 저장 등록
     *
     * @param contentsSaveDto contentsIdx, memberIdx, regDate
     * @return 처리결과
     */
    Integer insertContentsSave(ContentsSaveDto contentsSaveDto);

    /*****************************************************
     * Update
     ****************************************************/
    /**
     * 저장 상태값 변경
     *
     * @param contentsSaveDto contentsIdx, memberIdx, regDate
     * @return 처리결과
     */
    Integer updateContentsSave(ContentsSaveDto contentsSaveDto);

    /**
     * 저장 cnt +1
     *
     * @param contentsSaveDto contentsIdx, regDate
     */
    void updateContentsSaveCntUp(ContentsSaveDto contentsSaveDto);

    /**
     * 저장 cnt -1
     *
     * @param contentsSaveDto contentsIdx, regDate
     */
    void updateContentsSaveCntDown(ContentsSaveDto contentsSaveDto);

}
