package com.architecture.admin.models.daosub.report;

import com.architecture.admin.models.dto.report.CommentReportDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface CommentReportDaoSub {

    /*****************************************************
     * Select
     ****************************************************/
    /**
     * 신고사유 리스트
     *
     * @return code 리스트 [sns_report_code]
     */
    List<CommentReportDto> lGetListReportCode();

    /**
     * 댓글 신고 내역 가져오기
     *
     * @param commentReportDto memberUuid, commentIdx
     * @return ContentsReportDto
     */
    CommentReportDto oGetTargetInfo(CommentReportDto commentReportDto);

    /**
     * 콘텐츠 신고 당시 내용 가져오기
     *
     * @return commentReportDto
     */
    String oGetContentsData(CommentReportDto commentReportDto);

}
