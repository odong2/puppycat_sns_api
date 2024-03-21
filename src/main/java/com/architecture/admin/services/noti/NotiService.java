package com.architecture.admin.services.noti;

import com.architecture.admin.config.NotiConfig;
import com.architecture.admin.models.dto.noti.NotiDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

/*****************************************************
 * 알림 공통 모델러
 ****************************************************/
@Service
@RequiredArgsConstructor
public class NotiService extends BaseService {

    @Value("${noti.show.date}")
    private Integer notiDate;                                           // 중복 제거 할 알림 기간

    /*****************************************************
     *  Modules
     ****************************************************/
    /**
     * 알림 제목 및 내용 가져오기
     *
     * @param notiDto SubType
     */
    public void getTitleBody(NotiDto notiDto) {
        // 제목 가져오기
        Map<String, String> notiTitle = NotiConfig.getNotiTitle();
        String title = notiTitle.get(notiDto.getSubType());
        notiDto.setTitle(title);

        // body 내용 가져오기
        Map<String, String> notiBody = NotiConfig.getNotiBody();
        String body = notiBody.get(notiDto.getSubType());
        notiDto.setBody(body);
    }

    /**
     * 중복 제거 할 날짜 구하기
     *
     * @return N일 전 날짜
     */
    public String getNotiDate() {
        // 일주일 전 날짜 구하기 (UTC)
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -notiDate);
        String beforeDate = formatDatetime.format(calendar.getTime());
        return dateLibrary.localTimeToUtc(beforeDate);
    }

    /*****************************************************
     *  SubFunction - select
     ****************************************************/
    /*****************************************************
     *  SubFunction - insert
     ****************************************************/
    /*****************************************************
     *  SubFunction - Update
     ****************************************************/

    /*****************************************************
     *  SubFunction - Etc
     ****************************************************/
}
