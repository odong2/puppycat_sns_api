package com.architecture.admin.services.contents;

import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.contents.ContentsHideDao;
import com.architecture.admin.models.daosub.contents.ContentsDaoSub;
import com.architecture.admin.models.daosub.contents.ContentsHideDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.contents.ContentsHideDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.services.BaseService;
import com.architecture.admin.services.block.BlockMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentsHideService extends BaseService {

    private final ContentsHideDao contentsHideDao;
    private final ContentsHideDaoSub contentsHideDaoSub;
    private final ContentsDaoSub contentsDaoSub;                // 컨텐츠
    private final ContentsService contentsService;
    private final BlockMemberService blockMemberService;

    /*****************************************************
     *  Modules
     ****************************************************/
    /**
     * 콘텐츠 숨기기
     *
     * @param token           access token
     * @param contentsHideDto contentsIdx
     */
    @Transactional
    public void hideContents(String token, ContentsHideDto contentsHideDto) {

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);
        contentsHideDto.setMemberUuid(memberDto.getUuid());

        // 유효성 검사
        commonHideContentsValidate(contentsHideDto);

        // 등록일 set
        contentsHideDto.setRegDate(dateLibrary.getDatetime());

        // 숨기기된 내역 가져오기
        ContentsHideDto oTargetInfo = getTargetInfo(contentsHideDto);

        // 기존 내역이 존재하면
        if (oTargetInfo != null) {

            // 이미 숨기기된 경우
            if (oTargetInfo.getState() == 1) {
                throw new CustomException(CustomError.HIDE_STATE); // 이미 숨기기하였습니다.
            }

            // 내역 존재 하고 상태가 0이라면 <취소 후 다시 숨기기한 경우>
            if (oTargetInfo.getState() == 0) {
                // idx set [sns_contents_Hide]
                contentsHideDto.setIdx(oTargetInfo.getIdx());
                // 숨기기 상태 업데이트
                updateHideState(contentsHideDto);
            }

        } else {
            // 콘텐츠 숨기기
            insertContentsHide(contentsHideDto);
        }

    }

    /**
     * 콘텐츠 숨기기 취소
     *
     * @param token           access token
     * @param contentsHideDto contentsIdx
     */
    @Transactional
    public void contentsHideCancel(String token, ContentsHideDto contentsHideDto) {

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);
        contentsHideDto.setMemberUuid(memberDto.getUuid());

        // 유효성 검사
        commonHideContentsValidate(contentsHideDto);

        // 숨기기 내역 가져오기
        ContentsHideDto oTargetInfo = getTargetInfo(contentsHideDto);

        // 숨기기 한 적 없는데 취소 요청한 경우
        if (oTargetInfo == null) {
            throw new CustomException(CustomError.HIDE_DATA_ERROR); // 잘못된 요청입니다.
        }

        // 숨기기 insert 된 적이 있고 상태값이 정상인 경우
        if (oTargetInfo.getState() == 1) {

            // 등록일 set
            contentsHideDto.setRegDate(dateLibrary.getDatetime());

            // idx set [sns_contents_like]
            contentsHideDto.setIdx(oTargetInfo.getIdx());

            // 숨기기 상태값 0으로 변경
            updateUnHideState(contentsHideDto);

        }
        // 이미 취소되어 있다면
        else {
            throw new CustomException(CustomError.HIDE_CANCEL_STATE); // 이미 숨기기 취소하였습니다.
        }

    }

    /*****************************************************
     *  SubFunction - Select
     ****************************************************/
    /**
     * 숨기기 내역 가져오기
     *
     * @param contentsHideDto contentsIdx, memberUuid
     * @return contentsDto
     */
    public ContentsHideDto getTargetInfo(ContentsHideDto contentsHideDto) {
        return contentsHideDaoSub.oGetTargetInfo(contentsHideDto);
    }

    /*****************************************************
     *  SubFunction - Insert
     ****************************************************/
    /**
     * 콘텐츠 숨기기 등록
     *
     * @param contentsHideDto contentsIdx, memberUuid, regDate
     */
    public void insertContentsHide(ContentsHideDto contentsHideDto) {
        Integer iResult = contentsHideDao.insertContentsHide(contentsHideDto);
        if (iResult < 1) {
            throw new CustomException(CustomError.HIDE_ERROR); // 숨기기 실패하였습니다.
        }
    }

    /*****************************************************
     *  SubFunction - Update
     ****************************************************/
    /**
     * 숨기기 상태값 변경 (state : 1)
     *
     * @param contentsHideDto memberUuid, contentsIdx
     */
    public void updateHideState(ContentsHideDto contentsHideDto) {
        contentsHideDto.setState(1);
        Integer iResult = contentsHideDao.updateContentsHide(contentsHideDto);
        if (iResult != 1) {
            throw new CustomException(CustomError.HIDE_ERROR);  // 숨기기 실패하였습니다.
        }
    }

    /**
     * 숨기기 취소 상태값 변경 (state : 0)
     *
     * @param contentsHideDto memberUuid, contentsIdx
     */
    public void updateUnHideState(ContentsHideDto contentsHideDto) {
        contentsHideDto.setState(0);
        Integer iResult = contentsHideDao.updateContentsHide(contentsHideDto);
        if (iResult != 1) {
            throw new CustomException(CustomError.HIDE_CANCEL_ERROR); // 숨기기 취소 실패하였습니다.
        }
    }

    /*****************************************************
     *  Validation
     ****************************************************/

    /**
     * 게시물 숨기기 공통 유효성
     *
     * @param contentsHideDto : memberUuid[로그인 회원 uuid], contentsIdx[컨텐츠 idx]
     */
    private void commonHideContentsValidate(ContentsHideDto contentsHideDto) {

        String loginMemberUuid = contentsHideDto.getMemberUuid();
        Long contentsIdx = contentsHideDto.getContentsIdx();

        // 콘텐츠 idx 검증
        if (contentsIdx == null || contentsIdx < 1) {
            throw new CustomException(CustomError.CONTENTS_IDX_NULL); // 컨텐츠 IDX가 비었습니다.
        }

        contentsService.contentsIdxValidate(contentsIdx); // 컨텐츠 idx 유효성 검사

        // 컨텐츠 작성한 회원과 차단한 관계인지 검증
        String writerUuid = contentsDaoSub.getMemberUuidByIdx(contentsIdx); // 컨텐츠 작성자 uuid 조회
        blockMemberService.writerAndMemberBlockValidate(writerUuid, loginMemberUuid);

        // 신고 조회용 dto
        SearchDto reportSearchDto = new SearchDto();
        reportSearchDto.setLoginMemberUuid(loginMemberUuid);    // 로그인 회원 idx
        reportSearchDto.setContentsIdx(contentsIdx);            // 컨텐츠 idx

        // 팔로우 & 보관 조회용 dto
        SearchDto searchFollowAndKeep = new SearchDto();
        searchFollowAndKeep.setLoginMemberUuid(loginMemberUuid);    // 로그인 회원 idx
        searchFollowAndKeep.setMemberUuid(writerUuid);              // 컨텐츠 작성자 idx
        searchFollowAndKeep.setContentsIdx(contentsIdx);            // 컨텐츠 idx

        /** 게시물 신고 검증 **/
        contentsService.reportContentsValidate(reportSearchDto);
        /** 팔로우 공개 게시물 검증 **/
        contentsService.followContentsValidate(searchFollowAndKeep);
        /** 보관 게시물인지 검증 **/
        contentsService.keepValidate(searchFollowAndKeep);
    }

}
