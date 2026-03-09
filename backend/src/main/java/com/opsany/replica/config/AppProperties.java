package com.opsany.replica.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Cors cors = new Cors();
    private Session session = new Session();
    private Cache cache = new Cache();
    private Seed seed = new Seed();
    private Messaging messaging = new Messaging();

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<String>(Arrays.asList("http://localhost:5173"));
    }

    @Getter
    @Setter
    public static class Session {
        private long ttlHours = 8;
        private String store = "memory";
    }

    @Getter
    @Setter
    public static class Cache {
        private long bootstrapTtlMinutes = 5;
        private boolean bootstrapEnabled = false;
    }

    @Getter
    @Setter
    public static class Seed {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Messaging {
        private boolean consumerEnabled = false;
        private boolean publishEnabled = false;
        private boolean fallbackOnPublishFailure = true;
    }
}
