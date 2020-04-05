package dev.lunarcoffee.niochat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

public class ServerRunner {
    private static final Logger LOG = LogManager.getLogger(ServerRunner.class);

    public static void main(String[] args) {
        if (args.length != 1) {
            LOG.error("You must provide only a port number to serve on!");
            return;
        }

        try {
            var port = Integer.parseInt(args[0]);
            var channel = ServerSocketChannel.open().bind(new InetSocketAddress(port));
            new Server(channel).run();
        } catch (NumberFormatException e) {
            LOG.error("The provided port must be a number!");
        } catch (IOException e) {
            LOG.error("An IO error occurred while starting the server:");
            e.printStackTrace();
        }
    }
}
