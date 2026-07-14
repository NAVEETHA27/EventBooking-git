package com.eventbooking.config;

import com.mongodb.connection.ClusterSettings;
import com.mongodb.connection.SocketSettings;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configures aggressive MongoDB connection timeouts.
 *
 * Applied ALWAYS (regardless of mongodb.enabled) so that if MongoDB is not
 * running, the Spring Boot autoconfigured MongoClient fails fast (2 s)
 * instead of blocking every request for 30 s.
 *
 * Without this, even when mongodb.enabled=false the MongoClient still gets
 * autoconfigured from spring.data.mongodb.uri and its 30-second server
 * selection timeout causes login and other requests to hang.
 */
@Configuration
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
