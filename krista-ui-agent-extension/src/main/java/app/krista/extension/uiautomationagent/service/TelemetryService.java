/*
 * Krista Ui Agent Extension Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package app.krista.extension.uiautomationagent.service;

import app.krista.common.monitoring.TelemetryAccess;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.logs.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Singleton telemetry service for emitting metrics and structured logs to SigNoz
 * via OpenTelemetry. Uses TelemetryAccess.getInstance() directly — same pattern as studioAI.
 *
 * All methods are fire-and-forget: exceptions are caught and logged so telemetry
 * failures never propagate into the main request flow.
 */
public class TelemetryService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);
    private static volatile TelemetryService instance;

    private final TelemetryAccess telemetryAccess;

    private TelemetryService(TelemetryAccess telemetryAccess) {
        this.telemetryAccess = telemetryAccess;
    }

    public static TelemetryService getInstance() {
        if (instance == null) {
            synchronized (TelemetryService.class) {
                if (instance == null) {
                    instance = new TelemetryService(TelemetryAccess.getInstance());
                }
            }
        }
        return instance;
    }

    /**
     * Emit a structured log to SigNoz. Searchable in SigNoz Logs Explorer.
     */
    public void recordLog(String logName, Severity severity, String body, Attributes attributes) {
        try {
            telemetryAccess.getOtelSdk()
                    .getLogsBridge()
                    .get(logName)
                    .logRecordBuilder()
                    .setSeverity(severity)
                    .setAllAttributes(attributes)
                    .setTimestamp(Instant.now())
                    .setBody(body)
                    .emit();
        } catch (Throwable e) {
            log.debug("Failed to emit telemetry log: {}", e.getMessage());
        }
    }

    /**
     * Emit a counter metric to SigNoz.
     */
    public void recordCounter(String metricName, String description, String unit, long value, Attributes attributes) {
        try {
            telemetryAccess.getMeter()
                    .counterBuilder(metricName)
                    .setDescription(description)
                    .setUnit(unit)
                    .build()
                    .add(value, attributes);
        } catch (Throwable e) {
            log.debug("Failed to emit telemetry counter: {}", e.getMessage());
        }
    }

    /**
     * Emit a histogram metric to SigNoz.
     */
    public void recordHistogram(String metricName, String description, String unit, long value, Attributes attributes) {
        try {
            telemetryAccess.getMeter()
                    .histogramBuilder(metricName)
                    .setDescription(description)
                    .setUnit(unit)
                    .ofLongs()
                    .build()
                    .record(value, attributes);
        } catch (Throwable e) {
            log.debug("Failed to emit telemetry histogram: {}", e.getMessage());
        }
    }

    // ── Convenience helpers for building attributes ──

    public static AttributesBuilder attrs() {
        return Attributes.builder();
    }

    public static void putIfPresent(AttributesBuilder builder, String key, String value) {
        if (value != null && !value.isEmpty()) {
            builder.put(key, value);
        }
    }
}
