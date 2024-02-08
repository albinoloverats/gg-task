package io.anderson.ashley.gg.log;

import io.anderson.ashley.gg.log.model.LogRequestEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@DataJpaTest
public class LogServiceTest
{
	private static final URI REQUEST = URI.create("http://localhost");
	private static final URI VALIDATION = URI.create("http:/ip-api.com");
	private static final String IP = "86.8.134.206";
	private static final Instant END = Instant.now();
	private static final Instant START = END.minus(Duration.ofSeconds(5));
	private static final int HTTP_STATUS = 418;
	private static final String COUNTRY_CODE = "GB";
	private static final String ISP = "Virgin Media";

	@Autowired
	private LogRepository repository;
	private LogService target;

	@BeforeEach
	public void init()
	{
		target = new LogService(repository);
	}

	@Test
	public void logRequest()
	{
		final var eventId = UUID.randomUUID();
		final var logRequest = new LogRequestEvent(eventId, REQUEST, VALIDATION, IP, START, END, HTTP_STATUS, COUNTRY_CODE, ISP);

		target.logRequest(logRequest);

		final var entities = repository.findAll();
		assertEquals(1, entities.size());
		final var entity = entities.get(0);
		assertEquals(eventId, entity.getRequestId());
		assertEquals(REQUEST, entity.getRequestUri());
		assertEquals(START, entity.getRequestTimestamp().toInstant());
		assertEquals(HTTP_STATUS, entity.getResponseStatus());
		assertEquals(IP, entity.getRequestIpAddress());
		assertEquals(COUNTRY_CODE, entity.getRequestCountryCode());
		assertEquals(ISP, entity.getRequestIpProvider());
		assertEquals(5000L, entity.getTimeLapsed());
	}

	@Test
	public void logRequestNoDuplicates()
	{
		final var eventId = UUID.randomUUID();
		final var logRequest = new LogRequestEvent(eventId, REQUEST, VALIDATION, IP, START, END, HTTP_STATUS, COUNTRY_CODE, ISP);

		target.logRequest(logRequest);

		assertEquals(1, repository.findAll().size());

		target.logRequest(logRequest);

		assertEquals(1, repository.findAll().size());
	}
}
