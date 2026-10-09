# Archived migration plan

**Superseded:** Grimholt is no longer pursuing a Minestom source-fork migration as its active plan. This document is preserved only to explain prior design decisions; it is not a current work order.

The active direction is to build Grimholt as an independent runtime, with Chronos as the region-parallel simulation and scheduling kernel. The first implementation milestone is a bounded dispatch primitive; the next is integrating it into the real region lifecycle and removing duplicate tick-scheduling paths.

See:
- [Chronos architecture and rollout gates](CHRONOS.md)
- [Current architecture and ownership rules](ARCHITECTURE.md)
- [Forensic parity audit](FORENSIC-PARITY-AUDIT.md)

No full Minestom source import was completed. The old upstream pin and licensing notes in the repository must not be interpreted as evidence of an active dependency or completed fork.
