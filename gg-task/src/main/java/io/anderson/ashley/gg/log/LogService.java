package io.anderson.ashley.gg.log;

import io.anderson.ashley.gg.model.LogRequest;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.UUID;

@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class LogService implements ILogService
{
	private final LogRepository repository;

	/*
	 * On a bigger project, with more verbose logging, this service could be moved to its own microservice, and
	 * communicated with via an event messaging framework, maybe something like Axon.
	 */

	/**
	 * {@inheritDoc}.
	 */
	public void logRequest(@NonNull final LogRequest request)
	{
		final var entry = new LogEntity();
		entry.setRequestId(UUID.randomUUID());
		entry.setRequestUri(request.userRequest());
		entry.setValidationUri(request.validationRequest());
		entry.setRequestTimestamp(Timestamp.from(request.start()));
		entry.setResponseStatus(request.httpStatus());
		entry.setRequestIpAddress(request.ipAddress());
		entry.setRequestCountryCode(request.country());
		entry.setRequestIpProvider(request.isp());
		entry.setTimeLapsed(request.end().toEpochMilli() - request.start().toEpochMilli());
		repository.save(entry);
	}
}
