package dev.grimholt.server.vanilla;

public record BlockPos(int x, int y, int z) {
    public long packed() {
        return ((long) (x & 0x3ffffff) << 38)
                | ((long) (z & 0x3ffffff) << 12)
                | (y & 0xfffL);
    }
}
