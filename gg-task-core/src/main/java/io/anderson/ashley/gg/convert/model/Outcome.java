package io.anderson.ashley.gg.convert.model;

import java.math.BigDecimal;

public record Outcome(String name, String transport, BigDecimal topSpeed)
{

	/**
	 * Create an Outcome object from the given Entry object,
	 *
	 * @param entry An Entry
	 * @return An Outcome
	 */
	public static Outcome fromEntry(final Entry entry)
	{
		return new Outcome(entry.name(), entry.transport(), entry.topSpeed());
	}
}
