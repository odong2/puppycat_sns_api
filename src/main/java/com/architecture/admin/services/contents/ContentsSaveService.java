package com.architecture.admin.services.contents;

import com.architecture.admin.libraries.PaginationLibray;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.contents.ContentsSaveDao;
import com.architecture.admin.models.daosub.contents.ContentsSaveDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.contents.ContentsDto;
import com.architecture.admin.models.dto.contents.ContentsSaveDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentsSaveService extends BaseService {

    private final ContentsService contentsService;
    private final ContentsSaveDao contentsSaveDao;
    private final ContentsSaveDaoSub contentsSaveDaoSub;

    /*****************************************************
     *  Modules
     ****************************************************/
    /**
     * 콘텐츠 저장
     *
     * @param token           access token
     * @param contentsSaveDto contentsIdx
     */
    @Transactional
    public void saveContents(String token, ContentsSaveDto contentsSaveDto) {

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);
        contentsSaveDto.setMemberUuid(memberDto.getUuid());

        // 컨텐츠 저장 공통 유효성
        commonSaveValidate(contentsSaveDto);

        // 등록일 set
        contentsSaveDto.setRegDate(dateLibrary.getDatetime());

        // 저장된 내역 가져오기
        ContentsSaveDto oTargetInfo = getTargetInfo(contentsSaveDto);

        // 기존 내역이 존재하면
        if (oTargetInfo != null) {

            // 이미 저장된 경우
            if (oTargetInfo.getState() == 1) {
                throw new CustomException(CustomError.SAVE_STATE); // 이미 저장하였습니다.
            }

            // 내역 존재 하고 상태가 0이라면 <취소 후 다시 저장한 경우>
            if (oTargetInfo.getState() == 0) {
                // idx set [sns_contents_Save]
                contentsSaveDto.setIdx(oTargetInfo.getIdx());
                // 저장 상태 업데이트
                updateSaveState(contentsSaveDto);
                // cnt +1
                updateContentsSaveCntUp(contentsSaveDto);
            }
        } else {
            // 콘텐츠 저장
            insertContentsSave(contentsSaveDto);

            // cnt 테이블에 해당 idx 있는지 확인
            Boolean bResult = checkCntByIdx(contentsSaveDto);

            // cnt 테이블에 값 없으면
            if (Boolean.FALSE.equals(bResult)) {
                insertContentsSaveCnt(contentsSaveDto);
            }
            updateContentsSaveCntUp(contentsSaveDto);
        }

    }

    /**
     * 콘텐츠 저장 취소
     *
     * @param token           access token
     * @param contentsSaveDto contentsIdx
     */
    @Transactional
    public void contentsSaveCancel(String token, ContentsSaveDto contentsSaveDto) {

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);
        contentsSaveDto.setMemberUuid(memberDto.getUuid());

        // 컨텐츠 저장 공통 유효성
        commonSaveValidate(contentsSaveDto);

        // 저장 내역 가져오기
        ContentsSaveDto oTargetInfo = getTargetInfo(contentsSaveDto);

        // 저장 한 적 없는데 취소 요청한 경우
        if (oTargetInfo == null) {
            throw new CustomException(CustomError.SAVE_DATA_ERROR); // 잘못된 요청입니다.
        }

        // 저장 insert 된 적이 있고 상태값이 정상인 경우
        if (oTargetInfo.getState() == 1) {

            // 등록일 set
            contentsSaveDto.setRegDate(dateLibrary.getDatetime());

            // idx set [sns_contents_like]
            contentsSaveDto.setIdx(oTargetInfo.getIdx());

            // 저장 상태값 0으로 변경
            updateUnSaveState(contentsSaveDto);

            // 저장 cnt -1
            updateContentsSaveCntDown(contentsSaveDto);

        }
        // 이미 취소되어 있다면
        else {
            throw new CustomException(CustomError.SAVE_CANCEL_STATE); // 이미 저장 취소하였습니다.
        }

    }

    /**
     * 내가 저장한 컨텐츠 리스트
     *
     * @param token
     * @param searchDto
     * @return
     */
    public List<ContentsDto> getMySaveContentsList(String token, SearchDto searchDto) {
        // 로그인 회원 uuid curl 조회
        MemberDto loginUserInfo = super.getMemberUuidByToken(token);

        // 로그인 회원 uuid set
        searchDto.setLoginMemberUuid(loginUserInfo.getUuid());

        // 목록 전체 count
        int iTotalCount = contentsSaveDaoSub.iGetTotalMySaveContentsCount(searchDto);

        // 리스트가 비었을때
        if (iTotalCount < 1) {
            return new ArrayList<>(); // 리스트가 비었습니다
        }

        // paging
        PaginationLibray pagination = new PaginationLibray(iTotalCount, searchDto);
        searchDto.setPagination(pagination);

        // list
        List<ContentsDto> contentsList = contentsSaveDaoSub.lGetMySaveContentsList(searchDto);
        // 첫 번째 이미지 setting
        contentsService.setFirstImgUrl(contentsList);

        return contentsList;
    }

    /*****************************************************
     *  SubFunction - Select
     ****************************************************/
    /**
     * 저장 내역 가져오기
     *
     * @param contentsSaveDto contentsIdx, memberUuid
     * @return contentsDto
     */
    public ContentsSaveDto getTargetInfo(ContentsSaveDto contentsSaveDto) {
        return contentsSaveDaoSub.oGetTargetInfo(contentsSaveDto);
    }

    /**
     * cnt 테이블에 해당 idx 있는지 확인
     *
     * @param contentsSaveDto contentsIdx
     * @return Boolean
     */
    public Boolean checkCntByIdx(ContentsSaveDto contentsSaveDto) {
        return contentsSaveDaoSub.lCheckCntByIdx(contentsSaveDto) != null;
    }

    /*****************************************************
     *  SubFunction - Insert
     ****************************************************/
    /**
     * 콘텐츠 저장 등록
     *
     * @param contentsSaveDto contentsIdx, memberUuid, regDate
     */
    public void insertContentsSave(ContentsSaveDto contentsSaveDto) {
        Integer iResult = contentsSaveDao.insertContentsSave(contentsSaveDto);
        if (iResult < 1) {
            throw new CustomException(CustomError.SAVE_ERROR); // 저장 실패하였습니다.
        }
    }

    /**
     * 콘텐츠 저장 cnt 등록
     *
     * @param contentsSaveDto contentsIdx
     */
    public void insertContentsSaveCnt(ContentsSaveDto contentsSaveDto) {
        // 콘텐츠 idx set
        contentsSaveDao.insertContentsSaveCnt(contentsSaveDto);
    }

    /*****************************************************
     *  SubFunction - Update
     ****************************************************/
    /**
     * 저장 상태값 변경 (state : 1)
     *
     * @param contentsSaveDto memberUuid, contentsIdx
     */
    public void updateSaveState(ContentsSaveDto contentsSaveDto) {
        contentsSaveDto.setState(1);
        Integer iResult = contentsSaveDao.updateContentsSave(contentsSaveDto);
        if (iResult != 1) {
            throw new CustomException(CustomError.SAVE_ERROR);  // 저장 실패하였습니다.
        }
    }

    /**
     * 저장 취소 상태값 변경 (state : 0)
     *
     * @param contentsSaveDto memberUuid, contentsIdx
     */
    public void updateUnSaveState(ContentsSaveDto contentsSaveDto) {
        contentsSaveDto.setState(0);
        Integer iResult = contentsSaveDao.updateContentsSave(contentsSaveDto);
        if (iResult != 1) {
            throw new CustomException(CustomError.SAVE_CANCEL_ERROR); // 저장 취소 실패하였습니다.
        }
    }

    /**
     * 저장 cnt +1
     *
     * @param contentsSaveDto memberUuid, contentsIdx
     */
    public void updateContentsSaveCntUp(ContentsSaveDto contentsSaveDto) {
        contentsSaveDao.updateContentsSaveCntUp(contentsSaveDto);
    }

    /**
     * 저장 cnt -1
     *
     * @param contentsSaveDto memberUuid, contentsIdx
     */
    public void updateContentsSaveCntDown(ContentsSaveDto contentsSaveDto) {
        contentsSaveDao.updateContentsSaveCntDown(contentsSaveDto);
    }

    /*****************************************************
     *  SubFunction - Delete
     ****************************************************/


    /*****************************************************
     *  Validation
     ****************************************************/

    /**
     * 컨텐츠 저장 공통 유효성
     *
     * @param contentsSaveDto : memberUuid, contentsIdx
     */
    private void commonSaveValidate(ContentsSaveDto contentsSaveDto) {

        String loginMemberUuid = contentsSaveDto.getMemberUuid(); // 로그인 회원 idx
        Long contentsIdx = contentsSaveDto.getContentsIdx();  // 컨텐츠 idx

        // 콘텐츠 idx 검증
        if (contentsSaveDto.getContentsIdx() == null || contentsSaveDto.getContentsIdx() < 1) {
            throw new CustomException(CustomError.CONTENTS_IDX_NULL); // 컨텐츠 IDX가 비었습니다.
        }

        // 컨텐츠 검증용 dto
        SearchDto searchContents = new SearchDto();
        searchContents.setLoginMemberUuid(loginMemberUuid);
        searchContents.setContentsIdx(contentsIdx);

        /** 컨텐츠 공통 검증 [로그인, 비 로그인 나누어 검증] **/
        contentsService.commonContentsValidate(searchContents);

    }
}
