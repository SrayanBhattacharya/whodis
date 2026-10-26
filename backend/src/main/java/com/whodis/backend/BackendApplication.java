package com.whodis.backend;

import com.whodis.backend.common.config.SessionProperties;
import com.whodis.backend.common.config.StorageProperties;
import com.whodis.backend.ml.config.MlServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({
        SessionProperties.class,
        StorageProperties.class,
        MlServiceProperties.class
})
@EnableScheduling
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}
