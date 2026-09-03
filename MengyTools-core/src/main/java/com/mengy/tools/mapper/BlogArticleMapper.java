package com.mengy.tools.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mengy.tools.entity.BlogArticle;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 博客文章 Mapper。
 */
public interface BlogArticleMapper extends BaseMapper<BlogArticle> {

    /**
     * 阅读量自增（原子操作，避免读改写竞态）。
     */
    @Update("UPDATE blog_article SET view_count = view_count + 1 WHERE id = #{id} AND deleted = 0")
    int incrementViewCount(@Param("id") Long id);
}
