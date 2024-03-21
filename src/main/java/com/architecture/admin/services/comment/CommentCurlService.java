package com.architecture.admin.services.comment;

import com.architecture.admin.models.dto.member.MemberDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "commentCurlService", url = "${member.domain}/v1/comment")
public interface CommentCurlService {

    /**
     * 좋아요 많은 댓글 회원 정보 조회
     *
     * @param uuid
     * @return
     */
    @GetMapping("/like/many/member/info")
    MemberDto getLikeManyCommentMemberInfo(@RequestParam(name = "uuid") String uuid);
}
