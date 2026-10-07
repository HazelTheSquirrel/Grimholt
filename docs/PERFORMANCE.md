# Grimholt Performance Architecture

## Target

Grimholt is being engineered for high concurrency, with a target workload of **500–1000 simultaneous players** on suitable hardware. This is a measurable engineering target, never a universal hardware guarantee.

## Automatic hardware profile

At startup Grimholt detects:
- available logical processors/cores;
- physical system memory where the JVM exposes it;
- JVM maximum heap as a secondary constraint.

CPU capacity is a scheduling input. The architecture must avoid a hidden single-thread bottleneck and partition work by ownership instead of putting all world activity behind one global lock.

## Memory policy

Grimholt reserves **20% of physical system memory as a safety/system buffer**.

- Maximum planned Grimholt resource budget: **80%** of detected physical memory.
- Reserved system/safety budget: **20%**.
- Caches, worker queues and background systems must remain bounded by this policy.
- The JVM cannot safely resize its maximum heap after startup, so the launcher/startup configuration must honor the same budget. Runtime code must not try to consume the reserved 20%.

If physical-memory detection is unavailable, Grimholt falls back to the JVM-reported maximum heap for budgeting and reports that limitation.

## Multithreading model

The long-term runtime is divided into owned work domains:
1. region/world simulation;
2. entity/player simulation;
3. network processing;
4. world generation;
5. storage I/O;
6. background calculations;
7. monitoring/telemetry.

Each domain has explicit ownership, bounded queues and backpressure. Cross-domain state changes are explicit handoffs.

## Telemetry

At minimum measure:
- MSPT and tick jitter;
- TPS;
- CPU load;
- heap usage and GC pressure;
- active players;
- entities;
- loaded chunks;
- queue depth and wait time;
- chunk load/save latency;
- network pressure;
- plugin execution time;
- backpressure events.

## Adaptive runtime and KI

The first adaptive layer is deterministic and rule-based. A later Grimholt KI/ML component may optimize safe runtime parameters, but it is never allowed to mutate arbitrary gameplay state.

Required control path:
```
Telemetry
  -> Decision Engine
  -> Safety Limits
  -> Runtime Controller
  -> Audit / Rollback
```

Every automatic change must have:
- a measurable trigger;
- a bounded parameter range;
- cooldown/hysteresis;
- rollback;
- audit logging.

## Benchmark matrix

Every major performance change is validated at:

| Load | Required evidence |
|---:|---|
| 50 | startup, steady-state tick, memory |
| 100 | same + chunk load |
| 250 | same + entity load |
| 500 | same + network load |
| 750 | same + plugin stress |
| 1000 | same + long soak |

Results must record CPU, physical memory, JVM settings, world size, view/simulation distance, entity density and plugin set.
