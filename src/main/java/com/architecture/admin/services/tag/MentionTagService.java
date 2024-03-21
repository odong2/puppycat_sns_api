package com.architecture.admin.services.tag;

import com.architecture.admin.libraries.exception.CurlException;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.tag.MentionTagDao;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.tag.MentionTagDto;
import com.architecture.admin.services.BaseService;
import com.architecture.admin.services.contents.ContentsCurlService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/*****************************************************
 * 회원 멘션 모델러
 ****************************************************/
@Service
@RequiredArgsConstructor
@Transactional
public class MentionTagService extends BaseService {

    private final MentionTagDao mentionTagDao;
    private final ContentsCurlService contentsCurlService;

    /*****************************************************
     *  Modules
     ****************************************************/
    /**
     * 멘션 등록
     *
     * @param mentionTagDto
     * @return
     */
    @SneakyThrows
    public String reigstMention(String token, MentionTagDto mentionTagDto) {

        String contents = mentionTagDto.getContents();
        String type = mentionTagDto.getType();

        /** @ 멘션 */
        Pattern mentionPattern = Pattern.compile("\\@([0-9|a-z|A-Z|ㄱ-ㅎ|ㅏ-ㅣ|가-힝|_|.]*)");
        Matcher mentionMatcher = mentionPattern.matcher(contents);

        String extractMention;
        List<String> mentionList = new ArrayList<>();
        List<String> memberUuidList = new ArrayList<>();

        while (mentionMatcher.find()) {
            // 패턴에 일치하는 문자열 반환 ex) @멘션
            extractMention = mentionMatcher.group();
            // @만 추출된 경우 insert 안함
            if (!extractMention.equals("@")) {
                mentionList.add(extractMention);
            }
        }

        // 멘션 중복 제거
        mentionList = mentionList.stream().distinct().collect(Collectors.toList());

        if (!mentionList.isEmpty()) {

            // 추출된 닉네임 담을 리스트
            List<String> mentionNickList = new ArrayList<>();

            for (String mention : mentionList) {
                // @제거한 회원 닉네임 추출
                String reMemberNick = mention.replace("@", "");
                mentionNickList.add(reMemberNick);
            }

            String jsonString = contentsCurlService.getUuidByNick(token, mentionNickList);

            JSONObject jsonObject = new JSONObject(jsonString);
            if (!(jsonObject.getBoolean("result"))) {
                throw new CurlException(jsonObject);
            }
            ObjectMapper mapper = new ObjectMapper();

            JsonObject listJson = (JsonObject) JsonParser.parseString(jsonObject.get("data").toString()); // data 안 list 파싱
            List<MemberDto> memberInfoList = mapper.readValue(listJson.get("list").toString(), new TypeReference<>() {
            }); // 회원 정보 list

            // 조회된 uuid 있다면 치환
            if (!ObjectUtils.isEmpty(memberInfoList)) {

                // 닉네임에 일치하는 uuid 있다면 [@[uuid]] 형식으로 변환
                for (int i = 0; i < mentionNickList.size(); i++) {
                    String mentionNick = mentionNickList.get(i);

                    for (int j = 0; j < memberInfoList.size(); j++) {
                        MemberDto member = memberInfoList.get(j);
                        String nick = member.getNick();
                        String uuid = member.getUuid();

                        if (mentionNick.equals(nick)) {

                            // ex) @냐옹이 > [@[ko07a1b758553]]
                            contents = contents.replace("@" + mentionNick, "[@[" + uuid + "]]");
                            memberInfoList.remove(j); // 해당 인덱스 삭제
                            mentionNickList.remove(i--); // 해당 인덱스 삭제

                            // 멘션 테이블에 insert 하기 위해 memberUuid 리스트 세팅
                            memberUuidList.add(member.getUuid());

                            break;
                        }

                    }

                }
            }

            // 치환되지 않은 닉네임이 남아 있다면
            if (!mentionNickList.isEmpty()) {
                for (String mentionNick : mentionNickList) {
                    // ex) @냐옹이 > [@[냐옹이]]
                    contents = contents.replace("@" + mentionNick, "[@[" + mentionNick + "]]");
                }
            }
        }

        // 멘션 테이블 등록
        if (!memberUuidList.isEmpty()) {
            for (String memberUuid : memberUuidList) {

                mentionTagDto.setMemberUuid(memberUuid);

                // 맨션 테이블에 기존에 등록된 회원이 있는지 확인
                Long mentionIdx = getIdxByMentionTag(mentionTagDto);
                // 등록되지 않은 회원 uuid -> insert
                if (mentionIdx == null) {
                    insertMentionTag(mentionTagDto);
                    mentionIdx = mentionTagDto.getInsertedIdx();
                }
                // 기존에 등록된 해시태그 -> cnt + 1
                else {
                    // 멘션 idx 세팅
                    mentionTagDto.setMentionIdx(mentionIdx);
                    updateMentionTagCnt(mentionTagDto);
                }
                // 멘션 idx 세팅
                mentionTagDto.setMentionIdx(mentionIdx);

                // 컨텐츠이면
                if (Objects.equals(type, "contents")) {
                    boolean checkContentsMentionTag = getContentsMentionTag(mentionTagDto);
                    if (!checkContentsMentionTag) {
                        //컨텐츠 멘션 매핑 insert [sns_contents_mention_mapping]
                        insertContentsMentionTagMapping(mentionTagDto);
                    }
                }
                // 댓글이면
                else if (Objects.equals(type, "comment")) {
                    boolean checkContentsMentionTag = getCommentMentionTag(mentionTagDto);
                    if (!checkContentsMentionTag) {
                        // 댓글 해시태그 매핑 insert [sns_contents_comment_mention_mapping]
                        insertCommentMentionTagMapping(mentionTagDto);
                    }
                }
                // 그 외 상황
                else {
                    // 잘못된 요청입니다.
                    throw new CustomException(CustomError.TAG_MENTION_DATA_ERROR);
                }
            }
        }
        return contents;
    }

