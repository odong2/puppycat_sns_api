package com.architecture.admin.services.tag;

import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.tag.HashTagDao;
import com.architecture.admin.models.daosub.tag.HashTagDaoSub;
import com.architecture.admin.models.dto.tag.HashTagDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/*****************************************************
 * 해시태그 모델러
 ****************************************************/
@Service
@RequiredArgsConstructor
@Transactional
public class HashTagService extends BaseService {
    private final HashTagDaoSub hashTagDaoSub;
    private final HashTagDao hashTagDao;

    /*****************************************************
     *  Modules
     ****************************************************/
    /**
     * 해시태그 치환 및 해시태그/cnt 업데이트
     *
     * @param hashTagDto 댓글일때 : type(댓글:comment) commentIdx (댓글idx) contents(내용)
     *                   컨텐츠일때 : type(컨텐츠:contents) contentsIdx(컨텐츠idx) contents(내용)
     * @return 치환 된 댓글
     */
    public String reigstHashTag(HashTagDto hashTagDto) {

        String contents = hashTagDto.getContents();
        String type = hashTagDto.getType();

        /** # 해시태그 */
        // 특수 문자는 언더바만 포함, 문자 및 숫자, 이모티콘(제어 문자)로 이루어진 패턴
        Pattern hashTagPattern = Pattern.compile("\\#([_\\p{So}\\p{L}\\p{M}\\p{N}\\uD83C-\\uDBFF\\uDC00-\\uDFFF]*)");
        Matcher hashTagMatcher = hashTagPattern.matcher(contents);
        String extractHashTag;
        List<String> hashTagList = new ArrayList<>();

        while (hashTagMatcher.find()) {
            // 패턴에 일치하는 문자열 반환 ex) #해시태그
            extractHashTag = hashTagMatcher.group();
            // #만 추출된 경우 insert 안함
            if (!extractHashTag.equals("#")) {
                hashTagList.add(extractHashTag);
            }
        }

        // 해시태그 중복 제거
        hashTagList = hashTagList.stream().distinct().collect(Collectors.toList());

        // 문자열을 길이에 따라 정렬
        hashTagList.sort((String a, String b) -> b.length() - a.length());

        if (!hashTagList.isEmpty()) {
            for (String tag : hashTagList) {
                // #제거한 해시태그 내용만 추출
                String reHashTag = tag.replace("#", "");
                // ex) #해시태그 > [#[해시태그]]
                contents = contents.replace(tag, "[#[" + reHashTag + "]]");

                hashTagDto.setHashTag(reHashTag);

                // 해시태그 테이블에 기존에 등록된 태그가 있는지 확인
                Integer hashTagIdx = getIdxByHashTag(hashTagDto);
                // 등록되지 않은 해시태그 -> insert
                if (hashTagIdx == null) {
                    insertHashTag(hashTagDto);
                    hashTagIdx = hashTagDto.getInsertedIdx();
                }
                // 기존에 등록된 해시태그 > cnt + 1
                else {
                    hashTagDto.setHashTagIdx((long) hashTagIdx);
                    updateHashTagCnt(hashTagDto);
                }
                hashTagDto.setHashTagIdx((long) hashTagIdx);

                // 컨텐츠이면
                if (Objects.equals(type, "contents")) {
                    boolean checkContnetsHashTag = getContentsHashTag(hashTagDto);
                    if(!checkContnetsHashTag){
                        // 컨텐츠 해시태그 매핑 insert [sns_contents_hash_tag_mapping]
                        insertContentsHashTagMapping(hashTagDto);
                    }
                }
                // 댓글이면
                else if (Objects.equals(type, "comment")) {
                    boolean checkCommentHashTag = getCommentHashTag(hashTagDto);
                    if(!checkCommentHashTag){
                        // 댓글 해시태그 매핑 insert [sns_comment_hash_tag_mapping]
                        insertCommentHashTagMapping(hashTagDto);
                    }
                }
                // 그 외 상황
                else {
                    // 잘못된 요청입니다.
                    throw new CustomException(CustomError.TAG_HASH_DATA_ERROR);
                }
            }
        }
        return contents;
    }

