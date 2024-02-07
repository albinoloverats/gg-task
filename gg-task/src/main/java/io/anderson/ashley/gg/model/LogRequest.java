package io.anderson.ashley.gg.model;

import lombok.NonNull;

import java.net.URI;
import java.time.Instant;

public record LogRequest(@NonNull URI userRequest,
                         URI validationRequest,
                         @NonNull String ipAddress,
                         @NonNull Instant start,
                         @NonNull Instant end,
                         int httpStatus,
                         String country,
                         String isp)
{
}
