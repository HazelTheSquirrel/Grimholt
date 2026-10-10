package dev.grimholt.protocol;

import java.io.IOException;
import java.util.Objects;

/** Per-connection STATUS state machine; packet input and output buffers remain transport-owned. */
public final class StatusSession {
    public enum State { HANDSHAKE, STATUS, CLOSE_AFTER_WRITE, CLOSED }
    private final StatusResponse response;
    private State state = State.HANDSHAKE;

    public StatusSession(StatusResponse response) {
        this.response = Objects.requireNonNull(response, "response");
    }

    public State state() { return state; }

    /** Processes one complete packet payload and appends any response to caller-owned output storage. */
    public int handleFrame(byte[] payload, int offset, int length, byte[] output, int outputOffset)
            throws IOException {
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(output, "output");
        if (offset < 0 || length < 0 || offset > payload.length - length) {
            throw new IndexOutOfBoundsException("Invalid packet payload range");
        }
        if (outputOffset < 0 || outputOffset > output.length) {
            throw new IndexOutOfBoundsException("Invalid output offset");
        }
        if (state == State.CLOSE_AFTER_WRITE || state == State.CLOSED) {
            throw new IOException("Packet received after session began closing");
        }

        if (state == State.HANDSHAKE) {
            HandshakePacket handshake = HandshakePacket.parse(payload, offset, length);
            if (handshake.nextState() != HandshakePacket.NextState.STATUS) {
                throw new IOException("LOGIN is not enabled yet");
            }
            state = State.STATUS;
            return 0;
        }

        final int packetId;
        try {
            packetId = VarInt.read(payload, offset, offset + length);
        } catch (IllegalArgumentException malformed) {
            throw new IOException("Malformed STATUS packet id", malformed);
        }
        if (packetId < 0) throw new IOException("Missing STATUS packet id");
        int packetIdLength = VarInt.sizeOf(packetId);
        return switch (packetId) {
            case 0 -> {
                if (length != packetIdLength) throw new IOException("STATUS request must not contain fields");
                yield StatusPacketCodec.encodeStatusResponse(response.toJson(), output, outputOffset);
            }
            case 1 -> {
                long token = StatusPacketCodec.readPingPayload(payload, offset, length);
                int written = StatusPacketCodec.encodePong(token, output, outputOffset);
                state = State.CLOSE_AFTER_WRITE;
                yield written;
            }
            default -> throw new IOException("Unsupported STATUS packet id: " + packetId);
        };
    }

    public void markClosed() { state = State.CLOSED; }
}
