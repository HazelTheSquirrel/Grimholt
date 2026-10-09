# Minestom upstream provenance

## Fork policy

Grimholt's chosen technical base is a **source-level fork** of Minestom. The source must be checked into the Grimholt repository and built as part of Grimholt. A dependency on the published `net.minestom:minestom` artifact is not an acceptable substitute.

## Pinned starting point

- Upstream repository: https://github.com/Minestom/Minestom
- Upstream branch: `26_3`
- Upstream commit: `b7c32f3bfa079e426ce817ec41a7fca0e625e48a`
- Commit URL: https://github.com/Minestom/Minestom/commit/b7c32f3bfa079e426ce817ec41a7fca0e625e48a
- License: Apache License 2.0 (upstream `LICENSE`)
- Minecraft target for Grimholt: Java Edition 26.4 Snapshot 3

This revision is a **starting point for source integration**, not proof that it supports Minecraft 26.4 Snapshot 3. Protocol compatibility must be tested against the exact pinned client before it is claimed.

## Required import procedure

1. Import the source tree and build logic from the pinned upstream revision into Grimholt.
2. Retain upstream copyright, patent, attribution and license notices. Include a copy of the Apache-2.0 license and any applicable upstream NOTICE material in source and binary distributions.
3. Mark files modified from upstream with prominent modification notices as required by Apache-2.0.
4. Keep a changelog or patch series that maps Grimholt changes to the upstream base; do not silently erase provenance.
5. Build and test the imported source locally in this repository. Do not depend on a published Minestom runtime artifact.
6. Reconcile existing Grimholt code by subsystem; avoid two independent network stacks, session models or world authorities in the production path.
7. Update the pinned upstream commit only through a reviewed update that records the diff, license/NOTICE changes, compatibility results and performance comparisons.

## Scaling principles

- Detect available processors and container CPU limits, but do not equate logical thread count with useful parallelism.
- Separate blocking I/O from CPU-bound work; use bounded queues and explicit backpressure.
- Preserve a single mutation owner for each mutable world/entity/region partition; communicate cross-owner changes through ordered messages.
- Keep packet writes ordered across login, compression and encryption transitions.
- Measure tick duration percentiles, queue depth, task wait time, allocation rate, GC pauses, network throughput and per-core utilization.
- Size worker pools adaptively from measured workload and hardware; do not create one unbounded thread per task.
- Require repeatable networked benchmarks before claiming 500–1000-player capacity.

## Current status

This file records the selected upstream and policy. The source import itself is **not yet complete** on the current `test` branch.
