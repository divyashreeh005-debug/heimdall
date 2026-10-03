package com.heimdall.workspace;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Clones the PR head into a scratch dir and computes the diff against base.
 * docs/01-c4-architecture.md: "analysis-workspace" module, worker role.
 * Runs in the rootless sandbox pod with no network beyond the initial clone
 * (docs/07-security.md, docs/08-deployment-podman.md).
 *
 * Uses the system `git` binary via ProcessBuilder rather than JGit so the
 * sandbox's network egress policy (git clone only, then offline) is easy to
 * enforce at the pod/network layer instead of in library code.
 */
@Service
public class AnalysisWorkspaceService {

    public AnalysisWorkspace prepare(String cloneUrl, String baseSha, String headSha) throws IOException, InterruptedException {
        Path scratch = Files.createTempDirectory("heimdall-run-");
        run(scratch, "git", "clone", "--no-checkout", cloneUrl, ".");
        run(scratch, "git", "fetch", "origin", headSha, baseSha);
        run(scratch, "git", "checkout", headSha);

        List<ChangedFile> changed = new ArrayList<>();
        String diffNames = run(scratch, "git", "diff", "--name-status", baseSha, headSha);
        for (String line : diffNames.lines().toList()) {
            if (line.isBlank()) continue;
            String[] parts = line.split("\\s+", 2);
            String status = parts[0];
            String path = parts.length > 1 ? parts[1] : "";
            String hash = status.equals("D") ? null : sha256(scratch.resolve(path));
            changed.add(new ChangedFile(path, status, hash));
        }
        return new AnalysisWorkspace(scratch, baseSha, headSha, changed);
    }

    /** Incremental re-run support: skip re-analyzing files whose content hash is unchanged since parent_run_id. */
    public boolean unchangedSince(ChangedFile file, java.util.Map<String, String> parentRunFileHashes) {
        String previous = parentRunFileHashes.get(file.path());
        return previous != null && previous.equals(file.contentHash());
    }

    private String sha256(Path file) {
        try {
            if (!Files.exists(file)) return null;
            byte[] bytes = Files.readAllBytes(file);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String run(Path cwd, String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .directory(cwd.toFile())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        boolean finished = process.waitFor(120, TimeUnit.SECONDS);
        if (!finished || process.exitValue() != 0) {
            throw new IOException("Command failed: " + String.join(" ", command) + "\n" + output);
        }
        return output;
    }
}