    /**
     * 수정 시 mention
     *
     * @param mentionTagDto
     * @return
     */
    public String modifyMention(String token, MentionTagDto mentionTagDto) {

        String contents = mentionTagDto.getContents();
        String type = mentionTagDto.getType();
        List<MentionTagDto> lGetMentionTagList;


        // 기존에 등록 된 회원 멘션 리스트
        if (Objects.equals(type, "contents")) {
            lGetMentionTagList = getContentsMentionTagList(mentionTagDto);
        }
        // 댓글이면
        else if (Objects.equals(type, "comment")) {
            lGetMentionTagList = getCommentMentionTagList(mentionTagDto);
        }
        // 그 외 상황
        else {
            // 잘못된 요청입니다.
            throw new CustomException(CustomError.TAG_HASH_DATA_ERROR);
        }

        if (lGetMentionTagList != null) {
            // 수정된 컨텐츠에 기존에 있던 [@[UUID]] 가져오기
            Pattern subMentionPattern = Pattern.compile("\\@\\[([0-9|a-z|_]*)");
            Matcher subMentionMatcher = subMentionPattern.matcher(contents);
            String extractSubMention;

            while (subMentionMatcher.find()) {
                // 패턴에 일치하는 문자열 반환 ex) @멘션
                extractSubMention = subMentionMatcher.group();
                // 넘어온 기존 멘션
                String reMemberMention = extractSubMention.replace("@[", "");
                // DB에 있는 멘션이랑 같으면 리스트에서 제거
                removeMention(lGetMentionTagList, reMemberMention);
            }
            // 제거 된 기존 멘션 state = 0으로 변경
            for (MentionTagDto dto : lGetMentionTagList) {
                // 컨텐츠일때
                if (Objects.equals(type, "contents")) {
                    removeContentsMentionTagMapping(dto.getIdx());
                }
                // 댓글일때
                else if (Objects.equals(type, "comment")) {
                    removeCommentMentionTagMapping(dto.getIdx());
                }
                // 그 외 상황
                else {
                    // 잘못된 요청입니다.
                    throw new CustomException(CustomError.TAG_HASH_DATA_ERROR);
                }
            }

            // 신규로 넘어온 멘션태그 인서트
            contents = reigstMention(token, mentionTagDto);
        }
        return contents;
    }

    /**
     * DB에 저장된 기존 멘션과 넘어온 멘션이 같으면 리스트에서 제거
     *
     * @param mentionTagDtoList
     * @param targetMention
     */
    public void removeMention(List<MentionTagDto> mentionTagDtoList, String targetMention) {
        Iterator<MentionTagDto> iterator = mentionTagDtoList.iterator();
        while (iterator.hasNext()) {
            MentionTagDto dto = iterator.next();
            if (dto.getMemberUuid().equals(targetMention)) {
                iterator.remove();
            }
        }
    }
    /*****************************************************
     *  SubFunction - Select
     ****************************************************/
    /**
     * 멘션 태그 idx 검색
     *
     * @param mentionTagDto 회원 IDX
     * @return 멘션 태그 idx
     */
    public Long getIdxByMentionTag(MentionTagDto mentionTagDto) {
        return mentionTagDao.getIdxByMentionTag(mentionTagDto);
    }

