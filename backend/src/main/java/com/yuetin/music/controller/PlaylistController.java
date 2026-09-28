package com.yuetin.music.controller;

import com.yuetin.music.common.Result;
import com.yuetin.music.entity.Playlist;
import com.yuetin.music.entity.Song;
import com.yuetin.music.service.PlaylistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    /* ---------------- 歌单本体 ---------------- */

    @GetMapping("/playlist/list")
    public Result<List<Playlist>> list(@RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String style) {
        return Result.ok(playlistService.list(keyword, style));
    }

    /** 我创建的 + 我收藏的。 */
    @GetMapping("/playlist/my")
    public Result<Map<String, Object>> mine() {
        return Result.ok(playlistService.mine());
    }

    @GetMapping("/playlist/detail/{id}")
    public Result<Playlist> detail(@PathVariable Long id) {
        return Result.ok(playlistService.detail(id));
    }

    @PostMapping("/playlist/add")
    public Result<Playlist> add(@RequestBody Map<String, Object> body) {
        return Result.ok(playlistService.create(body));
    }

    @PutMapping("/playlist/update/{id}")
    public Result<Playlist> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Result.ok(playlistService.update(id, body));
    }

    @DeleteMapping("/playlist/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        playlistService.delete(id);
        return Result.ok();
    }

    /** 收藏 / 取消收藏，返回切换后的状态。 */
    @PostMapping("/playlist/favorite/{id}")
    public Result<Map<String, Object>> favorite(@PathVariable Long id) {
        return Result.ok(playlistService.toggleFavorite(id));
    }

    @GetMapping("/playlist/favorite/state/{id}")
    public Result<Map<String, Object>> favoriteState(@PathVariable Long id) {
        return Result.ok(playlistService.favoriteState(id));
    }

    /* ---------------- 歌单歌曲关联 ---------------- */

    @GetMapping("/playlist/song/list/{id}")
    public Result<List<Song>> songs(@PathVariable Long id) {
        return Result.ok(playlistService.songsOf(id));
    }

    @PostMapping("/playlist/song/add")
    public Result<List<Song>> addSong(@RequestBody Map<String, Object> body) {
        return Result.ok(playlistService.addSong(toLong(body.get("playlistId")), body));
    }

    @PostMapping("/playlist/song/remove")
    public Result<List<Song>> removeSong(@RequestBody Map<String, Object> body) {
        return Result.ok(playlistService.removeSong(toLong(body.get("playlistId")),
                toLong(body.get("songId"))));
    }

    /** 上移 / 下移一首：direction = -1 上移，1 下移。 */
    @PutMapping("/playlist/song/order")
    public Result<List<Song>> order(@RequestBody Map<String, Object> body) {
        return Result.ok(playlistService.moveSong(toLong(body.get("playlistId")), body));
    }

    private Long toLong(Object v) {
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }
}
