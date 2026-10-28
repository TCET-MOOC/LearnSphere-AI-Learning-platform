package com.MOOC.OnlineLearningPlatfrom.Config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * W2-11: Caffeine cache configuration for public read models (catalogue, course detail, departments).
 * Rules: NFR-02, NFR-03, TTL <= 60 s, recordStats for hit/miss metrics.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_PUBLIC_CATALOGUE = "public_catalogue";
    public static final String CACHE_PUBLIC_COURSE_DETAIL = "public_course_detail";
    public static final String CACHE_PUBLIC_DEPARTMENTS = "public_departments";
    public static final String CACHE_PUBLIC_DEPARTMENT_COURSES = "public_department_courses";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                CACHE_PUBLIC_CATALOGUE,
                CACHE_PUBLIC_COURSE_DETAIL,
                CACHE_PUBLIC_DEPARTMENTS,
                CACHE_PUBLIC_DEPARTMENT_COURSES
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(1000)
                .recordStats());
        return cacheManager;
    }
}
