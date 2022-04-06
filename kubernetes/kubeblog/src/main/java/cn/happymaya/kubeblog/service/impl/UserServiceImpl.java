package cn.happymaya.kubeblog.service.impl;

import cn.happymaya.kubeblog.service.IUserService;
import cn.happymaya.kubeblog.dao.UserRepository;
import cn.happymaya.kubeblog.po.User;
import cn.happymaya.kubeblog.util.MD5Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserRepository userRepository;
    private String DEFALT_AVATAR_URL = "/images/wechat.jpg";

    @Override
    public User checkuser(String username, String password) {
        User user = userRepository.findByUsernameAndPassword(username, MD5Utils.code(password));
        return user;
    }

    @Override
    public User saveUser(String username, String password) {
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(MD5Utils.code(password));
        newUser.setAvatar(DEFALT_AVATAR_URL);
        User user = userRepository.save(newUser);
        return user;
    }

}
