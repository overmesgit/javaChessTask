import java.io.IOException;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by user on 12/12/14.
 */
public class Request {
    public Map<String, String> state= new HashMap<>();
    private final SocketAddress address;

    private String message = "";

    public Request(SocketAddress address) throws IOException {
        this.address = address;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public SocketAddress getAddress() {
        return address;
    }

    public void appendMessage(String s) {
        message += s;
    }

    public void clearMessage() {
        message = "";
    }
}
