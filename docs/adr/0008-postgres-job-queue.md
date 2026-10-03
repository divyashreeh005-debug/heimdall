# ADR-008 — PostgreSQL job queue before Redis

- Status: accepted
- Date: 2026-09-12

## Context

The originating essay included Redis for cache/queue. The POC already requires PostgreSQL. A second data plane adds failure modes for little throughput benefit at expected load (tens of PRs per hour).

## Decision

- Analysis jobs are rows claimed with `FOR UPDATE SKIP LOCKED`.
- File-hash cache for incremental analysis lives on the worker volume and in Postgres metadata, not Redis.
- Redis is explicitly later, if queue latency or pub/sub requires it.

## Consequences

- Fewer pods and secrets.
- Job semantics are transactional with domain writes.
- Very high event rates would need a later broker; that is not the POC.
