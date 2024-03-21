package com.architecture.admin.models.dao.block;

import com.architecture.admin.models.dto.block.BlockMemberDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface BlockMemberDao {

    /*****************************************************
     * Insert
     ****************************************************/
    /**
     * 회원 차단 등록
     *
     * @param blockMemberDto memberUuid blockUuid regDate
     * @return Integer
     */
    Integer insertBlockMember(BlockMemberDto blockMemberDto);

    /*****************************************************
     * Update
     ****************************************************/
    /**
     * 차단 상태값 변경 (state 1: 차단, 0: 해제)
     *
     * @param blockMemberDto memberUuid blockUuid state
     * @return Integer
     */
    Integer updateBlockState(BlockMemberDto blockMemberDto);

}
