package com.architecture.admin.services.contents;

import com.architecture.admin.libraries.PaginationLibray;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.daosub.contents.ContentsTagDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.contents.ContentsDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ContentsTagService extends BaseService {

    private final ContentsTagDaoSub contentsTagDaoSub;
    private final ContentsService contentsService;
    @Value("${cloud.aws.s3.img.url}")
    private String imgDomain;

    /*****************************************************
     *  SubFunction - Select
     ****************************************************/
    /**
     * 내가 태그 된 내역 가져오기
     *
     * @param token
     * @param searchDto
     * @return
     */
    public List<ContentsDto> getMyTagContentsList(String token, SearchDto searchDto) {
        List<ContentsDto> contentsList = new ArrayList<>();

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);
        searchDto.setLoginMemberUuid(memberDto.getUuid());

        // 목록 전체 count
        int iTotalCount = contentsTagDaoSub.iGetTotalMyTagContentsCount(searchDto);

        // 리스트가 비었을때
        if (iTotalCount > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(iTotalCount, searchDto);
            searchDto.setPagination(pagination);

            // 이미지 도메인 set
            searchDto.setImgDomain(imgDomain);

            // list
            contentsList = contentsTagDaoSub.lGetMyTagContentsList(searchDto);
        }

        return contentsList;
    }

    /**
     * 해당 유저가 태그 된 내역 가져오기
     *
     * @param searchDto
     * @return contentsDto
     */
    public List<ContentsDto> getMemberTagContentsList(String token, SearchDto searchDto) {

        // 로그인 중
        if (!ObjectUtils.isEmpty(token)) {
            MemberDto loginUserInfo = super.getMemberUuidByToken(token);
            // 회원 로그인 uuid set
            searchDto.setLoginMemberUuid(loginUserInfo.getUuid());
        }

        // 회원 존재 유무 curl 통신
        Boolean isExist = super.getCheckMemberByUuid(searchDto.getMemberUuid());

        if (isExist == false) {
            // 회원 UUID가 유효하지 않습니다.
            throw new CustomException(CustomError.MEMBER_UUID_ERROR);
        }

        List<ContentsDto> contentsList = new ArrayList<>();

        // 목록 전체 count
        int iTotalCount = contentsTagDaoSub.iGetTotalMemberTagContentsCount(searchDto);

        // 리스트가 비었을때
        if (iTotalCount > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(iTotalCount, searchDto);
            searchDto.setPagination(pagination);

            // list
            contentsList = contentsTagDaoSub.lGetMemberTagContentsList(searchDto);

            // 첫 번째 이미지 url setting
            contentsService.setFirstImgUrl(contentsList);
        }

        return contentsList;
    }


    /*****************************************************
     *  SubFunction - Insert
     ****************************************************/


    /*****************************************************
     *  SubFunction - Update
     ****************************************************/


}
