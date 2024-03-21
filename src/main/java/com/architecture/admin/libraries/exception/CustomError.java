package com.architecture.admin.libraries.exception;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.EnumSet;

/**
 * ****** 오류코드 작성 규칙 ******
 * - 영문4자 와  숫자4자리로 구성 ex) ELGI-9999
 * - 앞4자리 영문은 기능이나 페이지를 알 수 있도록 작성
 * - 뒤4자리 숫자는 아래 규칙에 따라 분류
 * 오류번호   /   설명
 * 1000    =   정상
 * 2xxx    =   필수값 없음
 * 3xxx    =   유효성오류
 * 4xxx    =   sql구문오류
 * 5xxx    =   DB데이터오류
 * 6xxx    =   파일오류
 * 7xxx    =   권한오류
 * 9xxx    =   기타오류
 */
public enum CustomError {
    // EBAD : 유저의 잘못된 요청
    BAD_REQUEST("AEBAD-3999", "lang.common.exception.bad.request")                            // 잘못된 요청입니다.(bad request 공통)
    , BAD_REQUEST_PARAMETER_TYPE_MISMATCH("EBAD-3998", "lang.common.exception.bad.request")  // 잘못된 요청입니다.(메소드로 넘어오는 파라미터의 타입 미스매치등)
    , BAD_REQUEST_REQUIRED_VALUE("EBAD-3997", "lang.common.exception.bad.required.value")    // 필수값을 입력해주세요.
    , EMPTY_LIST("1000", "lang.common.exception.empty.list")
    , NOT_FOUND_URL("ANOFU-9999", "lang.common.exception.not.found.url")      // 해당 URL을 찾을 수 없습니다.
    , SERVER_ERROR("AESER-9999", "lang.common.exception.server.error")        // 서버 에러가 발생 하였습니다.
    , FOUR_HUNDRED_OVER_SERVER_ERROR("AFOHER-9998", "lang.common.exception.server.four.hundred.error")        // 서버 에러가 발생 하였습니다.
    , FIVE_HUNDRED_OVER_SERVER_ERROR("AFOHER-9997", "lang.common.exception.server.five.hundred.error")        // 서버 에러가 발생 하였습니다.

    // ESER : 서버 오류(SQL,DB)
    , SERVER_DATABASE_ERROR("ESER-5999", "lang.common.exception.server.database")   // 죄송합니다.서버에 문제가 발생했습니다. 잠시후 다시 이용해주세요.
    , SERVER_SQL_ERROR("ESER-5998", "lang.common.exception.server.database")        // 죄송합니다.서버에 문제가 발생했습니다. 잠시후 다시 이용해주세요.

    // ECOM : 공통 오류
    , TOKEN_ERROR("ERTE-9999", "lang.token.expire.time.error.publish.fail")             // 토큰 값 에러 다시 로그인 해주세요.
    , SWITCH_FALSE_ERROR("ECOM-9998", "lang.common.exception.switch.false")             // 이용 불가한 기능입니다.

    // EMEM : 회원 관련 오류
    , MEMBER_UUID_ERROR("EMEM-3999", "lang.member.exception.uuid.null")        // 회원 UUID가 유효하지 않습니다.
    , MEMBER_UUID_EMPTY("EMEM-2999", "lang.member.exception.uuid.empty")       // 회원 UUID가 비었습니다.

    // EFOL : 팔로우 관련 오류
    , FOLLOW_MEMBER_EMPTY("EFOL-2999", "lang.follow.exception.memberUuid.empty")    // 회원 UUID가 비어있습니다
    , FOLLOW_FOLLOW_EMPTY("EFOL-2998", "lang.follow.exception.followUuid.empty")    // 팔로우 UUID가 비어있습니다
    , FOLLOW_SEARCHTYPE_EMPTY("EFOL-2997", "lang.follow.exception.searchType.empty") // 검색 타입이 비어있습니다
    , FOLLOW_DUPLE("EFOL-9999", "lang.follow.exception.follow.duple")                // 이미 팔로우 된 회원 입니다
    , FOLLOW_NOT_FOLLOW("EFOL-9998", "lang.follow.exception.follow.not")             // 팔로우 된 회원이 아닙니다.
    , FOLLOW_NOT_FOLLOWER("EFOL-9997", "lang.follow.exception.follower.not")         // 나를 팔로우 한 회원이 아닙니다.
    , FOLLOW_SELF_FOLLOW("EFOL-9996", "lang.follow.exception.follow.self")           // 자기 자신은 팔로우 할 수 없습니다
    , FOLLOW_SEARCHTYPE_ERROR("EFOL-9995", "lang.follow.exception.searchType")       // 검색은 id 와 nick만 가능합니다

