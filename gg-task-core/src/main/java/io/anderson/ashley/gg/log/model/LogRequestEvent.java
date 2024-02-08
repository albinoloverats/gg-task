package io.anderson.ashley.gg.log.model;

import lombok.NonNull;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record LogRequestEvent(@NonNull UUID eventId,
                              @NonNull URI userRequest,
                              URI validationRequest,
                              @NonNull String ipAddress,
                              @NonNull Instant start,
                              @NonNull Instant end,
                              int httpStatus,
                              String country,
                              String isp)
{
}
