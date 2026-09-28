package com.yuetin.music.service;

import com.yuetin.music.common.BizException;
import com.yuetin.music.common.CurrentUser;
import com.yuetin.music.common.UserContext;
import com.yuetin.music.entity.Playlist;
import com.yuetin.music.entity.Song;
import com.yuetin.music.mapper.PlaylistMapper;
import com.yuetin.music.mapper.PlaylistSongMapper;
import com.yuetin.music.mapper.SongMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistMapper playlistMapper;
    private final PlaylistSongMapper playlistSongMapper;
    private final SongMapper songMapper;

    public List<Playlist> list(String keyword, String style) {
        return playlistMapper.search(blank(keyword), blank(style));
    }

    /** 「我的歌单」：同时返回我创建的与我收藏的。 */
    public Map<String, Object> mine() {
        Long uid = requireLogin();
        Map<String, Object> data = new HashMap<>();
        data.put("created", playlistMapper.findByCreator(uid));
        data.put("favorited", playlistMapper.findFavorited(uid));
        return data;
    }

    /** 歌单详情：直接返回歌单对象（含 creatorName / songCount），字段与 mock 层一致。 */
    public Playlist detail(Long id) {
        Playlist pl = playlistMapper.findById(id);
        if (pl == null) throw BizException.notFound("歌单不存在");
        return pl;
    }

    /** 当前用户是否已收藏该歌单；歌单详情页按需调用。 */
    public Map<String, Object> favoriteState(Long id) {
        Long uid = requireLogin();
        if (playlistMapper.findById(id) == null) throw BizException.notFound("歌单不存在");
        Map<String, Object> data = new HashMap<>();
        data.put("favorited", playlistMapper.isFavorited(uid, id));
        return data;
    }

    public Playlist create(Map<String, Object> body) {
        Long uid = requireLogin();
        String name = str(body.get("name"));
        if (name == null) throw BizException.badRequest("请输入歌单名");

        Playlist pl = new Playlist();
        pl.setName(name);
        pl.setIntro(str(body.get("intro")));
        pl.setStyle(str(body.get("style")));
        pl.setCover(str(body.get("cover")));
        if (pl.getCover() == null) {
            // 未指定封面时用种子图生成一张，保证歌单卡片不出现空图
            pl.setCover("https://picsum.photos/seed/yuetin-" + name.hashCode() + "/400");
        }
        pl.setCreatorId(uid);
        pl.setIsPublic(body.get("isPublic") == null ? 1 : Integer.valueOf(String.valueOf(body.get("isPublic"))));
        playlistMapper.insert(pl);
        return pl;
    }

    public Playlist update(Long id, Map<String, Object> body) {
        Long uid = requireLogin();
        Playlist existing = playlistMapper.findById(id);
        if (existing == null) throw BizException.notFound("歌单不存在");
        if (!canManage(existing)) throw BizException.forbidden("没有权限修改该歌单");

        existing.setName(str(body.get("name")));
        existing.setIntro(str(body.get("intro")));
        existing.setStyle(str(body.get("style")));
        existing.setCover(str(body.get("cover")));
        playlistMapper.update(existing);
        return existing;
    }

    @Transactional
    public void delete(Long id) {
        Long uid = requireLogin();
        Playlist existing = playlistMapper.findById(id);
        if (existing == null) throw BizException.notFound("歌单不存在");
        if (!canManage(existing)) throw BizException.forbidden("没有权限删除该歌单");
        // playlist_song 由外键 ON DELETE CASCADE 连带清理
        playlistMapper.deleteById(id);
    }

    /** 收藏 / 取消收藏，返回切换后的状态。 */
    public Map<String, Object> toggleFavorite(Long id) {
        Long uid = requireLogin();
        if (playlistMapper.findById(id) == null) throw BizException.notFound("歌单不存在");

        boolean favorited;
        if (playlistMapper.isFavorited(uid, id)) {
            playlistMapper.removeFavorite(uid, id);
            favorited = false;
        } else {
            playlistMapper.addFavorite(uid, id);
            favorited = true;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("favorited", favorited);
        return data;
    }

    public List<Song> songsOf(Long playlistId) {
        List<Long> ids = playlistSongMapper.findSongIds(playlistId);
        if (ids.isEmpty()) return new ArrayList<>();
        return songMapper.findByIdsInOrder(ids);
    }

    /** 向歌单添加歌曲；重复添加静默跳过并返回当前歌单曲目。 */
    @Transactional
    public List<Song> addSong(Long playlistId, Map<String, Object> body) {
        Long uid = requireLogin();
        Playlist pl = playlistMapper.findById(playlistId);
        if (pl == null) throw BizException.notFound("歌单不存在");
        if (!canManage(pl)) throw BizException.forbidden("没有权限修改该歌单");

        Long songId = Long.valueOf(String.valueOf(body.get("songId")));
        if (songMapper.findById(songId) == null) throw BizException.notFound("歌曲不存在");

        int next = playlistSongMapper.maxSortNo(playlistId) + 1;
        playlistSongMapper.add(playlistId, songId, next);
        return songsOf(playlistId);
    }

    @Transactional
    public List<Song> removeSong(Long playlistId, Long songId) {
        Long uid = requireLogin();
        Playlist pl = playlistMapper.findById(playlistId);
        if (pl == null) throw BizException.notFound("歌单不存在");
        if (!canManage(pl)) throw BizException.forbidden("没有权限修改该歌单");

        playlistSongMapper.remove(playlistId, songId);
        return songsOf(playlistId);
    }

    /**
     * 单曲上移 / 下移：direction 为 -1 或 1。
     *
     * <p>与前端播放列表抽屉的「上移一位 / 下移一位」操作对应；
     * 相邻两首互换 sort_no，其余歌曲的顺序保持不变。</p>
     */
    @Transactional
    public List<Song> moveSong(Long playlistId, Map<String, Object> body) {
        requireLogin();
        Playlist pl = playlistMapper.findById(playlistId);
        if (pl == null) throw BizException.notFound("歌单不存在");
        if (!canManage(pl)) throw BizException.forbidden("没有权限修改该歌单");

        Long songId = Long.valueOf(String.valueOf(body.get("songId")));
        int direction = Integer.parseInt(String.valueOf(body.get("direction")));
        if (direction != -1 && direction != 1) {
            throw BizException.badRequest("direction 只能是 -1（上移）或 1（下移）");
        }

        List<Long> ids = new ArrayList<>(playlistSongMapper.findSongIds(playlistId));
        int index = ids.indexOf(songId);
        if (index < 0) throw BizException.notFound("该歌曲不在歌单中");

        int target = index + direction;
        if (target < 0 || target >= ids.size()) {
            return songMapper.findByIdsInOrder(ids); // 已在边界，原样返回
        }
        ids.remove(index);
        ids.add(target, songId);

        playlistSongMapper.deleteByPlaylist(playlistId);
        playlistSongMapper.reorder(playlistId, ids);
        return songMapper.findByIdsInOrder(ids);
    }

    /** 与 mock 层 canManagePlaylist 保持一致：管理员或创建者本人。 */
    private boolean canManage(Playlist pl) {
        CurrentUser user = UserContext.get();
        return user != null && (user.isAdmin() || pl.getCreatorId() != null
                && pl.getCreatorId().equals(user.getId()));
    }

    private Long requireLogin() {
        Long uid = UserContext.id();
        if (uid == null) throw BizException.unauthorized("登录已过期，请重新登录");
        return uid;
    }

    private String blank(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private String str(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }
}
