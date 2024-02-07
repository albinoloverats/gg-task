package io.anderson.ashley.gg.validation;

import io.anderson.ashley.gg.model.ValidationResult;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static io.anderson.ashley.gg.model.ValidationResult.Status.BLOCKED_IP;
import static io.anderson.ashley.gg.model.ValidationResult.Status.BLOCKED_ISP;
import static io.anderson.ashley.gg.model.ValidationResult.Status.SUCCESS;
import static io.anderson.ashley.gg.model.ValidationResult.Status.UNKNOWN_ERROR;

@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class ValidationService implements IValidationService
{
	private final ValidationConfig config;

	/*
	 * On a bigger project, with more in-depth IP address validation, this validation service could be moved to its own
	 * microservice, and communicated with via an event messaging framework, maybe something like Axon.
	 */

	/**
	 * {@inheritDoc}.
	 */
	public CompletableFuture<ValidationResult> validateIpAddress(@NonNull final String ipAddress)
	{
		final var request = buildRequestUrl(ipAddress);

		/*
		 * If expanded, it might be worth using something like Swagger to auto-generate the client code.
		 */
		return WebClient.create(request)
				.get()
				.retrieve()
				.bodyToMono(IpApiResponse.class)
				.toFuture()
				.thenApply(response ->
				{
					final var status = calculateStatus(response);
					final var countryCode = response.getCountryCode();
					final var isp = response.getIsp();
					return new ValidationResult(URI.create(request), status, countryCode, isp);
				});
	}

	/**
	 * Determine whether the response from IP-API is a success or not.
	 *
	 * @param response The IP-API response.
	 * @return A validation status.
	 */
	private ValidationResult.Status calculateStatus(final IpApiResponse response)
	{
		if (isCountryBlocked(response.getCountryCode()))
		{
			return BLOCKED_IP;
		}
		if (isIspBlocked(response.getIsp()))
		{
			return BLOCKED_ISP;
		}
		// IP-API could not determine IP details; if it's a local network IP then assume that's okay
		if (response.getStatus().equals("fail") && !isPrivateIp(response.getMessage()))
		{
			return UNKNOWN_ERROR;
		}
		return SUCCESS;
	}

	private boolean isCountryBlocked(final String countryCode)
	{
		if (countryCode == null)
		{
			return false;
		}
		return config.getBlockedCountries().contains(countryCode);
	}

	private boolean isIspBlocked(final String isp)
	{
		if (isp == null)
		{
			return false;
		}
		return config.getBlockedProviders().stream().anyMatch(isp::contains);
	}

	private boolean isPrivateIp(final String message)
	{
		if (message == null)
		{
			return false;
		}
		return message.contains("private");
	}

	private String buildRequestUrl(final String ipAddress)
	{
		return String.format("%s/%s?%s", config.getRequestUrl(), ipAddress, StringUtils.join(config.getRequestFields(), ","));
	}
}
