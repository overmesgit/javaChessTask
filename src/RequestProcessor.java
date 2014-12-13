import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by user on 12/12/14.
 */
public class RequestProcessor implements Runnable{
    private final Queue<Response> responseQueue;
    BlockingQueue<Request> requestQueue;
    RequestHandler requestHandler = new RequestHandler();

    public RequestProcessor(BlockingQueue<Request> requestQueue, Queue<Response> responseQueue) {
        this.requestQueue = requestQueue;
        this.responseQueue = responseQueue;
    }

    @Override
    public void run() {
        while (true) {
            Request request = null;
            try {
                request = requestQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Response[] responses = processRequest(request);
            if (responses != null) {
                Collections.addAll(responseQueue, responses);
            }
        }
    }

    private Response[] processRequest(Request request) {
        return requestHandler.processRequest(request);
    }
}
