package com.architecture.admin.services.noti;

import com.architecture.admin.libraries.exception.CurlException;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.noti.NotiDao;
import com.architecture.admin.models.daosub.contents.ContentsDaoSub;
import com.architecture.admin.models.daosub.follow.FollowDaoSub;
import com.architecture.admin.models.daosub.noti.NotiDaoSub;
import com.architecture.admin.models.dto.noti.NotiDto;
import com.architecture.admin.services.BaseService;
import com.architecture.admin.services.aws.SNSService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/*****************************************************
 * 컨텐츠 알림 모델러
 ****************************************************/
@Service
@RequiredArgsConstructor
public class ContentsNotiService extends BaseService {
    private final NotiService notiService;
    private final FollowDaoSub followDaoSub;
    private final NotiDao notiDao;
    private final NotiDaoSub notiDaoSub;
    private final ContentsDaoSub contentsDaoSub;
    private final SNSService snsService;                        // sqs /sns
    private final NotiCurlService notiCurlService;
    @Value("${cloud.aws.sns.contents.noti.topic.arn}")
    private String snsTopicARN;             // 푸시sns
    @Value("${cloud.aws.s3.img.url}")
    private String imgDomain;

    /*****************************************************
     *  Modules
     ****************************************************/

    /**
     * 콘텐츠 등록시 알림
     *
     * @param notiDto senderUUID, contentsIdx, subType, contents
     */
    public void contentsRegistSendNoti(String token, NotiDto notiDto) {

        // 유효성 검사
        validate(notiDto);

        // 컨텐츠 이미지 첫번째꺼 가져오기
        String imgUrl = selectContentsImg(notiDto.getContentsIdx());
        notiDto.setImg(imgDomain+imgUrl);

        // 알림 제목, 내용 세팅
        notiService.getTitleBody(notiDto);

        /**
         *   알림 노출 순위 
         *  - 태그(최상단)
         *  - 멘션(중앙)
         *  - 게시글(최하단)
         *  역순으로 인서트
         */

        // 1. 게시글 알림

        // 팔로워(나를 팔로우 한 회원) 수가 몇명인지 체크
        Long followerCnt = followDaoSub.getTotalFollowerCnt(notiDto.getSenderUuid());

        // 팔로워가 1명 이상 있으면
        if (followerCnt != null) {
            // 1~300명은 회원 조회 후 푸시 바로 보내기
            if (followerCnt <= 300) {
                // 팔로우 한 팔로워 리스트 가져오기
                List<String> followerList = selectNotiFollowerList(notiDto);

                if (!ObjectUtils.isEmpty(followerList)) {
                    notiDto.setMemberUuidList(followerList);        // 회원 uuid 리스트 set
                    notiCurlService.registNotiList(token, notiDto); // 알림 리스트 등록
                }
            }
            // 300명 초과시 que 사용
            else {
                // DeduplicationId 컨텐츠IDX + "contents_regist"
                String dpId = notiDto.getContentsIdx() + "contents_regist";
                // 데이터 세팅 action : regist, contents_idx : 등록된 컨텐츠idx
                JSONObject data = new JSONObject();
                data.put("action", "new_contents");
                data.put("contents_idx", notiDto.getContentsIdx());
                // 푸시 데이터 보내기
                snsService.publish(data.toString(), snsTopicARN, "contentsPush", "contents", dpId);
            }
        }

        // 2. 멘션 알림
        notiDto.setSubType("mention_contents");
        // 알림 제목, 내용 세팅
        notiService.getTitleBody(notiDto);

        // 컨텐츠에 맨션 된 회원 목록 조회
        List<String> metionMemberList = selectMentionMember(notiDto);
        // 멘션된 회원이 있다면
        if (!ObjectUtils.isEmpty(metionMemberList)) {
            List<String> mentionMemberList = new ArrayList<>(); // 멘션 회원 리스트

            for (String mentionMemberUuid : metionMemberList) {
                // 차단 내역 가져오기 ( 한명이라도 차단 했으면 true )
                boolean chekBlock = super.bChkBlock(mentionMemberUuid, notiDto.getSenderUuid());
                // 본인이 본인을 멘션했는지 체크
                boolean checkMe = Objects.equals(mentionMemberUuid, notiDto.getSenderUuid());
                // 본인이 아니고, 차단내역도 없으면
                if (!chekBlock && !checkMe) {
                    mentionMemberList.add(mentionMemberUuid); // 멘션 회원 리스트 추가
                }
            }
            // 등록할 멘션 알림이 있다면
            if (!ObjectUtils.isEmpty(mentionMemberList)) {
                notiDto.setMemberUuidList(mentionMemberList);
                notiCurlService.registNotiList(token, notiDto); // 알림 리스트 등록
            }
        }

        // 3. 이미지태그알림
        notiDto.setSubType("img_tag");
        // 알림 제목, 내용 세팅
        notiService.getTitleBody(notiDto);

        // 이미지 태그 된 회원 목록 조회
        List<String> imgTagMemberList = selectImgTagMember(notiDto);
        // 이미지 태그 된 회원이 존재한다면
        if (imgTagMemberList != null && !imgTagMemberList.isEmpty()) {
            List<String> tagMemberList = new ArrayList<>(); // 태그 알림 리스트

            for (String tagMemberUuid : imgTagMemberList) {
                boolean chekBlock = super.bChkBlock(tagMemberUuid, notiDto.getSenderUuid());
                // 본인이 본인을 멘션했는지 체크
                boolean checkMe = Objects.equals(tagMemberUuid, notiDto.getSenderUuid());
                // 본인이 아니고, 차단내역도 없으면
                if (!chekBlock && !checkMe) {
                    notiDto.setMemberUuid(tagMemberUuid);
                    tagMemberList.add(tagMemberUuid); // 태그 회원 리스트 추가
                }
            }

          
            // 등록할 태그 알림이 있다면
            if (!ObjectUtils.isEmpty(tagMemberList)) {
                notiDto.setMemberUuidList(tagMemberList);
                notiCurlService.registNotiList(token, notiDto); // 알림 리스트 등록
            }
        }
    }

