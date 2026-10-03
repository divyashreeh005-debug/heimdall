package com.heimdall.jobs;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * PostgreSQL-backed job queue. No Redis, no Temporal (see
 * docs/adr/0008-postgres-job-queue.md). Claim uses SELECT ... FOR UPDATE
 * SKIP LOCKED exactly as specified in docs/02-domain-model.md.
 */
@Service
public class JobQueueService {

    private final AnalysisJobRepository repository;
    private final JdbcTemplate jdbcTemplate;

    public JobQueueService(AnalysisJobRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Enqueue a job for a run. Idempotency is enforced upstream on analysis_runs(repository_id, pr_number, head_sha). */
    @Transactional
    public AnalysisJob enqueue(UUID runId) {
        return repository.save(new AnalysisJob(runId));
    }

    /**
     * Claim exactly one queued job for this worker, atomically, using
     * SKIP LOCKED so multiple worker replicas never race on the same row.
     */
    @Transactional
    public Optional<UUID> claimNext(String workerId) {
        String sql = """
            UPDATE analysis_jobs
            SET state = 'running', locked_at = now(), locked_by = ?, attempts = attempts + 1
            WHERE id = (
                SELECT id FROM analysis_jobs
                WHERE state = 'queued'
                ORDER BY created_at
                FOR UPDATE SKIP LOCKED
                LIMIT 1
            )
            RETURNING id
            """;
        return jdbcTemplate.query(sql, rs -> rs.next() ? Optional.of((UUID) rs.getObject("id")) : Optional.empty(), workerId);
    }

    @Transactional
    public void markSucceeded(UUID jobId) {
        jdbcTemplate.update("UPDATE analysis_jobs SET state = 'succeeded' WHERE id = ?", jobId);
    }

    @Transactional
    public void markFailed(UUID jobId, String error) {
        AnalysisJob job = repository.findById(jobId).orElseThrow();
        job.setLastError(error);
        if (job.getAttempts() >= job.getMaxAttempts()) {
            job.setState(AnalysisJob.State.poison);
        } else {
            job.setState(AnalysisJob.State.queued);
            job.setLockedAt(null);
            job.setLockedBy(null);
        }
        repository.save(job);
    }
}
