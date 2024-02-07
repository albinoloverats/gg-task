package io.anderson.ashley.gg.validation;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.maciejwalkowiak.wiremock.spring.ConfigureWireMock;
import com.maciejwalkowiak.wiremock.spring.EnableWireMock;
import com.maciejwalkowiak.wiremock.spring.InjectWireMock;
import io.anderson.ashley.gg.model.ValidationResult;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;

import java.nio.charset.Charset;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@EnableWireMock({
		@ConfigureWireMock(name = "validation-service", property = "validation.request-url")
})
public class ValidationServiceTest
{
	private static final String IP_OKAY = "86.8.134.206";
	private static final String IP_AWS = "52.17.7.98";
	private static final String IP_BLOCKED = "111.13.49.147";
	@Autowired
	private ValidationConfig config;
	private ValidationService target;
	@InjectWireMock("validation-service")
	private WireMockServer wireMock;
	@Value("classpath:Outcome.json")
	private Resource outcomeResource;
	@Value("classpath:ip-api-okay.json")
	private Resource okay;
	@Value("classpath:ip-api-blocked-ip.json")
	private Resource blockedIp;
	@Value("classpath:ip-api-blocked-isp.json")
	private Resource blockedIsp;

	@BeforeEach
	public void init()
	{
		target = new ValidationService(config);
	}

	@Test
	public void validateIpAddressOkay() throws JSONException
	{
		final var jsonObject = initWireMock(okay, IP_OKAY);

		final var result = target.validateIpAddress(IP_OKAY).join();

		assertEquals(ValidationResult.Status.SUCCESS, result.status());
		assertEquals(jsonObject.getString("countryCode"), result.country());
		assertEquals(jsonObject.getString("isp"), result.isp());
	}

	@Test
	public void validateIpAddressAWS() throws JSONException
	{
		final var jsonObject = initWireMock(blockedIsp, IP_AWS);

		final var result = target.validateIpAddress(IP_AWS).join();

		assertEquals(ValidationResult.Status.BLOCKED_ISP, result.status());
		assertEquals(jsonObject.getString("countryCode"), result.country());
		assertEquals(jsonObject.getString("isp"), result.isp());
	}

	@Test
	public void validateIpAddressChina() throws JSONException
	{
		final var jsonObject = initWireMock(blockedIp, IP_BLOCKED);

		final var result = target.validateIpAddress(IP_BLOCKED).join();

		assertEquals(ValidationResult.Status.BLOCKED_IP, result.status());
		assertEquals(jsonObject.getString("countryCode"), result.country());
		assertEquals(jsonObject.getString("isp"), result.isp());
	}

	@SneakyThrows
	private JSONObject initWireMock(final Resource resource, final String ipAddress)
	{
		final var jsonResponse = new JSONObject(resource.getContentAsString(Charset.defaultCharset()));
		final var request = String.format("/%s?%s", ipAddress, StringUtils.join(config.getRequestFields(), ","));

		wireMock.stubFor(get(request)
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/json")
						.withBody(jsonResponse.toString())
				));

		return jsonResponse;
	}
}
