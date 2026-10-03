package com.heimdall.workspace;

public record ChangedFile(String path, String status, String contentHash) { }
