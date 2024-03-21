package com.architecture.admin.services.noti;

import com.architecture.admin.libraries.exception.CurlException;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.noti.NotiDao;
import com.architecture.admin.models.daosub.noti.NotiDaoSub;
import com.architecture.admin.models.dto.noti.NotiDto;
import com.architecture.admin.services.BaseService;
import com.architecture.admin.services.wordcheck.ContentsWordCheckService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/*****************************************************
 * 댓글 알림 모델러
 ****************************************************/
@Service
@RequiredArgsConstructor
public class CommentNotiService extends BaseService {
    private final NotiService notiService;
    private final NotiDao notiDao;
    private final NotiDaoSub notiDaoSub;
    private final NotiCurlService notiCurlService;
    private final ContentsWordCheckService contentsWordCheckService;    // 콘텐츠 금칙어
    @Value("${word.check.contents.type}")
    private int contentsWordChk;  // 콘텐츠 금칙어 타입

    /*****************************************************
     *  Modules
     ****************************************************/
    public void commentSendNoti(String token, NotiDto notiDto, String actionType) {
        if (notiDto.getSenderUuid() == null || notiDto.getSenderUuid().equals("")) {
            // 알림 보내는 회원 UUID가 비어있습니다.
            throw new CustomException(CustomError.NOTI_SENDERUUID_ERROR);
        }
        if (notiDto.getSubType() == null || notiDto.getSubType().equals("")) {
            // 서브타입을 입력해주세요.
            throw new CustomException(CustomError.NOTI_SUBTYPE_ERROR);
        }
        if (notiDto.getContentsIdx() <= 0) {
            // 알림 보낼 컨텐츠 IDX가 비어있습니다.
            throw new CustomException(CustomError.NOTI_CONTENTSIDX_ERROR);
        }
        if (notiDto.getCommentIdx() <= 0) {
            // 알림 보낼 댓글 IDX가 비어있습니다.
            throw new CustomException(CustomError.NOTI_COMMENTIDX_ERROR);
        }

        // 알림 제목, 내용 세팅
        notiService.getTitleBody(notiDto);
        /**
         *   알림 노출 순위
         *  - 멘션(상단)
         *  - 게시글(최하단)
         *  역순으로 인서트
         */
        // 댓글 등록일 경우 (수정은 제외)
        if (actionType.equals("register")) {
            //1. 댓글을 달았습니다
            String receiverUuid;
            // 댓글이면 글작성자에게 알림
            if (notiDto.getParentIdx() == null || notiDto.getParentIdx() <= 0) {
                receiverUuid = selectContentsWriter(notiDto.getContentsIdx());
            }
            //  대댓글이면 댓글 작성자에게 알림
            else {
                receiverUuid = selectParentCommentWriter(notiDto.getParentIdx());
            }

            // 작성자 회원이 존재한다면
            if (receiverUuid != null) {
                // 차단 내역 가져오기 ( 한명이라도 차단 했으면 true )
                boolean chekBlock = super.bChkBlock(receiverUuid, notiDto.getSenderUuid());
                // 본인이 본인을 멘션했는지 체크
                boolean checkMe = Objects.equals(receiverUuid, notiDto.getSenderUuid());
                // 본인이 아니고, 차단내역도 없으면
                if (!chekBlock && !checkMe) {
                    notiDto.setMemberUuid(receiverUuid);
                    if (notiDto.getContents() != null && !notiDto.getContents().equals("")) {
                        // 금칙어 처리
                        String contents = contentsWordCheckService.contentsWordCheck(notiDto.getContents(), contentsWordChk);
                        notiDto.setContents(contents);
                    }
                    notiCurlService.registNoti(token, notiDto);
                }
            }
        }

        // 2. 멘션 알림
        notiDto.setSubType("mention_comment");
        // 알림 제목, 내용 세팅
        notiService.getTitleBody(notiDto);

        // 컨텐츠에 맨션 된 회원 목록 조회
        List<String> mentionMemberList = selectMentionMember(notiDto.getCommentIdx());
        List<String> existMentionMemberList = notiDto.getMentionMemberUuidList();  // 기존 맨션 회원 리스트

        // 기존 멘션 회원 중복 제거
        if (mentionMemberList != null && existMentionMemberList != null && !existMentionMemberList.isEmpty()) {
            Set<String> mentionSet = new HashSet<>(mentionMemberList);
            existMentionMemberList.forEach(mentionSet::remove);
            mentionMemberList.clear();            // 리스트 안의 모든 요소 삭제
            mentionMemberList.addAll(mentionSet); // 남은 멘션 추가
        }

        // 멘션된 회원이 있다면
        if (mentionMemberList != null && !mentionMemberList.isEmpty()) {

            // 한명씩 insert
            for (String mentionMemberUuid : mentionMemberList) {
                // 차단 내역 가져오기 ( 한명이라도 차단 했으면 true )
                boolean chekBlock = super.bChkBlock(mentionMemberUuid, notiDto.getSenderUuid());
                // 본인이 본인을 멘션했는지 체크
                boolean checkMe = Objects.equals(mentionMemberUuid, notiDto.getSenderUuid());
                // 본인이 아니고, 차단내역도 없으면
                if (!chekBlock && !checkMe) {
                    notiDto.setMemberUuid(mentionMemberUuid);

                    if (notiDto.getContents() != null && !notiDto.getContents().equals("")) {
                        // 금칙어 처리
                        String contents = contentsWordCheckService.contentsWordCheck(notiDto.getContents(), contentsWordChk);
                        notiDto.setContents(contents);
                    }

                    notiCurlService.registNoti(token, notiDto);
                }
            }
        }

    }

