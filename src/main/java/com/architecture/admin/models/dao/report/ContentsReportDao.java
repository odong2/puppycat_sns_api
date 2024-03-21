package com.architecture.admin.models.dao.report;

import com.architecture.admin.models.dto.report.ContentsReportDto;
import com.architecture.admin.models.dto.report.ContentsReportLogDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface ContentsReportDao {

    /*****************************************************
     * Insert
     ****************************************************/
    /**
     * 컨텐츠 신고 등록
     *
     * @param contentsReportDto memberUuid, contentsIdx, code, regDate
     * @return 쿼리 실행결과
     */
    Integer insertContentsReport(ContentsReportDto contentsReportDto);

    /**
     * 컨텐츠 신고 상세사유 등록
     *
     * @param contentsReportDto insertedIdx, reason
     */
    void insertContentsReportReason(ContentsReportDto contentsReportDto);

    /*****************************************************
     * Update
     ****************************************************/
    /**
     * 컨텐츠 신고 상태값 변경
     *
     * @param contentsReportDto idx, reportCode, state, regDate
     * @return 쿼리 실행결과
     */
    Integer updateContentsReport(ContentsReportDto contentsReportDto);

    /**
     * 컨텐츠 신고 상세사유 변경
     *
     * @param contentsReportDto idx, reason, regDate
     * @return 쿼리 실행결과
     */
    Integer updateContentsReportReason(ContentsReportDto contentsReportDto);

}
