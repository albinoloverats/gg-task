package io.anderson.ashley.gg.log;

import io.anderson.ashley.gg.model.LogRequest;

public interface ILogService
{
	/**
	 * Log request to the database.
	 *
	 * @param request The LogRequest object, with details of what to store.
	 */
	void logRequest(LogRequest request);
}
