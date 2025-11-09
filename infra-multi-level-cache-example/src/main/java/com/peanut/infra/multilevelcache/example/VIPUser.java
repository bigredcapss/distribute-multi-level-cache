package com.peanut.infra.multilevelcache.example;

import lombok.Data;

/**
 * @description 用户
 * @author peanut
 */
@Data
public class VIPUser extends User {

    private Integer level;

}
