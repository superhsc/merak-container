package cn.happymaya.kubeblog.config;

import cn.happymaya.kubeblog.po.Tag;
import cn.happymaya.kubeblog.po.Type;
import cn.happymaya.kubeblog.po.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class InitAdminConfig {

    @Autowired
    private cn.happymaya.kubeblog.service.IUserService IUserService;

    @Autowired
    private cn.happymaya.kubeblog.service.ITypeService ITypeService;

    @Autowired
    private cn.happymaya.kubeblog.service.ITagService ITagService;

    @Bean
    public User initAdminUser() {

        //init types
        List<Type> types = ITypeService.listType();

        if(types.isEmpty()){
            Type newType = new Type();
            newType.setName("技术");
            newType.setId(Long.valueOf(1));
            ITypeService.saveType(newType);
        }

        //init tags
        List<Tag> tags = ITagService.listTag();
        if(tags.isEmpty()){
            Tag newTag = new Tag();
            newTag.setId(Long.valueOf(1));
            newTag.setName("docker");
            ITagService.saveTag(newTag);
        }

//        init Admin user
        User user = IUserService.checkuser("admin", "password");
        if(user == null){
            return IUserService.saveUser("admin","password");
        } else {
            return user;
        }



    }
}