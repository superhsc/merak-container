package cn.happymaya.kubeblog.controller;

import cn.happymaya.kubeblog.po.Type;
import cn.happymaya.kubeblog.vo.BlogQueryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class TypeShowController {

    @Autowired
    private cn.happymaya.kubeblog.service.ITypeService ITypeService;

    @Autowired
    private cn.happymaya.kubeblog.service.IBlogService IBlogService;

    @GetMapping("/types/{id}")
    public String types(@PageableDefault(size = 8, sort = {"updateTime"}, direction = Sort.Direction.DESC) Pageable pageable,
                        @PathVariable Long id, Model model){

        List<Type> types = ITypeService.listTypeTop(10000);
        if(id == -1){
            id = types.get(0).getId();
        }
        BlogQueryVO blogQueryVO = new BlogQueryVO();
        blogQueryVO.setTypeId(id);
        model.addAttribute("types",types);
        model.addAttribute("page", IBlogService.listBlog(pageable, blogQueryVO));
        model.addAttribute("activeTypeId",id);
        return "types";
    }
}
