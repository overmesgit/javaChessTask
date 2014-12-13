/**
 * Created by user on 12/12/14.
 */
public class Response {
    private Request request;
    private final String message;

    public Response(Request request, String message) {
        this.request = request;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public Request getRequest() {
        return request;
    }
}
