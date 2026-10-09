# Architecture and implementation status

## Design constraints

- Target runtime: Java 25.
- Keep socket I/O, protocol decoding, and world work on separate execution paths.
- Never execute application work on an I/O thread after a bounded queue rejects it; overload must be explicit rather than silently moving work onto the caller.
- Parse packet payloads as borrowed byte slices. Copy data only when it must outlive the receive buffer.
- Validate frame sizes and packet fields before allocating or dispatching application work.
- Use bounded queues and connection limits to make memory use predictable under load.
- Optimize measured hot paths; avoid object pooling unless profiling demonstrates that reuse beats modern JVM allocation.

## Packet processing path

1. A transport layer reads bytes into a connection-owned receive buffer.
2. `PacketFrameDecoder` extracts complete length-prefixed frames without copying payloads.
3. The protocol layer parses the packet identifier and fields from the borrowed frame slice.
4. A connection state machine validates that the packet is legal for the current state.
5. Accepted work is handed to the appropriate bounded executor or region mailbox.

An incomplete frame remains in the receive buffer for the next read. Empty, negative, malformed, and oversized frames are rejected before dispatch.

## Implemented protocol pieces

- Signed 32-bit VarInt encode/decode primitives.
- Length-prefixed packet frame decoder with maximum-size enforcement.
- Client handshake parser for packet ID `0x00`, protocol version, UTF-8 server address, unsigned port, and STATUS/LOGIN next state.

The handshake parser is a protocol primitive, not yet a live network listener or a complete login implementation.

## Current limitation

The executable entry point is still diagnostic. A live TCP accept loop, status response/ping handling, encryption/compression negotiation, login authentication, chunk streaming, and gameplay are not enabled. Each stage should be added behind tests before the executable starts advertising itself as a playable server.