    /**
     *  수정 시 해시태그
     * 
     * @param hashTagDto
     * @return
     */
    public String modifyHashTag(HashTagDto hashTagDto) {

        String contents = hashTagDto.getContents();
        String type = hashTagDto.getType();
        List<HashTagDto> lGetHashTagList;

        // 기존에 등록 된 해시태그 리스트
        if (Objects.equals(type, "contents")) {
            lGetHashTagList = getContentsHashTagList(hashTagDto);
        }
        // 댓글이면
        else if (Objects.equals(type, "comment")) {
            lGetHashTagList = getCommentHashTagList(hashTagDto);
        }
        // 그 외 상황
        else {
            // 잘못된 요청입니다.
            throw new CustomException(CustomError.TAG_HASH_DATA_ERROR);
        }

        // DB에 저장된 기존 해시태그가 있다면 넘어온 값이랑 비교
        if (lGetHashTagList != null) {
            // 수정 된 컨텐츠에서 기존에 있던 [#[해시태그]] 태그 가져오기
            Pattern subHashTagPattern = Pattern.compile("\\#\\[([_\\p{So}\\p{L}\\p{M}\\p{N}\\uD83C-\\uDBFF\\uDC00-\\uDFFF]*)");
            Matcher subHashTagMatcher = subHashTagPattern.matcher(contents);
            String extractSubHashTag;

            while (subHashTagMatcher.find()) {
                // 패턴에 일치하는 문자열 반환 ex) [#[해시태그
                extractSubHashTag = subHashTagMatcher.group();
                // 넘어온 기존 해시태그
                String reHashTag = extractSubHashTag.replace("#[", "");
                // DB에 있는 해시태그랑 같으면 리스트에서 제거
                removeHashTag(lGetHashTagList, reHashTag);
            }

            // 제거 된 기존 해시태그 state = 0 으로 바꿔주기
            for (HashTagDto dto : lGetHashTagList) {

                // 컨텐츠일때
                if (Objects.equals(type, "contents")) {
                   removeContentsHashTagMapping(dto.getIdx());
                }
                // 댓글일때
                else if (Objects.equals(type, "comment")) {
                    removeCommentHashTagMapping(dto.getIdx());
                }
                // 그 외 상황
                else {
                    // 잘못된 요청입니다.
                    throw new CustomException(CustomError.TAG_HASH_DATA_ERROR);
                }
            }
        }
        
        // 신규로 넘어온 해시태그 인서트
        contents = reigstHashTag(hashTagDto);

        return contents;
    }


    // DB에 저장된 기존 해시태그와 넘어온 기존 해시태그가 같은 값이면 지우기
    public void removeHashTag(List<HashTagDto> hashTagDtoList, String targetTag) {
        Iterator<HashTagDto> iterator = hashTagDtoList.iterator();
        while (iterator.hasNext()) {
            HashTagDto dto = iterator.next();
            if (dto.getHashTag().equals(targetTag)) {
                iterator.remove();
            }
        }
    }

    /*****************************************************
     *  SubFunction - Select
     ****************************************************/

    /**
     * 해시태그 idx 검색
     *
     * @param hashTagDto hashTag 추출된 해시태그
     * @return 해시태그 idx
     */
    public Integer getIdxByHashTag(HashTagDto hashTagDto) {
        return hashTagDaoSub.getIdxByHashTag(hashTagDto);
    }

    /**
     * 컨텐츠에 매핑된 해시태그 리스트
     * 
     * @param hashTagDto
     * @return
     */
    public List<HashTagDto> getContentsHashTagList(HashTagDto hashTagDto) {
        return hashTagDaoSub.getContentsHashTagList(hashTagDto);
    }

    /**
     * 댓글에 매핑된 해시태그 리스트
     * 
     * @param hashTagDto
     * @return
     */
    public List<HashTagDto> getCommentHashTagList(HashTagDto hashTagDto) {
        return hashTagDaoSub.getCommentHashTagList(hashTagDto);
    }

