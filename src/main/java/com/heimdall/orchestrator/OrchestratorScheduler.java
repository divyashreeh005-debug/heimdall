package com.heimdall.orchestrator;

import com.heimdall.config.HeimdallProperties;
import com.heimdall.jobs.AnalysisJob;
import com.heimdall.jobs.AnalysisJobRepository;
import com.heimdall.jobs.JobQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Worker-role poll loop. Only active with --spring.profiles.active=worker
 * (docs/01-c4-architecture.md: "Orchestrator worker: same modular monolith,
 * different command"). The API role never claims jobs.
 */
@Component
@Profile("worker")
public class OrchestratorScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorScheduler.class);

    private final JobQueueService jobQueueService;
    private final AnalysisJobRepository jobRepository;
    private final OrchestratorService orchestratorService;
    private final HeimdallProperties properties;

    public OrchestratorScheduler(JobQueueService jobQueueService, AnalysisJobRepository jobRepository,
                                  OrchestratorService orchestratorService, HeimdallProperties properties) {
        this.jobQueueService = jobQueueService;
        this.jobRepository = jobRepository;
        this.orchestratorService = orchestratorService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${heimdall.worker.poll-interval-ms:2000}")
    public void pollOnce() {
        String workerId = properties.getWorker().getWorkerId();
        // claimNext returns the analysis_jobs.id; the job row carries the run_id
        // the orchestrator actually needs to process.
        Optional<UUID> claimedJobId = jobQueueService.claimNext(workerId);
        claimedJobId.ifPresent(jobId -> {
            AnalysisJob job = jobRepository.findById(jobId).orElseThrow();
            try {
                orchestratorService.runJob(job.getRunId());
                jobQueueService.markSucceeded(jobId);
            } catch (Exception e) {
                log.error("Job {} (run {}) failed", jobId, job.getRunId(), e);
                orchestratorService.markFailed(job.getRunId(), e.getMessage());
                jobQueueService.markFailed(jobId, e.getMessage());
            }
        });
    }
}
