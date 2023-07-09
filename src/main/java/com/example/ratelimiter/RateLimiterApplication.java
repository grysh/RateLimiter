package com.example.ratelimiter;

import com.example.ratelimiter.model.CounterPartyRequest;
import com.example.ratelimiter.service.RequestPoller;
import com.example.ratelimiter.service.RequestSubmitter;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@SpringBootApplication
@Slf4j
@AllArgsConstructor
public class RateLimiterApplication implements CommandLineRunner {
    @Setter
    private final RequestSubmitter requestSubmitter;

    @Setter
    private final RequestPoller requestPooler;

    public static void main(String[] args) {
        SpringApplication.run(RateLimiterApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        //очередь всех ожидающих запросов
        List<CounterPartyRequest> waitingTasks = new CopyOnWriteArrayList<>();

        // Класс эмулирующий поступление новых запросов в случайном порядке. Работает в отдельном потоке
        requestSubmitter.setWaitingTasks(waitingTasks);
        requestSubmitter.start();

        // Класс эмулирующий пулинг запросов из очереди waitingTasks. Работает в отдельном потоке
        requestPooler.setWaitingTasks(waitingTasks);
        requestPooler.start();
    }
}
