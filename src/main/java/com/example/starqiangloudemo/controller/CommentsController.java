package com.example.starqiangloudemo.controller;


import com.example.starqiangloudemo.dto.Result;
import com.example.starqiangloudemo.entity.Comment;
import com.example.starqiangloudemo.service.ICommentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;


@RestController
@RequestMapping("/comment")
public class CommentsController {
    @Resource
    ICommentService commentService;
    @PostMapping("/write")
    public Result writeComments(@RequestBody Comment comment) {
        return commentService.qianglou(comment);
    }
}
