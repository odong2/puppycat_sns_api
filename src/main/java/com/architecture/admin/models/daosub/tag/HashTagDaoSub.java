package com.architecture.admin.models.daosub.tag;

import com.architecture.admin.models.dto.tag.HashTagDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface HashTagDaoSub {
    /**
     * 유효한 해시태그인지 조회
     *
     * @param hashTag
     * @return
     */
    int getHashTagCntByHashTag(String hashTag);

    /**
     * 해시태그 등록된게 있는지 확인
     * [sns_hash_tag]
     *
     * @param hashTagDto hashTag
     * @return hashtag.idx
     */
    Integer getIdxByHashTag(HashTagDto hashTagDto);

    /**
     * 컨텐츠에 등록 된 해시태그 리스트
     *
     * @param hashTagDto contentsIdx
     * @return 해시태그 리스트
     */
    List<HashTagDto> getContentsHashTagList(HashTagDto hashTagDto);

    /**
     * 댓글에 등록 된 해시태그 리스트
     *
     * @param hashTagDto commentIdx
     * @return 해시태그 리스트
     */
    List<HashTagDto> getCommentHashTagList(HashTagDto hashTagDto);

    /**
     * 컨텐츠에 이미 사용된 해시태그인지 체크
     *
     * @param hashTagDto contentsIdx hashTagIdx
     * @return
     */
    Integer getContentsHashTag(HashTagDto hashTagDto);

    /**
     * 댓글에 이미 사용된 해시태그인지 체크
     *
     * @param hashTagDto commentIdx hashTagIdx
     * @return
     */
    Integer getCommentHashTag(HashTagDto hashTagDto);
}
