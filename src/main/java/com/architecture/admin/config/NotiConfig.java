package com.architecture.admin.config;

import java.util.HashMap;
import java.util.Map;

public class NotiConfig {
    public static Map<String, String> getNotiBody() {

        Map<String, String> mNotiBody = new HashMap<>();

        mNotiBody.put("follow", "님이 나를 팔로우하기 시작했습니다.");
        mNotiBody.put("new_contents", "님이 새 게시물을 올렸습니다.");
        mNotiBody.put("mention_contents", "님이 게시물에서 나를 멘션했습니다.");
        mNotiBody.put("img_tag", "님이 게시물에서 나를 태그했습니다.");
        mNotiBody.put("like_contents", "님이 내 게시물을 좋아합니다.");
        mNotiBody.put("new_comment", "님이 댓글을 달았습니다.");
        mNotiBody.put("mention_comment", "님이 댓글에서 나를 멘션했습니다.");
        mNotiBody.put("like_comment", "님이 내 댓글을 좋아합니다.");
        mNotiBody.put("chatting", "");
        mNotiBody.put("new_chatting", "채팅방이 개설 되었습니다.");

        return mNotiBody;
    }

    public static Map<String, String> getNotiTitle() {

        Map<String, String> mNotiTitle = new HashMap<>();

        mNotiTitle.put("follow", "새로운 팔로우");
        mNotiTitle.put("new_contents", "새 글");
        mNotiTitle.put("mention_contents", "멘션");
        mNotiTitle.put("img_tag", "태그");
        mNotiTitle.put("like_contents", "좋아요");
        mNotiTitle.put("new_comment", "댓글");
        mNotiTitle.put("mention_comment", "멘션");
        mNotiTitle.put("like_comment", "좋아요");
        mNotiTitle.put("chatting", null);

        return mNotiTitle;
    }
    
    
}
