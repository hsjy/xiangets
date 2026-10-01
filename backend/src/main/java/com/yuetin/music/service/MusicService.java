package com.yuetin.music.service;

import com.yuetin.music.common.BizException;
import com.yuetin.music.common.UserContext;
import com.yuetin.music.entity.Singer;
import com.yuetin.music.entity.Song;
import com.yuetin.music.mapper.SingerMapper;
import com.yuetin.music.mapper.SongLikeMapper;
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
public class MusicService {

    private final SongMapper songMapper;
    private final SingerMapper singerMapper;
    private final SongLikeMapper songLikeMapper;

    public List<Song> listSongs(String keyword, String style, Long singerId, Long poemId, String sort) {
        return songMapper.search(blank(keyword), blank(style), singerId, poemId, sort);
    }

    public List<Song> hotSongs(int limit) {
        return songMapper.findHot(limit <= 0 ? 10 : limit);
    }

    public Song songDetail(Long id) {
        Song song = songMapper.findById(id);
        if (song == null) throw BizException.notFound("歌曲不存在");
        return song;
    }

    /** 播放：自增播放量并回传最新计数，前端即时更新列表展示。 */
    @Transactional
    public Map<String, Object> play(Long id) {
        Song song = songDetail(id);
        songMapper.increasePlayCount(id);
        song.setPlayCount(song.getPlayCount() + 1);

        Map<String, Object> data = new HashMap<>();
        data.put("song", song);
        data.put("playCount", song.getPlayCount());
        return data;
    }

    public List<Singer> listSingers(String keyword) {
        return singerMapper.search(blank(keyword));
    }

    public Singer singerDetail(Long id) {
        Singer singer = singerMapper.findById(id);
        if (singer == null) throw BizException.notFound("歌手不存在");
        return singer;
    }

    /** 红心切换：已喜欢则取消，否则加入，返回切换后的状态供前端更新按钮。 */
    public Map<String, Object> toggleLike(Long songId) {
        Long uid = requireLogin();
        songDetail(songId);
        boolean liked;
        if (songLikeMapper.exists(uid, songId)) {
            songLikeMapper.remove(uid, songId);
            liked = false;
        } else {
            songLikeMapper.add(uid, songId);
            liked = true;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("liked", liked);
        return data;
    }

    public Map<String, Object> likeState(Long songId) {
        Long uid = UserContext.id();
        // 允许匿名查询：未登录时一律返回未喜欢，由前端降级为「登录后可用」
        boolean liked = uid != null && songLikeMapper.exists(uid, songId);
        Map<String, Object> data = new HashMap<>();
        data.put("liked", liked);
        return data;
    }

    /** 我喜欢的音乐：按喜欢时间倒序返回完整歌曲对象。 */
    public List<Song> likedSongs() {
        Long uid = requireLogin();
        List<Long> ids = songLikeMapper.findSongIds(uid);
        if (ids.isEmpty()) return new ArrayList<>();
        return songMapper.findByIdsInOrder(ids);
    }

    private Long requireLogin() {
        Long uid = UserContext.id();
        if (uid == null) throw BizException.unauthorized("登录已过期，请重新登录");
        return uid;
    }

    private String blank(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
}
