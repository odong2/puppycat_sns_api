package com.architecture.admin.controllers.v1.comment;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.comment.CommentLikeDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;
import com.architecture.admin.services.comment.CommentLikeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/comment/")
public class CommentLikeV1Controller extends BaseController {

    private final CommentLikeService commentLikeService;

    /**
     * 해당 댓글에 좋아요한 회원 list
     *
     * @param token        : access token
     * @param commentIdx   : 댓글 Idx
     * @param searchDto    : page [현재 페이지], imgLimit [이미지 개수], imgOffSet [이미지 시작 위치]
     * @return ResponseEntity
     */
    @GetMapping("{commentIdx}/like")
    public ResponseEntity commentLikeList(@RequestHeader("Authorization") String token, @PathVariable("commentIdx") Long commentIdx,
                                           @ModelAttribute SearchDto searchDto) throws JsonProcessingException {

        // 댓글 idx 세팅
        searchDto.setCommentIdx(commentIdx);

        // list
        List<MemberInfoDto> list = commentLikeService.getCommentLikeList(token, searchDto);

        String sMessage = super.langMessage("lang.comment.success.list");

        Map<String, Object> map = new HashMap<>();
        map.put("list", list);
        map.put("params", searchDto);

        JSONObject data = new JSONObject(map);
        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 댓글 좋아요
     *
     * @param token      access token
     * @param commentIdx 댓글 idx
     * @return ResponseEntity
     */
    @PostMapping("/{commentIdx}/like")
    public ResponseEntity likeContents (@RequestHeader("Authorization") String token, @PathVariable(name = "commentIdx") Long commentIdx) {

        CommentLikeDto commentLikeDto = CommentLikeDto.builder()
                                    .commentIdx(commentIdx)
                                    .build();

        // 댓글 좋아요 등록
        commentLikeService.commentLike(token, commentLikeDto);

        // response object
        String sMessage = super.langMessage("lang.comment.success.like");
        return displayJson(true, "1000", sMessage);
    }

    /**
     * 댓글 좋아요 취소
     *
     * @param token      access token
     * @param commentIdx 댓글 idx
     * @return ResponseEntity
     */
    @DeleteMapping("/{commentIdx}/like")
    public ResponseEntity likeCancelContents (@RequestHeader("Authorization") String token, @PathVariable(name = "commentIdx") Long commentIdx) {

        CommentLikeDto commentLikeDto = CommentLikeDto.builder()
                .commentIdx(commentIdx)
                .build();

        // 댓글 좋아요 취소
        commentLikeService.commentLikeCancel(token, commentLikeDto);

        // response object
        String sMessage = super.langMessage("lang.comment.success.like.cancel");
        return displayJson(true, "1000", sMessage);
    }

}
