package io.anderson.ashley.gg.validation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class IpApiResponse
{
	private String status;
	private String message;
	private String countryCode;
	private String isp;
}
