package com.architecture.admin.controllers.v1.contents;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.models.dto.contents.ContentsSaveDto;
import com.architecture.admin.services.contents.ContentsSaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/contents")
public class ContentsSaveV1Controller extends BaseController {

    private final ContentsSaveService contentsSaveService;

    /**
     * 콘텐츠 저장하기
     *
     * @param token       access token
     * @param contentsIdx 콘텐츠 idx
     * @return ResponseEntity
     */
    @PostMapping("{contentsIdx}/save")
    public ResponseEntity saveContents(@RequestHeader("Authorization") String token, @PathVariable("contentsIdx") Long contentsIdx) {

        ContentsSaveDto contentsSaveDto = ContentsSaveDto.builder()
                .contentsIdx(contentsIdx)
                .build();

        contentsSaveService.saveContents(token, contentsSaveDto);

        String sMessage = super.langMessage("lang.contents.success.save");
        return displayJson(true, "1000", sMessage);
    }

    /**
     * 콘텐츠 저장 취소
     *
     * @param token       access token
     * @param contentsIdx 콘텐츠 idx
     * @return ResponseEntity
     */
    @DeleteMapping("{contentsIdx}/save")
    public ResponseEntity likeCancelContents (@RequestHeader("Authorization") String token, @PathVariable("contentsIdx") Long contentsIdx) {

        ContentsSaveDto contentsSaveDto = ContentsSaveDto.builder()
                .contentsIdx(contentsIdx)
                .build();

        contentsSaveService.contentsSaveCancel(token, contentsSaveDto);

        String sMessage = super.langMessage("lang.contents.success.save.cancel");
        return displayJson(true, "1000", sMessage);
    }

}
