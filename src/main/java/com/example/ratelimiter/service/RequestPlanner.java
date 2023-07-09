package com.example.ratelimiter.service;

import com.example.ratelimiter.model.CounterPartyRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface RequestPlanner {
    Optional<CounterPartyRequest> getNext(List<CounterPartyRequest> waitingTasks,
                                          List<CounterPartyRequest> currentTasks,
                                          List<CounterPartyRequest> completedTasks);
}
