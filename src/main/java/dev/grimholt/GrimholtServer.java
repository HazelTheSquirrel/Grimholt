package dev.grimholt;

import dev.grimholt.protocol.StatusResponse;
import dev.grimholt.transport.StatusServer;

/** Executable Grimholt server entry point. Currently serves protocol STATUS queries only. */
public final class GrimholtServer {
    private GrimholtServer() { }

    public static void main(String[] args) throws Exception {
        String host = "0.0.0.0";
        int port = 25565;
        for (String arg : args) {
            if (arg.startsWith("--host=")) host = arg.substring("--host=".length());
            else if (arg.startsWith("--port=")) port = Integer.parseInt(arg.substring("--port=".length()));
            else throw new IllegalArgumentException("Unknown argument: " + arg);
        }

        StatusResponse response = new StatusResponse("Grimholt development server", "Grimholt Dev", 767, 512, 0);
        try (StatusServer server = new StatusServer(host, port, response)) {
            Runtime.getRuntime().addShutdownHook(new Thread(server::close, "grimholt-shutdown"));
            server.run();
        }
    }
}
