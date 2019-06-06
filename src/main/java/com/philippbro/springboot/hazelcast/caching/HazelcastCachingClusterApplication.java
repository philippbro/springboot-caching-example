package com.philippbro.springboot.hazelcast.caching;

import static java.lang.String.format;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.impl.proxy.MapProxyImpl;
import com.hazelcast.monitor.LocalMapStats;
import com.hazelcast.spring.cache.HazelcastCache;
import com.hazelcast.spring.cache.HazelcastCacheManager;

import lombok.extern.slf4j.Slf4j;


@SpringBootApplication(scanBasePackages = "com.philippbro.springboot.hazelcast.caching")
@EnableCaching
public class HazelcastCachingClusterApplication {

    private static final int FREE_HEAP_MB = 100;

    @Value("${hazelcast.group.name}")
    private String groupName;

    @Value("${hazelcast.group.password}")
    private String groupPassword;

    public static void main(String[] args) {
        new SpringApplicationBuilder()
                .profiles("cluster")
                .sources(HazelcastCachingClusterApplication.class)
                .properties(Map.of("server.port", 8091))
                .run(args);
    }

    @Bean
    @Profile("cluster")
    public Config createClusterConfig() {
        Config config = new Config();
        // maps
        MaxSizeConfig spaceForJvm = new MaxSizeConfig(FREE_HEAP_MB, MaxSizeConfig.MaxSizePolicy.FREE_HEAP_SIZE);

        MapConfig CITY_MAP_CONFIG = new MapConfig()
                .setName("city")
                .setMaxSizeConfig(spaceForJvm)
                .setEvictionPolicy(EvictionPolicy.LFU);

        // network
        NetworkConfig networkConfig = new NetworkConfig();
        networkConfig.setPort(5701);
        JoinConfig join = networkConfig.getJoin();
        join.getMulticastConfig().setEnabled(false);
        join.getTcpIpConfig()
                .setMembers(List.of("localhost"))
                .setEnabled(true);
        networkConfig.setJoin(join);
        config
                .setGroupConfig(new GroupConfig(groupName, groupPassword))
                .setNetworkConfig(networkConfig)
                .setInstanceName("hazelcast-instance")
                .addMapConfig(CITY_MAP_CONFIG);

        return config;
    }

    @RestController
    @Slf4j
    @Profile("cluster")
    static class StatisticsController {
        @Autowired
        HazelcastInstance hazelcastInstance;

        @Autowired
        HazelcastCacheManager hazelcastCacheManager;

        @RequestMapping("/cityStats")
        public String getCityStats() {
            String logFormat = "Cache city config: Eviction: %s, Backups: %s (read: %b), MaxSizePolicy: %s, %s";

            MapConfig city = hazelcastInstance.getConfig().getMapConfig("city");
            String formatted = format(logFormat, city.getEvictionPolicy().toString(), city.getBackupCount(), city.isReadBackupData(), city.getMaxSizeConfig().getMaxSizePolicy().toString(), city.getMaxSizeConfig().getSize());
            log.info(formatted);
            return formatted;
        }

        @RequestMapping("/cityCache/{city}")
        public String getCityValue(@PathVariable String city) {
            String logFormat = "Cache Entry for Key %s is %s";

            Cache cache = hazelcastCacheManager.getCache("city");
            String formatted = null;
            if (cache != null) {
                formatted = format(logFormat, city, Optional.ofNullable(cache.get(city)).map(ValueWrapper::get).orElse(null));
            }
            log.info(formatted);
            return formatted;
        }
    }
}
