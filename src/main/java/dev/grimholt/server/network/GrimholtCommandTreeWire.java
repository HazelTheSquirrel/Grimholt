package dev.grimholt.server.network;

import dev.grimholt.server.command.GrimholtCommandDispatcher;
import java.io.*;
import java.util.*;

/** Minimal Brigadier-compatible 26.2 command tree generated from Grimholt's own registry. */
public final class GrimholtCommandTreeWire {
    public byte[] encode(GrimholtCommandDispatcher dispatcher) {
        try {
            List<String> names = dispatcher.registeredNames();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            VanillaProtocol26_2.writeVarInt(out, names.size() + 1);
            for (int i = 0; i < names.size() + 1; i++) {
                if (i == 0) {
                    out.write(0); // root
                    VanillaProtocol26_2.writeVarInt(out, names.size());
                    for (int n = 1; n <= names.size(); n++) VanillaProtocol26_2.writeVarInt(out, n);
                } else {
                    out.write(1 | 4); // literal + executable
                    VanillaProtocolCodec.writeString(out, names.get(i - 1), 32767);
                    VanillaProtocol26_2.writeVarInt(out, 0);
                }
            }
            VanillaProtocol26_2.writeVarInt(out, 0); // root node index
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
