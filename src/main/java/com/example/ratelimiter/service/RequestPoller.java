package com.example.ratelimiter.service;

import com.example.ratelimiter.model.CounterParty;
import com.example.ratelimiter.model.CounterPartyRequest;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
@RequiredArgsConstructor
@Slf4j
@Service
public class RequestPoller extends Thread {
    private static final String DYNAMIC_ALGO = "dynamic";
    private static final String STATIC_ALGO = "static";
    /**
     * использовать ли алгоритм Rate Limit
     */
    @Value("${ratelimitting.algo:none}")
    private String rateLimitingAlgo;

    /**
     * размер пула выполняющихся запросов
     */
    @Value("${pool.size:5}")
    private int poolRequestsSize;

    /**
     * все ожидающие выполнения запросы
     */
    @Setter
    private List<CounterPartyRequest> waitingTasks;

    @Setter
    private final SimpleQueueRequestPlanner simpleQueueRequestPlanner;

    @Setter
    private final StaticRateLimitingRequestPlanner staticRateLimitingRequestPlanner;

    @Setter
    private final DynamicRateLimitingRequestPlanner dynamicRateLimitingRequestPlanner;

    /**
     * текущие выполняемые запросы
     */
    private final List<CounterPartyRequest> currentTasks = new CopyOnWriteArrayList<>();

    /**
     * все завершенные запросы
     */
    private final List<CounterPartyRequest> completedTasks = new CopyOnWriteArrayList<>();

    @Override
    public void run() {
        long completedRequests = 0;
        log.info("starting to pooling requests to queue with size {}", poolRequestsSize);
        RequestPlanner requestPlanner = DYNAMIC_ALGO.equals(rateLimitingAlgo) ?  dynamicRateLimitingRequestPlanner :
                                        STATIC_ALGO.equals(rateLimitingAlgo) ? staticRateLimitingRequestPlanner : simpleQueueRequestPlanner;
        while (!waitingTasks.isEmpty() || !currentTasks.isEmpty() || completedTasks.isEmpty()) {
            Optional<CounterPartyRequest> next = requestPlanner.getNext(waitingTasks, currentTasks, completedTasks);
            if (next.isPresent() && currentTasks.size() < poolRequestsSize && !currentTasks.contains(next.get())) {
                CounterPartyRequest request = next.get();
                request.setIndex(completedRequests++);
                request.setStartRequestTime(System.currentTimeMillis());
                Task task = new Task(request);
                CompletableFuture.supplyAsync(task::call).thenApply(this::onComplete);
                currentTasks.add(request);
                waitingTasks.remove(request);
            }
        }
        log.info("finished pooling all {} requests", completedTasks.size());
        showFullStatistic();
    }

    private CounterPartyRequest onComplete(CounterPartyRequest request) {
        request.setEndRequestTime(System.currentTimeMillis());
        currentTasks.remove(request);
        completedTasks.add(request);
        showStatistic(request);
        return request;
    }

    private void showStatistic(CounterPartyRequest request) {
        Map<CounterParty, List<CounterPartyRequest>> collect =
            completedTasks.stream().collect(Collectors.groupingBy(CounterPartyRequest::getCounterParty));
        String stat = collect.keySet().stream().map(k -> k.toString() + "-" + collect.get(k).size() + "[" + k.getRequestsCount() + "]")
            .collect(Collectors.joining (", "));
        log.info("[{}] finished. in pool/waiting/finished: [{}][{}][{}], by counterParty: [{}]", request.getIndex(), currentTasks.size(), waitingTasks.size(), completedTasks.size(), stat);
    }
    private void showFullStatistic() {
        //todo среднее время ожидания в очереди для каждого контрагента
        Map<CounterParty, List<CounterPartyRequest>> collect =
            completedTasks.stream().collect(Collectors.groupingBy(CounterPartyRequest::getCounterParty));
        for (CounterParty key: collect.keySet()) {
            List<CounterPartyRequest> counterPartyRequests = collect.get(key);
            double averageWaitingTime = counterPartyRequests.stream()
                .mapToLong(req ->  req.getStartRequestTime() - req.getArrivalTime())
                .average().getAsDouble();
            log.info("average waiting time for {} - {} ms", key, averageWaitingTime);
//            for (CounterPartyRequest req: counterPartyRequests) {
//                log.info("{}", req.getStartRequestTime() - req.getArrivalTime());
//            }
        }
    }

}
@AllArgsConstructor
@Slf4j
final class Task {
    private final CounterPartyRequest counterPartyRequest;
    public CounterPartyRequest call() {
//        log.info("[{}] executing request: {}", counterPartyRequest.getIndex(), counterPartyRequest);
        counterPartyRequest.request();
        return counterPartyRequest;
    }
}
