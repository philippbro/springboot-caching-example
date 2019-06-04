package com.philippbro.springboot.hazelcast.caching;


import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.lang.System.nanoTime;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication(scanBasePackages = "com.philippbro.springboot.hazelcast.caching")
@EnableCaching
public class HazelcastCachingClientApplication {

	@Value("${hazelcast.group.name}")
	private String groupName;

	@Value("${hazelcast.group.password}")
	private String groupPassword;

	public static void main(String[] args) {
		new SpringApplicationBuilder()
				.sources(HazelcastCachingClientApplication.class)
				.profiles("client")
				.run(args);
	}

	@Bean
	@Profile("client")
	public ClientConfig createClientConfig() {
		ClientConfig clientConfig = new ClientConfig();
		clientConfig.setGroupConfig(new GroupConfig(groupName, groupPassword));
		clientConfig.setNetworkConfig(new ClientNetworkConfig().setAddresses(List.of("localhost")));
		return clientConfig;
	}

	@Bean
	public IDummyBean dummyBean() {
		return new DummyBean();
	}

	@Bean
	CacheManager cacheManager() {
		return new HazelcastCacheManager(hazelcastInstance());
	}

	@Bean
	KeyGenerator keyGenerator() {
		return new SimpleKeyGenerator();
	}

	@Bean
	@Profile("client")
	HazelcastInstance hazelcastInstance() {
		// for client HazelcastInstance LocalMapStatistics will not available
		return HazelcastClient.newHazelcastClient();
	}

	@RestController
	@Slf4j
	@Profile("client")
	static class CityController {

		@Autowired
		IDummyBean dummy;

		@Autowired
		HazelcastInstance hazelcastInstance;

		@RequestMapping("/city")
		public String getCity() {
			String logFormat = "%s call took %d millis with result: %s";
			long start1 = nanoTime();
			String city = dummy.getCity();
			long end1 = nanoTime();
			log.info(format(logFormat, "Rest", TimeUnit.NANOSECONDS.toMillis(end1 - start1), city));
			return city;
		}

		@RequestMapping(value = "city/{city}", method = RequestMethod.GET)
		public String setCity(@PathVariable String city) {
			return dummy.setCity(city);
		}
	}

}
