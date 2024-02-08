package io.anderson.ashley.gg.log;

import io.anderson.ashley.gg.log.model.LogRequestEvent;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class LogService
{
	private final LogRepository repository;

	/**
	 * As something has happened - an event - namely a request was processed, we now persist its details to the DB.
	 *
	 * @param request The request details to persist.
	 */
	@EventHandler
	public void logRequest(@NonNull final LogRequestEvent request)
	{
		if (repository.existsById(request.eventId()))
		{
			return;
		}
		final var entry = new LogEntity();
		entry.setRequestId(request.eventId());
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
