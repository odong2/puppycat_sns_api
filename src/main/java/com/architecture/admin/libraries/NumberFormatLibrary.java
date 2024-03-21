package com.architecture.admin.libraries;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;

/*****************************************************
 * 숫자 표현 라이브러리
 ****************************************************/
@Component
@Data
public class NumberFormatLibrary {

    /**
     * 숫자를 string으로 변환
     * ex)11000 -> 1.1 만 으로 변환
     *
     * @param number
     * @return
     */
    public String krFormatNumber(Long number){
        String result = null;
        NumberFormat numberFormat = NumberFormat.getInstance();

        if( number < 10 ){
            result = String.valueOf(number);
        }else if( number < 1000){
            result = (number / 10 * 10) + "+";
        }else if( number < 10000){
            result = numberFormat.format((number / 10 * 10) )+ "+";
        }else{
            result = String.format("%.1f만", number / 10000.0)+ "+";
        }
        return result;
    }
}
