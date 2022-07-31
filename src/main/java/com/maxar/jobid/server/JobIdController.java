package com.maxar.jobid.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.lang.Thread.sleep;


@RestController
public class JobIdController {
    private static final Logger logger = LoggerFactory.getLogger(JobIdController.class);

    Random RANDOM_DELAY = new Random();
    private static int DELAY_MIN = 1;
    private static int DELAY_MAX = 9;


    @GetMapping(value = "/{jobId}")
    public JobIdResponse getId(@PathVariable("jobId") Integer jobId)  {

        sleep1To10(jobId);
        logger.info("Done sleeping for jobId: " + jobId);
        return new JobIdResponse(jobId, UUID.randomUUID().toString());
    }

    @GetMapping(value = "async/{jobId}")
    @Async
    public CompletableFuture<JobIdResponse> getAsyncId(@PathVariable("jobId") Integer jobId)  {

        sleep1To10(jobId);
        logger.info("ASync done sleeping for jobId: " + jobId);
        return CompletableFuture.completedFuture(new JobIdResponse(jobId, UUID.randomUUID().toString()));
    }

    protected void sleep1To10(Integer jobId)  {
        int delay = (RANDOM_DELAY.nextInt(DELAY_MAX)+DELAY_MIN) * 1000;
        logger.info("Sleeping job: " + jobId + " for: " + delay + " milli secs");
        try { sleep(delay); }
        catch (InterruptedException ie) {Thread.currentThread().interrupt(); }
    }
}
