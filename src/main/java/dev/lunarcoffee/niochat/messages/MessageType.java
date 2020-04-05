package dev.lunarcoffee.niochat.messages;

public enum MessageType {
    SET_NAME(0),
    GLOBAL(1),
    PRIVATE(2);

    private final byte code;

    MessageType(int code) {
        this.code = (byte) code;
    }

    public static MessageType withCode(byte code) {
        for (var type : values())
            if (type.code == code)
                return type;
        return null;
    }
}
