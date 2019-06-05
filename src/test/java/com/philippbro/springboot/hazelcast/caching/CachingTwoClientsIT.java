package com.philippbro.springboot.hazelcast.caching;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.context.ConfigurableApplicationContext;

import com.hazelcast.spring.cache.HazelcastCacheManager;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

@SpringBootTest
public class CachingTwoClientsIT extends CachingITBase {

    private static ConfigurableApplicationContext clientContext2 = new SpringApplicationBuilder()
            .sources(HazelcastCachingClientApplication.class)
            .profiles("client")
            .properties(Map.of("server.port", 8091))
            .run();

    @Test
    public void testReadCacheEntryOnSecondClientWithCache() {
        //Given
        HazelcastCacheManager hazelcastCacheManager = (HazelcastCacheManager) clusterContext.getBean("cacheManager");
        CityBean cityBean = (CityBean) clientContext.getBean("cityBean");
        CityBean cityBean2 = (CityBean) clientContext2.getBean("cityBean");
        Cache cache = hazelcastCacheManager.getCache("city");

        //When
        String city = cityBean.getCity("Berlin");
        String city2 = cityBean2.getCity("Berlin");


        //Then
        waitForCacheHandling(() -> {
            assertThat(city).isEqualTo("Berlin");
            assertThat(city2).isEqualTo("Berlin");
            assertThat(cache.get("Berlin", String.class)).isEqualTo("Berlin");
            assertThat(MemoryAppender.LOG_MESSAGES).noneMatch(this::isExpectedInfoLog);
        });
    }

    private boolean isExpectedInfoLog(ILoggingEvent m) {
        return m.getLevel() == Level.INFO && m.getLoggerName().equals(CityBean.class.getName());
    }
}
