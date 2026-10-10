# Grimholt Server

Grimholt is an independent Minecraft server implementation in Java 25. It is being rebuilt around its own runtime, protocol, session, world, simulation, and storage boundaries.

## Current status

The executable opens a non-blocking TCP listener and answers Minecraft STATUS handshakes, status requests, and ping/pong exchanges. LOGIN and gameplay are not enabled yet. This is a protocol milestone, not a playable game server.

## Requirements and run

- JDK 25
- Maven 3.9+

    mvn --batch-mode clean verify
    java -jar target/grimholt-server-0.1.0-SNAPSHOT.jar
    java -jar target/grimholt-server-0.1.0-SNAPSHOT.jar --host=127.0.0.1 --port=25566

The default listener binds to port 25565 on all interfaces. Open the firewall only if you intend to expose the status listener publicly.

## Implemented

- Java 25 bootstrap and runtime capacity detection.
- Bounded background executor and region mailbox primitives.
- Primitive chunk/block coordinate helpers.
- VarInt codecs, bounded frame decoder, and strict handshake parser.
- JSON status serialization and status/ping/pong packet codecs.
- Per-connection STATUS state machine and non-blocking selector TCP listener.
- Bounded to 512 connections, 16 KiB frames, fixed receive/send buffers, and a 30-second idle timeout.
- CI verification and runnable JAR artifact upload.

## Not implemented

LOGIN authentication, encryption/compression negotiation, player sessions, chunk streaming, authoritative simulation, world persistence, and gameplay are not enabled. Protocol version 767 is a development default and must be updated when targeting another Minecraft release.

## Performance and architecture

Network I/O and packet framing run on the selector thread; gameplay work must not be added there. Packet payloads are borrowed views into connection-owned buffers. Connection count, frame length, buffer sizes, and idle lifetime are bounded. Mutable world regions will have a single writer, with cross-region changes represented as explicit messages. Pooling is not introduced without profiling evidence.

The goal of 500+ concurrent players is a design target, not a performance claim. Load tests are required before asserting capacity.
