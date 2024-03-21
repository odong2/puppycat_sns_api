package com.architecture.admin.models.daosub.report;

import com.architecture.admin.models.dto.report.ContentsReportDto;
import com.architecture.admin.models.dto.report.ContentsReportLogDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface ContentsReportDaoSub {

    /*****************************************************
     * Select
     ****************************************************/
    /**
     * 신고사유 리스트
     *
     * @return code 리스트 [sns_report_code]
     */
    List<ContentsReportDto> lGetListReportCode();

    /**
     * 콘텐츠 신고 내역 가져오기
     *
     * @param contentsReportDto memberUuid, contentsIdx
     * @return ContentsReportDto
     */
    ContentsReportDto oGetTargetInfo(ContentsReportDto contentsReportDto);

    /**
     * 콘텐츠 신고 당시 내용 가져오기
     *
     * @return contentsReportDto
     */
    String oGetContentsData(ContentsReportDto contentsReportDto);

}
