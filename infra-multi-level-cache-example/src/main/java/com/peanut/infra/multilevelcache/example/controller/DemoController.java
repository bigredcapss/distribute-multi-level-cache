package com.peanut.infra.multilevelcache.example.controller;

import com.peanut.infra.multilevelcache.example.pojo.User;
import com.peanut.infra.multilevelcache.example.service.DemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @description 测试多级缓存
 * @author peanut
 */
@RestController
public class DemoController {

    @Autowired
    private DemoService demoService;

    @RequestMapping("cache-test")
    public List<User> demo(){
        return demoService.cacheTest("peanut.multi.cache.test");
    }
}
