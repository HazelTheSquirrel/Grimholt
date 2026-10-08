# Grimholt performance architecture and proof requirements

## Goal

The design target is 500–1000 concurrent players on a documented suitable hardware profile. This is an engineering target, not a capacity guarantee. No networked capacity evidence is currently established by the logical world-model microbenchmark.

## Resource budget

- Plan to use at most 80% of detected physical memory for Grimholt.
- Reserve 20% for the OS, native allocations and safety.
- If physical memory cannot be detected, use JVM maximum heap as a fallback and report that limitation.
- The launcher must set `-Xmx` consistently; a running JVM cannot safely resize its maximum heap.
- Bound queues, caches, generation concurrency and background work.

## Work ownership

Partition work by explicit region/world/entity ownership. Network, generation, storage and telemetry are separate work domains. No global lock should serialize the world. Cross-owner mutations use bounded queues with backpressure and metrics.

## Required telemetry

TPS; MSPT and jitter percentiles; CPU; heap/GC; player/entity/chunk counts; network throughput; chunk load/save latency; queue depth and wait time; rejected/backpressured work; plugin execution time; thread count and shutdown duration.

## Benchmark plan

| Players | Minimum evidence |
|---:|---|
| 50 | Join/quit, steady-state ticks, memory and chunk loads |
| 100 | Same plus chunk churn |
| 250 | Entity density and mixed movement |
| 500 | Network and chunk-load pressure |
| 750 | Plugin/event and queue pressure |
| 1000 | Long soak and failure/recovery behavior |

Record exact hardware, OS, JVM flags, world seed/size, view/simulation distances, entity density, workload scripts, client mix and commit SHA. Report p50/p95/p99 MSPT, TPS, GC pauses, memory, throughput, queue latency and errors.

## Adaptive controller

A future controller may tune only allow-listed bounded parameters using deterministic rules first. Required flow: telemetry → decision → safety limits → apply → audit/rollback. Every adjustment needs a trigger, range, cooldown/hysteresis and rollback. ML/AI must never mutate arbitrary gameplay state or be a prerequisite for correctness.
