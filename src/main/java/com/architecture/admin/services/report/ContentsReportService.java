package com.architecture.admin.services.report;

import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.report.ContentsReportDao;
import com.architecture.admin.models.daosub.contents.ContentsDaoSub;
import com.architecture.admin.models.daosub.report.ContentsReportDaoSub;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.report.ContentsReportDto;
import com.architecture.admin.models.dto.report.ContentsReportLogDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ContentsReportService extends BaseService {

    private final ContentsDaoSub contentsDaoSub;
    private final ContentsReportDao contentsReportDao;
    private final ContentsReportDaoSub contentsReportDaoSub;
    @Value("${report.code.max}")
    private int codeMax;  // 신고사유 코드 MAX

    /*****************************************************
     *  Modules
     ****************************************************/
    /**
     * 콘텐츠 신고
     *
     * @param token             access token
     * @param contentsReportDto memberUuid, contentsIdx, code, reason
     */
    @Transactional
    public void reportContents(String token, ContentsReportDto contentsReportDto) {

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);
        contentsReportDto.setMemberUuid(memberDto.getUuid());

        // validate
        validateIdx(contentsReportDto);
        validateInfo(contentsReportDto);

        // 등록일 set
        contentsReportDto.setRegDate(dateLibrary.getDatetime());

        // 신고 내역 가져오기
        ContentsReportDto oTargetInfo = oGetTargetInfo(contentsReportDto);

        // 신고 데이터 가져오기
        String contents = contentsReportDaoSub.oGetContentsData(contentsReportDto);
        contentsReportDto.setContents(contents);

        if (oTargetInfo != null) {

            if (oTargetInfo.getState() == 1) { // 이미 신고되어 있는 경우
                // 이미 신고된 글입니다.
                throw new CustomException(CustomError.REPORT_STATE_ERROR);
            }
            if (oTargetInfo.getState() == 0) { // 취소후 다시 신고한 경우

                // 신고 idx set [sns_contents_report]
                contentsReportDto.setIdx(oTargetInfo.getIdx());

                // 신고 상태, 사유 업데이트
                updateContentsReport(contentsReportDto);

                // 직접입력 상세사유 업데이트
                if (contentsReportDto.getReportCode() == codeMax) {

                    if (oTargetInfo.getReportCode() == codeMax) {
                        // 기존에 reason 테이블에 값이 있으면 update
                        updateContentsReportReason(contentsReportDto);
                    }
                    else {
                        // 신규 insert
                        insertContentsReportReason(contentsReportDto);
                    }

                }

            }

        } else {
            // 콘텐츠 신고 등록
            insertContentsReport(contentsReportDto);

            // 직접입력 상세사유 등록
            if (contentsReportDto.getReportCode() == codeMax) {
                // insertedIdx set
                contentsReportDto.setIdx(contentsReportDto.getInsertedIdx());
                insertContentsReportReason(contentsReportDto);
            }

        }

    }

    /**
     * 콘텐츠 신고취소
     *
     * @param contentsReportDto memberUuid, contentsIdx
     */
    @Transactional
    public void cancelReportContents(String token, ContentsReportDto contentsReportDto) {

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);
        contentsReportDto.setMemberUuid(memberDto.getUuid());

        // validate
        validateIdx(contentsReportDto);

        // 등록일 set
        contentsReportDto.setRegDate(dateLibrary.getDatetime());

        // 신고 내역 가져오기
        ContentsReportDto oTargetInfo = oGetTargetInfo(contentsReportDto);

        // 신고한 적 없는데 신고해제 요청
        if (oTargetInfo == null) {
            // 신고된 기록이 없습니다.
            throw new CustomException(CustomError.REPORT_DATA_ERROR);
        }

        // 이미 신고 취소되어 있다면
        if (oTargetInfo.getState() == 0) {
            // 이미 신고취소된 글입니다.
            throw new CustomException(CustomError.REPORT_CANCEL_STATE_ERROR);
        }

        // 신고 idx set [sns_contents_report]
        contentsReportDto.setIdx(oTargetInfo.getIdx());

        // 신고 상태 업데이트
        updateContentsReportCancel(contentsReportDto);
    }

    /*****************************************************
     *  Select
     ****************************************************/
    /**
     * 콘텐츠 신고내역 가져오기
     *
     * @param contentsReportDto memberUuid, contentsIdx
     * @return ContentsReportDto
     */
    public ContentsReportDto oGetTargetInfo(ContentsReportDto contentsReportDto) {
        return contentsReportDaoSub.oGetTargetInfo(contentsReportDto);
    }

    /**
     * 신고사유 list 가져오기
     *
     * @return 신고사유 list
     */
    public List<ContentsReportDto> lGetReportCode(){
        return contentsReportDaoSub.lGetListReportCode();
    }

    /*****************************************************
     *  Insert
     ****************************************************/
    /**
     * 콘텐츠 신고 등록
     *
     * @param contentsReportDto memberUuid, contentsIdx, code, reason, regDate
     */
    public void insertContentsReport(ContentsReportDto contentsReportDto) {
        Integer iResult = contentsReportDao.insertContentsReport(contentsReportDto);
        if (iResult != 1) {
            throw new CustomException(CustomError.REPORT_ERROR); // 신고 실패하였습니다.
        }
    }

    /**
     * 콘텐츠 신고 상세사유 등록
     *
     * @param contentsReportDto insertedIdx, reason
     */
    public void insertContentsReportReason(ContentsReportDto contentsReportDto) {
        contentsReportDao.insertContentsReportReason(contentsReportDto);
    }

    /*****************************************************
     *  Update
     ****************************************************/
    /**
     * 콘텐츠 신고 (state : 1)
     *
     * @param contentsReportDto idx, memberUuid, contentsIdx, code, reason
     */
    public void updateContentsReport(ContentsReportDto contentsReportDto) {
        contentsReportDto.setState(1);
        Integer iResult = contentsReportDao.updateContentsReport(contentsReportDto);
        if (iResult < 1) {
            throw new CustomException(CustomError.REPORT_ERROR); // 신고 실패하였습니다.
        }
    }

    /**
     * 콘텐츠 신고취소 (state : 0)
     *
     * @param contentsReportDto memberUuid, contentsIdx
     */
    public void updateContentsReportCancel(ContentsReportDto contentsReportDto) {
        contentsReportDto.setState(0);
        Integer iResult = contentsReportDao.updateContentsReport(contentsReportDto);
        if (iResult < 1) {
            throw new CustomException(CustomError.REPORT_CANCEL_ERROR); // 신고취소 실패하였습니다.
        }
    }


    /**
     * 콘텐츠 신고 상세사유 변경
     *
     * @param contentsReportDto idx, reason
     */
    public void updateContentsReportReason(ContentsReportDto contentsReportDto) {
        Integer iResult = contentsReportDao.updateContentsReportReason(contentsReportDto);
        if (iResult < 1) {
            throw new CustomException(CustomError.REPORT_ERROR); // 신고 실패하였습니다.
        }
    }

    /*****************************************************
     *  Validate
     ****************************************************/
    public void validateIdx(ContentsReportDto contentsReportDto) {

        if (ObjectUtils.isEmpty(contentsReportDto.getMemberUuid())) {
            throw new CustomException(CustomError.MEMBER_UUID_EMPTY); // 회원 UUID가 비었습니다.
        }
        if (contentsReportDto.getContentsIdx() == null || contentsReportDto.getContentsIdx() < 1) {
            throw new CustomException(CustomError.CONTENTS_IDX_NULL); // 존재하지 않는 콘텐츠입니다.
        }
        int contentsCnt = contentsDaoSub.getContentsCntByIdx(contentsReportDto.getContentsIdx());
        if (contentsCnt == 0) {
            throw new CustomException(CustomError.CONTENTS_IDX_ERROR); // 존재하지 않는 콘텐츠입니다.
        }

    }

    public void validateInfo(ContentsReportDto contentsReportDto) {
        if (contentsReportDto.getReportCode() == null || contentsReportDto.getReportCode() == 0) {
            throw new CustomException(CustomError.REPORT_CODE_EMPTY); // 신고사유를 선택해주세요.
        }
        if (contentsReportDto.getReportCode() < 1 || contentsReportDto.getReportCode() > codeMax) {
            throw new CustomException(CustomError.REPORT_CODE_INVALID); // 유효하지 않은 신고사유입니다.
        }
        if (contentsReportDto.getReportCode() == codeMax && (contentsReportDto.getReason() == null || contentsReportDto.getReason().trim().isEmpty())) {
                throw new CustomException(CustomError.REPORT_REASON_EMPTY); // 신고 상세사유를 입력해주세요.

        }
    }

}
