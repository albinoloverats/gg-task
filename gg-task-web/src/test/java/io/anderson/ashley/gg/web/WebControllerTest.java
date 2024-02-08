package io.anderson.ashley.gg.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.anderson.ashley.gg.convert.model.ConvertDocumentCommand;
import io.anderson.ashley.gg.convert.model.ConvertDocumentResponse;
import io.anderson.ashley.gg.convert.model.Outcome;
import io.anderson.ashley.gg.log.model.LogRequestEvent;
import io.anderson.ashley.gg.validation.model.ValidationQuery;
import io.anderson.ashley.gg.validation.model.ValidationResult;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.queryhandling.QueryGateway;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static io.anderson.ashley.gg.validation.model.ValidationResult.Status.BLOCKED_IP;
import static io.anderson.ashley.gg.validation.model.ValidationResult.Status.BLOCKED_ISP;
import static io.anderson.ashley.gg.validation.model.ValidationResult.Status.SUCCESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebController.class)
@EnableConfigurationProperties(WebConfig.class)
public class WebControllerTest
{
	@Autowired
	private MockMvc client;
	@Autowired
	private WebConfig config;
	@MockBean
	private CommandGateway commandGateway;
	@MockBean
	private QueryGateway queryGateway;
	@MockBean
	private EventGateway eventGateway;
	@Value("classpath:EntryFile.txt")
	private Resource entryFileResource;
	@Value("classpath:Outcome.json")
	private Resource outcomeResource;

	@Test
	public void convertIpOkay() throws Exception
	{
		final List<Outcome> outcome = new ObjectMapper().readValue(outcomeResource.getContentAsString(Charset.defaultCharset()), new TypeReference<>() {});

		when(commandGateway.send(any(ConvertDocumentCommand.class)))
				.thenReturn(CompletableFuture.completedFuture(new ConvertDocumentResponse(outcome)));
		when(queryGateway.query(any(ValidationQuery.class), isA(ResponseType.class)))
				.thenReturn(CompletableFuture.completedFuture(generateValidationResult(SUCCESS)));

		client.perform(asyncDispatch(client.perform(MockMvcRequestBuilders
								.post("/")
								.content(entryFileResource.getContentAsString(Charset.defaultCharset()))
								.contentType(MediaType.TEXT_PLAIN)
								.accept(MediaType.APPLICATION_JSON))
						.andReturn()))
				.andExpect(status().isOk())
				.andExpect(result ->
				{
					final var actual = new JSONArray(result.getResponse().getContentAsString());
					JSONAssert.assertEquals(new JSONArray(new JsonMapper().writeValueAsString(outcome)), actual, true);
				});

		verify(eventGateway).publish(any(LogRequestEvent.class));
	}

	@Test
	public void convertBlockedIp() throws Exception
	{
		when(queryGateway.query(any(ValidationQuery.class), isA(ResponseType.class)))
				.thenReturn(CompletableFuture.completedFuture(generateValidationResult(BLOCKED_IP)));

		client.perform(MockMvcRequestBuilders
						.post("/")
						.content(entryFileResource.getContentAsString(Charset.defaultCharset()))
						.contentType(MediaType.TEXT_PLAIN)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());

		verify(eventGateway).publish(any(LogRequestEvent.class));
	}

	@Test
	public void convertBlockedIsp() throws Exception
	{
		when(queryGateway.query(any(ValidationQuery.class), isA(ResponseType.class)))
				.thenReturn(CompletableFuture.completedFuture(generateValidationResult(BLOCKED_ISP)));

		client.perform(MockMvcRequestBuilders
						.post("/")
						.content(entryFileResource.getContentAsString(Charset.defaultCharset()))
						.contentType(MediaType.TEXT_PLAIN)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());

		verify(eventGateway).publish(any(LogRequestEvent.class));
	}

	private ValidationResult generateValidationResult(final ValidationResult.Status status)
	{
		return new ValidationResult(URI.create("localhost"),
				status,
				"GB",
				"Virgin Media");
	}
}
