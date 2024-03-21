package com.architecture.admin.models.dao.contents;

import com.architecture.admin.models.dto.contents.ContentsDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Mapper
public interface ContentsDao {

    /*****************************************************
     * Insert
     ****************************************************/
    /**
     * 콘텐츠 등록
     * [sns_contents]
     *
     * @param contentsDto uuid, memberUuid, menuIdx, contents, imageCnt, isView, regDate
     * @return 처리결과
     */
    Integer insert(ContentsDto contentsDto);

    /**
     * 콘텐츠 이미지 등록
     * [sns_contents_img]
     *
     * @param mapList 이미지 List
     * @return 처리결과
     */
    Integer insertImg(List<HashMap<String, Object>> mapList);

    /**
     * 콘텐츠 이미지 내 태그 회원 등록
     * [sns_img_member_tag_mapping]
     *
     * @param tagList imgIdx, memberUuid, width, height
     * @return 처리결과
     */
    Integer insertImgTag(List<Map<String, Object>> tagList);

    /**
     * 위치정보 등록
     * [sns_contents_location]
     *
     * @param contentsDto location
     * @return 처리결과
     */
    Integer insertLocation(ContentsDto contentsDto);

    /**
     * 콘텐츠 위치정보 매핑 등록
     * [sns_contents_location_mapping]
     *
     * @param contentsDto insertedIdx, locationIdx, regDate
     * @return 처리결과
     */
    Integer insertLocationMapping(ContentsDto contentsDto);

    /*****************************************************
     * Update
     ****************************************************/
    /**
     * 콘텐츠 내용 업데이트
     *
     * @param contentsDto contents idx
     * @return 처리결과
     */
    Long updateContentsContents(ContentsDto contentsDto);

    /**
     * 콘텐츠 이미지 내 태그 회원 수정
     * [sns_img_member_tag_mapping]
     *
     * @param tagList imgIdx, memberUuid, width, height
     * @return 처리결과
     */
    Integer modifyImgTag(List<Map<String, Object>> tagList);

    /*****************************************************
     * Delete
     ****************************************************/
    /**
     * 콘텐츠 삭제
     *
     * @param contentsDto 콘텐츠 idx
     * @return 처리결과
     */
    Long delete(ContentsDto contentsDto);    
    
    /**
     * 콘텐츠 이미지 내 태그 회원 삭제
     * [sns_img_member_tag_mapping]
     *
     * @param tagList imgIdx, memberUuid, width, height
     * @return 처리결과
     */
    Integer deleteImgTag(List<Map<String, Object>> tagList);

    /**
     * 컨텐츠 위치정보 삭제
     * 
     * @param contentsDto idx
     * @return 처리결과
     */
    Integer deleteLocation(ContentsDto contentsDto);

}
