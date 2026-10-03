package com.heimdall.workspace;

import java.nio.file.Path;
import java.util.List;

public record AnalysisWorkspace(
        Path root,
        String baseSha,
        String headSha,
        List<ChangedFile> changedFiles
) { }
