package com.architecture.admin.controllers.v1.contents;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.models.dto.contents.ContentsHideDto;
import com.architecture.admin.services.contents.ContentsHideService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/contents")
public class ContentsHideV1Controller extends BaseController {

    private final ContentsHideService contentsHideService;

    /**
     * 콘텐츠 숨기기
     *
     * @param token       access token
     * @param contentsIdx 콘텐츠 idx
     * @return ResponseEntity
     */
    @PostMapping("{contentsIdx}/hide")
    public ResponseEntity hideContents(@RequestHeader("Authorization") String token, @PathVariable("contentsIdx") Long contentsIdx) {

        ContentsHideDto contentsHideDto = ContentsHideDto.builder()
                .contentsIdx(contentsIdx)
                .build();

        contentsHideService.hideContents(token, contentsHideDto);

        String sMessage = super.langMessage("lang.contents.success.hide");
        return displayJson(true, "1000", sMessage);
    }

    /**
     * 콘텐츠 숨기기 취소
     *
     * @param token       access token
     * @param contentsIdx 콘텐츠 idx
     * @return ResponseEntity
     */
    @DeleteMapping("{contentsIdx}/hide")
    public ResponseEntity hideCancelContents (@RequestHeader("Authorization") String token, @PathVariable("contentsIdx") Long contentsIdx) {

        ContentsHideDto contentsHideDto = ContentsHideDto.builder()
                .contentsIdx(contentsIdx)
                .build();

        contentsHideService.contentsHideCancel(token, contentsHideDto);

        String sMessage = super.langMessage("lang.contents.success.hide.cancel");
        return displayJson(true, "1000", sMessage);
    }

}
