package com.eventbooking.config;

import com.mongodb.MongoClientSettings;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.connection.SocketSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configures aggressive MongoDB connection timeouts so that:
 *   1. If MongoDB is running    → connects normally.
 *   2. If MongoDB is NOT running → fails fast (2 s) instead of hanging 30 s.
 *
 * Without this, the background monitor thread keeps retrying and the first
 * request that touches a Mongo repo throws MongoTimeoutException after a
 * long wait, degrading the user experience even though we handle the error.
 *
 * Only active when mongodb.enabled=true (the default).
 */
@Configuration
@ConditionalOnProperty(name = "mongodb.enabled", havingValue = "true")
public class MongoHealthConfig {

    @Bean
    public MongoClientSettingsBuilderCustomizer mongoTimeoutCustomizer() {
        return builder -> builder
            .applyToClusterSettings((ClusterSettings.Builder cluster) ->
                cluster.serverSelectionTimeout(2_000, TimeUnit.MILLISECONDS))
            .applyToSocketSettings((SocketSettings.Builder socket) ->
                socket.connectTimeout(2_000, TimeUnit.MILLISECONDS)
                      .readTimeout(5_000, TimeUnit.MILLISECONDS));
    }
}
