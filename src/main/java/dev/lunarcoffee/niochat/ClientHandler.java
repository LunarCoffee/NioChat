package dev.lunarcoffee.niochat;

import dev.lunarcoffee.niochat.messages.MessageType;
import dev.lunarcoffee.niochat.messages.Message;
import dev.lunarcoffee.niochat.messages.MessageBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ClientHandler {
    private final Map<String, Client> clients = new HashMap<>();
    private final Queue<Message> messageQueue = new LinkedList<>();

    private static final Logger LOG = LogManager.getLogger(ClientHandler.class);

    public void processAccept(SelectionKey key, Selector selector) throws IOException {
        var server = (ServerSocketChannel) key.channel();
        var channel = server.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

        var client = new Client(channel);
        var remoteAddress = getRemoteAddress(channel);
        if (remoteAddress == null)
            return;

        clients.put(remoteAddress.toString(), client);
        LOG.info("Client connected, making %d clients.".formatted(clients.size()));
    }

    public void processRead(SelectionKey key) {
        var mapKey = getMapKey(key);
        var client = clients.get(mapKey);
        if (client == null)
            return;

        var buffer = client.readFully();
        if (buffer == null) {
            removeClientAndBroadcast(client, mapKey);
            return;
        }
        buffer.flip();

        var messageType = MessageType.withCode(buffer.get());
        if (messageType == null) {
            removeClientAndBroadcast(client, mapKey);
            return;
        }

        var messageBytes = new byte[buffer.remaining()];
        buffer.get(messageBytes);
        var message = new String(messageBytes, StandardCharsets.UTF_8);

        switch (messageType) {
            case SET_NAME -> handleSetNameMessage(client, message);
            case GLOBAL -> handleGlobalMessage(client, message);
            case PRIVATE -> handlePrivateMessage(client, message);
        }
    }

    public void processWrite(SelectionKey key) {
        var mapKey = getMapKey(key);
        var client = clients.get(mapKey);
        if (client == null)
            return;

        while (!messageQueue.isEmpty()) {
            var message = messageQueue.poll();
            switch (message.type()) {
                case GLOBAL -> broadcast(message.content() + '\n');
                case PRIVATE -> sendTo(message.recipient(), message.content() + '\n', mapKey);
            }
        }
    }

    private void handleSetNameMessage(Client client, String name) {
        var joinContent = client.isNameSet()
            ? "%s changed their display name to %s!".formatted(client.getName(), name)
            : "%s joined the room!".formatted(name);

        var joinMessage = new MessageBuilder(joinContent).setType(MessageType.GLOBAL)
            .fromServer()
            .build();
        messageQueue.offer(joinMessage);
        client.setName(name);
    }

    private void handleGlobalMessage(Client client, String content) {
        var message = new MessageBuilder(content)
            .setType(MessageType.GLOBAL)
            .fromClient(client)
            .build();
        messageQueue.offer(message);
    }

    private void handlePrivateMessage(Client client, String content) {
        // All private message content parts should have the recipient name before the first colon (":"), with all
        // colons replaced with the byte 0xFFFF.
        var colonIndex = content.indexOf(":");
        var recipientName = content.substring(0, colonIndex).replace('\uFFFF', ':');
        var recipient = clients.values().stream().filter(c -> c.getName().equals(recipientName)).findFirst();

        Message message;
        if (recipient.isEmpty()) {
            message = new MessageBuilder("No one connected has that nickname, so your private message was not sent.")
                .setType(MessageType.PRIVATE)
                .fromServer()
                .setRecipient(client)
                .build();
        } else {
            var messageContent = content.substring(colonIndex + 1);
            message = new MessageBuilder(messageContent)
                .setType(MessageType.PRIVATE)
                .fromClient(client)
                .setRecipient(recipient.get())
                .build();
        }
        messageQueue.offer(message);
    }

    // Utility to send [message] to all connected clients who have joined the room.
    private void broadcast(String message) {
        var buffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));

        for (var entry : clients.entrySet()) {
            var client = entry.getValue();
            if (client.isNameSet()) {
                tryWriteFully(buffer, client, entry.getKey());
                buffer.rewind();
            }
        }
    }

    // Utility to send [message] to a single recipient.
    private void sendTo(Client recipient, String message, String key) {
        var buffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
        tryWriteFully(buffer, recipient, key);
    }

    // Utility to write a buffer to a client channel, terminating the connection if it fails.
    private void tryWriteFully(ByteBuffer buffer, Client client, String key) {
        try {
            client.writeFully(buffer);
        } catch (IOException e) {
            removeClientAndBroadcast(client, key);
        }
    }

    // Utility to close a client connection, removing it from the [clients] list, then queueing a global broadcast of
    // this disconnection.
    private void removeClientAndBroadcast(Client client, String key) {
        client.close();
        clients.remove(key);

        var leaveMessage = new MessageBuilder("%s left the room!".formatted(client.getName()))
            .setType(MessageType.GLOBAL)
            .fromServer()
            .build();

        messageQueue.offer(leaveMessage);
        LOG.info("Client disconnected, leaving %d clients.".formatted(clients.size()));
    }

    private InetSocketAddress getRemoteAddress(SocketChannel channel) {
        try {
            return (InetSocketAddress) channel.getRemoteAddress();
        } catch (IOException e) {
            return null;
        }
    }

    private String getMapKey(SelectionKey key) {
        return Objects.requireNonNull(getRemoteAddress((SocketChannel) key.channel())).toString();
    }
}
