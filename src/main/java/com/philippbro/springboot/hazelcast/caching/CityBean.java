package com.philippbro.springboot.hazelcast.caching;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

public class CityBean {

    @Cacheable(cacheNames = "city", key = "#city")
    public String getCity(String city) {
        System.out.println("CityBean.getCity() called!");
        try {
            // emulation of slow method
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

