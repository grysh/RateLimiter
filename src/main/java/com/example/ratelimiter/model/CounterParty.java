package com.example.ratelimiter.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * КонтрАгент
 */
@AllArgsConstructor
@Data
public class CounterParty {
    /**
     * ID of counter party.
     */
    private final String id;
    /**
     * Response time in ms.
     */
    private final long responseTime;
    /**
     * Requests count.
     */
    private final long requestsCount;

    @Override
    public String toString() {
        return "{" + id + '}';
    }
}
