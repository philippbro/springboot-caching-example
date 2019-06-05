package com.philippbro.springboot.hazelcast.caching;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;

import com.hazelcast.spring.cache.HazelcastCacheManager;

@SpringBootTest
public class CachingSingleClientIT extends CachingITBase {


    @Test
    public void testAddCacheEntry() {
        //Given
        HazelcastCacheManager hazelcastCacheManager = (HazelcastCacheManager) clusterContext.getBean("cacheManager");
        CityBean cityBean = (CityBean) clientContext.getBean("cityBean");
        Cache cache = hazelcastCacheManager.getCache("city");

        //When
        String city = cityBean.getCity("Berlin");

        //Then
        waitForCacheHandling(() -> {
            assertThat(city).isEqualTo("Berlin");
            assertThat(cache.get("Berlin", String.class)).isEqualTo("Berlin");
        });
    }

    @Test
    public void testUpdateCacheEntry() {
        //Given
        HazelcastCacheManager hazelcastCacheManager = (HazelcastCacheManager) clusterContext.getBean("cacheManager");
        CityBean cityBean = (CityBean) clientContext.getBean("cityBean");
        Cache cache = hazelcastCacheManager.getCache("city");
        cache.put("Berlin", "Berlin");

        //When
        String city = cityBean.setCity("Berlin", "ZONK!");

        //Then
        waitForCacheHandling(() -> {
            assertThat(city).isEqualTo("ZONK!");
            assertThat(cache.get("Berlin", String.class)).isEqualTo("ZONK!");
        });
    }

    @Test
    public void testDeleteCacheEntry() {
        //Given
        HazelcastCacheManager hazelcastCacheManager = (HazelcastCacheManager) clusterContext.getBean("cacheManager");
        CityBean cityBean = (CityBean) clientContext.getBean("cityBean");
        Cache cache = hazelcastCacheManager.getCache("city");
        cache.put("Berlin", "Berlin");

        //When
        cityBean.evictCity("Berlin");

        //Then
        waitForCacheHandling(() -> {
            assertThat(cache.get("Berlin")).isNull();
        });
    }

    @Test
    public void testUpdateNullCacheEntry() {
        //Given
        HazelcastCacheManager hazelcastCacheManager = (HazelcastCacheManager) clusterContext.getBean("cacheManager");
        CityBean cityBean = (CityBean) clientContext.getBean("cityBean");
        Cache cache = hazelcastCacheManager.getCache("city");
        cache.put("Berlin", "Berlin");

        //When
        String city = cityBean.setCity("Berlin", null);

        //Then
        waitForCacheHandling(() -> {
            assertThat(city).isNull();
            assertThat(cache.get("Berlin")).isNotNull();
            assertThat(cache.get("Berlin").get()).isNull();
        });
    }
}
