package com.yuetin.music.controller;

import com.yuetin.music.common.Result;
import com.yuetin.music.entity.Singer;
import com.yuetin.music.entity.Song;
import com.yuetin.music.service.MusicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MusicController {

    private final MusicService musicService;

    /* ---------------- 歌曲 ---------------- */

    @GetMapping("/song/list")
    public Result<List<Song>> listSongs(@RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String style,
                                        @RequestParam(required = false) Long singerId,
                                        @RequestParam(required = false, defaultValue = "pop") String sort) {
        return Result.ok(musicService.listSongs(keyword, style, singerId, sort));
    }

    @GetMapping("/song/hot")
    public Result<List<Song>> hotSongs(@RequestParam(required = false, defaultValue = "10") int limit) {
        return Result.ok(musicService.hotSongs(limit));
    }

    @GetMapping("/song/detail/{id}")
    public Result<Song> songDetail(@PathVariable Long id) {
        return Result.ok(musicService.songDetail(id));
    }

    @PostMapping("/song/play/{id}")
    public Result<Map<String, Object>> play(@PathVariable Long id) {
        return Result.ok(musicService.play(id));
    }

    @PostMapping("/song/like/{id}")
    public Result<Map<String, Object>> like(@PathVariable Long id) {
        return Result.ok(musicService.toggleLike(id));
    }

    /** 查询当前用户对某首歌的红心状态；允许匿名访问。 */
    @GetMapping("/song/like/state/{id}")
    public Result<Map<String, Object>> likeState(@PathVariable Long id) {
        return Result.ok(musicService.likeState(id));
    }

    @GetMapping("/song/like/list")
    public Result<List<Song>> likedSongs() {
        return Result.ok(musicService.likedSongs());
    }

    /* ---------------- 歌手 ---------------- */

    @GetMapping("/singer/list")
    public Result<List<Singer>> listSingers(@RequestParam(required = false) String keyword) {
        return Result.ok(musicService.listSingers(keyword));
    }

    @GetMapping("/singer/detail/{id}")
    public Result<Singer> singerDetail(@PathVariable Long id) {
        return Result.ok(musicService.singerDetail(id));
    }
}
