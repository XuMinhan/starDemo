package com.example.starqiangloudemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.starqiangloudemo.dto.Result;
import com.example.starqiangloudemo.entity.Comment;


public interface ICommentService extends IService<Comment> {
    Result qianglou(Comment comment);
    void CommentInSql(Comment comment);

}
