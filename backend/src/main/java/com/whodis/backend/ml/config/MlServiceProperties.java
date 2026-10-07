package com.whodis.backend.ml.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "whodis.ml")
public class MlServiceProperties {
    private String baseUrl;

}
