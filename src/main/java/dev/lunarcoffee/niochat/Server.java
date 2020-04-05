package dev.lunarcoffee.niochat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

public class Server {
    private final ServerSocketChannel serverChannel;
    private final Selector selector = Selector.open();
    private final ClientHandler clientHandler = new ClientHandler();

    private static final Logger LOG = LogManager.getLogger(Server.class);

    public Server(ServerSocketChannel serverChannel) throws IOException {
        this.serverChannel = serverChannel;
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void run() {
        try (selector) {
            try (serverChannel) {
                while (serverChannel.isOpen()) {
                    selector.select();
                    var selected = selector.selectedKeys();

                    for (var key : selected)
                        if (key.isAcceptable())
                            clientHandler.processAccept(key, selector);
                        else if (key.isReadable())
                            clientHandler.processRead(key);
                        else if (key.isWritable())
                            clientHandler.processWrite(key);
                    selected.clear();
                }
            }
        } catch (IOException e) {
            LOG.error("An IO error occurred during normal operation:");
            e.printStackTrace();
        } finally {
            try {
                serverChannel.close();
            } catch (IOException e) {
                LOG.error("An IO error occurred during normal operation:");
                e.printStackTrace();
            }
        }
    }
}
