package com.philippbro.springboot.hazelcast.caching;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CityBean {

    @Cacheable(cacheNames = "city", key = "#city")
    public String getCity(String city) {
        log.info("Uncached: CityBean.getCity() called!");
        return city;
    }

    @CachePut(cacheNames = "city", key = "#city")
    public String setCity(String city, String newCityName) {
        return newCityName;
    }

    @CacheEvict(cacheNames = "city", key = "#city")
    public void evictCity(String city) {
    }
}

