package com.myh.blog.service.impl;

import club.javafan.blog.common.constant.RedisKeyConstant;
import club.javafan.blog.common.result.ResponseResult;
import club.javafan.blog.common.util.*;
import club.javafan.blog.domain.Blog;
import club.javafan.blog.domain.BlogCategory;
import club.javafan.blog.domain.BlogTag;
import club.javafan.blog.domain.BlogTagRelation;
import club.javafan.blog.domain.example.*;
import club.javafan.blog.domain.vo.BlogDetailVO;
import club.javafan.blog.domain.vo.BlogListVO;
import club.javafan.blog.domain.vo.SimpleBlogListVO;
import com.myh.blog.repository.*;
import com.myh.blog.service.BlogService;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static club.javafan.blog.common.constant.RedisKeyConstant.BLOG_VIEW_ZSET;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.math.NumberUtils.*;


@Service
public class BlogServiceImpl implements BlogService {

    @Autowired
    private BlogMapper blogMapper;
    @Autowired
    private BlogCategoryMapper categoryMapper;
    @Autowired
    private BlogTagMapper tagMapper;
    @Autowired
    private BlogTagRelationMapper blogTagRelationMapper;
    @Autowired
    private BlogCommentMapper blogCommentMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult saveBlog(Blog blog) {

        BlogCategory blogCategory = categoryMapper.selectByPrimaryKey(blog.getBlogCategoryId());
        //处理标签数据
        String[] tags = blog.getBlogTags().split(",");
        if (ArrayUtils.isNotEmpty(tags) && tags.length > 6) {
            return ResponseResult.failResult("标签数量限制为6!");
        }
        //保存文章
        int count = blogMapper.insertSelective(blog);
        if (count > INTEGER_ZERO) {
            if (batchTagsRelation(blog, blogCategory, tags)) {
                return ResponseResult.successResult("添加成功！");
            }
        }
        return ResponseResult.failResult("保存失败！");
    }

    private boolean batchTagsRelation(Blog blog, BlogCategory blogCategory, String[] tags) {
        //新增的tag对象
        List<BlogTag> newTagList = new ArrayList<>();
        //所有的tag对象，用于建立关系数据
        List<BlogTag> allTagsList = new ArrayList<>();
        Stream.of(tags).filter(Objects::nonNull).forEach(item -> {
            BlogTagExample example = new BlogTagExample();
            example.createCriteria().andTagNameEqualTo(item);
            List<BlogTag> blogTags = tagMapper.selectByExample(example);
            if (CollectionUtils.isEmpty(blogTags)) {
                BlogTag tag = new BlogTag();
                tag.setTagName(item);
                newTagList.add(tag);
                allTagsList.add(tag);
            }else{
                allTagsList.add(blogTags.get(INTEGER_ZERO));
            }
        });
        //新增标签数据并修改分类排序值
        if (!CollectionUtils.isEmpty(newTagList)) {
            tagMapper.batchInsertBlogTag(newTagList);
        }
        if (Objects.isNull(blogCategory)) {
            blog.setBlogCategoryId(INTEGER_ZERO);
            blog.setBlogCategoryName("默认分类");
        } else {
            //设置博客分类名称
            blog.setBlogCategoryName(blogCategory.getCategoryName());
            //分类的排序值加1
            blogCategory.setCategoryRank(blogCategory.getCategoryRank() + INTEGER_ONE);
        }
        categoryMapper.updateByPrimaryKeySelective(blogCategory);

        List<BlogTagRelation> blogTagRelations = new ArrayList<>();
        //新增关系数据
        allTagsList.addAll(newTagList);
        allTagsList.stream().forEach(item -> {
            BlogTagRelation blogTagRelation = new BlogTagRelation();
            blogTagRelation.setBlogId(blog.getBlogId());
            blogTagRelation.setTagId(item.getTagId());
            blogTagRelations.add(blogTagRelation);
        });
        int num = blogTagRelationMapper.batchInsert(blogTagRelations);
        if (num > INTEGER_ZERO) {
            return true;
        }
        return false;
    }

