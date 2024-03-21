package com.architecture.admin.services.login;

import com.architecture.admin.libraries.exception.CustomError;
import com.architecture.admin.libraries.exception.CustomException;
import com.architecture.admin.models.dao.follow.FollowDao;
import com.architecture.admin.models.daosub.follow.FollowDaoSub;
import com.architecture.admin.models.dto.follow.FollowDto;
import com.architecture.admin.services.BaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;


/*****************************************************
 * 회원 가입 모델러
 ****************************************************/
@RequiredArgsConstructor
@Service
@Transactional
public class JoinService extends BaseService {

    private final FollowDao followDao;
    private final FollowDaoSub followDaoSub;


    /*****************************************************
     *  Modules
     ****************************************************/
    /**
     * 회원 가입
     * [sns_member_follow_cnt] 등록
     *
     * @param memberUuid
     * @return
     */
    @Transactional
    public boolean regist(String memberUuid) {
        boolean result = true;

        if (ObjectUtils.isEmpty(memberUuid)) {
            throw new CustomException(CustomError.MEMBER_UUID_EMPTY);
        }
        // 팔로우 cnt 테이블에 해당 memberUuid 있는지 체크
        boolean isExist = checkDupleFollowCnt(memberUuid);

        if (isExist == true) {
            result = false;
        }
        // 팔로우 cnt 테이블 등록
        insertFollowCnt(memberUuid);

        return result;
    }

    /*****************************************************
     *  SubFunction - Select
     ****************************************************/
    /**
     * 팔로우 cnt 테이블에 memberUuid 중복 체크
     *
     * @param memberUuid
     * @return
     */
    private boolean checkDupleFollowCnt(String memberUuid) {

        FollowDto followDto = FollowDto.builder()
                .followUuid(memberUuid).build();

        int followCnt = followDaoSub.getFollowerCntCheck(followDto);

        return followCnt > 0;
    }

    /*****************************************************
     *  SubFunction - Insert
     ****************************************************/
    /**
     * 회원 팔로우 cnt 등록
     *
     * @param memberUuid
     */
    public void insertFollowCnt(String memberUuid) {
        FollowDto followDto = new FollowDto();
        followDto.setMemberUuid(memberUuid);
        followDao.insertFollowCnt(followDto);
    }

    /*****************************************************
     *  SubFunction - Update
     ****************************************************/

    /*****************************************************
     *  SubFunction - Delete
     ****************************************************/

}
