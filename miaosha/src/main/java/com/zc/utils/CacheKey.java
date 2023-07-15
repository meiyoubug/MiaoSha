package com.zc.utils;

/**
 * @program: miaosha
 * @description:
 * @author: ZC
 * @create: 2023-07-14 16:02
 **/
public enum CacheKey {
    HASH_KEY("miaosha_v1_user_hash"),
    LIMIT_KEY("miaosha_v1_user_limit"),
    STOCK_COUNT("miaosha_v1_stock_count"),
    USER_HAS_ORDER("miaosha_v1_user_has_order");

    private String key;
    private CacheKey(String key) {
        this.key = key;
    }
    public String getKey() {
        return key;
    }
}
