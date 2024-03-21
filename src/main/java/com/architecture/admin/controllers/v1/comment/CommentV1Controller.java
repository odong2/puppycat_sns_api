package com.architecture.admin.controllers.v1.comment;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.comment.CommentDto;
import com.architecture.admin.services.comment.CommentService;
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
@RequestMapping("/v1/contents/{contentsIdx}/comment")
public class CommentV1Controller extends BaseController {
    private final CommentService commentService;

    /**
     * 댓글 리스트
     *
     * @param contentsIdx : 컨텐츠 idx
     * @param searchDto   : content(댓글 내용)
     * @return
     */
    @GetMapping("")
    public ResponseEntity getContentInComment(@PathVariable(name = "contentsIdx") Long contentsIdx
                                              , @RequestHeader(value = "Authorization", defaultValue = "") String token
                                              , @ModelAttribute SearchDto searchDto) throws JsonProcessingException {

        List<CommentDto> lCommentList = null;

        // new Dto
        CommentDto commentDto = new CommentDto();

        // IDX 세팅
        searchDto.setContentsIdx(contentsIdx);

        // Contents IDX 세팅
        commentDto.setContentsIdx(contentsIdx);

        // 댓글 총 카운트
        CommentDto getCommentDto = commentService.getTotalSumCommentCount(commentDto);

        if (getCommentDto.getTotalCommentCnt() > 0) {
            // 댓글 리스트
            lCommentList = commentService.getCommentList(token, searchDto);
        }

        // map put
        Map<String, Object> mMap = new HashMap<>();
        mMap.put("commentTotalCount", getCommentDto.getTotalCommentCnt());
        mMap.put("list", lCommentList);
        mMap.put("params", searchDto);

        // data Object
        JSONObject data = new JSONObject(mMap);

        String sMessage;

        // 리스트 가져오기 성공
        sMessage = super.langMessage("lang.follow.success.list");

        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 대 댓글 리스트
     *
     * @param contentsIdx  : 콘텐츠 idx
     * @param commentIdx   : 댓글 idx
     * @param token        : access token
     * @param searchDto    : page limit
     * @return
     * @throws JsonProcessingException
     */
    @GetMapping("/{commentIdx}/child")
    public ResponseEntity getContentInChildComment(@PathVariable(name = "contentsIdx") Long contentsIdx,
                                                   @PathVariable(name = "commentIdx") Long commentIdx,
                                                   @RequestHeader(value = "Authorization", defaultValue = "") String token,
                                                   @ModelAttribute SearchDto searchDto) throws JsonProcessingException {

        // Contents IDX 세팅
        searchDto.setContentsIdx(contentsIdx);

        // Comment IDX 세팅
        searchDto.setCommentIdx(commentIdx);

        //parent IDX 셋팅
        searchDto.setParentIdx(commentIdx);

        // 댓글 리스트
        List<CommentDto> list = commentService.getChildCommentList(token, searchDto);

        Map<String, Object> mMap = new HashMap<>();

        mMap.put("list", list);
        mMap.put("params", searchDto);

        String sMessage;
        // data Object
        JSONObject data = new JSONObject(mMap);

        // 리스트 가져오기 성공
        sMessage = super.langMessage("lang.follow.success.list");

        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 포커싱 댓글 리스트
     *
     * @param token         : access token
     * @param contentsIdx   : 콘텐츠 idx
     * @param commentIdx    : 댓글 idx
     * @param searchDto     : page limit
     * @return
     * @throws JsonProcessingException
     */
    @GetMapping("/{commentIdx}/focus")
    public ResponseEntity getNotiDirectCommentList(@RequestHeader("Authorization") String token,
                                                   @PathVariable(name = "contentsIdx") Long contentsIdx,
                                                   @PathVariable(name = "commentIdx") Long commentIdx,
                                                   @ModelAttribute SearchDto searchDto) throws JsonProcessingException {

        // Contents IDX 세팅
        searchDto.setContentsIdx(contentsIdx);
        searchDto.setCommentIdx(commentIdx);

        // 댓글 리스트
        List<CommentDto> list = commentService.getFocusCommentList(token, searchDto);

        Map<String, Object> mMap = new HashMap<>();

        mMap.put("list", list);
        mMap.put("params", searchDto);

        String sMessage;
        // data Object
        JSONObject data = new JSONObject(mMap);

        // 리스트 가져오기 성공
        sMessage = super.langMessage("lang.follow.success.list");

        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 댓글 등록
     *
     * @param contentsIdx : 컨텐츠 idx
     * @param commentDto  : content(댓글 내용)
     * @return
     */
    @PostMapping("")
    public ResponseEntity registComment(@RequestHeader("Authorization") String token,
                                        @PathVariable(name = "contentsIdx") Long contentsIdx,
                                        @RequestBody CommentDto commentDto) {
        // IDX 세팅
        commentDto.setContentsIdx(contentsIdx);

        // 댓글 등록
        Long result = commentService.registComment(token, commentDto);

        // 등록 완료
        String sMessage = super.langMessage("lang.comment.success.regist");
        // response object
        JSONObject data = new JSONObject(result);

        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 댓글 수정
     *
     * @param token
     * @param contentsIdx : 컨텐츠 idx
     * @param idx         : 댓글 idx
     * @param commentDto
     * @return
     */
    @PutMapping("/{commentIdx}")
    public ResponseEntity modifyComment(@RequestHeader(value = "Authorization") String token,
                                        @PathVariable Long contentsIdx,
                                        @PathVariable(name = "commentIdx") Long idx,
                                        @RequestBody CommentDto commentDto) {
        // IDX 세팅
        commentDto.setContentsIdx(contentsIdx);
        commentDto.setIdx(idx);

        // 댓글 수정
        commentService.modifyComment(token, commentDto);

        // 수정 완료
        String sMessage = super.langMessage("lang.comment.success.modify");

        return displayJson(true, "1000", sMessage);
    }

    /**
     * 댓글 삭제
     *
     * @param token         access token
     * @param contentsIdx   콘텐츠 idx
     * @param commentIdx    댓글 idx
     * @param parentIdx     부모 댓글 idx
     * @return
     */
    @DeleteMapping("/{commentIdx}")
    public ResponseEntity remove(@RequestHeader("Authorization") String token,
                                 @PathVariable(name = "contentsIdx") Long contentsIdx,
                                 @PathVariable(name = "commentIdx") Long commentIdx,
                                 @RequestParam("parentIdx") Long parentIdx) {

        CommentDto commentDto = new CommentDto();
        // 삭제 할 댓글이 포함 된 컨테츠의 IDX
        commentDto.setContentsIdx(contentsIdx);
        // 삭제 할 대/댓글 IDX 세팅
        commentDto.setIdx(commentIdx);
        // 삭제 실행 ParentIdx
        commentDto.setParentIdx(parentIdx);

        // 댓글 삭제
        int result = commentService.removeComment(token, commentDto);

        //삭제 성공
        String sMessage = super.langMessage("lang.comment.delete.success");
        // response object
        JSONObject data = new JSONObject(result);
        return displayJson(true, "1000", sMessage, data);
    }
}
