package io.anderson.ashley.gg.model;

import lombok.NonNull;

import java.net.URI;

public record ValidationResult(@NonNull URI request,
                               @NonNull Status status,
                               String country,
                               String isp)
{
	public enum Status
	{
		SUCCESS,
		UNKNOWN_ERROR,
		BLOCKED_IP,
		BLOCKED_ISP
	}
}
