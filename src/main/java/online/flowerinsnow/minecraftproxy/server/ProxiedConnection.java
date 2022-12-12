package online.flowerinsnow.minecraftproxy.server;

import online.flowerinsnow.minecraftproxy.Main;
import online.flowerinsnow.minecraftproxy.config.Config;
import online.flowerinsnow.minecraftproxy.exception.InvalidPacketException;
import online.flowerinsnow.minecraftproxy.task.InBoundOutBoundTask;
import online.flowerinsnow.saussureautils.io.ByteArrayWriteStream;
import online.flowerinsnow.saussureautils.io.IOUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ProxiedConnection implements AutoCloseable {
    private final Logger logger;

    /**
     * 客户端 &lt;-&gt;本服务器
     */
    private final SocketChannel inBound;

    /**
     * 本服务器 &lt;-&gt;目标服务器
     */
    private SocketChannel outBound;

    private ScheduledFuture<?> inBoundTask;
    private ScheduledFuture<?> outBoundTask;

    /**
     * 协议版本
     */
    private int protocolVersion = -1;
    /**
     * 用户登录时使用的IP地址
     */
    private String loginAddress;
    private short loginPort;
    private String username;
    /**
     * 1 = ping
     * 2 = login
     */
    private byte requestedState;
    private String addressExtra;

    private final ByteArrayWriteStream hello;

    private final long start;

    public ProxiedConnection(SocketChannel socket) {
        inBound = socket;
        logger = Logger.getLogger(getRemoteAddress().toString());
        hello = new ByteArrayWriteStream();

        start = System.currentTimeMillis();
    }

    public boolean handShake() throws IOException {
        // 握手超时事件，如果该任务没有被提前取消，自动断开
        ScheduledFuture<?> handshakeTimedOutTask = Main.getScheduler().schedule(() -> {
            logger.warning(getRemoteAddress() + " 握手超时");
            Main.getServer().unProxy(ProxiedConnection.this);
        }, Config.Connection.handshakeMaxWait, TimeUnit.MILLISECONDS);

        ByteBuffer lastRead = ByteBuffer.allocate(1024);
        while (true) {
            ByteBuffer bb = ByteBuffer.allocate(1024);
            lastRead.flip();
            int len; // 每次数据包的第一个字节是数据包的长度
            if (lastRead.hasRemaining()) { // 如果有上一次没有读完的数据
                bb.put(lastRead);
            } else { // 没有，读一次
                ByteBuffer temp = ByteBuffer.allocate(1024);
                if (inBound.read(temp) == -1) {
                    logger.warning("握手失败：连接被对方关闭");
                    return false;
                } else {
                    temp.flip();
                    hello.write(temp.array(), 0, temp.limit()); // 每次读完数据，将其存入hello中，因为未来还要发送给目标服务器
                    bb.put(temp);
                }
            }
            len = bb.get(0);
            lastRead.clear();

            while (bb.position() - 1 < len) { // 现有长度=position-1 应当>=需要长度
                // 否则，继续读取
                ByteBuffer temp = ByteBuffer.allocate(1024);
                if (inBound.read(temp) == -1) {
                    logger.warning("握手失败：连接被对方关闭");
                    return false;
                } else {
                    temp.flip();
                    hello.write(temp.array(), 0, temp.limit());
                    bb.put(temp);
                }
            }

            // 读够了长度
            ByteBuffer current = ByteBuffer.allocate(1024);
            bb.flip();
            bb.position(1);
            // 读取本次的一定量到处理缓存内
            for (int i = 0; i < len; i++) { // 不用担心BufferUnderflowException，因为之前已经判断过了
                current.put(bb.get());
            }
            // 将该缓存交由其他方法处理
            try {
                if (handleHandShake(current)) { // 如果握手成功结束了
                    handshakeTimedOutTask.cancel(false); // 取消握手超时计划任务
                    return true;
                }
            } catch (InvalidPacketException e) {
                logger.warning("握手失败：无效的数据包");
                return false;
            }
            lastRead.put(bb);
        }
    }

    /**
     * 处理一次握手包
     *
     * @param bb 数据内容
     * @return 如果握手还未结束，返回false
     */
    @SuppressWarnings("resource")
    private boolean handleHandShake(ByteBuffer bb) throws InvalidPacketException {
        bb.flip();
        if (bb.limit() < 2) { // 可能是ping
            return false;
        }
        byte id = bb.get();
        if (id != (byte) 0) { // 不接收HandShake和LoginStart以外的包
            return false;
        }

        if (protocolVersion == -1) { // 0x00/Handshaking/Server/Handshake
            protocolVersion = bb.get();
            byte len = bb.get(); // 字符串(服务器IP)长度
            ByteBuffer buffer = ByteBuffer.allocate(len); // 记录服务器信息
            for (int i = 0; i < len; i++) {
                buffer.put(bb.get());
            }
            ByteArrayWriteStream baw = new ByteArrayWriteStream();
            byte b;
            buffer.flip();
            while (buffer.hasRemaining() && (b = buffer.get()) != 0) {
                baw.write(b);
            }
            loginAddress = baw.toString();

            baw = new ByteArrayWriteStream();
            for (int i = buffer.position(); i < len; i++) {
                baw.write(buffer.get());
            }
            addressExtra = baw.toString();
            loginPort = bb.getShort();
            requestedState = bb.get();
            return requestedState == 1;
        } else { // 0x00/Login/Server/LoginStart
            bb.position(bb.position() + 1);
            ByteArrayWriteStream baw = new ByteArrayWriteStream();
            byte b;
            while (bb.hasRemaining() && (b = bb.get()) != 0) {
                baw.write(b);
            }
            username = baw.toString();
            return true;
        }
    }

//    public int readVarInt(ByteBuffer bb) throws InvalidPacketException {
//        int i = 0;
//        int j = 0;
//
//        while (true) {
//            byte b0 = bb.get();
//            i |= (b0 & 127) << j++ * 7;
//
//            if (j > 5) {
//                throw new InvalidPacketException("VarInt too big");
//            }
//
//            if ((b0 & 128) != 128) {
//                break;
//            }
//        }
//
//        return i;
//    }

    public void start() {
        logger.info("用户信息：IP=" + getRemoteAddress() + " 用户名=" + (username == null ? "未知" : username) + " 通过IP " + loginAddress + ":" + loginPort + (requestedState == 1 ? " ping" : " 登入") + "。版本：" + protocolVersion);
        if (requestedState == 1) { // ping
            accept();
            return;
        }
        switch (Config.Connection.mode) {
            case "blacklist":
                if (Config.Connection.list.contains(username)) {
                    deny();
                } else {
                    accept();
                }
                break;
            case "whitelist":
                if (Config.Connection.list.contains(username)) {
                    accept();
                } else {
                    deny();
                }
                break;
            default:
                accept();
        }
    }

    private void accept() {
        String ip = Config.Connection.Target.accept;
        int port = Config.Connection.Target.acceptPort;
        logger.info(getRemoteAddress() + " 握手完成，验证通过。正在连接到目标服务器 " + ip + ":" + port + "...");
        outBound(ip, port, requestedState == 1);
    }

    private void deny() {
        if ("blackhole".equalsIgnoreCase(Config.Connection.Target.deny)) {
            logger.info(getRemoteAddress() + " 握手完成，验证不通过。正在断开连接...");
            close();
        } else {
            String ip = Config.Connection.Target.deny;
            int port = Config.Connection.Target.denyPort;
            logger.info(getRemoteAddress() + " 握手完成、验证不通过。正在连接到目标服务器 " + ip + ":" + port + "...");
            outBound(ip, port, requestedState == 1);
        }
    }

    private void outBound(String target, int port, boolean ping) {
        try {
            outBound = SocketChannel.open(new InetSocketAddress(target, port));
            if (Config.Connection.Target.sendThis) {
                ByteBuffer bb = ByteBuffer.allocate(1024);
                if (ping) {
                    {
                        bb.position(1); // 预留packet len的位置
                        bb.put((byte) 0); // packet id
                        bb.put((byte) protocolVersion); // protocol version
                        String str = target + "\0" + addressExtra;
                        bb.put((byte) str.getBytes().length); // login address len
                        bb.put(str.getBytes()); // login address
                        bb.putShort((short) port); // login port
                        bb.put((byte) 1); // state: ping
                        bb.put(0, (byte) (bb.position() - 1)); // packet len
                        bb.flip();
                        outBound.write(bb);
                    }
                } else {
                    {
                        bb.position(1); // 预留packet len的位置
                        bb.put((byte) 0); // packet id
                        bb.put((byte) protocolVersion); // protocol version
                        String str = target + "\0" + addressExtra;
                        bb.put((byte) str.getBytes().length); // login address len
                        bb.put(str.getBytes()); // login address
                        bb.putShort((short) port); // login port
                        bb.put((byte) 2); // state: login
                        bb.put(0, (byte) (bb.position() - 1)); // packet len
                        bb.flip();
                        outBound.write(bb);
                    }

                    {
                        bb.clear();
                        bb.position(1); // 预留packet len的位置
                        bb.put((byte) 0); // packet id
                        bb.put((byte) username.getBytes().length); // username len
                        bb.put(username.getBytes()); // username
                        bb.put(0, (byte) (bb.position() - 1)); // packet len
                        bb.flip();
                        outBound.write(bb);
                    }
                }
            } else {
                outBound.write(ByteBuffer.wrap(hello.toByteArray())); // 直接放行，把数据原封不动地发送出去
            }

            inBoundTask = Main.getScheduler().schedule(
                new InBoundOutBoundTask(inBound, outBound) {
                    @Override
                    protected void onClose(IOException e) {
                        if (e != null) {
                            logger.info("客户端因为异常已断开连接");
                        } else {
                            logger.info("客户端已正常断开连接");
                        }
                        Main.getServer().unProxy(ProxiedConnection.this);
                    }
                }
            , 0L, TimeUnit.MILLISECONDS);
            outBoundTask = Main.getScheduler().schedule(
                new InBoundOutBoundTask(outBound, inBound) {
                    @Override
                    protected void onClose(IOException e) {
                        if (e != null) {
                            logger.info("目标服务器因为异常已断开连接");
                        } else {
                            logger.info("目标服务器已正常断开连接");
                        }
                        Main.getServer().unProxy(ProxiedConnection.this);
                    }
                }
            , 0L, TimeUnit.MILLISECONDS);

            Main.getServer().connections.add(this);
            logger.info("成功建立连接。握手耗时 " + (System.currentTimeMillis() - start) + "ms");
        } catch (IOException e) {
            logger.warning("连接目标服务器失败：" + e);
            close();
        }
    }

    @Override
    public void close() {
        if (outBoundTask != null) {
            outBoundTask.cancel(true);
        }
        if (inBoundTask != null) {
            inBoundTask.cancel(true);
        }
        IOUtils.closeQuietly(outBound, inBound);
    }

    public SocketAddress getRemoteAddress() {
        try {
            return inBound.getRemoteAddress();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public String getLoginAddress() {
        return loginAddress;
    }

    public String getUsername() {
        return username;
    }
}
