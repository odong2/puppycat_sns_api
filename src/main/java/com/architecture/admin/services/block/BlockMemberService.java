package com.architecture.admin.services.block;

import com.architecture.admin.libraries.PaginationLibray;
import com.architecture.admin.libraries.exception.CurlException;
import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.block.BlockMemberDao;
import com.architecture.admin.models.daosub.block.BlockMemberDaoSub;
import com.architecture.admin.models.dto.SearchDto;
import com.architecture.admin.models.dto.block.BlockMemberDto;
import com.architecture.admin.models.dto.follow.FollowDto;
import com.architecture.admin.models.dto.member.MemberDto;
import com.architecture.admin.models.dto.member.MemberInfoDto;
import com.architecture.admin.services.BaseService;
import com.architecture.admin.services.follow.FollowService;
import com.architecture.admin.services.follow.FollowerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class BlockMemberService extends BaseService {

    private final BlockMemberDao blockMemberDao;
    private final BlockMemberDaoSub blockMemberDaoSub;
    private final FollowService followService;
    private final FollowerService followerService;

    /*****************************************************
     *  Modules
     ****************************************************/
    /**
     * 차단 리스트 가져오기
     *
     * @param token     token
     * @param searchDto 검색조건
     * @return 차단 리스트
     */
    public List<MemberInfoDto> getBlockList(String token, SearchDto searchDto) throws JsonProcessingException {
        List<MemberInfoDto> lGetBlockList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);
        searchDto.setMemberUuid(memberDto.getUuid());

        // 검색어가 있다면 일치하는 uuid 리스트 조회
        String searchWord = searchDto.getSearchWord();
        if (!ObjectUtils.isEmpty(searchWord)) {

            // 닉네임으로 회원 uuid 리스트 받아오기
            String jsonString = memberCurlService.getMemberUuidBySearchDto(searchWord);

            JSONObject jsonObject = new JSONObject(jsonString);
            if (!(jsonObject.getBoolean("result"))) {
                throw new CurlException(jsonObject);
            }
            JsonObject listJson = (JsonObject) JsonParser.parseString(jsonObject.get("data").toString()); // data 안 list 파싱
            List<String> searchMemberUuidList = mapper.readValue(listJson.get("searchMemberUuidList").toString(), new TypeReference<>() {
            }); // 닉네임에 해당하는 uuid 리스트

            // 조회된 회원이 없다면 바로 빈 리스트 리턴
            if (searchMemberUuidList.isEmpty()) {
                return lGetBlockList;
            }

            searchDto.setMemberUuidList(searchMemberUuidList);
        }

        // 목록 전체 count
        int iTotalCount = blockMemberDaoSub.iGetTotalBlockCount(searchDto);

        if (iTotalCount > 0) {
            // paging
            PaginationLibray pagination = new PaginationLibray(iTotalCount, searchDto);
            searchDto.setPagination(pagination);

            // list
            List<String> blockUuidList = blockMemberDaoSub.lGetBlockList(searchDto);

            // curl 회원 조회
            String jsonString = memberCurlService.getMemberInfoByUuidList(blockUuidList);

            JSONObject jsonObject = new JSONObject(jsonString);
            if (!(jsonObject.getBoolean("result"))) {
                throw new CurlException(jsonObject);
            }

            JsonObject listJson = (JsonObject) JsonParser.parseString(jsonObject.get("data").toString()); // data 안 list 파싱
            lGetBlockList = mapper.readValue(listJson.get("memberInfoList").toString(), new TypeReference<>() {
            }); // 회원 정보 list

        }

        // 검색 시 사용한 값 null 처리
        searchDto.setMemberUuidList(null);
        return lGetBlockList;
    }

    /**
     * 회원 차단
     *
     * @param token          access token
     * @param blockMemberDto blockUuid
     */
    @Transactional
    public MemberDto blockMember(String token, BlockMemberDto blockMemberDto) {

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);
        blockMemberDto.setMemberUuid(memberDto.getUuid());

        // validate
        validate(blockMemberDto);

        // 등록일 set
        blockMemberDto.setRegDate(dateLibrary.getDatetime());

        // 차단 내역 가져오기
        BlockMemberDto oTargetInfo = getTargetInfo(blockMemberDto);

        // 이미 차단되어 있는 경우
        if (oTargetInfo != null && oTargetInfo.getState() == 1) {
            throw new CustomException(CustomError.BLOCK_MEMBER_STATE_BLOCK); // 이미 차단되었습니다.
        }

        // 차단 해제 후 다시 차단한 경우
        if (oTargetInfo != null && oTargetInfo.getState() == 0) {
            // 차단 상태 업데이트
            updateBlockState(blockMemberDto);

        } else { // 차단한적 없는 경우
            // 회원 차단 등록
            insertBlockMember(blockMemberDto);
        }

        // 팔로우/팔로워 해제
        unfollow(blockMemberDto);

        // 닉네임 조회
        MemberDto memberNickInfo = memberCurlService.getMemberNickInfoByUuid(blockMemberDto.getBlockUuid());
        if (Boolean.FALSE.equals(memberNickInfo.getResult())) {
            throw new CustomException(CustomError.BLOCK_MEMBER_UUID_ERROR); // 차단 대상 UUID가 유효하지 않습니다.
        }

        // 차단된 회원의 닉네임 반환
        return memberNickInfo;
    }

    /**
     * 차단 해제
     *
     * @param blockMemberDto blockUuid
     */
    @Transactional
    public MemberDto unblockMember(String token, BlockMemberDto blockMemberDto) {

        // 회원 UUID 조회 & 세팅
        MemberDto memberDto = super.getMemberUuidByToken(token);
        blockMemberDto.setMemberUuid(memberDto.getUuid());

        // validate
        validate(blockMemberDto);

        // 차단 해제 내역 가져오기
        BlockMemberDto oTargetInfo = getTargetInfo(blockMemberDto);

        // 차단한 적 없는데 차단 해제하려고 하면
        if (oTargetInfo == null) {
            throw new CustomException(CustomError.BLOCK_MEMBER_DATA_ERROR);
        }

        // 이미 차단 해제되어 있다면
        if (oTargetInfo.getState() == 0) {
            throw new CustomException(CustomError.BLOCK_MEMBER_STATE_UNBLOCK);
        }

        // 등록일 set
        blockMemberDto.setRegDate(dateLibrary.getDatetime());

        // 차단 insert 된 적이 있고 차단되어 있다면
        updateUnblockState(blockMemberDto);

        // 닉네임 조회
        MemberDto memberNickInfo = memberCurlService.getMemberNickInfoByUuid(blockMemberDto.getBlockUuid());
        if (Boolean.FALSE.equals(memberNickInfo.getResult())) {
            throw new CustomException(CustomError.BLOCK_MEMBER_UUID_ERROR); // 차단 대상 UUID가 유효하지 않습니다.
        }

        // 차단된 회원의 닉네임 반환
        return memberNickInfo;
    }

    /*****************************************************
     *  Validate
     ****************************************************/
    /**
     * 차단 유효성 검사
     *
     * @param blockMemberDto memberUuid, blockUuid
     */
    public void validate(BlockMemberDto blockMemberDto) {

        String blockUuid = blockMemberDto.getBlockUuid();   // 차단할 회원 uuid
        String memberUuid = blockMemberDto.getMemberUuid(); // 로그인 회원 uuid

        if (ObjectUtils.isEmpty(blockUuid)) {
            throw new CustomException(CustomError.BLOCK_MEMBER_UUID_EMPTY);
        }
        if (ObjectUtils.isEmpty(memberUuid)) {
            throw new CustomException(CustomError.MEMBER_UUID_EMPTY);
        }
        if (blockUuid.equals(memberUuid)) {
            throw new CustomException(CustomError.BLOCK_MEMBER_SAME_TARGET);
        }
        // 차단할 회원 uuid 정상인지 curl 통신
        Boolean isExist = super.getCheckMemberByUuid(blockUuid);

        if (isExist == false) {
            // 회원 UUID가 유효하지 않습니다.
            throw new CustomException(CustomError.MEMBER_UUID_ERROR);
        }

    }

    /*****************************************************
     *  Select
     ****************************************************/
    /**
     * 차단 내역 가져오기
     *
     * @param blockMemberDto memberUuid blockUuid
     * @return BlockMemberDto
     */
    public BlockMemberDto getTargetInfo(BlockMemberDto blockMemberDto) {
        return blockMemberDaoSub.oGetTargetInfo(blockMemberDto);
    }

    /*****************************************************
     *  Insert
     ****************************************************/
    /**
     * 회원 차단
     *
     * @param blockMemberDto memberUuid, blockUuid
     */
    public void insertBlockMember(BlockMemberDto blockMemberDto) {
        Integer iResult = blockMemberDao.insertBlockMember(blockMemberDto);
        if (iResult != 1) {
            throw new CustomException(CustomError.BLOCK_MEMBER_ERROR); // 차단 실패하였습니다.
        }
    }


    /*****************************************************
     *  Update
     ****************************************************/
    /**
     * 차단(1)로 상태값 변경
     *
     * @param blockMemberDto memberUuid, blockUuid
     */
    public void updateBlockState(BlockMemberDto blockMemberDto) {
        blockMemberDto.setState(1);
        Integer iResult = blockMemberDao.updateBlockState(blockMemberDto);
        if (iResult != 1) {
            throw new CustomException(CustomError.BLOCK_MEMBER_ERROR);  // 차단 실패하였습니다.
        }
    }

    /**
     * 차단해제(0)로 상태값 변경
     *
     * @param blockMemberDto memberUuid, blockUuid
     */
    public void updateUnblockState(BlockMemberDto blockMemberDto) {
        blockMemberDto.setState(0);
        Integer iResult = blockMemberDao.updateBlockState(blockMemberDto);
        if (iResult != 1) {
            throw new CustomException(CustomError.UNBLOCK_MEMBER_ERROR); // 차단 해제 실패하였습니다.
        }
    }

    /*****************************************************
     *  Delete
     ****************************************************/
    /**
     * 팔로우/팔로워 해제
     *
     * @param blockMemberDto memberUuid, blockUuid
     */
    public void unfollow(BlockMemberDto blockMemberDto) {
        FollowDto followDto = new FollowDto();

        // 팔로우 되어 있다면 해제
        followDto.setMemberUuid(blockMemberDto.getMemberUuid());
        followDto.setFollowUuid(blockMemberDto.getBlockUuid());
        Boolean bFollow = followService.checkFollow(followDto);
        if (Boolean.TRUE.equals(bFollow)) {
            // 언팔로우
            followService.modifyUnFollow(followDto);
            // 카운트 테이블 업데이트
            followService.removeFollowCnt(followDto);
        }

        // 팔로워 되어 있다면 해제
        followDto.setMemberUuid(blockMemberDto.getBlockUuid());
        followDto.setFollowUuid(blockMemberDto.getMemberUuid());
        Boolean bFollower = followService.checkFollow(followDto);
        if (Boolean.TRUE.equals(bFollower)) {
            // 언팔로워
            followerService.modifyUnFollow(followDto);
            // 카운트 테이블 업데이트
            followerService.removeFollowerCnt(followDto);
        }
    }

    /******************************************************
     * Validation
     *****************************************************/

    /**
     * 작성자 &* 로그인 회원 차단 내역 유무
     *
     * @param writerUuid      : 작성자 uuid
     * @param loginMemberUuid : 로그인 uuid
     */
    public void writerAndMemberBlockValidate(String writerUuid, String loginMemberUuid) {

        if (!writerUuid.equals(loginMemberUuid)) { // 내가 작성한 컨텐츠 또는 댓글이 아닌 경우
            Boolean isBlock = super.bChkBlock(writerUuid, loginMemberUuid);

            // 차단 내역이 존재
            if (Boolean.TRUE.equals(isBlock)) {
                throw new CustomException(CustomError.BLOCK_MEMBER_EXIST); // 차단 내역이 존재합니다.
            }
        }
    }

}
