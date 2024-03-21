package com.architecture.admin.models.dao.report;

import com.architecture.admin.models.dto.report.CommentReportDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface CommentReportDao {

    /*****************************************************
     * Insert
     ****************************************************/
    /**
     * 댓글 신고 등록
     *
     * @param commentReportDto memberIdx, commentIdx, code, regDate
     * @return 쿼리 실행결과 [성공:1, 실패:0]
     */
    Integer insertCommentReport(CommentReportDto commentReportDto);

    /**
     * 댓글 신고 상세사유 등록
     *
     * @param commentReportDto insertedIdx, reason
     */
    void insertCommentReportReason(CommentReportDto commentReportDto);

    /*****************************************************
     * Update
     ****************************************************/
    /**
     * 댓글 신고상태 업데이트
     *
     * @param commentReportDto idx, reportCode, state, regDate
     * @return 쿼리 실행결과
     */
    Integer updateCommentReport(CommentReportDto commentReportDto);

    /**
     * 댓글 신고 상세사유 변경
     *
     * @param commentReportDto idx, reason, regDate
     * @return 쿼리 실행결과
     */
    Integer updateCommentReportReason(CommentReportDto commentReportDto);

}
