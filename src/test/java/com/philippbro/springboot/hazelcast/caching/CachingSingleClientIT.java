package com.philippbro.springboot.hazelcast.caching;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ConfigurableApplicationContext;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.impl.HazelcastClientInstanceImpl;
import com.hazelcast.client.impl.HazelcastClientProxy;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;

public class CachingSingleClientIT extends CachingITBase {
    private static ConfigurableApplicationContext clientContext;
    private static ConfigurableApplicationContext clusterContext;

    @BeforeClass
    public static void beforeAllTests() {
        clusterContext = createClusterApp(8092);
        clientContext = createClientApp(8090);
    }

    @AfterClass
    public static void afterAllTests() {
        clusterContext.stop();
        clientContext.stop();
    }

    @Test
    public void testAddCacheEntry() {
        //Given
        CacheManager hazelcastCacheManager = getCacheManager(clusterContext);
        CityBean cityBean = getCityBean(clientContext);
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
        CacheManager hazelcastCacheManager = getCacheManager(clusterContext);
        CityBean cityBean = getCityBean(clientContext);
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
        CacheManager hazelcastCacheManager = getCacheManager(clusterContext);
        CityBean cityBean = getCityBean(clientContext);
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
        CacheManager hazelcastCacheManager = getCacheManager(clusterContext);
        CityBean cityBean = getCityBean(clientContext);
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
