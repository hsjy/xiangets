package com.yuetin.music.controller;

import com.yuetin.music.common.Result;
import com.yuetin.music.mapper.PlaylistMapper;
import com.yuetin.music.mapper.SingerMapper;
import com.yuetin.music.mapper.SongMapper;
import com.yuetin.music.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 首页概览统计。
 */
@RestController
@RequiredArgsConstructor
public class StatsController {

    private final UserMapper userMapper;
    private final SingerMapper singerMapper;
    private final SongMapper songMapper;
    private final PlaylistMapper playlistMapper;

    @GetMapping("/stats/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> data = new HashMap<>();
        data.put("users", userMapper.countAll());
        data.put("singers", singerMapper.countAll());
        data.put("songs", songMapper.countAll());
        data.put("playlists", playlistMapper.countAll());
        data.put("totalPlays", songMapper.sumPlayCount());
        return Result.ok(data);
    }
}
