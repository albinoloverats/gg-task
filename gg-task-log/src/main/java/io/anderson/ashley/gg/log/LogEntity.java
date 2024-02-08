package io.anderson.ashley.gg.log;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.net.URI;
import java.sql.Timestamp;
import java.util.UUID;

@Entity(name = "log_event")
@NoArgsConstructor
@Data
public class LogEntity
{
	@Id
	@Column(name = "request_id")
	private UUID requestId;
	@NonNull
	@Column(name = "request_uri")
	private URI requestUri;
	@Column(name = "validation_uri")
	private URI validationUri;
	@NonNull
	@Column(name = "request_timestamp")
	private Timestamp requestTimestamp;
	@Column(name = "response_status")
	private int responseStatus;
	@NonNull
	@Column(name = "request_ip_address")
	private String requestIpAddress;
	@Column(name = "request_country_code")
	private String requestCountryCode;
	@Column(name = "request_ip_provider")
	private String requestIpProvider;
	@Column(name = "time_lapsed")
	private long timeLapsed;
}
