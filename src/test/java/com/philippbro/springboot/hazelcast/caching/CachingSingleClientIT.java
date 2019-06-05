package com.philippbro.springboot.hazelcast.caching;


import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.awaitility.core.ThrowingRunnable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.context.ConfigurableApplicationContext;

import com.hazelcast.spring.cache.HazelcastCacheManager;

@SpringBootTest
public class CachingSingleClientIT {

    private static ConfigurableApplicationContext clusterContext;
    private static ConfigurableApplicationContext clientContext;

    @BeforeClass
    public static void setup() {

        String[] args = new String[] {};

        clusterContext = new SpringApplicationBuilder()
                .profiles("cluster")
                .sources(HazelcastCachingClusterApplication.class)
                .properties(Map.of("server.port", 8092))
                .run(args);

        clientContext = new SpringApplicationBuilder()
                .sources(HazelcastCachingClientApplication.class)
                .profiles("client")
                .properties(Map.of("server.port", 8090))
                .run(args);
    }

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


    protected void waitForCacheHandling(ThrowingRunnable asserts) {
        await().atMost(15, TimeUnit.SECONDS)
                .pollDelay(200, TimeUnit.MILLISECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(asserts);
    }

}
