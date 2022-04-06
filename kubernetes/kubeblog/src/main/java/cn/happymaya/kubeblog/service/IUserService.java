package cn.happymaya.kubeblog.service;

import cn.happymaya.kubeblog.po.User;

public interface IUserService {

    User checkuser(String username, String password);

    User saveUser(String username, String password);
}
