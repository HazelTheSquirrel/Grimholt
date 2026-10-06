# Performance engineering

The architecture keeps tick-critical ownership explicit and provides a bounded async scheduler. Metrics capture join/quit/command/plugin-failure counters and sampled tick timing.

Required benchmark matrix before a production claim:

| Load | Required evidence |
|---:|---|
| 50 | startup, steady-state tick, memory |
| 100 | same + chunk load |
| 250 | same + entity load |
| 500 | same + network load |
| 750 | same + plugin stress |
| 1000 | same + long soak |

No concurrency number is a guarantee. Results must name CPU, memory, world size, view/simulation distance, entity density and plugin set.
