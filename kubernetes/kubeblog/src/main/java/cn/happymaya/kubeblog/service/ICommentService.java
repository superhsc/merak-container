package cn.happymaya.kubeblog.service;

import cn.happymaya.kubeblog.po.Comment;

import java.util.List;

public interface ICommentService {

    List<Comment> listCommentByBlogId(Long blogId);

    Comment saveComment(Comment comment);
}
