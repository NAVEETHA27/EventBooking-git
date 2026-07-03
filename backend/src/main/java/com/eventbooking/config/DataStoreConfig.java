package com.eventbooking.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Separates Spring Data JPA repositories from MongoDB repositories.
 *
 * JPA repos   → com.eventbooking.repository        (always enabled)
 * Mongo repos → com.eventbooking.repository.mongo  (only when mongodb.enabled=true)
 *
 * When mongodb.enabled=false:
 *   - No MongoRepository beans are created.
 *   - No MongoDB connection attempt is made at startup.
 *   - All MySQL/JPA features continue working normally.
 *   - Services that use Mongo repos inject them as Optional<> and degrade gracefully.
 */
@Configuration
public class DataStoreConfig {

    /**
     * JPA repository scanning — always active, regardless of MongoDB status.
     * Explicitly excludes the mongo sub-package to prevent cross-store confusion
     * when both Spring Data JPA and MongoDB are on the classpath.
     */
    @Configuration
    @EnableJpaRepositories(
        basePackages = "com.eventbooking.repository",
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
            type = org.springframework.context.annotation.FilterType.REGEX,
            pattern = "com\\.eventbooking\\.repository\\.mongo\\..*"
        )
    )
    static class JpaConfig { }

    /**
     * MongoDB repository scanning — only active when mongodb.enabled=true.
     * When false, none of the MongoRepository beans are created, so no
     * MongoClient connection is attempted during startup.
     */
    @Configuration
    @ConditionalOnProperty(name = "mongodb.enabled", havingValue = "true")
    @EnableMongoRepositories(
        basePackages = "com.eventbooking.repository.mongo"
    )
    static class MongoConfig { }
}
