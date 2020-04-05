package dev.lunarcoffee.niochat.messages;

import dev.lunarcoffee.niochat.Client;

public class MessageBuilder {
    private MessageType type;
    private String prefix;
    private final String content;
    private Client recipient = null;

    public MessageBuilder(String content) {
        this.content = content;
    }

    public MessageBuilder setType(MessageType type) {
        this.type = type;
        return this;
    }

    public MessageBuilder fromServer() {
        prefix = "[SERVER]";
        return this;
    }

    public MessageBuilder fromClient(Client client) {
        prefix = (type == MessageType.PRIVATE ? "<%s>" : "[%s]").formatted(client.getName());
        return this;
    }

    public MessageBuilder setRecipient(Client recipient) {
        this.recipient = recipient;
        return this;
    }

    public Message build() {
        return new Message(type, "%s %s".formatted(prefix, content), recipient);
    }
}
