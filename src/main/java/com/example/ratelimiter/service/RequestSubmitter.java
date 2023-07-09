package com.example.ratelimiter.service;

import com.example.ratelimiter.model.CounterParty;
import com.example.ratelimiter.model.CounterPartyRequest;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.LongStream;

/**
 * Класс эмулирующий поступление новых запросов в случайном порядке за период времени allRequestsPeriodMs
 * на основе структуры COUNTER_PARTIES и помещающий запросы в очередь waitingTasks. Работает в отдельном потоке
 */
@Slf4j
@Service
public class RequestSubmitter extends Thread {
    /**
     * структура описывающая всех контрагентов, время их отклика и общее число эмулируемых запросов по каждому
     */
    private static final List<CounterParty> COUNTER_PARTIES = List.of(
        new CounterParty("CP1", 1000, 100),
        new CounterParty("CP2",100, 10),
        new CounterParty("CP3",200, 10),
        new CounterParty("CP4",500, 10));

    /**
     * период времени в мс в течении которого эмулируется поступление всех запросов
     */
    @Value( "${requests.period:5000}" )
    private long allRequestsPeriodMs;

    /**
     * заполняемая очередь ожидающих запросов
     */
    @Setter
    private List<CounterPartyRequest> waitingTasks;

    @SneakyThrows
    @Override
    public void run() {
        List<CounterPartyRequest> initialRequests = new ArrayList<>();
        COUNTER_PARTIES.forEach(cp -> LongStream
            .range(0, cp.getRequestsCount())
            .forEach(i -> initialRequests
                .add(new CounterPartyRequest(cp, cp.getResponseTime()))));
        Collections.shuffle(initialRequests);
        log.info("all requests size: {}", initialRequests.size());

        List<Long> startingTimes = new Random()
            .longs(0, allRequestsPeriodMs)
            .distinct()
            .limit(initialRequests.size())
            .boxed()
            .sorted()
            .toList();

        long lastPoi = 0;
        log.info("starting to submitting requests");
        for (int i = 0; i < initialRequests.size(); i++) {
            long sleepTime = startingTimes.get(i) - lastPoi;
            lastPoi = startingTimes.get(i);
            Thread.sleep(sleepTime);
            CounterPartyRequest request = initialRequests.get(i);
            request.setArrivalTime(System.currentTimeMillis());
            waitingTasks.add(request);
//            log.info("submitted {} {}", i, request);
        }
        log.info("all {} requests submitted", initialRequests.size());
    }
}
