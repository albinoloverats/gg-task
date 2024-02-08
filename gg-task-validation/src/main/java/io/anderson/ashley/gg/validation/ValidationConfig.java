package io.anderson.ashley.gg.validation;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "validation")
@Data
public class ValidationConfig
{
	private String requestUrl;
	private List<String> requestFields;
	private List<String> blockedCountries;
	private List<String> blockedProviders;
}
