package com.architecture.admin.services.report;

import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.report.CommentReportDao;
import com.architecture.admin.models.daosub.comment.CommentDaoSub;
import com.architecture.admin.models.daosub.report.CommentReportDaoSub;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.report.CommentReportDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CommentReportService extends BaseService {

    private final CommentReportDao commentReportDao;
    private final CommentDaoSub commentDaoSub;
    private final CommentReportDaoSub commentReportDaoSub;
    @Value("${report.code.max}")
    private int codeMax;  // 신고사유 코드 MAX

    /*****************************************************
     *  Modules
     ****************************************************/
    /**
     * 댓글 신고
     *
     * @param token            access token
     * @param commentReportDto commentIdx, reportCode, reason
     */
    @Transactional
    public void reportComment(String token, CommentReportDto commentReportDto) {

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);
        commentReportDto.setMemberUuid(memberDto.getUuid());

        // validate
        validateIdx(commentReportDto);
        validateInfo(commentReportDto);

        // 등록일 set
        commentReportDto.setRegDate(dateLibrary.getDatetime());

        // 신고 내역 가져오기
        CommentReportDto oTargetInfo = oGetTargetInfo(commentReportDto);

        // 신고 데이터 가져오기
        String contents = commentReportDaoSub.oGetContentsData(commentReportDto);
        commentReportDto.setContents(contents);

        if (oTargetInfo != null) {

            if (oTargetInfo.getState() == 1) { // 이미 신고되어 있는 경우
                // 이미 신고된 글입니다.
                throw new CustomException(CustomError.REPORT_STATE_ERROR);
            }
            if (oTargetInfo.getState() == 0) { // 취소후 다시 신고한 경우
                // 신고 idx set [sns_contents_comment_report]
                commentReportDto.setIdx(oTargetInfo.getIdx());

                // 신고 상태, 사유 업데이트
                updateCommentReport(commentReportDto);

                // 직접입력 상세사유 업데이트
                if (commentReportDto.getReportCode() == codeMax) {

                    if (oTargetInfo.getReportCode() == codeMax) {
                        // 기존에 reason 테이블에 값이 있으면 update
                        updateCommentReportReason(commentReportDto);
                    }
                    else {
                        // 신규 insert
                        insertCommentReportReason(commentReportDto);
                    }

                }

            }

        } else {
            // 댓글 신고 등록
            insertCommentReport(commentReportDto);

            // 직접입력 상세사유 등록
            if (commentReportDto.getReportCode() == codeMax) {
                // insertedIdx set
                commentReportDto.setIdx(commentReportDto.getInsertedIdx());
                insertCommentReportReason(commentReportDto);
            }

        }

    }

    /**
     * 댓글 신고취소
     *
     * @param commentReportDto memberUuid, commentIdx
     */
    @Transactional
    public void cancelReportComment(String token, CommentReportDto commentReportDto) {

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);
        commentReportDto.setMemberUuid(memberDto.getUuid());

        // validate
        validateIdx(commentReportDto);

        // 등록일 set
        commentReportDto.setRegDate(dateLibrary.getDatetime());

        // 신고 내역 가져오기
        CommentReportDto oTargetInfo = oGetTargetInfo(commentReportDto);

        // 신고한 적 없는데 신고해제 요청
        if (oTargetInfo == null) {
            // 잘못된 요청입니다.
            throw new CustomException(CustomError.REPORT_DATA_ERROR);
        }

        // 이미 신고 취소되어 있다면
        if (oTargetInfo.getState() == 0) {
            // 이미 신고취소된 글입니다.
            throw new CustomException(CustomError.REPORT_CANCEL_STATE_ERROR);
        }

        // 신고 idx set [sns_contents_comment_report]
        commentReportDto.setIdx(oTargetInfo.getIdx());

        // 신고 상태 업데이트
        updateCommentReportCancel(commentReportDto);
    }

    /*****************************************************
     *  Select
     ****************************************************/
    /**
     * 댓글 신고내역 가져오기
     *
     * @param commentReportDto memberUuid, commentIdx
     * @return ContentsReportDto
     */
    public CommentReportDto oGetTargetInfo(CommentReportDto commentReportDto) {
        return commentReportDaoSub.oGetTargetInfo(commentReportDto);
    }

    /**
     * 신고사유 list 가져오기
     *
     * @return 신고사유 list
     */
    public List<CommentReportDto> lGetReportCode(){
        return commentReportDaoSub.lGetListReportCode();
    }

    /*****************************************************
     *  Insert
     ****************************************************/
    /**
     * 댓글 신고 등록
     *
     * @param commentReportDto memberUuid, commentIdx, reportCode, regDate
     */
    public Integer insertCommentReport(CommentReportDto commentReportDto) {
        Integer iResult = commentReportDao.insertCommentReport(commentReportDto);
        if (iResult != 1) {
            throw new CustomException(CustomError.REPORT_ERROR); // 신고 실패하였습니다.
        }

        return iResult;
    }

    /**
     * 댓글 신고 상세사유 등록
     *
     * @param commentReportDto memberUuid, commentIdx, reason
     */
    public void insertCommentReportReason(CommentReportDto commentReportDto) {
        commentReportDao.insertCommentReportReason(commentReportDto);
    }

    /*****************************************************
     *  Update
     ****************************************************/
    /**
     * 댓글 신고 (state : 1)
     *
     * @param commentReportDto
     */
    public void updateCommentReport(CommentReportDto commentReportDto) {
        commentReportDto.setState(1);
        Integer iResult = commentReportDao.updateCommentReport(commentReportDto);
        if (iResult != 1) {
            throw new CustomException(CustomError.REPORT_ERROR); // 신고 실패하였습니다.
        }
    }

    /**
     * 댓글 신고취소 (state : 0)
     *
     * @param commentReportDto
     */
    public void updateCommentReportCancel(CommentReportDto commentReportDto) {
        commentReportDto.setState(0);
        Integer iResult = commentReportDao.updateCommentReport(commentReportDto);
        if (iResult != 1) {
            throw new CustomException(CustomError.REPORT_CANCEL_ERROR); // 신고취소 실패하였습니다.
        }
    }

    /**
     * 댓글 신고 상세사유 변경
     *
     * @param commentReportDto idx, reason
     */
    public void updateCommentReportReason(CommentReportDto commentReportDto) {
        Integer iResult = commentReportDao.updateCommentReportReason(commentReportDto);
        if (iResult < 1) {
            throw new CustomException(CustomError.REPORT_ERROR); // 신고 실패하였습니다.
        }
    }

    /*****************************************************
     *  Validate
     ****************************************************/
    public void validateIdx(CommentReportDto commentReportDto) {
        if (ObjectUtils.isEmpty(commentReportDto.getMemberUuid())) {
            throw new CustomException(CustomError.MEMBER_UUID_EMPTY); // 회원 UUID가 비었습니다.
        }
        if (commentReportDto.getCommentIdx() == null || commentReportDto.getCommentIdx() < 1) {
            throw new CustomException(CustomError.COMMENT_IDX_NULL); // 존재하지 않는 댓글입니다.
        }
        int commentCnt = commentDaoSub.getCommentCntByIdx(commentReportDto.getCommentIdx());
        if (commentCnt == 0) {
            throw new CustomException(CustomError.COMMENT_IDX_ERROR); // 존재하지 않는 댓글입니다.
        }
    }

    public void validateInfo(CommentReportDto commentReportDto) {
        if (commentReportDto.getReportCode() == null || commentReportDto.getReportCode() == 0) {
            throw new CustomException(CustomError.REPORT_CODE_EMPTY); // 신고사유를 선택해주세요.
        }
        if (commentReportDto.getReportCode() < 1 || commentReportDto.getReportCode() > codeMax) {
            throw new CustomException(CustomError.REPORT_CODE_INVALID); // 유효하지 않은 신고사유입니다.
        }
        if (commentReportDto.getReportCode() == codeMax && (commentReportDto.getReason() == null || commentReportDto.getReason().trim().isEmpty())) {
                throw new CustomException(CustomError.REPORT_REASON_EMPTY); // 신고 상세사유를 입력해주세요.

        }
    }

}
