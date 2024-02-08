package io.anderson.ashley.gg.convert.model;

import java.math.BigDecimal;
import java.util.UUID;

public record Entry(UUID uuid,
                    String id,
                    String name,
                    String likes,
                    String transport,
                    BigDecimal averageSpeed,
                    BigDecimal topSpeed)
{
}

