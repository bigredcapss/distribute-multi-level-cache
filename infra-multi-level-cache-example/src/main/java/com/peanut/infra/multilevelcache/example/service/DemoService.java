package com.peanut.infra.multilevelcache.example.service;

import com.peanut.infra.multilevelcache.example.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @descrription 测试缓存Service
 * @author peanut
 */
@Service
public class DemoService {

    @Cacheable(cacheNames = "testCache", key = "#id")
    public List<User> cacheTest(String id){
        User user = new User();
        user.setAge(22);
        user.setName("xxx");

        List<User> users = new ArrayList<>();
        users.add(user);
        return users;
    }
}
