package com.philippbro.springboot.hazelcast.caching;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ConfigurableApplicationContext;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.impl.HazelcastClientProxy;
import com.hazelcast.spring.cache.HazelcastCacheManager;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class CachingTwoClientsIT extends CachingITBase {

    private static ConfigurableApplicationContext clientContext1;
    private static ConfigurableApplicationContext clientContext2;
    private static ConfigurableApplicationContext clusterContext;

    @BeforeClass
    public static void beforeAllTests() {
        clusterContext = createClusterApp(8092);
        clientContext1 = createClientApp(8090);
        clientContext2 = createClientApp(8091);
    }

    @AfterClass
    public static void afterAllTests() {
        clusterContext.stop();
        clientContext1.stop();
        clientContext2.stop();
    }


    @Test
    public void testReadCacheEntryOnSecondClient() {
        //Given
        CacheManager hazelcastCacheManager = getCacheManager(clusterContext);
        CityBean cityBean = getCityBean(clientContext1);
        CityBean cityBean2 = getCityBean(clientContext2);
        Cache clusterCache = hazelcastCacheManager.getCache("city");

        //When
        String city = cityBean.getCity("Berlin");
        String city2 = cityBean2.getCity("Berlin");

        //Then
        waitForCacheHandling(() -> {
            assertThat(city).isEqualTo("Berlin");
            assertThat(city2).isEqualTo("Berlin");

            assertThat(clusterCache.get("Berlin", String.class)).isEqualTo("Berlin");
            assertThat(MemoryAppender.LOG_MESSAGES).hasSize(1);
            assertThat(MemoryAppender.LOG_MESSAGES.poll()).matches(this::isExpectedInfoLog);
        });
    }

    @Test
    public void testUpdateCacheEntryOnSecondClientWithCache() {
        //Given
        CacheManager hazelcastCacheManager = getCacheManager(clusterContext);
        CityBean cityBean = getCityBean(clientContext1);
        CityBean cityBean2 = getCityBean(clientContext2);
        Cache cache = hazelcastCacheManager.getCache("city");
        cache.put("Berlin", "Berlin");

        //When
        cityBean.setCity("Berlin", "ZONK!");

        //Then
        waitForCacheHandling(() -> {
            String city = cityBean2.getCity("Berlin");
            assertThat(cache.get("Berlin", String.class)).isEqualTo("ZONK!");
            assertThat(city).isEqualTo("ZONK!");
        });

    }

    @Test
    public void testEvictCacheEntryIsReceivedBySecondClient() {
        //Given
        CacheManager hazelcastCacheManager = getCacheManager(clusterContext);
        CityBean cityBean = getCityBean(clientContext1);
        CityBean cityBean2 = getCityBean(clientContext2);
        Cache cache = hazelcastCacheManager.getCache("city");
        cache.put("Berlin", "ZONK!");

        //When
        cityBean.evictCity("Berlin");
        String city = cityBean2.getCity("Berlin");


        //Then
        waitForCacheHandling(() -> {
            assertThat(city).isEqualTo("Berlin");
            assertThat(cache.get("Berlin", String.class)).isEqualTo("Berlin");
        });
    }




    private boolean isExpectedInfoLog(ILoggingEvent m) {
        return m.getLevel() == Level.INFO && m.getLoggerName().equals(CityBean.class.getName());
    }
}