    /**
     * 컨텐츠 수정 시 알림
     *
     * @param notiDto senderUUID, contentsIdx, subType, contents
     */
    public void contentsModifySendNoti(String token, NotiDto notiDto) {
        // 유효성 검사
        validate(notiDto);

        // 컨텐츠 이미지 첫번째꺼 가져오기
        String imgUrl = selectContentsImg(notiDto.getContentsIdx());
        notiDto.setImg(imgDomain+imgUrl);

        /**
         *   알림 노출 순위 
         *  - 태그(최상단)
         *  - 멘션(하단)
         *  역순으로 인서트
         */

        // 1. 멘션

        // 알림 제목, 내용 세팅
        notiService.getTitleBody(notiDto);
        // 컨텐츠에 맨션 된 회원 목록 조회
        List<String> metionMemberList = selectMentionMember(notiDto);
        // 멘션된 회원이 있다면
        if (metionMemberList != null && !metionMemberList.isEmpty()) {
            List<String> mentionMemberList = new ArrayList<>();

            for (String mentionMemberUuid : metionMemberList) {
                // 차단 내역 가져오기 ( 한명이라도 차단 했으면 true )
                boolean chekBlock = super.bChkBlock(mentionMemberUuid, notiDto.getSenderUuid());
                // 본인이 본인을 멘션했는지 체크
                boolean checkMe = Objects.equals(mentionMemberUuid, notiDto.getSenderUuid());
                // 본인이 아니고, 차단내역도 없으면
                if (!chekBlock && !checkMe) {
                    mentionMemberList.add(mentionMemberUuid);
                }
            }

            // 등록할 멘션 알림이 있다면
            if (!ObjectUtils.isEmpty(mentionMemberList)) {
                notiDto.setMemberUuidList(mentionMemberList);
                notiCurlService.registNotiList(token, notiDto); // 알림 리스트 등록
            }
        }

        // 이미지 태그
        notiDto.setSubType("img_tag");
        // 알림 제목, 내용 세팅
        notiService.getTitleBody(notiDto);

        // 컨텐츠 수정 전 한번이라도 이미지 태그 된 회원 목록 조회 ( 상태값 신경 X )
        List<String> prevImgTagMemberList = selectPrevImgTagMember(notiDto);
        // 컨텐츠 수정 후 이미지 태그 된 회원 목록 조회
        List<String> imgTagMemberList = selectImgTagMember(notiDto);

        // 이미지 태그 된 회원이 존재한다면
        if (imgTagMemberList != null && !imgTagMemberList.isEmpty()) {
            List<String> tagMemberList = new ArrayList<>(); // 태그 알림 리스트

            for (String tagMemberUuid : imgTagMemberList) {
                if (!prevImgTagMemberList.contains(tagMemberUuid)) {
                    // 차단 내역 가져오기 ( 한명이라도 차단 했으면 true )
                    boolean chekBlock = super.bChkBlock(tagMemberUuid, notiDto.getSenderUuid());
                    // 본인이 본인을 멘션했는지 체크
                    boolean checkMe = Objects.equals(tagMemberUuid, notiDto.getSenderUuid());
                    // 본인이 아니고, 차단내역도 없으면
                    if (!chekBlock && !checkMe) {
                        tagMemberList.add(tagMemberUuid);
                    }
                }

                if (!ObjectUtils.isEmpty(tagMemberList)) {
                    notiCurlService.registNotiList(token, notiDto); // 알림 리스트 등록
                }
            }
        }
    }

