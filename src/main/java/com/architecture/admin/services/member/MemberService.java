package com.architecture.admin.services.member;

import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.daosub.contents.ContentsDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;


@RequiredArgsConstructor
@Service
@Transactional
public class MemberService extends BaseService {

    private final ContentsDaoSub contentsDaoSub;

    /*****************************************************
     *  Modules
     ****************************************************/

    /*****************************************************
     *  SubFunction - Select
     ****************************************************/
    /**
     * 회원 뱃지 정보 가져오기
     *
     * @param memberUuid 회원 uuid
     * @return followerCnt, isBadge
     */
    public MemberDto getMemberBadgeInfoByUuid(String memberUuid) {
        // member idx 검증
        if (memberUuid == null || "".equals(memberUuid)) {
            // 회원 UUID가 존재하지 않습니다.
            throw new CustomException(CustomError.MEMBER_UUID_ERROR);
        }
        return memberDaoSub.getMemberBadgeInfoByUuid(memberUuid);
    }

    /*****************************************************
     *  SubFunction - Update
     ****************************************************/
    /**
     * 회원 활동 정보
     *
     * @param token
     * @return
     */
    public MemberDto getMemberActivityInfo(String token) {

        SearchDto searchDto = new SearchDto();
        // 로그인 회원 uuid curl 조회
        MemberDto loginUserInfo = super.getMemberUuidByToken(token);

        // 로그인 회원 uuid set
        searchDto.setLoginMemberUuid(loginUserInfo.getUuid());

        // 내가 태그된 컨텐츠 카운트
        int tagCnt = contentsDaoSub.getTotalTagCnt(searchDto);
        // 저장한 컨텐츠 카운트
        int saveCnt = contentsDaoSub.getTotalSaveCnt(searchDto);

        // 내가 작성한 컨텐츠 카운트
        int contentsCnt = contentsDaoSub.iGetTotalMyContentsCount(searchDto);
        // 회원 가입일 & 가입 타임존 조회
        MemberDto memberDto = memberCurlService.getMemberRegdate(token, searchDto.getLoginMemberUuid());

        if (memberDto.getResult() == false) {
            throw new CustomException(CustomError.MEMBER_UUID_EMPTY); // 회원 uuid가 비었습니다.
        }

        // 활동 시간 구하기
        String activityTime = getActivityTime(memberDto);
        // 숫자 형식 지정
        String totalTagCnt = formatNumber(tagCnt);
        String totalSaveCnt = formatNumber(saveCnt);
        String totalContentsCnt = formatNumber(contentsCnt);

        /** return value **/
        return MemberDto.builder()
                .totalTagCnt(totalTagCnt)
                .totalSaveCnt(totalSaveCnt)
                .totalContentsCnt(totalContentsCnt)
                .totalActivityTime(activityTime).build();
    }

    /*****************************************************
     *  SubFunction - ETC
     ****************************************************/

    /**
     * 회원 활동 시간 구하기
     *
     * @param memberDto
     * @return
     */
    @SneakyThrows
    private String getActivityTime(MemberDto memberDto) {

        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String regDate = memberDto.getRegDate();   // 가입일
        String regDateTz = memberDto.getRegDateTz(); // 가입 타임존

        String nowDate = dateLibrary.getDatetimeByTimeZone(regDateTz);

        Date formatNowDate = formatDatetime.parse(nowDate);
        Date formatRegdate = formatDatetime.parse(regDate);

        long diffMilliseconds = formatNowDate.getTime() - formatRegdate.getTime();

        // Milliseconds -> Hour 변환
        long hour = TimeUnit.HOURS.convert(diffMilliseconds, TimeUnit.MILLISECONDS);
        // 숫자 형식 지정(,)
        String convertedHour = formatNumber(hour);

        convertedHour = convertedHour + super.langMessage("lang.common.time.hour");

        return convertedHour;
    }

    /**
     * 숫자 형식 지정
     * 3자리마다 (,) 쉼표
     *
     * @param number
     * @return
     */
    private String formatNumber(long number) {
        // 숫자 형식 지정(,)
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        return decimalFormat.format(number);
    }
}
