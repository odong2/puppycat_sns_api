package com.architecture.admin.services.noti;

import com.architecture.admin.libraries.exception.CurlException;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dto.noti.NotiDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;


/*****************************************************
 * 팔로우 알림 모델러
 ****************************************************/
@Service
@RequiredArgsConstructor
public class FollowNotiService extends BaseService {
    private final NotiService notiService;
    private final NotiCurlService notiCurlService;

    /*****************************************************
     *  Modules
     ****************************************************/
    public void followSendNoti(String token, NotiDto notiDto) {
        if (notiDto.getSenderUuid() == null || notiDto.getSenderUuid().equals("")) {
            // 알림 보내는 회원 UUID가 비어있습니다.
            throw new CustomException(CustomError.NOTI_SENDERUUID_ERROR);
        }
        if (notiDto.getMemberUuid() == null || notiDto.getMemberUuid().equals("")) {
            // 알림 받는 회원 UUID가 비어있습니다.
            throw new CustomException(CustomError.NOTI_MEMBERUUID_ERROR);
        }
        if (notiDto.getSubType() == null || notiDto.getSubType().equals("")) {
            // 서브타입을 입력해주세요.
            throw new CustomException(CustomError.NOTI_SUBTYPE_ERROR);
        }

        // 알림 제목, 내용 세팅
        notiService.getTitleBody(notiDto);

        // 중복 날짜 제거할 날짜 가져오기
        String checkDate = notiService.getNotiDate();
        notiDto.setCheckNotiDate(checkDate);

        // DB에 있는 30일 이내 내역 idx 가져오기
        String getNotiIdx = notiCurlService.getNotiDuple(token, notiDto);
        JSONObject notiDupleIdxObject = new JSONObject(getNotiIdx);

        if (!((boolean) notiDupleIdxObject.get("result"))) {
            throw new CurlException(notiDupleIdxObject);
        }
        JSONObject notiDupleIdxResult = (JSONObject) notiDupleIdxObject.get("data");

        long notiIdx = notiDupleIdxResult.getLong("notiDupleIdx");

        // 있으면 reg_date 업데이트
        if (notiIdx > 0) {
            notiCurlService.modiNotiRegDate(token, notiIdx);
        }
        // 없으면 알림 인서트
        else {
            notiCurlService.registNoti(token, notiDto);
        }
    }
}
