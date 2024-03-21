package com.architecture.admin.models.daosub.block;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.block.BlockMemberDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface BlockMemberDaoSub {

    /**
     * 차단 카운트 가져오기 [sns_member_block]
     *
     * @param searchDto 회원 idx, 검색조건
     * @return count
     */
    Integer iGetTotalBlockCount(SearchDto searchDto);

    /**
     * 차단 회원 리스트 가져오기
     *
     * @param searchDto 회원 idx, 검색조건
     * @return 차단 리스트
     */
    List<String> lGetBlockList(SearchDto searchDto);

    /**
     * 차단(해제) 내역 가져오기
     *
     * @param blockMemberDto memberUuid blockUuid
     * @return BlockMemberDto
     */
    BlockMemberDto oGetTargetInfo(BlockMemberDto blockMemberDto);

    /**
     * 정상적인 차단 내역 체크
     *
     * @param blockMemberDto memberUuid blockUuid
     * @return count
     */
    Integer getBlockByUuid(BlockMemberDto blockMemberDto);
}
