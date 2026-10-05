package com.yuetin.music.service;

import com.yuetin.music.common.BizException;
import com.yuetin.music.common.UserContext;
import com.yuetin.music.entity.Comment;
import com.yuetin.music.mapper.CommentMapper;
import com.yuetin.music.mapper.SongMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final SongMapper songMapper;

    public List<Comment> list(Long songId) {
        return commentMapper.findBySong(songId);
    }

    public Comment add(Long songId, String content) {
        Long uid = requireLogin();
        if (content == null || content.trim().isEmpty()) {
            throw BizException.badRequest("评论内容不能为空");
        }
        if (content.length() > 500) {
            throw BizException.badRequest("评论最多 500 字");
        }
        if (songMapper.findById(songId) == null) {
            throw BizException.notFound("歌曲不存在");
        }

        Comment c = new Comment();
        c.setSongId(songId);
        c.setUserId(uid);
        c.setContent(content.trim());
        commentMapper.insert(c);
        // 回填展示字段，前端无需再请求一次
        c.setUsername(currentName());
        return c;
    }

    public void delete(Long id) {
        Long uid = requireLogin();
        // 管理员可删除任意感悟；普通用户只能删自己的
        if (isAdmin()) {
            commentMapper.deleteById(id);
            return;
        }
        int rows = commentMapper.deleteByIdAndUser(id, uid);
        if (rows == 0) {
            throw BizException.forbidden("只能删除自己的感悟");
        }
    }

    /** 全站感悟（管理后台用）：带用户名与诗题。 */
    public List<Map<String, Object>> allForAdmin() {
        if (!isAdmin()) throw BizException.forbidden("需要管理员身份");
        return commentMapper.findAllWithMeta();
    }

    private boolean isAdmin() {
        var cu = UserContext.get();
        return cu != null && cu.isAdmin();
    }

    private String currentName() {
        return UserContext.get() == null ? null : UserContext.get().getUsername();
    }

    private Long requireLogin() {
        Long uid = UserContext.id();
        if (uid == null) throw BizException.unauthorized("登录已过期，请重新登录");
        return uid;
    }
}
