package online.flowerinsnow.minecraftproxy.exception;

public class InvalidPacketException extends RuntimeException {
    public InvalidPacketException() {
        super();
    }

    public InvalidPacketException(String message) {
        super(message);
    }

    public InvalidPacketException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPacketException(Throwable cause) {
        super(cause);
    }
}
