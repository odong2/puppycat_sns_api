package com.architecture.admin.models.dao.noti;

import com.architecture.admin.models.dto.noti.NotiDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface NotiDao {

    /**
     * 컨텐츠 첫번째 이미지 가져오기
     *
     * @param contentsIdx 컨텐츠IDX
     * @return 이미지url
     */
    String getContentsImg(Long contentsIdx);

    /**
     * 컨텐츠에 멘션 된 회원 리스트 가져오기
     *
     * @param notiDto 컨텐츠IDX
     * @return 멘션 된 회원 리스트
     */
    List<String> getContentsMentionMember(NotiDto notiDto);

    /**
     * 댓글에 멘션 된 회원 리스트 가져오기
     *
     * @param commentIdx 댓글IDX
     * @return 멘션 된 회원 리스트
     */
    List<String> getCommentMentionMember(Long commentIdx);

    /**
     * 컨텐츠에 이미지 태그 된 회원 리스트 가져오기
     *
     * @param notiDto 컨텐츠IDX
     * @return 이미지 태그 된 회원 리스트
     */
    List<String> getImgTagMember(NotiDto notiDto);

    /**
     * 수정전 컨텐츠에 이미지 태그 된 회원 리스트
     * 
     * @param notiDto 컨텐츠IDX
     * @return
     */
    List<String> getPrevImgTagMember(NotiDto notiDto);
}
