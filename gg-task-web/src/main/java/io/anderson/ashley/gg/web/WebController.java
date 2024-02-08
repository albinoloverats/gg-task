package io.anderson.ashley.gg.web;

import io.anderson.ashley.gg.convert.model.ConvertDocumentCommand;
import io.anderson.ashley.gg.convert.model.ConvertDocumentResponse;
import io.anderson.ashley.gg.convert.model.Outcome;
import io.anderson.ashley.gg.log.model.LogRequestEvent;
import io.anderson.ashley.gg.validation.model.ValidationQuery;
import io.anderson.ashley.gg.validation.model.ValidationResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@AllArgsConstructor(onConstructor = @__(@Autowired))
@RequestMapping("/")
public class WebController
{
	private final WebConfig config;
	private final CommandGateway commandGateway;
	private final QueryGateway queryGateway;
	private final EventGateway eventGateway;

	/**
	 * Convert a CSV (in this case | [pipe]) document to JSON.
	 * CSV fields: UUID, ID, Name, Likes, Transport, Avg Speed, Top Speed
	 * JSON fields: Name, Transport, Top Speed
	 *
	 * @param httpRequest The HTTP request.
	 * @return A JSON document.
	 * @throws IOException Thrown if the request cannot be parsed.
	 */
	@PostMapping
	public CompletableFuture<List<Outcome>> convert(final HttpServletRequest httpRequest) throws IOException
	{
		final var start = Instant.now();

		final var requestUri = URI.create(httpRequest.getRequestURI());
		final var ipAddress = httpRequest.getRemoteAddr();

		ValidationResult validationResult = null;
		HttpStatusCode status = HttpStatus.OK;
		try
		{
			if (config.isIpValidationEnabled())
			{
				validationResult = queryGateway.query(new ValidationQuery(ipAddress), ResponseTypes.instanceOf(ValidationResult.class)).join();
				switch (validationResult.status())
				{
					case SUCCESS:
						break;

					case BLOCKED_IP:
						throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Blocking request based on country.");

					case BLOCKED_ISP:
						throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Blocking request based on ISP.");

					default:
						throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
				}
			}


			return commandGateway.<ConvertDocumentResponse>send(
							new ConvertDocumentCommand(IOUtils.toString(httpRequest.getReader())))
					.thenApply(ConvertDocumentResponse::outcomeList);
		}
		catch (final Exception e)
		{
			if (e instanceof ResponseStatusException)
			{
				status = ((ResponseStatusException)e).getStatusCode();
			}
			else
			{
				status = HttpStatus.INTERNAL_SERVER_ERROR;
			}
			throw e;
		}
		finally
		{
			final var logRequest = validationResultToLogRequest(start, Instant.now(), requestUri, ipAddress, validationResult, status);
			eventGateway.publish(logRequest);
		}
	}


	/**
	 * Helper method to create a LogRequest instance.
	 *
	 * @param uri              The request URI
	 * @param ipAddress        The IP address that was validated.
	 * @param validationResult The result of that validation.
	 * @param httpStatus       The HTTP status to return to the user.
	 * @return The new LogRequest object.
	 */
	private static LogRequestEvent validationResultToLogRequest(final Instant start,
	                                                            final Instant end,
	                                                            final URI uri,
	                                                            final String ipAddress,
	                                                            final ValidationResult validationResult,
	                                                            final HttpStatusCode httpStatus)
	{
		final var request = validationResult != null ? validationResult.request() : null;
		final var country = validationResult != null ? validationResult.country() : null;
		final var isp = validationResult != null ? validationResult.isp() : null;

		return new LogRequestEvent(UUID.randomUUID(),
				uri,
				request,
				ipAddress,
				start,
				end,
				httpStatus.value(),
				country,
				isp);
	}
}
