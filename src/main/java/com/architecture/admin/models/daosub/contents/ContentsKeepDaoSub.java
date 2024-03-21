package com.architecture.admin.models.daosub.contents;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.contents.ContentsDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface ContentsKeepDaoSub {

    /*****************************************************
     * Select
     ****************************************************/
    /**
     * 내가 보관 한 콘텐츠 카운트
     *
     * @param searchDto loginMemberUuid
     * @return count
     */
    Integer iGetTotalMyKeepContentsCount(SearchDto searchDto);

    /**
     * 내가 보관 한 콘텐츠 리스트
     *
     * @param searchDto loginMemberUuid
     * @return list
     */
    List<ContentsDto> lGetMyKeepContentsList(SearchDto searchDto);

}