    /**
     * 컨텐츠에 사용된 해시태그인지 체크
     *
     * @param hashTagDto
     * @return
     */
    public boolean getContentsHashTag(HashTagDto hashTagDto){
        Integer iCount = hashTagDaoSub.getContentsHashTag(hashTagDto);

        return iCount > 0;
    }

    /**
     * 댓글에 사용된 해시태그인지 체크
     *
     * @param hashTagDto
     * @return
     */
    public boolean getCommentHashTag(HashTagDto hashTagDto){
        Integer iCount = hashTagDaoSub.getCommentHashTag(hashTagDto);

        return iCount > 0;
    }

    /*****************************************************
     *  SubFunction - Insert
     ****************************************************/
    /**
     * 해시태그 등록하기
     *
     * @param hashTagDto hashTag
     */
    public void insertHashTag(HashTagDto hashTagDto) {
        Integer iResult = hashTagDao.insertHashTag(hashTagDto);
        if (iResult < 1) {
            throw new CustomException(CustomError.TAG_REGISTER_HASH_TAG_ERROR); // 해시태그 등록에 실패하였습니다.
        }

    }

    /**
     * 컨텐츠 해시태그 매핑 등록하기
     *
     * @param hashTagDto
     */
    public void insertContentsHashTagMapping(HashTagDto hashTagDto) {
        // 등록일
        hashTagDto.setRegDate(dateLibrary.getDatetime());

        Integer iResult = hashTagDao.insertContentsHashTagMapping(hashTagDto);
        if (iResult < 1) {
            // 해시태그 매핑 등록에 실패하였습니다.
            throw new CustomException(CustomError.TAG_REGISTER_HASH_TAG_MAPPING_ERROR);
        }
    }

    /**
     * 댓글 해시태그 매핑 등록하기
     *
     * @param hashTagDto
     */
    public void insertCommentHashTagMapping(HashTagDto hashTagDto) {
        // 등록일
        hashTagDto.setRegDate(dateLibrary.getDatetime());

        Integer iResult = hashTagDao.insertCommentHashTagMapping(hashTagDto);
        if (iResult < 1) {
            // 해시태그 매핑 등록에 실패하였습니다.
            throw new CustomException(CustomError.TAG_REGISTER_HASH_TAG_MAPPING_ERROR);
        }
    }

    /*****************************************************
     *  SubFunction - Update
     ****************************************************/
    /**
     * 해시태그 카운트 +1
     *
     * @param hashTagDto hashTag
     */
    public void updateHashTagCnt(HashTagDto hashTagDto) {
        Integer iResult = hashTagDao.updateHashTagCnt(hashTagDto);
        if (iResult < 1) {
            // 해시태그 업데이트 실패하였습니다.
            throw new CustomException(CustomError.TAG_UPDATE_HASH_TAG_ERROR);
        }
    }
    /*****************************************************
     *  SubFunction - Delete
     ****************************************************/
    /**
     * 컨텐츠 해시태그 매핑 제거
     * 
     * @param mappingIdx sns_contents_hash_tag_mapping.idx
     */
    public void removeContentsHashTagMapping(Long mappingIdx){
        Integer iResult = hashTagDao.removeContentsHashTagMapping(mappingIdx);
        if (iResult < 1) {
            // 해시태그 삭제 실패하였습니다.
            throw new CustomException(CustomError.TAG_DELETE_HASH_TAG_ERROR);
        }
    }

    /**
     * 댓글 해시태그 매핑 제거
     *
     * @param mappingIdx sns_contents_comment_hash_tag_mapping.idx
     */
    public void removeCommentHashTagMapping(Long mappingIdx){
        Integer iResult = hashTagDao.removeCommentHashTagMapping(mappingIdx);
        if (iResult < 1) {
            // 해시태그 삭제 실패하였습니다.
            throw new CustomException(CustomError.TAG_DELETE_HASH_TAG_ERROR);
        }
    }
    /*****************************************************
     *  SubFunction - Etc
     ****************************************************/
}
