package io.anderson.ashley.gg.web;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "web")
@Data
public class WebConfig
{
	private boolean ipValidationEnabled;
}
