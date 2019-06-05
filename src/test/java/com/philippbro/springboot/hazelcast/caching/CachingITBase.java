package com.philippbro.springboot.hazelcast.caching;


import static org.awaitility.Awaitility.await;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.awaitility.core.ThrowingRunnable;
import org.junit.BeforeClass;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class CachingITBase {

    static ConfigurableApplicationContext clusterContext;
    static ConfigurableApplicationContext clientContext;

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


    protected void waitForCacheHandling(ThrowingRunnable asserts) {
        await().atMost(15, TimeUnit.SECONDS)
                .pollDelay(200, TimeUnit.MILLISECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(asserts);
    }

}
