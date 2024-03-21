package com.architecture.admin.controllers.v1.report;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.models.dto.report.ContentsReportDto;
import com.architecture.admin.services.report.ContentsReportService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/contents/")
public class ContentsReportV1Controller extends BaseController {

    private final ContentsReportService contentsReportService;

    /**
     * 신고 사유 가져오기
     *
     * @return 신고사유 list
     */
    @GetMapping("report/code")
    public ResponseEntity getListReportCode(){
        // list
        List<ContentsReportDto> list = contentsReportService.lGetReportCode();

        Map<String, Object> map = new HashMap<>();
        map.put("list", list);

        JSONObject data = new JSONObject(map);
        String sMessage = super.langMessage("lang.report.code.list.success");

        // return value
        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 콘텐츠 신고
     *
     * @param contentsIdx       콘텐츠 idx
     * @param contentsReportDto code, reason
     * @return 처리결과
     */
    @PostMapping("{contentsIdx}/report")
    public ResponseEntity reportContents(@RequestHeader("Authorization") String token, @PathVariable("contentsIdx") Long contentsIdx,
                                         @RequestBody ContentsReportDto contentsReportDto) {

        // contentsIdx 세팅
        contentsReportDto.setContentsIdx(contentsIdx);

        // 콘텐츠 신고
        contentsReportService.reportContents(token, contentsReportDto);

        // return value
        String message = super.langMessage("lang.report.success");
        return displayJson(true, "1000", message);
    }

    /**
     * 콘텐츠 신고 취소
     *
     * @param contentsIdx       콘텐츠 idx
     * @return 처리결과
     */
    @DeleteMapping("{contentsIdx}/report")
    public ResponseEntity cancelReportContents(@RequestHeader("Authorization") String token, @PathVariable("contentsIdx") Long contentsIdx) {

        // contentsIdx 세팅
        ContentsReportDto contentsReportDto = new ContentsReportDto();
        contentsReportDto.setContentsIdx(contentsIdx);

        // 콘텐츠 신고취소
        contentsReportService.cancelReportContents(token, contentsReportDto);

        // return value
        String message = super.langMessage("lang.report.cancel.success");
        return displayJson(true, "1000", message);
    }

}
