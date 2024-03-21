package com.architecture.admin.controllers.v1.report;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.models.dto.report.CommentReportDto;
import com.architecture.admin.services.report.CommentReportService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/comment")
public class CommentReportV1Controller extends BaseController {

    private final CommentReportService commentReportService;

    /**
     * 신고 사유 가져오기
     *
     * @return 신고사유 list
     */
    @GetMapping("report/code")
    public ResponseEntity getListReportCode(){
        // list
        List<CommentReportDto> list = commentReportService.lGetReportCode();

        Map<String, Object> map = new HashMap<>();
        map.put("list", list);

        JSONObject data = new JSONObject(map);
        String sMessage = super.langMessage("lang.report.code.list.success");

        // return value
        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 댓글 신고
     *
     * @param token             access token
     * @param commentIdx        댓글 idx
     * @param commentReportDto  code, reason
     * @return
     */
    @PostMapping("{commentIdx}/report")
    public ResponseEntity reportComment(@RequestHeader("Authorization") String token,
                                        @PathVariable("commentIdx") Long commentIdx,
                                        @RequestBody CommentReportDto commentReportDto) {

        // commentIdx 세팅
        commentReportDto.setCommentIdx(commentIdx);

        // 댓글 신고
        commentReportService.reportComment(token, commentReportDto);

        // return value
        String message = super.langMessage("lang.report.success");
        return displayJson(true, "1000", message);
    }

    /**
     * 댓글 신고 취소
     *
     * @param token             access token
     * @param commentIdx       댓글 idx
     * @return 처리결과
     */
    @DeleteMapping("{commentIdx}/report")
    public ResponseEntity cancelReportComment(@RequestHeader("Authorization") String token,
                                              @PathVariable("commentIdx") Long commentIdx) {

        CommentReportDto commentReportDto = new CommentReportDto();
        commentReportDto.setCommentIdx(commentIdx);

        // 댓글 신고취소
        commentReportService.cancelReportComment(token, commentReportDto);

        // return value
        String message = super.langMessage("lang.report.cancel.success");
        return displayJson(true, "1000", message);
    }

}
