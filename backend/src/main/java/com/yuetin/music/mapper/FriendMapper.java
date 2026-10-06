package com.yuetin.music.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface FriendMapper {

    /** 发送加友请求 */
    int insertRequest(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /** 检查是否已存在（任一方向） */
    Map<String, Object> findPair(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /** 接受/拒绝（仅被邀请方操作） */
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("userId") Long userId);

    /** 我的好友（accepted，双向） */
    List<Map<String, Object>> listFriends(@Param("userId") Long userId);

    /** 收到的待处理请求 */
    List<Map<String, Object>> listIncoming(@Param("userId") Long userId);

    /** 发出的待处理请求 */
    List<Map<String, Object>> listOutgoing(@Param("userId") Long userId);

    /** 删除好友关系（双向） */
    int deletePair(@Param("id") Long id);

    /** 好友间消息（按时间升序） */
    List<Map<String, Object>> listMessages(@Param("senderId") Long senderId,
                                           @Param("receiverId") Long receiverId);

    /** 发消息 */
    int insertMessage(@Param("senderId") Long senderId,
                      @Param("receiverId") Long receiverId,
                      @Param("content") String content);

    /** 标记已读 */
    int markRead(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    /** 未读数 */
    int unreadCount(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId);

    /** 管理员：全部好友关系 */
    List<Map<String, Object>> adminAllFriendships();

    /** 管理员：全部消息 */
    List<Map<String, Object>> adminAllMessages(@Param("limit") int limit);
}