    /**
     * 댓글 좋아요시 알림
     *
     * @param notiDto senderUUID, memberUUID, commentIdx, subType
     */
    public void commentLikeNoti(String token, NotiDto notiDto) {
        // 알림 제목, 내용 세팅
        if (notiDto.getSenderUuid() == null || notiDto.getSenderUuid().equals("")) {
            // 알림 보내는 회원 UUID가 비어있습니다.
            throw new CustomException(CustomError.NOTI_SENDERUUID_ERROR);
        }
        if (notiDto.getSubType() == null || notiDto.getSubType().equals("")) {
            // 서브타입을 입력해주세요.
            throw new CustomException(CustomError.NOTI_SUBTYPE_ERROR);
        }
        if (notiDto.getContentsIdx() <= 0) {
            // 알림 보낼 컨텐츠 IDX가 비어있습니다.
            throw new CustomException(CustomError.NOTI_CONTENTSIDX_ERROR);
        }
        if (notiDto.getCommentIdx() <= 0) {
            // 알림 보낼 댓글 IDX가 비어있습니다.
            throw new CustomException(CustomError.NOTI_COMMENTIDX_ERROR);
        }

        notiService.getTitleBody(notiDto);

        String receiverUuid = selectParentCommentWriter(notiDto.getCommentIdx());

        // 작성자 회원이 존재한다면
        if (receiverUuid != null && !receiverUuid.isEmpty()) {
            boolean chekBlock = super.bChkBlock(receiverUuid, notiDto.getSenderUuid());
            // 본인이 본인을 멘션했는지 체크
            boolean checkMe = Objects.equals(receiverUuid, notiDto.getSenderUuid());
            // 본인이 아니고, 차단내역도 없으면
            if (!chekBlock && !checkMe) {
                // 중복 날짜 제거할 날짜 가져오기
                String checkDate = notiService.getNotiDate();
                notiDto.setCheckNotiDate(checkDate);
                notiDto.setMemberUuid(receiverUuid);

                // DB에 있는 30일 이내 내역 idx 가져오기
                String getNotiIdx = notiCurlService.getNotiDuple(token, notiDto);
                JSONObject notiDupleIdxObject = new JSONObject(getNotiIdx);

                if (!((boolean) notiDupleIdxObject.get("result"))) {
                    throw new CurlException(notiDupleIdxObject);
                }
                JSONObject notiDupleIdxResult = (JSONObject) notiDupleIdxObject.get("data");

                long notiIdx = notiDupleIdxResult.getLong("notiDupleIdx");

                // 있으면 reg_date 업데이트
                if (notiIdx > 0) {
                    notiCurlService.modiNotiRegDate(token, notiIdx);
                }
                // 없으면 알림 인서트
                else {
                    notiCurlService.registNoti(token, notiDto);
                }
            }
        }
    }

    /*****************************************************
     *  SubFunction - select
     ****************************************************/

    /**
     * 댓글에 멘션 된 회원 리스트 가져오기
     *
     * @param commentIdx 댓글IDX
     * @return 댓글에 멘션 회원 리스트
     */
    public List<String> selectMentionMember(Long commentIdx) {
        return notiDao.getCommentMentionMember(commentIdx);
    }

    /**
     * 댓글에 멘션 된 회원 리스트 Sub db에서 가져오기
     *
     * @param commentIdx
     * @return
     */
    public List<String> selectMentionMemberFromSub(Long commentIdx) {
        return notiDaoSub.getCommentMentionMember(commentIdx);
    }


    /**
     * 댓글 시 컨텐츠 작성자 idx가져오기
     *
     * @param contentsIdx 컨텐츠IDX
     * @return 컨텐츠 작성자 UUID
     */
    public String selectContentsWriter(Long contentsIdx) {
        return notiDaoSub.getContentsMember(contentsIdx);
    }

    /**
     * 대댓글 시 댓글 작성자 idx가져오기
     *
     * @param commentIdx 부모댓글IDX
     * @return 부모댓글 작성자 UUID
     */
    public String selectParentCommentWriter(Long commentIdx) {
        return notiDaoSub.getParentCommentMember(commentIdx);
    }
}
