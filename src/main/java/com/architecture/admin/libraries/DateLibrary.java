package com.architecture.admin.libraries;

import lombok.Data;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/*****************************************************
 * 시간 라이브러리
 ****************************************************/
@Component
@Data
public class DateLibrary {
    private final int ONE_MIN = 60;
    private final int ONE_SECONDS = 1;
    private final int ONE_HOUR = 3600;
    private final int ONE_DAY = 86400;
    private final int ONE_WEEK = 604800;
    private final int ONE_MONTH = 2592000;
    private final int ONE_YEAR = 31104000;
    @Autowired
    protected MessageSource messageSource;

    /**
     * yyyy-MM-dd HH:mm:ss -> yyyy-MM-dd 오전/오후 hh시 mm분 변환
     *
     * @param inputDate yyyy-MM-dd HH:mm:ss
     * @return yyyy-MM-dd 오전/오후 hh시 mm분
     */
    @SneakyThrows
    public static String getConvertAmPmRegdate(String inputDate) {
        String dateString = inputDate;

        // 기존 형식의 DateTimeFormatter 생성
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 변환할 형식의 DateTimeFormatter 생성
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd a hh시 mm분");

        // 문자열을 LocalDateTime 객체로 파싱
        LocalDateTime dateTime = LocalDateTime.parse(dateString, inputFormatter);

        // 새로운 형식으로 변환
        return dateTime.format(outputFormatter);
    }

    /**
     * date 형식 시간 구하기
     *
     * @return UTC 기준 시간 yyyy-MM-dd hh:mm:ss
     */
    public String getDatetime() {
        java.util.Date dateNow = new java.util.Date(System.currentTimeMillis());

        // 타임존 UTC 기준
        TimeZone utcZone = TimeZone.getTimeZone("UTC");
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        formatDatetime.setTimeZone(utcZone);

        // 현재 날짜 구하기 (시스템 시계, 시스템 타임존)
        return formatDatetime.format(dateNow);
    }

    /**
     * 로컬시간을 UTC 시간으로 변경
     *
     * @param date 로컬 시간 yyyy-MM-dd hh:mm:ss
     * @return UTC 기준 시간 yyyy-MM-dd hh:mm:ss
     */
    public String localTimeToUtc(String date) {
        // 타임존 UTC 기준값
        TimeZone utcZone = TimeZone.getTimeZone("UTC");
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        formatDatetime.setTimeZone(utcZone);
        Timestamp timestamp = Timestamp.valueOf(date);

        // 현재 날짜 구하기 (시스템 시계, 시스템 타임존)
        return formatDatetime.format(timestamp);
    }

    /**
     * UTC 시간을 로컬시간으로 변경
     *
     * @param date UTC 시간 yyyy-MM-dd hh:mm:ss
     * @return 로컬 시간 yyyy-MM-dd hh:mm:ss
     */
    public String utcToLocalTime(String date) {
        // 입력시간을 Timestamp 변환
        long utcTime = Timestamp.valueOf(date).getTime();
        // 현재 로컬 타임 존을 가져옵니다.
        TimeZone localTimeZone = TimeZone.getDefault();
        // 로컬 타임 존의 썸머 타임 오프셋을 가져옵니다.
        int localOffset = localTimeZone.getOffset(utcTime);
        // UTC 시간에 로컬 오프셋을 더해 로컬 시간을 계산합니다.
        long localDateTime = utcTime + localOffset;
        // 현재 날짜 구하기 (시스템 시계, 시스템 타임존)
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatDatetime.format(new Timestamp(localDateTime));
    }

    /**
     * timestamp 형식 시간 구하기
     */
    public String getTimestamp() {
        Timestamp time = new Timestamp(System.currentTimeMillis());
        return String.valueOf(time.getTime() / 1000L);
    }

    /**
     * 입력받은 타임존 기준 시간 구하기
     *
     * @param timeZone
     * @return
     */
    public String getDatetimeByTimeZone(String timeZone) {
        java.util.Date dateNow = new java.util.Date(System.currentTimeMillis());

        // 입력 받은 타임존 기준
        TimeZone tz = TimeZone.getTimeZone(timeZone);
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        formatDatetime.setTimeZone(tz);

        // 현재 날짜 구하기 (시스템 시계, 시스템 타임존)
        return formatDatetime.format(dateNow);
    }

