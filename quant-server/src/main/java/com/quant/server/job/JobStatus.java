package com.quant.server.job;

/**
 * Enum for async job status tracking.
 */
public enum JobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
