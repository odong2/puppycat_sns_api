package com.architecture.admin.models.daosub.contents;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.contents.ContentsDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface ContentsTagDaoSub {

    /*****************************************************
     * Select
     ****************************************************/
    /**
     * 내가 태그 된 콘텐츠 카운트
     *
     * @param searchDto loginMemberUuid
     * @return count
     */
    Integer iGetTotalMyTagContentsCount(SearchDto searchDto);

    /**
     * 해당 멤버가 태그 된 콘텐츠 카운트
     *
     * @param searchDto loginMemberUuid
     * @return count
     */
    Integer iGetTotalMemberTagContentsCount(SearchDto searchDto);

    /**
     * 내가 태그 된 콘텐츠 리스트
     *
     * @param searchDto loginMemberUuid
     * @return list
     */
    List<ContentsDto> lGetMyTagContentsList(SearchDto searchDto);

    /**
     * 해당 멤버가 태그 된 콘텐츠 리스트
     *
     * @param searchDto loginMemberUuid
     * @return list
     */
    List<ContentsDto> lGetMemberTagContentsList(SearchDto searchDto);

}