    /**
     * 등록일 초, 분, 시간 단위 구하기
     *
     * @param regDate
     * @param timeZone
     * @return
     */
    @SneakyThrows
    public String getConvertDateToTime(String regDate, String timeZone) {

        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");

        // 입력 받은 타임존 기준 현재 날짜 구하기
        String nowDate = getDatetimeByTimeZone(timeZone);

        // 현재 날짜 String -> Date 변환
        Date formatNowDate = formatDatetime.parse(nowDate);

        // 입력 받은 등록 날짜 -> Date 변환
        Date formatInputDate = formatDatetime.parse(regDate);

        // 비교 날짜와 현재 날짜 크기 비교
        int result = formatInputDate.compareTo(formatNowDate);

        // return value
        String calculatedTime = "";

        /** 등록일이 현재 시간 이전이거나 같은 경우만 계산 **/
        if (result <= 0) {
            // 시간 간격을 초 단위로 변환
            long differenceInMillis = formatNowDate.getTime() - formatInputDate.getTime();
            // 밀리세컨드 초 단위로 변경
            long second = TimeUnit.SECONDS.convert(differenceInMillis, TimeUnit.MILLISECONDS);

            // 현재 시간 대비 지난 시간이 얼마인지 계산
            if (second < ONE_MIN) {
                calculatedTime = langMessage("lang.common.time.just.before"); // 방금 전

            } else if (ONE_MIN <= second && second < ONE_HOUR) { // 1분 후 ~ 한 시간 경과 전
                calculatedTime = (second / ONE_MIN) + langMessage("lang.common.time.minute.ago"); // 몇 분 전

            } else if (ONE_HOUR <= second && second < ONE_DAY) { // 1시간 후 ~ 24시간 경과 전
                calculatedTime = second / ONE_HOUR + langMessage("lang.common.time.hour.ago");    // 몇 시간 전

            } else if (ONE_DAY <= second && second < ONE_MONTH) { // 하루 후 ~ 한달 경과 전
                calculatedTime = second / ONE_DAY + langMessage("lang.common.time.day.ago");       // 몇 일 전
            } else { // 한달 경과 -> 년도 및 날짜만 출력
                Date date = formatDate.parse(regDate);
                calculatedTime = formatDate.format(date);
            }

        }

        return calculatedTime;
    }

    /**
     * 몇 주 전 날짜 구하기 (UTC 기준)
     *
     * @return
     */
    public String getAgoWeek(int amount) {

        Calendar cal = Calendar.getInstance();
        String stringAgoWeek = null;

        cal.add(Calendar.DATE, -amount);
        Date agoWeek = cal.getTime();
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        // 타임존 UTC 기준
        TimeZone utcZone = TimeZone.getTimeZone("UTC");
        formatDatetime.setTimeZone(utcZone);

        // 현재 날짜 구하기 (시스템 시계, 시스템 타임존)
        stringAgoWeek = formatDatetime.format(agoWeek);

        return stringAgoWeek;
    }

    /**
     * 몇 시간 전 날짜 구하기 (UTC 기준)
     *
     * @return
     */
    public String getAgoHour(int amount) {

        Calendar cal = Calendar.getInstance();
        String stringAgoHourDate = null;

        cal.add(Calendar.HOUR, -amount);
        Date agoOneHour = cal.getTime();
        SimpleDateFormat formatDatetime = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        // 타임존 UTC 기준
        TimeZone utcZone = TimeZone.getTimeZone("UTC");
        formatDatetime.setTimeZone(utcZone);

        // 현재 날짜 구하기 (시스템 시계, 시스템 타임존)
        stringAgoHourDate = formatDatetime.format(agoOneHour);

        return stringAgoHourDate;
    }

    /**
     * 만 나이 계산
     *
     * @param birth 생년월일 yyyymmdd
     * @return
     */
    public int calculateAge(String birth) {

        // 연도, 월, 일 부분 추출
        int year = Integer.parseInt(birth.substring(0, 4));
        int month = Integer.parseInt(birth.substring(4, 6));
        int day = Integer.parseInt(birth.substring(6, 8));

        // LocalDate 객체 생성
        LocalDate dateOfBirth = LocalDate.of(year, month, day);

        // 현재 날짜 가져오기
        LocalDate currentDate = LocalDate.now();

        // 만 나이 계산
        Period period = Period.between(dateOfBirth, currentDate);

        return period.getYears();
    }

    public String getTimeStamp(String date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        Date parsedDate = null;
        try {
            parsedDate = dateFormat.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Timestamp timestamp = new java.sql.Timestamp(parsedDate != null ? parsedDate.getTime() : 0);
        return String.valueOf(timestamp);
    }

    /*****************************************************
     * Language 값 가져오기
     ****************************************************/
    public String langMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    public String langMessage(String code, @Nullable Object[] args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

}
