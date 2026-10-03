package com.heimdall.jobs;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, UUID> {
}