    // EBLO : 차단 관련 오류
    , BLOCK_MEMBER_ERROR("EBLO-9999", "lang.block.member.exception")                           // 차단 실패하였습니다.
    , UNBLOCK_MEMBER_ERROR("EBLO-9998", "lang.unblock.member.exception")                       // 차단 해제 실패하였습니다.
    , BLOCK_MEMBER_STATE_BLOCK("EBLO-9997", "lang.block.member.state.block.exception")         // 이미 차단되었습니다.
    , BLOCK_MEMBER_STATE_UNBLOCK("EBLO-9996", "lang.block.member.state.unblock.exception")     // 이미 차단 해제되었습니다.
    , BLOCK_MEMBER_EXIST("EBLO-3999", "lang.block.member.exist")                               // 차단 내역이 존재합니다.
    , BLOCK_MEMBER_DATA_ERROR("EBLO-3998", "lang.block.member.data.exception")                 // 차단한 내역이 없습니다.
    , BLOCK_MEMBER_SAME_TARGET("EBLO-3997", "lang.block.member.data.target.same.exception")    // 요청자와 대상자 IDX가 일치합니다.
    , BLOCK_MEMBER_UUID_EMPTY("EBLO-2999", "lang.block.member.uuid.empty")                     // 차단 대상 UUID가 비어있습니다.
    , BLOCK_MEMBER_UUID_ERROR("EBLO-2998", "lang.block.member.uuid.error")                     // 차단 대상 UUID가 유효하지 않습니다.

    // ECRE : 신고 관련 오류
    , REPORT_ERROR("ECRE-9999", "lang.report.exception")                              // 신고 실패하였습니다.
    , REPORT_CANCEL_ERROR("ECRE-9998", "lang.report.cancel.exception")                // 신고취소 실패하였습니다.
    , REPORT_STATE_ERROR("ECRE-9997", "lang.report.state.exception")                  // 이미 신고된 글입니다.
    , REPORT_CANCEL_STATE_ERROR("ECRE-9996", "lang.report.cancel.state.exception")    // 이미 신고취소된 글입니다.
    , REPORT_DATA_ERROR("ECRE-9995", "lang.report.data.exception")                    // 신고된 기록이 없습니다.
    , REPORT_CODE_EMPTY("ECRE-2999", "lang.report.exception.code.empty")              // 신고사유를 선택해주세요.
    , REPORT_REASON_EMPTY("ECRE-2998", "lang.report.exception.reason.empty")          // 신고 상세사유를 입력해주세요.
    , REPORT_CODE_INVALID("ECRE-3999", "lang.report.exception.code.invalid")          // 유효하지 않은 신고사유입니다.

