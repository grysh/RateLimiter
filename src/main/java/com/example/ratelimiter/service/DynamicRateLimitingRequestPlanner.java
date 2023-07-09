package com.example.ratelimiter.service;

import com.example.ratelimiter.model.CounterParty;
import com.example.ratelimiter.model.CounterPartyRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Сначала из общей очереди ожидания выбираются самые долго ожидающие запросы для каждого контрагента
 * Затем для каждого такого запроса вычисляется функция приоритета которая зависит от 3-х параметров
 * 1. процент использования общей очереди данного контрагента
 * 2. время ожидания в очереди запросов
 * 3. среднее время ответа контрагента
 */
@Service
public class DynamicRateLimitingRequestPlanner implements RequestPlanner {
    private static final long DEFAULT_RESPONSE_TIME_WHEN_UNKNOWN = 100L;
    /**
     * максимальный процент использования общей очереди
     */
    @Value( "${ratelimitting.percent.max:80}" )
    private int maxLimitInPercent;
    /**
     * размер пула выполняющихся запросов
     */
    @Value("${pool.size:5}")
    private int poolRequestsSize;

    @Override
    public Optional<CounterPartyRequest> getNext(List<CounterPartyRequest> waitingTasks,
                                                 List<CounterPartyRequest> currentTasks,
                                                 List<CounterPartyRequest> completedTasks) {
        Map<CounterParty, List<CounterPartyRequest>> collect = waitingTasks.stream()
            .collect(Collectors.groupingBy(CounterPartyRequest::getCounterParty));
        List<CounterPartyRequest> list = new ArrayList<>(collect.values().stream().map(l -> l.get(0)).toList());
        list.removeIf(r -> maxLimitInPercent <= 100 * currentTasks.stream()
            .filter(req -> req.getCounterParty().equals(r.getCounterParty()))
            .count() / poolRequestsSize);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        Map<CounterParty, List<CounterPartyRequest>> collectStat = completedTasks.stream()
            .collect(Collectors.groupingBy(CounterPartyRequest::getCounterParty));
        Map<CounterParty, Long> averageResponseTimes = new HashMap<>();
        for (CounterParty key: collectStat.keySet()) {
            averageResponseTimes.put(key, (long) collectStat.get(key).stream()
                .mapToLong(req -> req.getEndRequestTime() - req.getStartRequestTime())
                .average().orElse(DEFAULT_RESPONSE_TIME_WHEN_UNKNOWN));
        }

        Comparator<CounterPartyRequest> comparator = (req1, req2) -> {
            double c1 = 1;
            double c2 = 1;
            long value1 = (long) (c1 * req1.getWaitingTime() - c2 * averageResponseTimes.getOrDefault(req1.getCounterParty(), DEFAULT_RESPONSE_TIME_WHEN_UNKNOWN));
            long value2 = (long) (c1 * req2.getWaitingTime() - c2 * averageResponseTimes.getOrDefault(req2.getCounterParty(), DEFAULT_RESPONSE_TIME_WHEN_UNKNOWN));
            return Long.compare(value1, value2);
        };

        return Optional.of(list.stream().sorted(comparator).toList().get(0));
    }
}
