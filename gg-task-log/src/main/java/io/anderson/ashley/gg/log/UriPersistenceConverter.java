package io.anderson.ashley.gg.log;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.util.StringUtils;

import java.net.URI;

@Converter(autoApply = true)
public class UriPersistenceConverter implements AttributeConverter<URI, String>
{

	@Override
	public String convertToDatabaseColumn(final URI entityValue)
	{
		return (entityValue == null) ? null : entityValue.toASCIIString();
	}

	@Override
	public URI convertToEntityAttribute(final String databaseValue)
	{
		return (StringUtils.hasLength(databaseValue) ? URI.create(databaseValue.trim()) : null);
	}
}
