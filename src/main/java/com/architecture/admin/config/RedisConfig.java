package com.architecture.admin.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.ConfigureRedisAction;

import java.time.Duration;

/*****************************************************
 * redis 설정
 ****************************************************/
@EnableRedisRepositories
@Configuration
@RequiredArgsConstructor
public class RedisConfig {
    private final RedisProperties redisProperties;

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    @Value("${env.server}")
    private String server;

    /**
     * AWS 레디스 연동 오류로 추가
     * @return
     */
    @Bean
    ConfigureRedisAction configureRedisAction() {

        return ConfigureRedisAction.NO_OP;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        if (server.equals("local") || server.equals("dev")) {
            return new LettuceConnectionFactory(host, port);
        } else {
            RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration();
            clusterConfiguration.clusterNode(redisProperties.getHost(), redisProperties.getPort());
            LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
                    .clientOptions(ClientOptions.builder()
                            .socketOptions(SocketOptions.builder()
                                    .connectTimeout(Duration.ofMillis(RedisURI.DEFAULT_TIMEOUT)).build())
                            .build())
                    .commandTimeout(Duration.ofSeconds(RedisURI.DEFAULT_TIMEOUT)).build();
            return new LettuceConnectionFactory(clusterConfiguration, clientConfiguration);
        }
    }

    /**
     * DefaultRedisSerializer 추가하여 redis session 데이터에 불필요한 문자 입력 방지
     * @return RedisSerializer
     */
    @Bean(name="springSessionDefaultRedisSerializer")
    public RedisSerializer serializer() {
        return new GenericJackson2JsonRedisSerializer();
    }
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());

        //일반적인 key:value의 경우 시리얼라이저
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }
}
