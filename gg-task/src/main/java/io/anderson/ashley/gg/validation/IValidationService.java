package io.anderson.ashley.gg.validation;

import io.anderson.ashley.gg.model.ValidationResult;

import java.util.concurrent.CompletableFuture;

public interface IValidationService
{
	/**
	 * Validate the given IP address.
	 *
	 * @param ipAddress The IP address
	 * @return Details of whether the validation was successful or not.
	 */
	CompletableFuture<ValidationResult> validateIpAddress(String ipAddress);
}
