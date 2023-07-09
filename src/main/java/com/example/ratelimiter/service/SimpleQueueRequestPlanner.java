package com.example.ratelimiter.service;

import com.example.ratelimiter.model.CounterPartyRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SimpleQueueRequestPlanner implements RequestPlanner {
    @Override
    public Optional<CounterPartyRequest> getNext(List<CounterPartyRequest> waitingTasks,
                                                 List<CounterPartyRequest> currentTasks,
                                                 List<CounterPartyRequest> completedTasks) {
        return waitingTasks.isEmpty()
               ? Optional.empty()
               : Optional.of(waitingTasks.get(0));
    }
}
