package com.architecture.admin.models.daosub.noti;

import com.architecture.admin.models.dto.noti.NotiDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface NotiDaoSub {

    /**
     * 알림용 회원의 팔로워 리스트 가져오기
     *
     * @param notiDto senderIdx
     * @return 팔로워리스트
     */
    List<String> getFollowerList(NotiDto notiDto);

    /**
     * 컨텐츠 작성자 가져오기
     *
     * @param contentsIdx 컨텐츠IDX
     * @return 컨텐츠 작성자 uuid
     */
    String getContentsMember(Long contentsIdx);

    /**
     * 부모댓글 작성자 가져오기
     *
     * @param commentIdx 부모댓글IDX
     * @return 부모댓글 작성자 uuid
     */
    String getParentCommentMember(Long commentIdx);

    /**
     * 댓글에 멘션된 회원 리스트
     *
     * @param commentIdx
     * @return
     */
    List<String> getCommentMentionMember(Long commentIdx);
}
