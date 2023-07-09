package com.example.ratelimiter.service;

import com.example.ratelimiter.model.CounterPartyRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Берется первый контрагент из очереди который не занимает больше чем maxLimitInPercent основного пула запросов
 */
@Service
public class StaticRateLimitingRequestPlanner implements RequestPlanner {
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
        for (CounterPartyRequest request: waitingTasks) {
            long counterPartyInProgress = currentTasks.stream()
                .filter(req -> req.getCounterParty().equals(request.getCounterParty()))
                .count();
            if (maxLimitInPercent > 100 * counterPartyInProgress / poolRequestsSize) {
                return Optional.of(request);
            }
        }
        return Optional.empty();
    }
}
