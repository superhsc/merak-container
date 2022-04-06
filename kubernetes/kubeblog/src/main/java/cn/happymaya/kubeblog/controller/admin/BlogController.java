package cn.happymaya.kubeblog.controller.admin;

import cn.happymaya.kubeblog.po.Blog;
import cn.happymaya.kubeblog.po.User;
import cn.happymaya.kubeblog.vo.BlogQueryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class BlogController {

    private static final String INPUT = "admin/blogs-input";
    private static final String LIST = "admin/blogs";
    private static final String REDIRECT_LIST = "redirect:/admin/blogs";


    @Autowired
    private cn.happymaya.kubeblog.service.IBlogService IBlogService;
    @Autowired
    private cn.happymaya.kubeblog.service.ITypeService ITypeService;
    @Autowired
    private cn.happymaya.kubeblog.service.ITagService ITagService;

    @GetMapping("/blogs")
    public String blogs(@PageableDefault(size = 8, sort = {"updateTime"}, direction = Sort.Direction.DESC) Pageable pageable,
                        BlogQueryVO blog, Model model) {
        model.addAttribute("types", ITypeService.listType());
        model.addAttribute("page", IBlogService.listBlog(pageable, blog));
        return LIST;
    }

    @PostMapping("/blogs/search")
    public String search(@PageableDefault(size = 8, sort = {"updateTime"}, direction = Sort.Direction.DESC) Pageable pageable,
                         BlogQueryVO blog, Model model) {
        model.addAttribute("page", IBlogService.listBlog(pageable, blog));
        return "admin/blogs :: blogList";
    }


    @GetMapping("/blogs/input")
    public String input(Model model) {
        setTypeAndTag(model);
        model.addAttribute("blog", new Blog());
        return INPUT;
    }

    private void setTypeAndTag(Model model) {
        model.addAttribute("types", ITypeService.listType());
        model.addAttribute("tags", ITagService.listTag());
    }


    @GetMapping("/blogs/{id}/input")
    public String editInput(@PathVariable Long id, Model model) {
        setTypeAndTag(model);
        Blog blog = IBlogService.getBlog(id);
        blog.init();
        model.addAttribute("blog",blog);
        return INPUT;
    }



    @PostMapping("/blogs")
    public String post(Blog blog, RedirectAttributes attributes, HttpSession session) {
        blog.setUser((User) session.getAttribute("user"));
        blog.setType(ITypeService.getType(blog.getType().getId()));
        blog.setTags(ITagService.listTag(blog.getTagIds()));
        Blog b;
        if (blog.getId() == null) {
            b =  IBlogService.saveBlog(blog);
        } else {
            b = IBlogService.updateBlog(blog.getId(), blog);
        }

        if (b == null ) {
            attributes.addFlashAttribute("message", "Failure");
        } else {
            attributes.addFlashAttribute("message", "Successful");
        }
        return REDIRECT_LIST;
    }


    @GetMapping("/blogs/{id}/delete")
    public String delete(@PathVariable Long id,RedirectAttributes attributes) {
        IBlogService.deleteBlog(id);
        attributes.addFlashAttribute("message", "Successfully deleted");
        return REDIRECT_LIST;
    }



}