    // ECON : 콘텐츠 관련 오류
    , CONTENTS_REGISTER_ERROR("ECON-9998", "lang.contents.exception.regist")                                      // 콘텐츠 등록에 실패하였습니다.
    , CONTENTS_REGISTER_MEMBER_TAG_ERROR("ECON-9997", "lang.contents.exception.memberTag.regist")                 // 회원 태그 등록에 실패하였습니다.
    , CONTENTS_UPDATE_CONTENTS_ERROR("ECON-9996", "lang.contents.exception.contents.update")                      // 내용 업데이트 실패하였습니다.
    , CONTENTS_UPDATE_CONTENTS_IDX_ERROR("ECON-9995", "lang.contents.exception.contents.update.idx")              // 내용 업데이트 IDX 오류.
    , CONTENTS_REGISTER_IMG_TAG_ERROR("ECON-9994", "lang.contents.exception.imgTag.uuid")                         // 이미지내 태그 uuid 오류.
    , CONTENTS_REGISTER_LOCATION_ERROR("ECON-9993", "lang.contents.exception.location.regist")                    // 위치 등록에 실패하였습니다.
    , CONTENTS_REGISTER_LOCATION_MAPPING_ERROR("ECON-9992", "lang.contents.exception.location.mapping.regist")    // 위치 매핑 등록에 실패하였습니다.
    , CONTENTS_REGISTER_IMG_TAG_MEMBER_ERROR("ECON-9991", "lang.contents.exception.imgTag.memberUuid")            // 존재하지 않는 회원이 이미지내 태그 되었습니다.
    , CONTENTS_DELETE_IMG_TAG_MEMBER_ERROR("ECON-9990", "lang.contents.exception.imgTag.delete")                  // 회원 이미지 태그 삭제에 실패하였습니다.
    , CONTENTS_DELETE_LOCATION_ERROR("ECON-9989", "lang.contents.exception.location.delete")                      // 위치정보 삭제 실패
    , CONTENTS_MODI_IMG_TAG_MEMBER_ERROR("ECON-9988", "lang.contents.exception.imgTag.modify")                    // 회원 이미지 태그 수정에 실패하였습니다.
    , CONTENTS_UID_DUPLE("ECON-3999", "lang.contents.exception.uuid.duple")                                       // 이미 존재하는 고유아이디입니다.
    , CONTENTS_TEXT_LIMIT_ERROR("ECON-3998", "lang.contents.exception.text.limit.over")                           // 최대 입력 가능 글자수를 초과하였습니다.
    , CONTENTS_IMAGE_LIMIT_ERROR("ECON-3997", "lang.contents.exception.image.limit.over")                         // 사진은 최대 12장만 선택 가능합니다.
    , CONTENTS_IMAGE_MEMBER_TAG_LIMIT_ERROR("ECON-3996", "lang.contents.exception.image.memberTag.limit.over")    // 최대 10명까지 태그 가능합니다.
    , CONTENTS_IMAGE_UID_DUPLE("ECON-3995", "lang.contents.exception.image.uuid.duple")                           // 이미 존재하는 이미지 고유아이디입니다.
    , CONTENTS_IDX_ERROR("ECON-3994", "lang.contents.exception.idx")                                              // 존재하지 않는 콘텐츠입니다.
    , CONTENTS_DETAIL_SEARCH_TYPE_ERROR("ECON-3991", "lang.contents.exception.search.type.error")                 // searchType이 유효하지 않습니다.
    , CONTENTS_HASH_TAG_ERROR("ECON-3990", "lang.contents.exception.hashTag.error")                               // 존재하지않는 해시태그입니다.
    , CONTENTS_HIDE_ERROR("ECON-3989", "lang.contents.exception.hide.error")                                      // 숨김 처리된 게시물입니다.
    , CONTENTS_BLOCK_ERROR("ECON-3988","lang.contents.exception.block.error")                                     // 해당 게시물을 차단하여 볼 수 없습니다.
    , CONTENTS_REPORT_ERROR("ECON-3987", "lang.contents.exception.report.error")                                  // 신고한 게시물입니다.
    , CONTENTS_ONLY_FOLLOW_VIEW_ERROR("ECON-3986", "lang.contents.exception.only.follow.view.error")              // 팔로우 후 게시물을 볼 수 있습니다.
    , CONTENTS_NOT_KEEP_ERROR("ECON-3985", "lang.contents.exception.not.keep.error")                              // 보관한 게시물이 아닙니다.
    , CONTENTS_KEEP_ERROR("ECON-3984", "lang.contents.exception.keep.error")                                      // 보관한 게시물입니다.
    , CONTENTS_NOT_MY_POST_ERROR("ECON-3983", "lang.contents.exception.not.my.post.error")                        // 내 작성글이 아닙니다.
    , CONTENTS_NEED_LOGIN_ONLY_FOLLOW("ECON-3982","lang.contents.exception.need.login.only.follow")               // 팔로우 공개 게시물이므로 로그인 후 이용해주세요.
    , CONTENTS_NOT_IMG_TAG_IDX_ERROR("ECON-3981", "lang.contents.exception.imgTag.not")                           // 컨텐츠에 등록된 IMG가 아닌 이미지 태그가 넘어왔습니다.
    , CONTENTS_SNS_TYPE_ERROR("ECON-3980", "lang.contents.exception.sns.type.error")                              // sns 글이 아닙니다
    , CONTENTS_TEXT_EMPTY("ECON-2999", "lang.contents.exception.text.empty")                                      // 내용을 입력해주세요.
    , CONTENTS_IMAGE_EMPTY("ECON-2998", "lang.contents.exception.image.empty")                                    // 사진을 등록해주세요.
    , CONTENTS_ISVIEW_EMPTY("ECON-2997", "lang.contents.exception.isView.empty")                                  // 공개범위를 선택해주세요.
    , CONTENTS_MENUIDX_EMPTY("ECON-2996", "lang.contents.exception.menu.idx.empty")                               // 카테고리를 선택해주세요.
    , CONTENTS_IMAGE_LIMIT_EMPTY("ECON-2995", "lang.contents.exception.image.limit.empty")                        // imgLimit을 입력해주세요.
    , CONTENTS_IMAGE_OFFSET_EMPTY("ECON-2994", "lang.contents.exception.image.offset.empty")                      // imgOffset을 입력해주세요.
    , CONTENTS_SEARCH_TYPE_EMPTY("ECON-2993", "lang.contents.exception.searchType.empty")                         // 검색 유형을 입력해주세요.
    , CONTENTS_HASH_TAG_EMPTY("ECON-2992", "lang.contents.exception.hashTag.empty")                               // 해시태그를 입력해주세요.
    , CONTENTS_MENUIDX_ERROR("ECON-2991", "lang.contents.exception.menuIdx.error")                                // 카테고리 idx 오류
    , CONTENTS_IDX_NULL("ECON-2989", "lang.contents.exception.idx.null")                                          // 컨텐츠 IDX가 비었습니다.

