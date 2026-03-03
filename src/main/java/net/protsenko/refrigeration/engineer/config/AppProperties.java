package net.protsenko.refrigeration.engineer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.pdf")
public class AppProperties {
    private int maxImagePages = 10;

    private int imageDpi = 200;
}