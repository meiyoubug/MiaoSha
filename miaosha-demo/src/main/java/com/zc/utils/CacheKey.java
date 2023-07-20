package com.zc.utils;

/**
 * @program: MiaoSha
 * @description:
 * @author: ZC
 * @create: 2023-07-19 19:27
 **/
public enum CacheKey {
    GoodsKey("miaosha_goods"),
    HASH_KEY("miaosha_hash"),
    LIMIT_KEY("miaosha_limit");

    private String key;

    private CacheKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
