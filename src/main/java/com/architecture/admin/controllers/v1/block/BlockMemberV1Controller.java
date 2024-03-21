package com.architecture.admin.controllers.v1.block;

import com.architecture.admin.controllers.v1.BaseController;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.block.BlockMemberDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;
import com.architecture.admin.services.block.BlockMemberService;
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
@RequestMapping("/v1/block/member")
public class BlockMemberV1Controller extends BaseController {

    private final BlockMemberService blockMemberService;

    /**
     * 내가 차단한 회원 리스트
     *
     * @param token     token
     * @param searchDto page 페이지 searchType 검색항목 searchWord 검색내용 limit 페이지에 노출 될 개수
     * @return ResponseEntity
     */
    @GetMapping("")
    public ResponseEntity blockList(@RequestHeader("Authorization") String token, @ModelAttribute SearchDto searchDto) throws JsonProcessingException {

        // list
        List<MemberInfoDto> list = blockMemberService.getBlockList(token, searchDto);

        String sMessage;

        if (list.isEmpty()) {
            sMessage = super.langMessage("lang.block.list.empty"); // 차단한 유저가 없습니다.
        } else {
            sMessage = super.langMessage("lang.block.success.list"); // 리스트 가져오기 성공
        }

        Map<String, Object> map = new HashMap<>();
        map.put("list", list);
        map.put("params", searchDto);

        JSONObject data = new JSONObject(map);
        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 회원 차단
     *
     * @param token      token
     * @param blockUuid  차단대상 회원 uuid
     * @return ResponseEntity
     */
    @PostMapping("/{blockUuid}")
    public ResponseEntity blockMember(@RequestHeader("Authorization") String token,
                                      @PathVariable("blockUuid") String blockUuid) {

        // dto set
        BlockMemberDto blockMemberDto = BlockMemberDto.builder()
                .blockUuid(blockUuid) // 차단 대상 memberUuid
                .build();

        // 회원 차단
        MemberDto memberDto = blockMemberService.blockMember(token, blockMemberDto);

        // 리턴할 차단 대상 정보
        JSONObject data = new JSONObject();
        data.put("memberUuid", memberDto.getUuid());
        data.put("nick", memberDto.getNick());

        // response object
        String sMessage = super.langMessage("lang.block.success");
        return displayJson(true, "1000", sMessage, data);
    }

    /**
     * 회원 차단 해제
     *
     * @param token      token
     * @param blockUuid  차단대상 회원 uuid
     * @return ResponseEntity
     */
    @DeleteMapping("/{blockUuid}")
    public ResponseEntity unblockMember(@RequestHeader("Authorization") String token,
                                        @PathVariable("blockUuid") String blockUuid) {

        // dto set
        BlockMemberDto blockMemberDto = BlockMemberDto.builder()
                .blockUuid(blockUuid) // 차단 해제 대상 memberUuid
                .build();

        // 차단 해제
        MemberDto memberDto = blockMemberService.unblockMember(token, blockMemberDto);

        // 리턴할 차단 대상 정보
        JSONObject data = new JSONObject();
        data.put("memberUuid", memberDto.getUuid());
        data.put("nick", memberDto.getNick());

        // response object
        String sMessage = super.langMessage("lang.unblock.success");
        return displayJson(true, "1000", sMessage, data);
    }

}
