package io.anderson.ashley.gg.log;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.net.URI;
import java.sql.Timestamp;
import java.util.UUID;

@Entity
@NoArgsConstructor
@Data
public class LogEntity
{
	@Id
	private UUID requestId;
	@NonNull
	private URI requestUri;
	private URI validationUri;
	@NonNull
	private Timestamp requestTimestamp;
	private int responseStatus;
	@NonNull
	private String requestIpAddress;
	private String requestCountryCode;
	private String requestIpProvider;
	private long timeLapsed;
}
