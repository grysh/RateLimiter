package com.example.ratelimiter.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * CounterParty task
 */
@RequiredArgsConstructor
@Data
public class CounterPartyRequest {
    /**
     * Контрагент.
     */
    private final CounterParty counterParty;
    /**
     * Время выполнения запроса в ms.
     */
    private final long responseTime;
    /**
     * Время поступления запроса.
     */
    private long arrivalTime;
    /**
     * Время начала выполнения запроса.
     */
    private long startRequestTime;
    /**
     * Время окончания выполнения запроса.
     */
    private long endRequestTime;
    /**
     * Index.
     */
    private long index;

    public long getWaitingTime() {
        return System.currentTimeMillis() - arrivalTime;
    }

    @SneakyThrows
    public void request() {
        Thread.sleep(responseTime);
    }

    @Override
    public String toString() {
        return "{" +
               "cp=" + counterParty +
               ", responseTime=" + responseTime +
               '}';
    }
}
