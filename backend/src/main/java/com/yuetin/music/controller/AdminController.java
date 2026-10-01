package com.yuetin.music.controller;

import com.yuetin.music.common.BizException;
import com.yuetin.music.common.CurrentUser;
import com.yuetin.music.common.Result;
import com.yuetin.music.common.UserContext;
import com.yuetin.music.entity.Singer;
import com.yuetin.music.entity.Song;
import com.yuetin.music.mapper.SingerMapper;
import com.yuetin.music.mapper.SongMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 曲库管理接口：新增 / 修改 / 删除歌手与歌曲，仅管理员可用。
 *
 * <p>前端 {@code src/api/index.js} 已声明这些接口，供后台管理页面调用；
 * 普通用户调用会收到 403。</p>
 */
@RestController
@RequiredArgsConstructor
public class AdminController {

    private final SingerMapper singerMapper;
    private final SongMapper songMapper;

    /* ---------------- 歌手 ---------------- */

    @PostMapping("/singer/add")
    public Result<Singer> addSinger(@RequestBody Map<String, Object> body) {
        requireAdmin();
        String name = str(body.get("name"));
        if (name == null) throw BizException.badRequest("请输入歌手名");

        Singer s = new Singer();
        s.setName(name);
        s.setGender(intOf(body.get("gender"), 0));
        s.setAvatar(str(body.get("avatar")));
        s.setStyle(str(body.get("style")));
        s.setLocation(str(body.get("location")));
        s.setBirth(str(body.get("birth")));
        s.setIntro(str(body.get("intro")));
        s.setSongCount(0);
        singerMapper.insert(s);
        return Result.ok(s);
    }

    @PutMapping("/singer/update/{id}")
    public Result<Singer> updateSinger(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requireAdmin();
        Singer s = singerMapper.findById(id);
        if (s == null) throw BizException.notFound("歌手不存在");

        if (body.get("name") != null) s.setName(str(body.get("name")));
        if (body.get("gender") != null) s.setGender(intOf(body.get("gender"), s.getGender()));
        if (body.get("avatar") != null) s.setAvatar(str(body.get("avatar")));
        if (body.get("style") != null) s.setStyle(str(body.get("style")));
        if (body.get("location") != null) s.setLocation(str(body.get("location")));
        if (body.get("birth") != null) s.setBirth(str(body.get("birth")));
        if (body.get("intro") != null) s.setIntro(str(body.get("intro")));
        singerMapper.update(s);
        return Result.ok(s);
    }

    /** 删除歌手：其名下歌曲由外键 ON DELETE CASCADE 一并清理。 */
    @DeleteMapping("/singer/delete/{id}")
    @Transactional
    public Result<Void> deleteSinger(@PathVariable Long id) {
        requireAdmin();
        if (singerMapper.findById(id) == null) throw BizException.notFound("歌手不存在");
        singerMapper.deleteById(id);
        return Result.ok();
    }

    /* ---------------- 歌曲 ---------------- */

    @PostMapping("/song/add")
    public Result<Song> addSong(@RequestBody Map<String, Object> body) {
        requireAdmin();
        String name = str(body.get("name"));
        Long singerId = body.get("singerId") == null ? null : Long.valueOf(String.valueOf(body.get("singerId")));
        String url = str(body.get("url"));
        if (name == null) throw BizException.badRequest("请输入歌曲名");
        if (singerId == null) throw BizException.badRequest("请选择所属歌手");
        if (url == null) throw BizException.badRequest("请输入音频地址");
        if (singerMapper.findById(singerId) == null) throw BizException.notFound("歌手不存在");

        Song s = new Song();
        s.setName(name);
        s.setSingerId(singerId);
        s.setUrl(url);
        s.setCover(str(body.get("cover")));
        s.setStyle(str(body.get("style")));
        s.setDuration(new java.math.BigDecimal(String.valueOf(
                body.get("duration") == null ? 0 : body.get("duration"))));
        s.setPlayCount(0L);
        songMapper.insert(s);
        return Result.ok(s);
    }

    @PutMapping("/song/update/{id}")
    public Result<Song> updateSong(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        requireAdmin();
        Song s = songMapper.findById(id);
        if (s == null) throw BizException.notFound("歌曲不存在");

        if (body.get("name") != null) s.setName(str(body.get("name")));
        if (body.get("singerId") != null) s.setSingerId(Long.valueOf(String.valueOf(body.get("singerId"))));
        if (body.get("cover") != null) s.setCover(str(body.get("cover")));
        if (body.get("url") != null) s.setUrl(str(body.get("url")));
        if (body.get("style") != null) s.setStyle(str(body.get("style")));
        if (body.get("duration") != null) {
            s.setDuration(new java.math.BigDecimal(String.valueOf(body.get("duration"))));
        }
        songMapper.update(s);
        return Result.ok(s);
    }

    /** 删除歌曲：歌单关联、评论、红心均由外键级联清理。 */
    @DeleteMapping("/song/delete/{id}")
    @Transactional
    public Result<Void> deleteSong(@PathVariable Long id) {
        requireAdmin();
        if (songMapper.findById(id) == null) throw BizException.notFound("歌曲不存在");
        songMapper.deleteById(id);
        return Result.ok();
    }

    private void requireAdmin() {
        CurrentUser user = UserContext.get();
        if (user == null) throw BizException.unauthorized("登录已过期，请重新登录");
        if (!user.isAdmin()) throw BizException.forbidden("只有管理员可以管理曲库");
    }

    private String str(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private Integer intOf(Object v, Integer def) {
        if (v == null) return def;
        try {
            return Integer.valueOf(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
