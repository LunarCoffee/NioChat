package dev.lunarcoffee.niochat.messages;

import dev.lunarcoffee.niochat.Client;

public record Message(MessageType type, String content, Client recipient) {}
