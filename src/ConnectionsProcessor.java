import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by user on 12/12/14.
 */
public class ConnectionsProcessor implements Runnable{
    private Selector requestSelector = Selector.open();
    private ByteBuffer buffer = ByteBuffer.allocate(100);

    private Queue<Response> responseQueue = new ConcurrentLinkedQueue<>();
    private BlockingQueue<Request> requestQueue = new LinkedBlockingQueue<>();

    private Map<Request, Response> responseMap = new ConcurrentHashMap<>();
    private Map<SocketAddress, Request> requestMap = new ConcurrentHashMap<>();

    private AtomicBoolean isWorked = new AtomicBoolean();
    private RequestProcessor requestProcessor;

    ConnectionsProcessor() throws IOException {
        isWorked.set(true);
        requestProcessor = new RequestProcessor(requestQueue, responseQueue);
        new Thread(requestProcessor).start();
    }

    public void stop() {
        isWorked.set(false);
    }

    public void addChannel(SocketChannel channel){
        try {
            channel.register(requestSelector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }
    }

    private void addAllResponsesToMap() {
        Response response;
        while ((response = responseQueue.poll()) != null) {
            Request request = response.getRequest();
            if (requestMap.containsKey(request.getAddress())) {
                responseMap.put(request, response);
            }
        }
    }

    @Override
    public void run() {
        while(isWorked.get()){
            addAllResponsesToMap();
            int selectedKeysCount = 0;
            try {
                selectedKeysCount = requestSelector.select(10);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (selectedKeysCount > 0) {
                Iterator<SelectionKey> keyIterator = requestSelector.selectedKeys().iterator();
                while(keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    SocketChannel channel = (SocketChannel)key.channel();

                    if (key.isReadable()) {
                        processRequest(channel);
                    } else if (key.isWritable()) {
                        processResponse(channel);
                    }
                    keyIterator.remove();
                }
            }
        }
    }

    private void processRequest(SocketChannel channel) {
        int bytesRead = 0;
        try {
            buffer.clear();
            SocketAddress remoteAddress = channel.getRemoteAddress();
            Request request = getRequest(remoteAddress);

            if ((bytesRead = channel.read(buffer)) > 0) {
                buffer.flip();
                CharBuffer message = Charset.defaultCharset().decode(buffer);
                buffer.clear();
                request.appendMessage(message.toString());
                requestQueue.add(request);
            }
            if (bytesRead < 0) {
                closeChannel(channel, request);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void processResponse(SocketChannel channel) {
        try {
            SocketAddress address = channel.getRemoteAddress();
            Request r = getRequest(address);
            if (r != null) {
                Response response = responseMap.remove(r);
                if (response != null) {
                    // bug: can't reuse buffer because after clear it still have data
                    String message = response.getMessage();
                    ByteBuffer currentBuffer = ByteBuffer.allocate(message.length() * 2);
                    CharBuffer charBuffer = currentBuffer.asCharBuffer();
                    charBuffer.put(message);
                    channel.write(currentBuffer);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private Request getRequest(SocketAddress remoteAddress) throws IOException {
        Request request = requestMap.get(remoteAddress);
        if (request == null) {
            request = new Request(remoteAddress);
            requestMap.put(remoteAddress, request);
            requestQueue.add(request);
        }
        return request;
    }

    private void closeChannel(SocketChannel channel, Request r) throws IOException {
        channel.close();
        requestMap.remove(r.getAddress());
        responseMap.remove(r);

        r.state.put("state", "leave");
        requestQueue.add(r);
    }
}
