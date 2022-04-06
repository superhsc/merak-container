package cn.happymaya.kubeblog.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ArchiveShowController {

    @Autowired
    private cn.happymaya.kubeblog.service.IBlogService IBlogService;

    @GetMapping("/archives")
    public String archives(Model model){
        model.addAttribute("archiveMap", IBlogService.archiveBlog());
        model.addAttribute("blogCount", IBlogService.countBlog());
        return "archives";
    }


}
