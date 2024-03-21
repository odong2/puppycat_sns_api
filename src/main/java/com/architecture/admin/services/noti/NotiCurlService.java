package com.architecture.admin.services.noti;

import com.architecture.admin.models.dto.noti.NotiDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "notiCurlService", url = "${member.domain}/v1/noti")
public interface NotiCurlService {

    /*****************************************************
     *  SubFunction - select
     ****************************************************/
    /**
     * 회원 N일내 중복 알림 IDX 가져오기
     *
     * @param token
     * @param notiDto
     * @return
     */
    @PostMapping("/duple")
    String getNotiDuple(@RequestHeader("Authorization") String token,
                      @ModelAttribute NotiDto notiDto);

    /*****************************************************
     *  SubFunction - Insert
     ****************************************************/
    /**
     * noti 리스트 등록
     *
     * @param notiDto : memberUuidList, sub_type, sender_uuid, type, title, body
     * @return
     */
    @PostMapping()
    String registNoti(@RequestHeader("Authorization") String token,
                       @ModelAttribute NotiDto notiDto);

    /**
     * 알림 등록
     *
     * @param token
     * @param notiDto
     * @return
     */
    @PostMapping("/list")
    String registNotiList(@RequestHeader("Authorization") String token,
                          @RequestBody NotiDto notiDto);

    /*****************************************************
     *  SubFunction - Update
     ****************************************************/
     @PutMapping("{notiIdx}")
     String modiNotiRegDate(@RequestHeader("Authorization") String token,
                            @PathVariable("notiIdx") Long notiIdx);

}
