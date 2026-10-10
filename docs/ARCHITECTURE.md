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

1. The selector accepts a socket and assigns connection-owned receive/send buffers.
2. PacketFrameDecoder extracts complete length-prefixed frames without copying payloads.
3. HandshakePacket validates the first packet and selects STATUS or LOGIN.
4. StatusSession permits only legal STATUS request and ping packets; LOGIN is rejected until implemented.
5. Responses are appended to the connection-owned output buffer; partial writes remain queued until the socket accepts them.
6. Connection limits, frame limits, fixed buffers, and idle expiry bound resource consumption.

## Implemented protocol pieces

- Signed 32-bit VarInt encode/decode primitives.
- Length-prefixed packet frame decoder with maximum-size enforcement.
- Strict handshake parser for packet ID 0x00, protocol version, UTF-8 server address, unsigned port, and STATUS/LOGIN next state.
- JSON status response serialization with protocol/version and player counts.
- STATUS request and ping/pong processing, preserving the client's 64-bit token.
- Non-blocking TCP listener with a selector-based event loop and bounded connection resources.

Status JSON is generated on request, not in a tick loop. Packet codecs write framed packets into caller-owned output buffers. This is a development milestone only: login, encryption, compression, chunk streaming, simulation, and persistence are not enabled.

## Next milestones

1. Integration tests against real Minecraft client status requests, including fragmented/coalesced TCP reads.
2. Versioned login state machine and encryption/compression negotiation.
3. Player lifecycle and bounded packet dispatch.
4. World/chunk representation, region ownership, and asynchronous persistence.
5. Load testing on documented hardware, measuring memory, queue wait, throughput, and p50/p95/p99 latency before making capacity claims.
