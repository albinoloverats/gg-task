package io.anderson.ashley.gg.convert;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "web")
@Data
public class ConvertConfig
{
	private String entryRecordDelimiter;
	private boolean dataValidationEnabled;
}
