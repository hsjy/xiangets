package com.yuetin.music.controller;

import com.yuetin.music.common.Result;
import com.yuetin.music.entity.Comment;
import com.yuetin.music.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/comment/list/{songId}")
    public Result<List<Comment>> list(@PathVariable Long songId) {
        return Result.ok(commentService.list(songId));
    }

    @PostMapping("/comment/add")
    public Result<Comment> add(@RequestBody Map<String, Object> body) {
        Long songId = Long.valueOf(String.valueOf(body.get("songId")));
        String content = body.get("content") == null ? null : String.valueOf(body.get("content"));
        return Result.ok(commentService.add(songId, content));
    }

    @DeleteMapping("/comment/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return Result.ok();
    }
}
