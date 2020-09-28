package com.myh.blog.service.impl;

import club.javafan.blog.common.util.PageQueryUtil;
import club.javafan.blog.common.util.PageResult;
import club.javafan.blog.domain.BlogComment;
import com.myh.blog.repository.BlogCommentMapper;
import com.myh.blog.service.CommentService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ONE;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;


@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private BlogCommentMapper blogCommentMapper;

    @Override
    public Boolean addComment(BlogComment blogComment) {
        return blogCommentMapper.insertSelective(blogComment) > INTEGER_ZERO;
    }

    @Override
    public BlogComment getComment(Long blogCommentId) {
        return blogCommentMapper.selectByPrimaryKey(blogCommentId);
    }

    @Override
    public PageResult getCommentsPage(PageQueryUtil pageUtil) {
        List<BlogComment> comments = blogCommentMapper.findBlogCommentList(pageUtil);
        int total = blogCommentMapper.getTotalBlogComments(pageUtil);
        PageResult pageResult = new PageResult(comments, total, pageUtil.getLimit(), pageUtil.getPage());
        return pageResult;
    }

    @Override
    public int getTotalComments() {
        return blogCommentMapper.getTotalBlogComments(null);
    }

    @Override
    public Boolean checkDone(Integer[] ids) {
        return blogCommentMapper.checkDone(ids) > INTEGER_ZERO;
    }

    @Override
    public Boolean deleteBatch(Integer[] ids) {
        return blogCommentMapper.deleteBatch(ids) > INTEGER_ZERO;
    }

    @Override
    public Boolean reply(Long commentId, String replyBody) {
        BlogComment blogComment = blogCommentMapper.selectByPrimaryKey(commentId);
        //blogComment不为空且状态为已审核，则继续后续操作
        if (blogComment != null && blogComment.getCommentStatus().intValue() == 1) {
            blogComment.setReplyBody(replyBody);
            blogComment.setReplyCreateTime(new Date());
            return blogCommentMapper.updateByPrimaryKeySelective(blogComment) > INTEGER_ZERO;
        }
        return false;
    }

    @Override
    public PageResult getCommentPageByBlogIdAndPageNum(Long blogId, int page) {
        if (page < INTEGER_ONE) {
            page = INTEGER_ONE;
        }
        PageQueryUtil pageUtil = new PageQueryUtil(page,8);
        pageUtil.put("blogId", blogId);
        pageUtil.put("commentStatus", INTEGER_ONE);
        List<BlogComment> comments = blogCommentMapper.findBlogCommentList(pageUtil);
        if (CollectionUtils.isNotEmpty(comments)) {
            int total = blogCommentMapper.getTotalBlogComments(pageUtil);
            PageResult pageResult = new PageResult(comments, total, pageUtil.getLimit(), pageUtil.getPage());
            return pageResult;
        }
        return null;
    }
}
