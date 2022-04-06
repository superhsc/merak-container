package cn.happymaya.kubeblog.controller.admin;

import cn.happymaya.kubeblog.po.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

/**
 * Created by limi on 2017/10/16.
 */

@Controller
@RequestMapping("/admin")
public class TagController {

    @Autowired
    private cn.happymaya.kubeblog.service.ITagService ITagService;

    @GetMapping("/tags")
    public String tags(@PageableDefault(size = 3,sort = {"id"},direction = Sort.Direction.DESC)
                               Pageable pageable, Model model) {
        model.addAttribute("page", ITagService.listTag(pageable));
        return "admin/tags";
    }

    @GetMapping("/tags/input")
    public String input(Model model) {
        model.addAttribute("tag", new Tag());
        return "admin/tags-input";
    }

    @GetMapping("/tags/{id}/input")
    public String editInput(@PathVariable Long id, Model model) {
        model.addAttribute("tag", ITagService.getTag(id));
        return "admin/tags-input";
    }


    @PostMapping("/tags")
    public String post(@Valid Tag tag,BindingResult result, RedirectAttributes attributes) {
        Tag tag1 = ITagService.getTagByName(tag.getName());
        if (tag1 != null) {
            result.rejectValue("name","nameError","Cannot add duplicate tags");
        }
        if (result.hasErrors()) {
            return "admin/tags-input";
        }
        Tag t = ITagService.saveTag(tag);
        if (t == null ) {
            attributes.addFlashAttribute("message", "Add failed");
        } else {
            attributes.addFlashAttribute("message", "Add successfully");
        }
        return "redirect:/admin/tags";
    }


    @PostMapping("/tags/{id}")
    public String editPost(@Valid Tag tag, BindingResult result,@PathVariable Long id, RedirectAttributes attributes) {
        Tag tag1 = ITagService.getTagByName(tag.getName());
        if (tag1 != null) {
            result.rejectValue("name","nameError","Cannot add duplicate tags");
        }
        if (result.hasErrors()) {
            return "admin/tags-input";
        }
        Tag t = ITagService.updateTag(id,tag);
        if (t == null ) {
            attributes.addFlashAttribute("message", "Update failed");
        } else {
            attributes.addFlashAttribute("message", "Update successfully");
        }
        return "redirect:/admin/tags";
    }

    @GetMapping("/tags/{id}/delete")
    public String delete(@PathVariable Long id,RedirectAttributes attributes) {
        ITagService.deleteTag(id);
        attributes.addFlashAttribute("message", "Delete successfully");
        return "redirect:/admin/tags";
    }


}
