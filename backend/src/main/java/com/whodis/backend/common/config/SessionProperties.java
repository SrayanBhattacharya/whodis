package com.whodis.backend.common.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "whodis.session")
public class SessionProperties {
    private Duration duration;
}