    /**
     * 콘텐츠 좋아요시 알림
     *
     * @param notiDto senderUUID, memberUUID, contentsIdx, subType
     */
    public void contentsLikeNoti(String token, NotiDto notiDto) {

        // 유효성 검사
        validate(notiDto);

        notiService.getTitleBody(notiDto);

        boolean checkBlock = super.bChkBlock(notiDto.getMemberUuid(), notiDto.getSenderUuid());
        // 본인 게시물에 좋아요한건지 체크
        boolean checkMe = Objects.equals(notiDto.getMemberUuid(), notiDto.getSenderUuid());

        // 본인이 아니고, 차단내역도 없으면
        if (!checkBlock && !checkMe) {

            // 중복 날짜 제거할 날짜 가져오기
            String checkDate = notiService.getNotiDate();
            notiDto.setCheckNotiDate(checkDate);

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

    /*****************************************************
     *  SubFunction - select
     ****************************************************/
    /**
     * 알림용 회원의 팔로워 리스트 가져오기
     *
     * @param notiDto senderUUID
     * @return 팔로워리스트
     */
    public List<String> selectNotiFollowerList(NotiDto notiDto) {
        notiDto.setRegDate(dateLibrary.getDatetime());
        return notiDaoSub.getFollowerList(notiDto);
    }

    /**
     * 첫번째 이미지 가져오기
     *
     * @param contentsIdx 컨텐츠idx
     * @return img url
     */
    public String selectContentsImg(Long contentsIdx) {
        return notiDao.getContentsImg(contentsIdx);
    }

    /**
     * 컨텐츠에 멘션 된 회원 리스트 가져오기
     *
     * @param notiDto 컨텐츠IDX
     * @return 컨텐츠에 멘션 회원 리스트
     */
    public List<String> selectMentionMember(NotiDto notiDto) {
        return notiDao.getContentsMentionMember(notiDto);
    }

    /**
     * 컨텐츠에 이미지 태그 된 회원 리스트 가져오기
     *
     * @param notiDto 컨텐츠IDX
     * @return 이미지 태그 된 회원 리스트
     */
    public List<String> selectImgTagMember(NotiDto notiDto) {
        return notiDao.getImgTagMember(notiDto);
    }

    /**
     * 수정 전 컨텐츠에 태그 되었던 이미지 태그 리스트
     *
     * @param notiDto
     * @return
     */
    public List<String> selectPrevImgTagMember(NotiDto notiDto) {
        return notiDao.getPrevImgTagMember(notiDto);
    }
    /*****************************************************
     *  SubFunction - Etc
     ****************************************************/
    /**
     * 유효성 검사
     *
     * @param notiDto senderUuid, subType, contentsIdx
     */
    public void validate(NotiDto notiDto) {
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
    }
}
