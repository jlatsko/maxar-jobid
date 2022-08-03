package com.maxar.jobid.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobsJson {
    public List<JobIdResponse> jobs = new ArrayList<>();
}

@SpringBootTest
class JobidApplicationTests {
    private static final Logger logger = LoggerFactory.getLogger(JobidApplicationTests.class);

    RestTemplate restTemplate = new RestTemplate();

    List<Integer> ids = new ArrayList();

    static String GetJobUri = "http://localhost:8080/";
    static String GetAsyncJobUri = "http://localhost:8080/async/";

    @BeforeEach
    void setup() {
        for (int cnt = 1; cnt != 2001; cnt++) {
            ids.add(cnt);
        }
    }

    @Test
    void invoke_20_requests() {
        List<JobIdResponse> responses = new ArrayList<>();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        logger.info("starting 20");
        for (int cnt = 1; cnt != 21; cnt++) {
            // invoke sync request
            JobIdResponse res = restTemplate.getForObject(GetJobUri + cnt, JobIdResponse.class);
            responses.add(res);
        }
        stopWatch.stop();
        logger.info("finished 2k in: " + stopWatch.getTotalTimeMillis());
        assertEquals(20, responses.size());
    }

    @Test
    void invoke_20_async_requests() {
        List<JobIdResponse> responses = new ArrayList<>();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        logger.info("starting async 20: ");
        for (int cnt = 1; cnt != 21; cnt++) {
            JobIdResponse res = restTemplate.getForObject(GetAsyncJobUri + cnt, JobIdResponse.class);
            responses.add(res);
        }
        stopWatch.stop();
        logger.info("finished async 20 in: " + stopWatch.getTotalTimeMillis());
        assertEquals(20, responses.size());
    }

    @Test
    public void http_uri_method_is_get() {
        RequestEntity request = RequestEntity
                .get(GetJobUri + "21")
                .accept(MediaType.APPLICATION_JSON).build();

        ResponseEntity<JobIdResponse> response = restTemplate.exchange(request, JobIdResponse.class);

        assertTrue(response.getStatusCode() == HttpStatus.OK && request.getMethod() == HttpMethod.GET);
    }

    @Test
    public void sleep1To10_sleeps_between_1and10_seconds() throws Exception {
        Map<Integer, Long> elapsedTimeMap = new HashMap();
        StopWatch stopWatch = new StopWatch();
        JobIdController jic = new JobIdController();

        for (int cnt = 1; cnt != 10; cnt++) {
            stopWatch.start("cnt " + cnt);
            jic.sleep1To10(cnt);
            stopWatch.stop();
            long elapsedTime = stopWatch.getLastTaskTimeMillis();
            elapsedTimeMap.put(cnt, elapsedTime);
        }

        elapsedTimeMap.entrySet().stream().forEach(entry -> assertTrue(entry.getValue() <= 10000 && entry.getValue() >= 1000));
    }

    @Test
    public void async_response_contains_listof_jobids_andUUid() throws Exception {

        final Executor executor = Executors.newFixedThreadPool(Math.min(ids.size(), 100),
                (Runnable r) -> {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    return t;
                });

        List<CompletableFuture<JobIdResponse>> jobIdFutures = ids.stream()
                .map(id -> CompletableFuture.supplyAsync(() ->
                                restTemplate.getForObject("http://localhost:8080/" + id, JobIdResponse.class),
                        executor))
                .collect(Collectors.toList());

        List<JobIdResponse> respList = jobIdFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        JobsJson jsonJob = new JobsJson();
        respList.forEach(resp -> jsonJob.jobs.add(resp));

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String jobsJson = ow.writeValueAsString(jsonJob);

        System.out.println("### FINISHED PROCESSING ###");
        System.out.println(jobsJson);
    }
}
