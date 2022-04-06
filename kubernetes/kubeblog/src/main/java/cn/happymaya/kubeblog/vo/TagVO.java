package cn.happymaya.kubeblog.vo;

import cn.happymaya.kubeblog.po.Blog;
import cn.happymaya.kubeblog.po.Tag;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

public class TagVO {

    private Long id;
    private String name;

    private List<Blog> blogs = new ArrayList<>();

    public TagVO() {
    }

    public Tag convertToPo(TagVO tagVo, Tag tag){
        BeanUtils.copyProperties(tagVo, tag);
        return tag;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Blog> getBlogs() {
        return blogs;
    }

    public void setBlogs(List<Blog> blogs) {
        this.blogs = blogs;
    }

    @Override
    public String toString() {
        return "";
    }
}
