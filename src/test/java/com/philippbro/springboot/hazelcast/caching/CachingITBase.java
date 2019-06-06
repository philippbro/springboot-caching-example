package com.philippbro.springboot.hazelcast.caching;


import static org.awaitility.Awaitility.await;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.awaitility.core.ThrowingRunnable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.CacheManager;
import org.springframework.context.ConfigurableApplicationContext;

import com.hazelcast.spring.cache.HazelcastCacheManager;

class CachingITBase {

    static ConfigurableApplicationContext createClusterApp(int port) {
        return new SpringApplicationBuilder()
                .profiles("cluster")
                .sources(HazelcastCachingClusterApplication.class)
                .properties(Map.of("server.port", port))
                .run();
    }

    static ConfigurableApplicationContext createClientApp(int port) {
        return new SpringApplicationBuilder()
                .sources(HazelcastCachingClientApplication.class)
                .profiles("client")
                .properties(Map.of("server.port", port))
                .run();
    }


    void waitForCacheHandling(ThrowingRunnable asserts) {
        await().atMost(25, TimeUnit.SECONDS)
                .pollDelay(200, TimeUnit.MILLISECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(asserts);
    }

    protected CityBean getCityBean(ConfigurableApplicationContext context) {
        return context.getBean(CityBean.class);
    }

    protected CacheManager getCacheManager(ConfigurableApplicationContext context) {
        return context.getBean(CacheManager.class);
    }
}
