package dev.lunarcoffee.niochat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Client {
    private final SocketChannel channel;
    private String name = null;

    private static final int BUFFER_SIZE = Short.MAX_VALUE / 4;

    public Client(SocketChannel channel) {
        this.channel = channel;
    }

    public ByteBuffer readFully() {
        var buffer = ByteBuffer.allocate(BUFFER_SIZE);
        try {
            if (channel.read(buffer) < 0)
                return null;
        } catch (IOException e) {
            return null;
        }
        return buffer;
    }

    public void writeFully(ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining())
            channel.write(buffer);
    }

    public void close() {
        try {
            channel.close();
        } catch (IOException ignored) {}
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isNameSet() {
        return name != null;
    }
}