    // ECMT : 댓글 관련 오류
    , COMMENT_UPDATE_CONTENTS_ERROR("ECMT-9996", "lang.comment.exception.contents.update")          // 댓글 내용 업데이트 실패하였습니다.
    , COMMENT_UPDATE_CONTENTS_IDX_ERROR("ECMT-9995", "lang.comment.exception.contents.update.idx")  // 댓글 내용 업데이트 IDX 오류.
    , COMMENT_MODIFY_ERROR("ECMT-9994", "lang.comment.exception.modify")                            // 댓글 수정에 실패하였습니다.
    , COMMENT_IDX_NULL("ECMT-2999", "lang.comment.exception.idx.null")                              // 댓글 IDX가 비었습니다.
    , COMMENT_CONTENTS_IDX_EMPTY("ECMT-2998", "lang.comment.exception.contents.empty")              // 컨텐츠 IDX가 비어있습니다.
    , COMMENT_CONTENTS_EMPTY("ECMT-2997", "lang.comment.exception.text.empty")                      // 댓글을 입력해주세요.
    , COMMENT_PARENT_IDX_EMPTY("ECMT-2996", "lang.comment.exception.parentIdx.empty")               // 부모 컨텐츠 IDX가 비었습니다.
    , COMMENT_TEXT_LIMIT_ERROR("ECMT-3999", "lang.comment.exception.text.limit.over")               // 최대 입력 가능 글자수를 초과하였습니다.
    , COMMENT_UUID_DUPLE("ECMT-3998", "lang.comment.exception.uuid_duple")                          // 이미 존재하는 고유아이디입니다.
    , COMMENT_NOT_AUTH("ECMT-3997", "lang.comment.exception.not.auth")                              // 해당 글의 작성자가 아닙니다.
    , COMMENT_DELETE_ERROR("ECMT-3996", "lang.comment.exception.delete.already")                    // 이미 삭제 처리 되었습니다.
    , COMMENT_IDX_ERROR("ECMT-3995","lang.comment.exception.idx")                                   // 존재하지 않는 댓글입니다.
    , COMMENT_REPLY_ERROR("ECMT-3994", "lang.comment.exception.reply.error")                        // 답글을 등록할 수 없습니다.
    , COMMENT_ERROR("ECMT-3993", "lang.comment.exception.comment.error")                            // 댓글을 등록할 수 없습니다.
    , COMMENT_PARENT_IDX_DIFFERENT("ECMT-3992", "lang.comment.exception.parent.idx.different")      // 부모 댓글 번호가 일치하지 않습니다.
    , COMMENT_REPORT_ERROR("ECMT-3991", "lang.comment.exception.report.error")                      // 신고한 댓글입니다.

