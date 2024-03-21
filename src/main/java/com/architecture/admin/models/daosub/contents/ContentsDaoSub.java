package com.architecture.admin.models.daosub.contents;

import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.contents.ContentsDto;
import com.architecture.admin.models.dto.contents.ContentsImgDto;
import com.architecture.admin.models.dto.contents.ContentsImgMemberTagDto;
import com.architecture.admin.models.dto.tag.MentionTagDto;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface ContentsDaoSub {

    /*****************************************************
     * Select
     ****************************************************/
    /**
     * 팔로잉 콘텐츠 카운트
     * [sns_contents]
     *
     * @param searchDto loginMemberUuid
     * @return count
     */
    Integer iGetTotalFollowContentsCount(SearchDto searchDto);

    /**
     * 팔로잉 콘텐츠 리스트
     * [sns_contents]
     *
     * @param searchDto loginMemberUuid
     * @return list
     */
    List<ContentsDto> lGetFollowContentsList(SearchDto searchDto);

    /**
     * 해당 유저가 작성한 콘텐츠 카운트
     * [sns_contents]
     *
     * @param searchDto loginMemberUuid
     * @return count
     */
    Integer iGetTotalMemberContentsCount(SearchDto searchDto);

    /**
     * 해당 유저가 작성한 콘텐츠 리스트
     * [sns_contents]
     *
     * @param searchDto loginMemberUuid
     * @return list
     */
    List<ContentsDto> lGetMemberContentsList(SearchDto searchDto);

    /**
     * 내가 작성한 콘텐츠 카운트
     * [sns_contents]
     *
     * @param searchDto loginMemberUuid
     * @return count
     */
    Integer iGetTotalMyContentsCount(SearchDto searchDto);

    /**
     * 내가 작성한 콘텐츠 리스트
     * [sns_contents]
     *
     * @param searchDto loginMemberUuid
     * @return list
     */
    List<ContentsDto> lGetMyContentsList(SearchDto searchDto);

    /**
     * 콘텐츠 고유아이디 중복체크
     * [sns_contents]
     *
     * @param uuid 고유아이디
     * @return count
     */
    Integer getCountByUuid(String uuid);

    /**
     * 콘텐츠 이미지 고유아이디 중복체크
     * [sns_contents_img]
     *
     * @param uuid 고유아이디
     * @return count
     */
    Integer getCountByImgUuid(String uuid);

    /**
     * 콘텐츠 위치 등록된게 있는지 확인
     * [sns_contents_location]
     *
     * @param location 위치
     * @return location.idx
     */
    Long getIdxByLocation(String location);

    /**
     * 콘텐츠 작성자 IDX 가져오기
     *
     * @param contentsIdx 콘텐츠 idx
     * @return loginMemberUuid
     */
    String getMemberUuidByIdx(Long contentsIdx);

    /**
     * 내가 쓴 콘텐츠 인지 확인
     *
     * @param contentsDto loginMemberUuid, 콘텐츠 idx
     * @return count
     */
    Integer getCountByMyContentsIdx(ContentsDto contentsDto);

    /**
     * 소셜/산책 콘텐츠 타입 확인
     *
     * @param contentsDto menuIdx, 콘텐츠 idx
     * @return count
     */
    Integer getCountByContentsMenu(ContentsDto contentsDto);

    /**
     * 컨텐츠에 사용 된 이미지 idx리스트
     *
     * @param contentsDto idx
     * @return img_idx
     */
    List<Long> getContentsImgIdxList(ContentsDto contentsDto);

    /**
     * 컨텐츠에 사용 된 위치정보값
     *
     * @param contentsDto idx
     * @return string 위치 정보
     */
    String getContentsLocation(ContentsDto contentsDto);

    /**
     * 첫번째 이미지 가져오기
     *
     * @param contentsIdx 콘텐츠 idx
     * @return img url
     */
    String getContentsImg(Long contentsIdx);

    /**
     * 첫번째 이미지 조회
     *
     * @param idx : 컨텐츠 idx
     * @return
     */
    ContentsImgDto getContentsFirstImg(Long idx);

    /**
     * 유효한 컨텐츠인지 조회
     *
     * @param idx
     * @return
     */
    int getContentsCntByIdx(Long idx);

    /**
     * 컨텐츠 상세 카운트
     *
     * @param searchDto
     * @return
     */
    Integer getTotalContentsDetailCnt(SearchDto searchDto);

    /**
     * 컨텐츠 상세 리스트
     *
     * @param searchDto
     * @return
     */
    List<ContentsDto> getContentsDetailList(SearchDto searchDto);

    /**
     * 컨텐츠 멘션 리스트
     *
     * @param idxList : contentsIdx 리스트
     * @return
     */
    List<MentionTagDto> getMentionTagList(List<Long> idxList);

    /**
     * 멘션된 회원 uuid 조회
     *
     * @param contentsIdx
     * @return
     */
    List<String> getMentionMemberUuid(Long contentsIdx);

    /**
     * 컨텐츠 이미지 리스트
     *
     * @param searchDto : contentsIdx[컨텐츠 idx], imgLimit, imgOffSet
     * @return
     */
    List<ContentsImgDto> getContentsImgList(SearchDto searchDto);

    /**
     * 이미지 내 회원 태그 리스트
     *
     * @param searchDto : loginMemberUuid[로그인 회원]
     * @return
     */
    List<ContentsImgMemberTagDto> getImgMemberTagList(SearchDto searchDto);

    /**
     * 인기 컨텐츠 카운트
     *
     * @param searchDto : loginMemberUuid[로그인 회원], date [일주일 전 or 한시간 전], nowDate[현재]
     * @return count
     */
    int getTotalWeekPopularCnt(SearchDto searchDto);

    /**
     * 인기 컨텐츠 상세 리스트
     *
     * @param searchDto : loginMemberUuid[로그인 회원], date [일주일 전], nowDate[현재]
     * @return
     */
    List<ContentsDto> getWeekPopularList(SearchDto searchDto);

    /**
     * 급상승 인기 컨텐츠 카운트
     *
     * @param searchDto
     * @return
     */
    int getTotalHourPopularCnt(SearchDto searchDto);

    /**
     * 급상승 인기 컨텐츠 상세 리스트
     *
     * @param searchDto
     * @return
     */
    List<ContentsDto> getHourPopularList(SearchDto searchDto);

    /**
     * 저장한 컨텐츠 카운트
     *
     * @param searchDto : loginMemberUuid[로그인 회원]
     * @return count
     */
    int getTotalSaveCnt(SearchDto searchDto);

    /**
     * 저장한 컨텐츠 상세 리스트 조회
     *
     * @param searchDto : loginMemberUuid[로그인 회원]
     * @return
     */
    List<ContentsDto> getSaveList(SearchDto searchDto);

    /**
     * 좋아요한 컨텐츠 카운트
     *
     * @param searchDto : loginMemberUuid[로그인 회원]
     * @return count
     */
    int getTotalLikeCnt(SearchDto searchDto);

    /**
     * 좋아요한 컨텐츠 상세 리스트
     *
     * @param searchDto : loginMemberUuid[로그인 회원]
     * @return
     */
    List<ContentsDto> getLikeList(SearchDto searchDto);

    /**
     * 내가 태그된 컨텐츠 상세 카운트
     *
     * @param searchDto : loginMemberUuid[로그인 회원]
     * @return count
     */
    int getTotalTagCnt(SearchDto searchDto);

    /**
     * 내가 태그된 컨텐츠 상세 리스트
     *
     * @param searchDto : loginMemberUuid[로그인 회원]
     * @return
     */
    List<ContentsDto> getTagList(SearchDto searchDto);

    /**
     * 해시태그 컨텐츠 카운트
     *
     * @param searchDto
     * @return
     */
    int getTotalHashTagCnt(SearchDto searchDto);

    /**
     * 해시태그 컨텐츠 상세 리스트
     *
     * @param searchDto
     * @return
     */
    List<ContentsDto> getHashTagContentsList(SearchDto searchDto);

    /**
     * 컨텐츠 상세
     *
     * @param searchDto
     * @return
     */
    ContentsDto getContentsDetail(SearchDto searchDto);

    /**
     * 숨김 컨텐츠인지 조회
     *
     * @param searchDto : contentsIdx[컨텐츠 idx], loginMemberUuid[로그인 회원 idx}
     * @return
     */
    int getContentsHideCnt(SearchDto searchDto);

    /**
     * 신고한 컨텐츠인지 조회
     *
     * @param searchDto
     * @return
     */
    int getContentsReportCnt(SearchDto searchDto);

    /**
     * 내가 작성한 컨텐츠 상세
     *
     * @param searchDto
     * @return
     */
    ContentsDto getMyContentsDetail(SearchDto searchDto);

    /**
     * 컨텐츠 keep 조회
     *
     * @param contentsIdx
     * @return
     */
    int getIsKeepByIdx(Long contentsIdx);

    /**
     * 컨텐츠 is_view 조회
     *
     * @param contentsIdx
     * @return
     */
    int getIsViewByIdx(Long contentsIdx);

    /**
     * 내가 작성한 컨텐츠 개수
     *
     * @param searchDto
     * @return
     */
    int getTotalWrittenByMeCnt(SearchDto searchDto);

    /**
     * 내가 작성한 컨텐츠 리스트 [일상글]
     *
     * @param searchDto
     * @return
     */
    List<ContentsDto> getWrittenByMeList(SearchDto searchDto);

    /**
     * 최신 컨텐츠 개수
     *
     * @param searchDto
     * @return
     */
    int getTotalRecentCnt(SearchDto searchDto);

    /**
     * 최신 컨텐츠 리스트
     *
     * @param searchDto
     * @return
     */
    List<ContentsDto> getRecentList(SearchDto searchDto);

    /**
     * 컨텐츠 idx 조회
     *
     * @param commentIdx 댓글 idx
     * @return
     */
    Long getContentsIdxByCommentIdx(Long commentIdx);

    /**
     * 컨텐츠 info 조회
     *
     * @param contentsIdx contentsIdx
     * @return
     */
    ContentsDto getContentsInfo(Long contentsIdx);
}
