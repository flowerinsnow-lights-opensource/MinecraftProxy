package online.flowerinsnow.minecraftproxy.server;

import online.flowerinsnow.minecraftproxy.Main;
import online.flowerinsnow.saussureautils.io.IOUtils;

import java.io.IOException;
import java.net.*;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ProxyServer implements AutoCloseable {
    private final String ip;
    private final int port;
    private ServerSocketChannel server;

    private ScheduledFuture<?> acceptTask;
    public HashSet<ProxiedConnection> connections;

    public ProxyServer(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public void bind() throws IOException {
        server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(ip, port));
    }

    public void start() {
        acceptTask = Main.getScheduler().schedule(() -> {
            while (true) {
                try {
                    SocketChannel socket = server.accept();
                    socket.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
                    ProxiedConnection connection = new ProxiedConnection(socket);
                    Main.getScheduler().schedule(() -> { // 创建异步线程，防止阻塞其他连接
                        try {
                            if (connection.handShake()) { // 进行握手
                                connection.start(); // 握手成功，开始代理
                            } else {
                                IOUtils.closeQuietly(connection); // 否则，关闭连接
                            }
                        } catch (IOException e) {
                            IOUtils.closeQuietly(connection); // 出现异常，关闭连接
                        }
                    }, 0L, TimeUnit.MILLISECONDS);
                } catch (ClosedByInterruptException e) {
                    return null;
                } catch (IOException ignored) {
                }
            }
        }, 0L, TimeUnit.MILLISECONDS);
    }

    public SocketAddress getLocalAddress() throws IOException {
        return server.getLocalAddress();
    }

    @Override
    public void close() {
        if (acceptTask != null) {
            acceptTask.cancel(true);
        }
        // 断开所有连接
        connections.forEach(IOUtils::closeQuietly);
        connections.clear();
        IOUtils.closeQuietly(server);
    }

    public void unProxy(ProxiedConnection connection) {
        connections.remove(connection);
        IOUtils.closeQuietly(connection);
    }
}
