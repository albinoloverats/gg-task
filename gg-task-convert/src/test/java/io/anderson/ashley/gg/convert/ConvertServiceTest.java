package io.anderson.ashley.gg.convert;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.anderson.ashley.gg.convert.model.ConvertDocumentCommand;
import io.anderson.ashley.gg.convert.model.Outcome;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.charset.Charset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ConvertServiceTest
{
	@Autowired
	private ConvertConfig config;
	@Value("classpath:EntryFile.txt")
	private Resource entryFileResource;
	@Value("classpath:Outcome.json")
	private Resource outcomeResource;
	private ConvertService target;
	private String entriesRaw;
	private List<Outcome> expected;

	@BeforeEach
	@SneakyThrows
	public void init()
	{
		target = new ConvertService(config);
		entriesRaw = entryFileResource.getContentAsString(Charset.defaultCharset());
		final var string = outcomeResource.getContentAsString(Charset.defaultCharset());
		final var objectMapper = new ObjectMapper();
		expected = objectMapper.readValue(string, new TypeReference<>() {});
	}

	@Test
	public void convertDocument()
	{
		final var actual = target.convertDocument(new ConvertDocumentCommand(entriesRaw)).outcomeList();
		assertEquals(expected.size(), actual.size());
		for (final var outcome : actual)
		{
			assertTrue(expected.contains(outcome));
		}
	}
}
