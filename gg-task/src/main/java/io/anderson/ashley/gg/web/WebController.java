package io.anderson.ashley.gg.web;

import io.anderson.ashley.gg.log.ILogService;
import io.anderson.ashley.gg.model.Entry;
import io.anderson.ashley.gg.model.LogRequest;
import io.anderson.ashley.gg.model.Outcome;
import io.anderson.ashley.gg.model.ValidationResult;
import io.anderson.ashley.gg.validation.IValidationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor(onConstructor = @__(@Autowired))
@RequestMapping("/")
public class WebController
{
	private static final String DEFAULT_DELIMITER = "|";

	private final WebConfig config;
	private final IValidationService validationService;
	private final ILogService logService;

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

		if (config.isIpValidationEnabled())
		{
			return validationService.validateIpAddress(ipAddress)
					.thenApply(result ->
					{
						HttpStatus status = HttpStatus.OK;
						try
						{
							switch (result.status())
							{
								case SUCCESS:
									try
									{
										return process(parseEntries(httpRequest.getReader()));
									}
									catch (final IOException e)
									{
										status = HttpStatus.INTERNAL_SERVER_ERROR;
										throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing EntryFile", e);
									}

								case BLOCKED_IP:
									status = HttpStatus.FORBIDDEN;
									throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Blocking request based on country.");

								case BLOCKED_ISP:
									status = HttpStatus.FORBIDDEN;
									throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Blocking request based on ISP.");

								default:
									status = HttpStatus.INTERNAL_SERVER_ERROR;
									throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
							}
						}
						finally
						{
							logService.logRequest(validationResultToLogRequest(start, Instant.now(), requestUri, ipAddress, result, status));
						}
					});
		}
		else
		{
			HttpStatus status = HttpStatus.OK;
			try
			{
				return CompletableFuture.completedFuture(process(parseEntries(httpRequest.getReader())));
			}
			catch (final Exception e)
			{
				status = HttpStatus.INTERNAL_SERVER_ERROR;
				throw e;
			}
			finally
			{
				logService.logRequest(validationResultToLogRequest(start, Instant.now(), requestUri, ipAddress, null, status));
			}
		}
	}

	/**
	 * Process all entries in to outcomes.
	 *
	 * @param entries The list of Entry objects.
	 * @return A list of Outcome objects.
	 */
	private List<Outcome> process(final List<Entry> entries)
	{
		return entries.stream().map(Outcome::fromEntry).collect(Collectors.toList());
	}

	/**
	 * Parse the request body for Entries
	 *
	 * @param reader The request body Reader instance.
	 * @return A list of Entry objects.
	 * @throws IOException Thrown if there is an issue parsing the Reader.
	 */
	private List<Entry> parseEntries(final Reader reader) throws IOException
	{
		final var csvFormat = CSVFormat.Builder
				.create(CSVFormat.DEFAULT)
				.setDelimiter(StringUtils.defaultIfEmpty(config.getEntryRecordDelimiter(), DEFAULT_DELIMITER))
				.build();
		final List<Entry> entries = new ArrayList<>();
		for (final CSVRecord record : csvFormat.parse(reader))
		{
			try
			{
				entries.add(new Entry(UUID.fromString(record.get(0)),
						record.get(1),
						record.get(2),
						record.get(3),
						record.get(4),
						new BigDecimal(record.get(5)),
						new BigDecimal(record.get(6))));
			}
			catch (final Exception e)
			{
				/*
				 * This does require restarting the application is the flag is changed. Using a service similar to
				 * Unleash would allow this to be toggled externally to the application and with (alsmot) immediate
				 * availability.
				 */
				if (config.isDataValidationEnabled())
				{
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed request data - disable data validation flag and try again!");
				}
			}
		}
		return entries;
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
	private static LogRequest validationResultToLogRequest(final Instant start,
	                                                       final Instant end,
	                                                       final URI uri,
	                                                       final String ipAddress,
	                                                       final ValidationResult validationResult,
	                                                       final HttpStatus httpStatus)
	{
		final var request = validationResult != null ? validationResult.request() : null;
		final var country = validationResult != null ? validationResult.country() : null;
		final var isp = validationResult != null ? validationResult.isp() : null;

		return new LogRequest(uri,
				request,
				ipAddress,
				start,
				end,
				httpStatus.value(),
				country,
				isp);
	}
}
