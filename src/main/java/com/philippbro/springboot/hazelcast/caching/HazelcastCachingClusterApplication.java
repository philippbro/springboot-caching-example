package com.philippbro.springboot.hazelcast.caching;

import static java.lang.String.format;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.config.NearCacheConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.HazelcastInstance;

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
//                .setNearCacheConfig(new NearCacheConfig()
//                        .setCacheLocalEntries(true)
//                        .setInMemoryFormat(InMemoryFormat.OBJECT))
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

        @RequestMapping("/city")
        public String getCityStats() {
            String logFormat = "Cache city config: Eviction: %s, Backups: %s (read: %b), MaxSizePolicy: %s, %s";

            MapConfig city = hazelcastInstance.getConfig().getMapConfig("city");
            String formatted = format(logFormat, city.getEvictionPolicy().toString(), city.getBackupCount(), city.isReadBackupData(), city.getMaxSizeConfig().getMaxSizePolicy().toString(), city.getMaxSizeConfig().getSize());
            log.info(formatted);
            return formatted;
        }
    }
}
