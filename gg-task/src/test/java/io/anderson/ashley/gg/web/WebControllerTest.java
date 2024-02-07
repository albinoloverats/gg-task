package io.anderson.ashley.gg.web;

import io.anderson.ashley.gg.log.ILogService;
import io.anderson.ashley.gg.model.LogRequest;
import io.anderson.ashley.gg.model.ValidationResult;
import io.anderson.ashley.gg.validation.IValidationService;
import lombok.SneakyThrows;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

import static io.anderson.ashley.gg.model.ValidationResult.Status.BLOCKED_IP;
import static io.anderson.ashley.gg.model.ValidationResult.Status.BLOCKED_ISP;
import static io.anderson.ashley.gg.model.ValidationResult.Status.SUCCESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebController.class)
public class WebControllerTest
{
	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private WebConfig config;
	@MockBean
	private IValidationService validationService;
	@MockBean
	private ILogService logService;

	@Value("classpath:EntryFile.txt")
	private Resource entryFileResource;
	@Value("classpath:MalformedEntryFile.txt")
	private Resource malformedEntryFileResource;
	@Value("classpath:Outcome.json")
	private Resource outcomeResource;
	@Value("classpath:MalformedOutcome.json")
	private Resource malformedOutcomeResource;

	@Test
	public void convert() throws Exception
	{
		config.setIpValidationEnabled(false);
		config.setDataValidationEnabled(true);

		final var mvcResult = mockMvc.perform(MockMvcRequestBuilders
						.post("/")
						.content(getEntryFile(entryFileResource))
						.contentType(MediaType.TEXT_PLAIN)
						.accept(MediaType.APPLICATION_JSON))
				.andReturn();

		mockMvc.perform(asyncDispatch(mvcResult))
				.andExpect(status().isOk())
				.andExpect(result ->
				{
					final var actual = new JSONArray(result.getResponse().getContentAsString());
					JSONAssert.assertEquals(getOutcome(outcomeResource), actual, true);
				});
	}

	@Test
	public void convertMalformedEntryValidationEnabled() throws Exception
	{
		config.setIpValidationEnabled(false);
		config.setDataValidationEnabled(true);

		mockMvc.perform(MockMvcRequestBuilders
						.post("/")
						.content(getEntryFile(malformedEntryFileResource))
						.contentType(MediaType.TEXT_PLAIN)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void convertMalformedEntryValidationDisabled() throws Exception
	{
		config.setIpValidationEnabled(false);
		config.setDataValidationEnabled(false);

		final var mvcResult = mockMvc.perform(MockMvcRequestBuilders
						.post("/")
						.content(getEntryFile(malformedEntryFileResource))
						.contentType(MediaType.TEXT_PLAIN)
						.accept(MediaType.APPLICATION_JSON))
				.andReturn();

		mockMvc.perform(asyncDispatch(mvcResult))
				.andExpect(status().isOk())
				.andExpect(result ->
				{
					final var actual = new JSONArray(result.getResponse().getContentAsString());
					JSONAssert.assertEquals(getOutcome(malformedOutcomeResource), actual, true);
				});
	}

	@Test
	public void convertIpValidationDisabled() throws Exception
	{
		config.setIpValidationEnabled(false);
		config.setDataValidationEnabled(true);

		final var mvcResult = mockMvc.perform(MockMvcRequestBuilders
						.post("/")
						.content(getEntryFile(entryFileResource))
						.contentType(MediaType.TEXT_PLAIN)
						.accept(MediaType.APPLICATION_JSON))
				.andReturn();

		mockMvc.perform(asyncDispatch(mvcResult))
				.andExpect(status().isOk())
				.andExpect(result ->
				{
					final var actual = new JSONArray(result.getResponse().getContentAsString());
					JSONAssert.assertEquals(getOutcome(outcomeResource), actual, true);
				});
	}

	@Test
	public void convertIpOkay() throws Exception
	{
		config.setIpValidationEnabled(true);
		config.setDataValidationEnabled(true);

		final var validationResult = generateValidationResult(SUCCESS);

		when(validationService.validateIpAddress(any(String.class)))
				.thenReturn(CompletableFuture.completedFuture(validationResult));

		final var mvcResult = mockMvc.perform(MockMvcRequestBuilders
						.post("/")
						.content(getEntryFile(entryFileResource))
						.contentType(MediaType.TEXT_PLAIN)
						.accept(MediaType.APPLICATION_JSON))
				.andReturn();

		mockMvc.perform(asyncDispatch(mvcResult))
				.andExpect(status().isOk())
				.andExpect(result ->
				{
					final var actual = new JSONArray(result.getResponse().getContentAsString());
					JSONAssert.assertEquals(getOutcome(outcomeResource), actual, true);
				});

		verify(logService).logRequest(any(LogRequest.class));
	}

	@Test
	public void convertBlockedIp() throws Exception
	{
		config.setIpValidationEnabled(true);
		config.setDataValidationEnabled(true);

		final var validationResult = generateValidationResult(BLOCKED_IP);

		when(validationService.validateIpAddress(any(String.class)))
				.thenReturn(CompletableFuture.completedFuture(validationResult));

		final var mvcResult = mockMvc.perform(MockMvcRequestBuilders
						.post("/")
						.content(getEntryFile(entryFileResource))
						.contentType(MediaType.TEXT_PLAIN)
						.accept(MediaType.APPLICATION_JSON))
				.andReturn();

		mockMvc.perform(asyncDispatch(mvcResult))
				.andExpect(status().isForbidden());

		verify(logService).logRequest(any(LogRequest.class));
	}

	@Test
	public void convertBlockedIsp() throws Exception
	{
		config.setIpValidationEnabled(true);
		config.setDataValidationEnabled(true);

		final var validationResult = generateValidationResult(BLOCKED_ISP);

		when(validationService.validateIpAddress(any(String.class)))
				.thenReturn(CompletableFuture.completedFuture(validationResult));

		final var mvcResult = mockMvc.perform(MockMvcRequestBuilders
						.post("/")
						.content(getEntryFile(entryFileResource))
						.contentType(MediaType.TEXT_PLAIN)
						.accept(MediaType.APPLICATION_JSON))
				.andReturn();

		mockMvc.perform(asyncDispatch(mvcResult))
				.andExpect(status().isForbidden());

		verify(logService).logRequest(any(LogRequest.class));
	}

	private ValidationResult generateValidationResult(final ValidationResult.Status status)
	{
		return new ValidationResult(URI.create("localhost"),
				status,
				"GB",
				"Virgin Media");
	}

	@SneakyThrows
	private String getEntryFile(final Resource entryFile)
	{
		return entryFile.getContentAsString(Charset.defaultCharset());
	}

	@SneakyThrows
	private JSONArray getOutcome(final Resource outcome)
	{
		return new JSONArray(outcome.getContentAsString(Charset.defaultCharset()));
	}
}
