package com.example.starqiangloudemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.starqiangloudemo.dto.LoginFormDTO;
import com.example.starqiangloudemo.dto.Result;
import com.example.starqiangloudemo.entity.User;


import javax.servlet.http.HttpSession;

public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);
}
