package com.yuetin.music.service;

import com.yuetin.music.common.BizException;
import com.yuetin.music.common.UserContext;
import com.yuetin.music.mapper.FriendMapper;
import com.yuetin.music.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 好友：加友请求、同意/拒绝、好友列表、私信。 */
@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendMapper friendMapper;
    private final UserMapper userMapper;

    private Long uid() {
        Long id = UserContext.id();
        if (id == null) throw BizException.unauthorized("请先登录");
        return id;
    }

    /** 按用户名搜索（可加好友）。 */
    public List<Map<String, Object>> searchUsers(String keyword) {
        Long me = uid();
        if (keyword == null || keyword.isBlank()) return List.of();
        var users = userMapper.search(keyword.trim());
        return users.stream()
                .filter(u -> !u.getId().equals(me))
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", u.getId());
                    m.put("username", u.getUsername());
                    m.put("name", u.getName() != null ? u.getName() : u.getUsername());
                    var pair = friendMapper.findPair(me, u.getId());
                    m.put("relation", pair == null ? "none" : String.valueOf(pair.get("status")));
                    return m;
                })
                .toList();
    }

    /** 发送加友请求。 */
    public Map<String, Object> sendRequest(String username) {
        Long me = uid();
        if (username == null || username.isBlank()) throw BizException.badRequest("请输入用户名");
        var target = userMapper.findByUsername(username.trim());
        if (target == null) throw BizException.notFound("用户不存在");
        if (target.getId().equals(me)) throw BizException.badRequest("不能加自己为好友");
        var existing = friendMapper.findPair(me, target.getId());
        if (existing != null) {
            String st = String.valueOf(existing.get("status"));
            if ("accepted".equals(st)) throw BizException.badRequest("你们已经是好友了");
            throw BizException.badRequest("已有待处理的好友请求");
        }
        friendMapper.insertRequest(me, target.getId());
        return Map.of("ok", true, "targetName", target.getName() != null ? target.getName() : username.trim());
    }

    /** 同意/拒绝（仅被邀请方）。 */
    public Map<String, Object> respond(Long relId, boolean accept) {
        Long me = uid();
        int rows = friendMapper.updateStatus(relId, accept ? "accepted" : "rejected", me);
        if (rows == 0) throw BizException.badRequest("请求不存在或已处理");
        return Map.of("ok", true);
    }

    /** 删除好友。 */
    public Map<String, Object> removeFriend(Long relId) {
        Long me = uid();
        // 确认这条关系是与我相关的
        var pair = friendMapper.adminAllFriendships().stream()
                .filter(m -> Long.valueOf(String.valueOf(m.get("id"))).equals(relId))
                .findFirst().orElse(null);
        if (pair == null) throw BizException.notFound("好友关系不存在");
        Long a = Long.valueOf(String.valueOf(pair.get("userId")));
        Long b = Long.valueOf(String.valueOf(pair.get("friendId")));
        if (!a.equals(me) && !b.equals(me)) throw BizException.forbidden("只能删除自己的好友关系");
        friendMapper.deletePair(relId);
        return Map.of("ok", true);
    }

    /** 我的好友。 */
    public List<Map<String, Object>> friends() {
        Long me = uid();
        List<Map<String, Object>> list = friendMapper.listFriends(me);
        for (Map<String, Object> f : list) {
            f.put("unread", friendMapper.unreadCount(me, Long.valueOf(String.valueOf(f.get("friendId")))));
        }
        return list;
    }

    /** 收到的请求。 */
    public List<Map<String, Object>> incoming() {
        return friendMapper.listIncoming(uid());
    }

    /** 发出的请求。 */
    public List<Map<String, Object>> outgoing() {
        return friendMapper.listOutgoing(uid());
    }

    /** 与某好友的消息（并标记已读）。 */
    public Map<String, Object> messages(Long friendId) {
        Long me = uid();
        List<Map<String, Object>> list = friendMapper.listMessages(me, friendId);
        friendMapper.markRead(friendId, me);
        int unread = friendMapper.unreadCount(me, friendId);
        return Map.of("messages", list, "unread", unread);
    }

    /** 发消息。 */
    public Map<String, Object> sendMessage(Long friendId, String content) {
        Long me = uid();
        if (content == null || content.isBlank()) throw BizException.badRequest("消息不能为空");
        if (content.length() > 500) throw BizException.badRequest("消息最多 500 字");
        // 确认是好友
        var pair = friendMapper.findPair(me, friendId);
        if (pair == null || !"accepted".equals(String.valueOf(pair.get("status")))) {
            throw BizException.badRequest("你们还不是好友");
        }
        friendMapper.insertMessage(me, friendId, content.trim());
        return Map.of("ok", true);
    }

    /** 管理员：全部好友关系。 */
    public List<Map<String, Object>> adminFriendships() {
        requireAdmin();
        return friendMapper.adminAllFriendships();
    }

    /** 管理员：全部消息。 */
    public List<Map<String, Object>> adminMessages(int limit) {
        requireAdmin();
        return friendMapper.adminAllMessages(Math.min(500, Math.max(1, limit)));
    }

    private void requireAdmin() {
        var cu = UserContext.get();
        if (cu == null) throw BizException.unauthorized("请先登录");
        if (!cu.isAdmin()) throw BizException.forbidden("需要管理员身份");
    }
}
