import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;

/**
 * Created by user on 12/12/14.
 */
public class Server {
    public static void main(String[] args) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

        serverSocketChannel.socket().bind(new InetSocketAddress(50505));
        serverSocketChannel.configureBlocking(false);

        Selector selector = Selector.open();

        ConnectionsProcessor requestHandler = new ConnectionsProcessor();
        Thread thread = new Thread(requestHandler);
        thread.start();

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        while(true){
            int count = selector.select();
            if (count > 0) {
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while(keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    ServerSocketChannel channel = (ServerSocketChannel)key.channel();
                    SocketChannel socketChannel = channel.accept();
                    if (socketChannel != null) {
                        socketChannel.configureBlocking(false);
                        requestHandler.addChannel(socketChannel);
                    }
                    keyIterator.remove();
                }
            }

        }
    }
}

