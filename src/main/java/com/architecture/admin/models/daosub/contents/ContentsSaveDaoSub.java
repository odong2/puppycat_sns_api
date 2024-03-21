package com.architecture.admin.models.daosub.contents;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.contents.ContentsDto;
import com.architecture.admin.models.dto.contents.ContentsSaveDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface ContentsSaveDaoSub {

    /*****************************************************
     * Select
     ****************************************************/
    /**
     * 저장 내역 가져오기
     *
     * @param contentsSaveDto contentsIdx, memberUuid
     * @return ContentsSaveDto
     */
    ContentsSaveDto oGetTargetInfo(ContentsSaveDto contentsSaveDto);

    /**
     * cnt 테이블에 해당 idx 있는지 확인
     *
     * @param contentsSaveDto contentsIdx
     * @return contentsIdx
     */
    Long lCheckCntByIdx(ContentsSaveDto contentsSaveDto);

    /**
     * 내가 저장 한 콘텐츠 카운트
     *
     * @param searchDto loginMemberUuid
     * @return count
     */
    Integer iGetTotalMySaveContentsCount(SearchDto searchDto);

    /**
     * 내가 저장 한 콘텐츠 리스트
     *
     * @param searchDto loginMemberUuid
     * @return list
     */
    List<ContentsDto> lGetMySaveContentsList(SearchDto searchDto);

}
