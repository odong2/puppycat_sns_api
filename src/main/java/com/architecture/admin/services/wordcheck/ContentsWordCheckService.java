package com.architecture.admin.services.wordcheck;

import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.daosub.wordcheck.ContentsWordCheckDaoSub;
import com.architecture.admin.models.dto.wordcheck.ContentsWordCheckDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.List;


/*****************************************************
 * 회원 모델러
 ****************************************************/
@Service
@RequiredArgsConstructor
@Transactional
public class ContentsWordCheckService extends BaseService {

    private final ContentsWordCheckDaoSub contentsWordcheckDaoSub;

    /*****************************************************
     *  Modules
     ****************************************************/
    /**
     * 컨텐츠 금칙어 체크
     *
     * @param contents 검사 할 내용
     */
    public String contentsWordCheck(String contents, int type) {

        // validate
        if (ObjectUtils.isEmpty(contents)) {
            throw new CustomException(CustomError.WORD_EMPTY);
        }
        if (ObjectUtils.isEmpty(type)) {
            throw new CustomException(CustomError.WORD_TYPE_EMPTY);
        }

        // 금칙어 리스트
        List<ContentsWordCheckDto> list = getList(type);
        for (ContentsWordCheckDto str : list) {
            String noWord = str.getWord();
            String changeWord = str.getChangeWord();
            // 금칙어가 포함되어있으면 치환
            contents = contents.replace(noWord, changeWord);
        }
        return contents;
    }

    /**
     * 컨텐츠 금칙어 체크
     * [금칙어 리스트 외부에서 조회하여 파라미터로 넘김] -> 금칙어 리스트 한번만 조회하기 위해
     *
     * @param contents     검사 내용
     * @param badWordList  금칙어 리스트
     * @return
     */
    public String contentsWordCheck(String contents, List<ContentsWordCheckDto> badWordList) {
        if (!ObjectUtils.isEmpty(contents) && !ObjectUtils.isEmpty(badWordList)) {
            for (ContentsWordCheckDto str : badWordList) {
                String noWord = str.getWord();
                String changeWord = str.getChangeWord();
                // 금칙어가 포함되어있으면 치환
                contents = contents.replace(noWord, changeWord);
            }
        }
        return contents;
    }

    /*****************************************************
     *  SubFunction - select
     ****************************************************/
    /**
     * 금칙어 목록
     *
     * @return 금칙어 리스트
     */
    @Transactional(readOnly = true)
    public List<ContentsWordCheckDto> getList(int type) {

        return contentsWordcheckDaoSub.getList(type);
    }

    /*****************************************************
     *  SubFunction - insert
     ****************************************************/

    /*****************************************************
     *  SubFunction - Update
     ****************************************************/
    /*****************************************************
     *  SubFunction - Delete
     ****************************************************/
}
