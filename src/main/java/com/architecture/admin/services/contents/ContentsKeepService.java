package com.architecture.admin.services.contents;

import com.architecture.admin.libraries.PaginationLibray;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.contents.ContentsKeepDao;
import com.architecture.admin.models.daosub.contents.ContentsKeepDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.contents.ContentsDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentsKeepService extends BaseService {

    private final ContentsKeepDao contentsKeepDao;
    private final ContentsKeepDaoSub contentsKeepDaoSub;
    private final ContentsService contentsService;

    /*****************************************************
     *  SubFunction - Select
     ****************************************************/
    /**
     * 보관한 콘텐츠 가져오기
     *
     * @param searchDto
     * @return contentsDto
     */
    @Transactional(readOnly = true)
    public List<ContentsDto> getMyKeepContentsList(String token, SearchDto searchDto) {

        MemberDto loginUserInfo = super.getMemberUuidByToken(token);

        // 로그인 회원 uuid set
        searchDto.setLoginMemberUuid(loginUserInfo.getUuid());

        List<ContentsDto> contentsList = new ArrayList<>();

        // 목록 전체 count
        int iTotalCount = contentsKeepDaoSub.iGetTotalMyKeepContentsCount(searchDto);

        if (iTotalCount > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(iTotalCount, searchDto);
            searchDto.setPagination(pagination);

            // list
            contentsList = contentsKeepDaoSub.lGetMyKeepContentsList(searchDto);
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
    /**
     * 콘텐츠 보관
     *
     * @param token       access token
     * @param contentsDto idxList
     */
    @Transactional
    public void keepContents(String token, ContentsDto contentsDto) {

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);
        contentsDto.setMemberUuid(memberDto.getUuid());

        List<Long> idxList = contentsDto.getIdxList();

        // 콘텐츠 idx 리스트 비어 있는지 검증
        if (idxList.isEmpty()) {
            throw new CustomException(CustomError.KEEP_IDX_EMPTY_ERROR); // 콘텐츠를 선택해주세요.
        }

        for (Long idx : idxList) { // idx 돌면서 state 변경

            // 콘텐츠 idx 검증
            if (idx == null || idx < 1) {
                throw new CustomException(CustomError.CONTENTS_IDX_NULL); // 존재하지 않는 콘텐츠입니다.
            }

            // 콘텐츠 idx set
            contentsDto.setIdx(idx);

            // 내가 작성한 콘텐츠 인지 검증
            Boolean bResult = contentsService.checkMyContents(contentsDto);

            if (Boolean.TRUE.equals(bResult)) {

                // 콘텐츠 보관
                contentsDto.setIsKeep(1);
                Integer iResult = contentsKeepDao.updateIsStore(contentsDto);

                if (iResult < 1) {
                    throw new CustomException(CustomError.KEEP_ERROR); // 보관 실패하였습니다.
                }

            } else {
                throw new CustomException(CustomError.KEEP_NOT_MY_CONTENTS_ERROR); // 내가 작성한 콘텐츠가 아닙니다.
            }

        }

    }

    /**
     * 콘텐츠 보관 해제
     *
     * @param token       access token
     * @param contentsDto idxList
     */
    @Transactional
    public void unKeepContents(String token, ContentsDto contentsDto) {

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);
        contentsDto.setMemberUuid(memberDto.getUuid());

        List<Long> idxList = contentsDto.getIdxList();

        // 콘텐츠 idx 리스트 비어 있는지 검증
        if (idxList.isEmpty()) {
            throw new CustomException(CustomError.KEEP_IDX_EMPTY_ERROR); // 삭제할 콘텐츠를 선택해주세요.
        }

        for (Long idx : idxList) { // idx 돌면서 state 변경

            // 콘텐츠 idx 검증
            if (idx == null || idx < 1) {
                throw new CustomException(CustomError.CONTENTS_IDX_NULL); // 존재하지 않는 콘텐츠입니다.
            }

            contentsDto.setIdx(idx);

            // 내가 작성한 콘텐츠 인지 검증
            Boolean bResult = contentsService.checkMyContents(contentsDto);

            if (Boolean.TRUE.equals(bResult)) {

                // 콘텐츠 보관 해제
                contentsDto.setIsKeep(0);
                Integer iResult = contentsKeepDao.updateIsStore(contentsDto);

                if (iResult < 1) {
                    throw new CustomException(CustomError.KEEP_CANCEL_ERROR); // 보관 취소 실패하였습니다.
                }

            } else {
                throw new CustomException(CustomError.KEEP_NOT_MY_CONTENTS_ERROR); // 내가 작성한 콘텐츠가 아닙니다.
            }

        }

    }

}