    /**
     * 컨텐츠에 매핑 된 멘션 리스트
     *
     * @param mentionTagDto
     * @return
     */
    public List<MentionTagDto> getContentsMentionTagList(MentionTagDto mentionTagDto) {
        return mentionTagDao.getContentsMentionTagList(mentionTagDto);
    }

    /**
     * 댓글에 매핑 된 멘션 리스트
     *
     * @param mentionTagDto
     * @return
     */
    public List<MentionTagDto> getCommentMentionTagList(MentionTagDto mentionTagDto) {
        return mentionTagDao.getCommentMentionTagList(mentionTagDto);
    }

    /**
     * 컨텐츠에 사용된 멘션태그인지 체크
     *
     * @param mentionTagDto
     * @return
     */
    public boolean getContentsMentionTag(MentionTagDto mentionTagDto) {
        Integer iCount = mentionTagDao.getContentsMentionTag(mentionTagDto);

        return iCount > 0;
    }

    /**
     * 댓글에 사용된 멘션태그인지 체크
     *
     * @param mentionTagDto
     * @return
     */
    public boolean getCommentMentionTag(MentionTagDto mentionTagDto) {
        Integer iCount = mentionTagDao.getCommentMentionTag(mentionTagDto);

        return iCount > 0;
    }

    /*****************************************************
     *  SubFunction - Insert
     ****************************************************/
    /**
     * 멘션 등록하기
     *
     * @param mentionTagDto memberIdx
     */
    public void insertMentionTag(MentionTagDto mentionTagDto) {
        Long iResult = mentionTagDao.insertMentionTag(mentionTagDto);
        if (iResult < 1) {
            // 멘션 등록에 실패하였습니다.
            throw new CustomException(CustomError.TAG_REGISTER_MENTION_ERROR);
        }
    }

    /**
     * 컨텐츠 멘션 태그 매핑 등록하기
     *
     * @param mentionTagDto contentsIdx mentionIdx
     */
    public void insertContentsMentionTagMapping(MentionTagDto mentionTagDto) {
        // 등록일
        mentionTagDto.setRegDate(dateLibrary.getDatetime());

        Integer iResult = mentionTagDao.insertContentsMentionTagMapping(mentionTagDto);
        if (iResult < 1) {
            // 멘션 매핑 등록에 실패하였습니다.
            throw new CustomException(CustomError.TAG_REGISTER_MENTION_MAPPING_ERROR);
        }
    }

    /**
     * 댓글 멘션 태그 매핑 등록하기
     *
     * @param mentionTagDto commentIdx mentionIdx
     */
    public void insertCommentMentionTagMapping(MentionTagDto mentionTagDto) {
        // 등록일
        mentionTagDto.setRegDate(dateLibrary.getDatetime());

        Integer iResult = mentionTagDao.insertCommentMentionTagMapping(mentionTagDto);
        if (iResult < 1) {
            //  멘션 매핑 등록에 실패하였습니다.
            throw new CustomException(CustomError.TAG_REGISTER_MENTION_MAPPING_ERROR);
        }
    }

    /*****************************************************
     *  SubFunction - Update
     ****************************************************/
    /**
     * 콘텐츠 멘션태그 +1
     *
     * @param mentionTagDto mentionIdx
     */
    public void updateMentionTagCnt(MentionTagDto mentionTagDto) {
        Integer iResult = mentionTagDao.updateMentionTagCnt(mentionTagDto);
        if (iResult < 1) {
            // 멘션 태그 업데이트 실패하였습니다.
            throw new CustomException(CustomError.TAG_UPDATE_MENTION_ERROR);
        }
    }
    /*****************************************************
     *  SubFunction - Delete
     ****************************************************/
    /**
     * 컨텐츠 멘션태그 매핑 제거
     *
     * @param mappingIdx sns_contents_mention_mapping.idx
     */
    public void removeContentsMentionTagMapping(Long mappingIdx) {
        Integer iResult = mentionTagDao.removeContentsMentionTagMapping(mappingIdx);
        if (iResult < 1) {
            // 멘션 삭제에 실패하였습니다.
            throw new CustomException(CustomError.TAG_DELETE_MENTION_ERROR);
        }
    }

    /**
     * 댓글 멘션태그 매핑 제거
     *
     * @param mappingIdx sns_contents_comment_hash_tag_mapping.idx
     */
    public void removeCommentMentionTagMapping(Long mappingIdx) {
        Integer iResult = mentionTagDao.removeCommentMentionTagMapping(mappingIdx);
        if (iResult < 1) {
            // 멘션 삭제에 실패하였습니다.
            throw new CustomException(CustomError.TAG_DELETE_MENTION_ERROR);
        }
    }

    /*****************************************************
     *  SubFunction - Etc
     ****************************************************/
}

