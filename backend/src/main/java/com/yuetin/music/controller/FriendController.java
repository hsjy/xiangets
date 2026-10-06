package com.yuetin.music.controller;

import com.yuetin.music.common.Result;
import com.yuetin.music.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 好友：搜索/加友/同意/消息。 */
@RestController
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @GetMapping("/friend/search")
    public Result<List<Map<String, Object>>> search(@RequestParam String keyword) {
        return Result.ok(friendService.searchUsers(keyword));
    }

    @PostMapping("/friend/request")
    public Result<Map<String, Object>> sendRequest(@RequestBody Map<String, Object> body) {
        return Result.ok(friendService.sendRequest(String.valueOf(body.get("username"))));
    }

    @PostMapping("/friend/respond/{relId}")
    public Result<Map<String, Object>> respond(@PathVariable Long relId,
                                               @RequestBody Map<String, Object> body) {
        boolean accept = Boolean.parseBoolean(String.valueOf(body.getOrDefault("accept", "false")));
        return Result.ok(friendService.respond(relId, accept));
    }

    @DeleteMapping("/friend/remove/{relId}")
    public Result<Map<String, Object>> remove(@PathVariable Long relId) {
        return Result.ok(friendService.removeFriend(relId));
    }

    @GetMapping("/friend/list")
    public Result<List<Map<String, Object>>> friends() {
        return Result.ok(friendService.friends());
    }

    @GetMapping("/friend/incoming")
    public Result<List<Map<String, Object>>> incoming() {
        return Result.ok(friendService.incoming());
    }

    @GetMapping("/friend/outgoing")
    public Result<List<Map<String, Object>>> outgoing() {
        return Result.ok(friendService.outgoing());
    }

    @GetMapping("/friend/messages/{friendId}")
    public Result<Map<String, Object>> messages(@PathVariable Long friendId) {
        return Result.ok(friendService.messages(friendId));
    }

    @PostMapping("/friend/message/{friendId}")
    public Result<Map<String, Object>> sendMessage(@PathVariable Long friendId,
                                                   @RequestBody Map<String, Object> body) {
        return Result.ok(friendService.sendMessage(friendId, String.valueOf(body.get("content"))));
    }

    /* ---- 管理员 ---- */

    @GetMapping("/friend/admin/all")
    public Result<List<Map<String, Object>>> adminFriendships() {
        return Result.ok(friendService.adminFriendships());
    }

    @GetMapping("/friend/admin/messages")
    public Result<List<Map<String, Object>>> adminMessages(
            @RequestParam(defaultValue = "100") int limit) {
        return Result.ok(friendService.adminMessages(limit));
    }
}
