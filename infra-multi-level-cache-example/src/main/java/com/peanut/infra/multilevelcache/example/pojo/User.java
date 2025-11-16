package com.peanut.infra.multilevelcache.example.pojo;

import lombok.Data;

import java.io.Serializable;

/**
 * @description 用户
 * @author peanut
 */
@Data
public class User implements Serializable {

    private String name;

    private Integer age;
}