    // ETAG : 해시태그,멘션 관련 오류
    , TAG_HASH_DATA_ERROR("ETAG-9999", "lang.tag.exception.data")                                           // 잘못된 요청입니다.
    , TAG_MENTION_DATA_ERROR("ETAG-9998", "lang.tag.exception.data")                                        // 잘못된 요청입니다.
    , TAG_REGISTER_HASH_TAG_ERROR("ETAG-9997", "lang.tag.exception.hashTag.regist")                         // 해시태그 등록에 실패하였습니다.
    , TAG_REGISTER_HASH_TAG_MAPPING_ERROR("ETAG-9996", "lang.tag.exception.hashTag.mapping.regist")         // 해시태그 매핑 등록에 실패하였습니다.
    , TAG_UPDATE_HASH_TAG_ERROR("ETAG-9995", "lang.tag.exception.hashTag.update")                           // 해시태그 업데이트 실패하였습니다.
    , TAG_REGISTER_MENTION_ERROR("ETAG-9994", "lang.tag.exception.mention.regist")                          // 멘션 등록에 실패하였습니다.
    , TAG_REGISTER_MENTION_MAPPING_ERROR("ETAG-9993", "lang.tag.exception.mention.mapping.regist")          // 멘션 매핑 등록에 실패하였습니다.
    , TAG_UPDATE_MENTION_ERROR("ETAG-9992", "lang.tag.exception.mention.update")                            // 멘션 업데이트 실패하였습니다.
    , TAG_DELETE_HASH_TAG_ERROR("ETAG-9991", "lang.tag.exception.hashTag.delete")                           // 해시태그 삭제 실패하였습니다.
    , TAG_DELETE_MENTION_ERROR("ETAG-9990", "lang.tag.exception.mention.delete")                            // 멘션 삭제 실패하였습니다.

    // ENTI : 알림 관련 오류
    , NOTI_SENDERUUID_ERROR("ENTI-2999", "lang.noti.exception.senderUuid.empty")     // 알림 보내는 회원 UUID가 비어있습니다.
    , NOTI_MEMBERUUID_ERROR("ENTI-2998", "lang.noti.exception.memberUuid.empty")     // 알림 받는 회원 UUID가 비어있습니다.
    , NOTI_CONTENTSIDX_ERROR("ENTI-2997", "lang.noti.exception.contentsIdx.null")    // 알림 보낼 컨텐츠 IDX가 비어있습니다.
    , NOTI_SUBTYPE_ERROR("ENTI-2996", "lang.noti.exception.subType.null")            // 서브타입을 입력해주세요.
    , NOTI_COMMENTIDX_ERROR("ENTI-2995", "lang.noti.exception.commentIdx.null")      // 알림 보낼 댓글 IDX가 비어있습니다.

    // ELIK : 좋아요 관련 오류
    , LIKE_ERROR("ELIK-9999", "lang.contents.like.exception")                                               // 좋아요 실패하였습니다.
    , LIKE_CANCEL_ERROR("ELIK-9998", "lang.contents.like.cancel.exception")                                 // 좋아요 취소 실패하였습니다.
    , LIKE_DATA_ERROR("ELIK-9997", "lang.contents.like.data.exception")                                     // 잘못된 요청입니다.
    , LIKE_STATE("ELIK-3999", "lang.contents.like.state.exception")                                         // 이미 좋아요를 누르셨습니다.
    , LIKE_CANCEL_STATE("ELIK-3998", "lang.contents.like.cancel.state.exception")                           // 이미 좋아요 취소하였습니다.
    , LIKE_ERROR_BY_MEMBER_BLOCK("ELIK-3997", "lang.contents.like.exception.by.member.block")               // 좋아요를 할 수 없습니다.
    , LIKE_CANCEL_ERROR_BY_MEMBER_BLOCK("ELIK-3996","lang.contents.like.cancel.exception.by.member.block")  // 좋아요를 취소할 수 없습니다.

