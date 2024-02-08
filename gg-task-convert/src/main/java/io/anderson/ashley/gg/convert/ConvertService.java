package io.anderson.ashley.gg.convert;

import com.google.common.annotations.VisibleForTesting;
import io.anderson.ashley.gg.convert.model.ConvertDocumentCommand;
import io.anderson.ashley.gg.convert.model.ConvertDocumentResponse;
import io.anderson.ashley.gg.convert.model.Entry;
import io.anderson.ashley.gg.convert.model.Outcome;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.axonframework.commandhandling.CommandHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class ConvertService
{
	@VisibleForTesting
	static final String DEFAULT_DELIMITER = "|";

	private final ConvertConfig config;


	/**
	 * Handle the command issued to do some work, namely convert the list of Entry objects into a list of Outcome
	 * objects.
	 *
	 * @param command The command issued to convert the objects.
	 * @return The response with the list of converted objects.
	 */
	@CommandHandler
	public ConvertDocumentResponse convertDocument(@NonNull final ConvertDocumentCommand command)
	{
		try
		{
			return new ConvertDocumentResponse(parseEntries(command.data())
					.stream()
					.map(Outcome::fromEntry)
					.collect(Collectors.toList()));
		}
		catch (final IOException e)
		{
			return new ConvertDocumentResponse(List.of());
		}
	}

	/**
	 * Parse the request body for Entries
	 *
	 * @param data The raw text to parse for Entry objects.
	 * @return A list of Entry objects.
	 * @throws IOException Thrown if there is an issue parsing the Reader.
	 */
	private List<Entry> parseEntries(final String data) throws IOException
	{
		final var csvFormat = CSVFormat.Builder
				.create(CSVFormat.DEFAULT)
				.setDelimiter(StringUtils.defaultIfEmpty(config.getEntryRecordDelimiter(), DEFAULT_DELIMITER))
				.build();
		final List<Entry> entries = new ArrayList<>();
		for (final CSVRecord record : csvFormat.parse(new StringReader(data)))
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
					throw new IOException("Malformed request data!");
				}
			}
		}
		return entries;
	}
}