    @Override
    public PageResult getBlogsPage(PageQueryUtil pageUtil) {
        List<Blog> blogList = blogMapper.findBlogList(pageUtil);
        blogList.stream().map(blog -> {
            //设置浏览量
            blog.setBlogViews(getBlogView(blog.getBlogId()));
            return blog;
        }).collect(Collectors.toList());
        int total = blogMapper.getTotalBlogs(pageUtil);
        PageResult pageResult = new PageResult(blogList, total, pageUtil.getLimit(), pageUtil.getPage());
        return pageResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteBatch(Integer[] ids) {
        if (ArrayUtils.isEmpty(ids)) {
            return false;
        }
        //删除标签的关系
        blogTagRelationMapper.batchDelete(ids);
        //删除相关评论
        blogCommentMapper.deleteBatchByBlogId(ids);

        return blogMapper.deleteBatch(ids) > INTEGER_ZERO;
    }

    @Override
    public int getTotalBlogs() {
        return blogMapper.getTotalBlogs(null);
    }

    @Override
    public Blog getBlogById(Long blogId) {
        return blogMapper.selectByPrimaryKey(blogId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult updateBlog(Blog blog) {
        //处理标签数据
        String[] tags = blog.getBlogTags().split(",");
        if (tags.length > 6) {
            return ResponseResult.failResult("标签数量限制为6！");
        }
        BlogCategory blogCategory = categoryMapper.selectByPrimaryKey(blog.getBlogCategoryId());
        //修改blog信息->修改分类排序值->删除原关系数据->保存新的关系数据
        BlogTagRelationExample example = new BlogTagRelationExample();
        example.createCriteria().andBlogIdEqualTo(blog.getBlogId());
        blogTagRelationMapper.deleteByExample(example);
        batchTagsRelation(blog, blogCategory, tags);
        int con = blogMapper.updateByPrimaryKeySelective(blog);
        if (con > INTEGER_ZERO) {
            return ResponseResult.successResult("修改成功！");
        }
        return ResponseResult.failResult("修改失败！");
    }

    @Override
    public PageResult getBlogsForIndexPage(int cur, int pageSize) {
        PageQueryUtil pageUtil = new PageQueryUtil(cur, pageSize);
        pageUtil.put("blogStatus", INTEGER_ONE);
        List<Blog> blogList = blogMapper.findBlogList(pageUtil);
        int totalBlogs = blogMapper.getTotalBlogs(pageUtil);
        return getPageResult(pageUtil, blogList, totalBlogs);
    }

    @Override
    public List<SimpleBlogListVO> getBlogListForIndexPage(int type) {
        List<SimpleBlogListVO> simpleBlogListVOS = new ArrayList<>();
        BlogExample example = new BlogExample();
        if (type == INTEGER_ONE) {
            example.setOrderByClause("update_time desc");
        }
        if (type == INTEGER_ZERO) {
            example.setOrderByClause("blog_views desc");
        }
        //默认设置9条
        example.setLimit(7);
        example.setStart(0);
        example.createCriteria().andIsDeletedEqualTo(BYTE_ZERO).
                andBlogStatusEqualTo(BYTE_ONE);
        List<Blog> blogs = blogMapper.selectByExample(example);
        if (!CollectionUtils.isEmpty(blogs)) {
            blogs.stream().forEach(blog -> {
                SimpleBlogListVO simpleBlogListVO = new SimpleBlogListVO();
                BeanUtils.copyProperties(blog, simpleBlogListVO);
                simpleBlogListVOS.add(simpleBlogListVO);
            });
        }
        return simpleBlogListVOS;
    }

    @Override
    public List<SimpleBlogListVO> getHotBlogs() {
        Set<Object> blogIdSet = redisUtil.zRevRange(BLOG_VIEW_ZSET, 0, 6);
        if (isNotEmpty(blogIdSet)) {
            List<SimpleBlogListVO> collect = blogIdSet.stream().parallel().filter(Objects::nonNull).map(id -> {
                long blogId = Long.parseLong(String.valueOf(id));
                Blog blog = blogMapper.selectByPrimaryKey(blogId);
                if (Objects.nonNull(blog)) {
                    SimpleBlogListVO simpleBlogListVO = new SimpleBlogListVO();
                    BeanUtils.copyProperties(blog, simpleBlogListVO);
                    return simpleBlogListVO;
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
            return isEmpty(collect) ? new ArrayList<>(1) : collect;
        }
        return new ArrayList<>(1);
    }

    @Override
    public BlogDetailVO getBlogDetail(Long id) {
        Blog blog = blogMapper.selectByPrimaryKey(id);
        return getBlogDetailVO(blog);
    }

    @Override
    public PageResult getBlogsPageByTag(String tagName, int page) {
        if (PatternUtil.validKeyword(tagName) && page > INTEGER_ZERO) {
            BlogTagExample example = new BlogTagExample();
            example.createCriteria().andTagNameEqualTo(tagName);
            List<BlogTag> blogTags = tagMapper.selectByExample(example);
            if (CollectionUtils.isEmpty(blogTags)) {
                return null;
            }
            BlogTag tag = blogTags.get(INTEGER_ZERO);
            PageQueryUtil pageUtil = new PageQueryUtil(page, 9);
            pageUtil.put("tagId", tag.getTagId());
            List<Blog> blogsPageByTagId = blogMapper.getBlogsPageByTagId(pageUtil);
            int totalBlogsByTagId = blogMapper.getTotalBlogsByTagId(pageUtil);
            return getPageResult(pageUtil, blogsPageByTagId, totalBlogsByTagId);
        }
        return null;
    }

    @Override
    public PageResult getBlogsPageByCategory(String categoryName, int page) {
        if (PatternUtil.validKeyword(categoryName) && page > INTEGER_ZERO) {
            BlogCategoryExample example = new BlogCategoryExample();
            example.createCriteria().andCategoryNameEqualTo(categoryName).andIsDeletedEqualTo(BYTE_ZERO);
            List<BlogCategory> blogCategories = categoryMapper.selectByExample(example);
            if (CollectionUtils.isEmpty(blogCategories)) {
                return null;
            }
            BlogCategory blogCategory = blogCategories.get(INTEGER_ZERO);
            if ("默认分类".equals(categoryName)) {
                blogCategory = new BlogCategory();
                blogCategory.setCategoryId(INTEGER_ZERO);
            }
            PageQueryUtil pageUtil = new PageQueryUtil(page, 9);
            //过滤发布状态下的数据
            pageUtil.put("blogCategoryId", blogCategory.getCategoryId());
            pageUtil.put("blogStatus", INTEGER_ONE);
            List<Blog> blogList = blogMapper.findBlogList(pageUtil);
            int totalBlogs = blogMapper.getTotalBlogs(pageUtil);
            return getPageResult(pageUtil, blogList, totalBlogs);
        }
        return null;
    }

    private PageResult getPageResult(PageQueryUtil pageUtil, List<Blog> blogList2, int totalBlogs) {
        List<Blog> blogList = blogList2;
        List<BlogListVO> blogListVOS = getBlogListVOsByBlogs(blogList);
        int total = totalBlogs;
        PageResult pageResult = new PageResult(blogListVOS, total, pageUtil.getLimit()
                , pageUtil.getPage());
        return pageResult;
    }

    @Override
    public PageResult getBlogsPageBySearch(String keyword, int page) {
        if (page > INTEGER_ZERO && PatternUtil.validKeyword(keyword)) {
            Map param = new HashMap();
            PageQueryUtil pageUtil = new PageQueryUtil(page, 9);
            pageUtil.put("keyword", keyword);
            //过滤发布状态下的数据
            pageUtil.put("blogStatus", INTEGER_ONE);
            List<Blog> blogList = blogMapper.findBlogList(pageUtil);
            return getPageResult(pageUtil, blogList, blogMapper.getTotalBlogs(pageUtil));
        }
        return null;
    }

    @Override
    public BlogDetailVO getBlogDetailBySubUrl(String subUrl) {
        BlogExample example = new BlogExample();
        example.createCriteria().andBlogSubUrlEqualTo(subUrl);
        List<Blog> blogs = blogMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(blogs)) {
            return null;
        }

        //不为空且状态为已发布
        BlogDetailVO blogDetailVO = getBlogDetailVO(blogs.get(INTEGER_ZERO));
        if (ObjectUtils.allNotNull(blogDetailVO)) {
            return blogDetailVO;
        }
        return null;
    }

    /**
     * 方法抽取
     *
     * @param blog
     * @return
     */
    private BlogDetailVO getBlogDetailVO(Blog blog) {
        if (blog != null && blog.getBlogStatus().equals(BYTE_ONE)) {
            BlogDetailVO blogDetailVO = new BlogDetailVO();
            BeanUtils.copyProperties(blog, blogDetailVO);
            blogDetailVO.setBlogContent(MarkDownUtil.mdToHtml(blogDetailVO.getBlogContent()));
            BlogCategory blogCategory = categoryMapper.selectByPrimaryKey(blog.getBlogCategoryId());
            if (blogCategory == null) {
                blogCategory = new BlogCategory();
                blogCategory.setCategoryId(INTEGER_ZERO);
                blogCategory.setCategoryName("默认分类");
                blogCategory.setCategoryIcon("/admin/dist/img/category/00.png");
            }
            //分类信息
            blogDetailVO.setBlogCategoryIcon(blogCategory.getCategoryIcon());
            if (!StringUtils.isEmpty(blog.getBlogTags())) {
                //标签设置
                List<String> tags = Arrays.asList(blog.getBlogTags().split(","));
                blogDetailVO.setBlogTags(tags);
            }
            //设置评论数
            BlogCommentExample example = new BlogCommentExample();
            example.createCriteria().andBlogIdEqualTo(blog.getBlogId())
                    .andCommentStatusEqualTo(BYTE_ZERO);
            long l = blogCommentMapper.countByExample(example);
            blogDetailVO.setCommentCount((int) l);
            return blogDetailVO;
        }
        return null;
    }

    private List<BlogListVO> getBlogListVOsByBlogs(List<Blog> blogList) {
        List<BlogListVO> blogListVOS = new ArrayList<>();
        if (!CollectionUtils.isEmpty(blogList)) {
            List<Integer> categoryIds = blogList.stream().map(Blog::getBlogCategoryId).collect(Collectors.toList());
            Map<Integer, String> blogCategoryMap = new HashMap<>();
            if (!CollectionUtils.isEmpty(categoryIds)) {
                List<BlogCategory> blogCategories = categoryMapper.selectByCategoryIds(categoryIds);
                if (!CollectionUtils.isEmpty(blogCategories)) {
                    blogCategoryMap = blogCategories.stream().collect(Collectors.
                            toMap(BlogCategory::getCategoryId, BlogCategory::getCategoryIcon, (key1, key2) -> key2));
                }
            }
            Map<Integer, String> finalBlogCategoryMap = blogCategoryMap;
            blogList.stream().filter(Objects::nonNull).forEach(blog -> {
                BlogListVO blogListVO = new BlogListVO();
                BeanUtils.copyProperties(blog, blogListVO);
                //计算摘要 内容大于100的进行截取
                String abC = blog.getBlogContent();
                if (isNotEmpty(abC) && abC.length() > 50) {
                    abC = abC.substring(INTEGER_ZERO, 50);
                }
                blogListVO.setAbstractContent(abC);
                boolean b = finalBlogCategoryMap.containsKey(blog.getBlogCategoryId());
                if (b) {
                    String s = finalBlogCategoryMap.get(blog.getBlogCategoryId());
                    blogListVO.setBlogCategoryIcon(s);
                } else {
                    blogListVO.setBlogCategoryId(INTEGER_ZERO);
                    blogListVO.setBlogCategoryName("默认分类");
                    blogListVO.setBlogCategoryIcon("/admin/dist/img/category/00.png");
                }
                blogListVOS.add(blogListVO);
            });

        }
        return blogListVOS;
    }

    /**
     * 获取博客浏览量
     *
     * @param id
     * @return Long
     */
    private Long getBlogView(Long id) {
        Object o = redisUtil.get(RedisKeyConstant.BLOG_PAGE_VIEW + id);
        return Objects.isNull(o) ? 0L : Long.parseLong(o.toString());
    }
}