    // ESAV : 콘텐츠 저장하기 관련 오류
    , SAVE_ERROR("ESAV-9999", "lang.contents.save.exception")                         // 저장 실패하였습니다.
    , SAVE_CANCEL_ERROR("ESAV-9998", "lang.contents.save.cancel.exception")           // 저장 취소 실패하였습니다.
    , SAVE_DATA_ERROR("ESAV-9997", "lang.contents.save.data.exception")               // 저장한 기록이 없습니다.
    , SAVE_STATE("ESAV-3999", "lang.contents.save.state.exception")                   // 이미 저장하였습니다.
    , SAVE_CANCEL_STATE("ESAV-3998", "lang.contents.save.cancel.state.exception")     // 이미 저장 취소하였습니다.

    // EHID : 콘텐츠 숨기기 관련 오류
    , HIDE_ERROR("EHID-9999", "lang.contents.hide.exception")                         // 숨기기 실패하였습니다.
    , HIDE_CANCEL_ERROR("EHID-9998", "lang.contents.hide.cancel.exception")           // 숨기기 취소 실패하였습니다.
    , HIDE_DATA_ERROR("EHID-9997", "lang.contents.hide.data.exception")               // 숨긴 기록이 없습니다.
    , HIDE_STATE("EHID-3999", "lang.contents.hide.state.exception")                   // 이미 숨긴 콘텐츠입니다.
    , HIDE_CANCEL_STATE("EHID-3998", "lang.contents.hide.cancel.state.exception")     // 이미 숨기기 취소하였습니다.

    // EKEP : 콘텐츠 보관하기 관련 오류
    , KEEP_ERROR("EKEP-9999", "lang.contents.store.exception")                            // 보관 실패하였습니다.
    , KEEP_CANCEL_ERROR("EKEP-9998", "lang.contents.store.cancel.exception")              // 프로필에 표시를 실패하였습니다.
    , KEEP_NOT_MY_CONTENTS_ERROR("EKEP-3999", "lang.contents.not.myContents.exception")   // 내가 작성한 콘텐츠가 아닙니다.
    , KEEP_IDX_EMPTY_ERROR("EKEP-2999", "lang.contents.keep.empty.idx")                   // 콘텐츠를 선택해주세요.

    // EDEL : 콘텐츠 삭제 관련 오류
    , DELETE_ERROR("EDEL-9999", "lang.contents.exception.delete")                           // 콘텐츠 삭제에 실패하였습니다.
    , DELETE_NOT_MY_CONTENTS_ERROR("EDEL-3999", "lang.contents.not.myContents.exception")   // 내가 작성한 콘텐츠가 아닙니다.
    , DELETE_IDX_EMPTY_ERROR("EDEL-2999", "lang.contents.delete.empty.idx")                 // 삭제할 콘텐츠를 선택해주세요.

    // EIMG : 이미지 관련 오류
    , IMAGE_NOT_EXIST_ERROR("EIMG-9999", "lang.common.exception.image.not.exist")           // 이미지가 존재하지 않습니다.
    , IMAGE_EXTENSION_ERROR("EIMG-9998", "lang.common.exception.image.extension.error")     // 허용하지않는 확장자를 가진 파일입니다.
    , IMAGE_SIZE_LIMIT_OVER("EIMG-9997", "lang.common.exception.image.size.error")          // 등록할 이미지 용량이 너무 큽니다.

    // EBWO : 금칙어 관련 오류
    , WORD_EMPTY("EBWO-99999", "lang.common.exception.word.empty")                   // 검사할 단어가 존재하지 않습니다.
    , WORD_TYPE_EMPTY("EBWO-9998", "lang.common.exception.word.type.empty")          // 금칙어 타입이 존재하지 않습니다.

    , CHAT_ROOM_ID_EMPTY("SECHA-2998", "lang.chat.room.id.exception.empty")    // Room Id 생성 오류
    ;

    @Autowired
    MessageSource messageSource;
    private String code;
    private String message;
    private Object data;

    CustomError(String code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    CustomError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return messageSource.getMessage(message, null, LocaleContextHolder.getLocale());
    }

    public CustomError setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
        return this;
    }

    @Component
    public static class EnumValuesInjectionService {

        @Autowired
        private MessageSource messageSource;

        // bean
        @PostConstruct
        public void postConstruct() {
            for (CustomError customError : EnumSet.allOf(CustomError.class)) {
                customError.setMessageSource(messageSource);
            }
        }
    }
}
