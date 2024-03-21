package com.architecture.admin.controllers.v1.contents;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.models.dto.contents.ContentsDto;
import com.architecture.admin.services.contents.ContentsKeepService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/contents")
public class ContentsKeepV1Controller extends BaseController {

    private final ContentsKeepService contentsKeepService;

    /**
     * 콘텐츠 보관 (나만보기)
     *
     * @param token         access token
     * @param contentsDto   보관할 콘텐츠 idx
     * @return ResponseEntity
     */
    @PostMapping("keep")
    public ResponseEntity keepContents(@RequestHeader("Authorization") String token, @RequestBody ContentsDto contentsDto) {

        contentsKeepService.keepContents(token, contentsDto);

        String sMessage = super.langMessage("lang.contents.success.store");
        return displayJson(true, "1000", sMessage);
    }

    /**
     * 콘텐츠 보관 해제 (프로필 표시)
     *
     * @param token     access token
     * @param idxList   보관 해제할 콘텐츠 idx
     * @return ResponseEntity
     */
    @DeleteMapping("keep")
    public ResponseEntity unKeepContents(@RequestHeader("Authorization") String token, @RequestParam("idx") List<Long> idxList) {

        ContentsDto contentsDto = ContentsDto.builder()
                .idxList(idxList)
                .build();

        contentsKeepService.unKeepContents(token, contentsDto);

        String sMessage = super.langMessage("lang.contents.success.store.cancel");
        return displayJson(true, "1000", sMessage);
    }

}
