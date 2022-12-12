package online.flowerinsnow.minecraftproxy;

import online.flowerinsnow.minecraftproxy.config.Config;
import online.flowerinsnow.minecraftproxy.config.ConfigurationEntryMissedException;
import online.flowerinsnow.minecraftproxy.server.ProxyServer;
import online.flowerinsnow.minecraftproxy.util.FileUtils;
import online.flowerinsnow.saussureautils.io.IOUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Main {
    private static ScheduledThreadPoolExecutor scheduler;
    private static ProxyServer server;
    private static Logger logger;

    public static void main(String[] args) {
        scheduler = new ScheduledThreadPoolExecutor(32);
        logger = Logger.getLogger("main");

        logger.info("加载配置文件...");
        Path file = FileUtils.getCodeSourcePath().resolveSibling("config.xml");
        try {
            Config.load(file);
        } catch (IOException | ConfigurationEntryMissedException e) {
            logger.severe("配置文件加载出错，请检查控制台输出");
            e.printStackTrace();
            return;
        }

        server = new ProxyServer(Config.Server.ip, Config.Server.port);
        try {
            server.bind();
        } catch (IOException e) {
            logger.severe("无法开启服务器");
            e.printStackTrace();
            return;
        }

        server.start();
        logger.info("Listening on " + Config.Server.ip + ":" + Config.Server.port);

        String log = "验证：开启了%s模式，名单中有 %s 个ID。修改配置文件后名单会自动刷新，无需手动重载。";
        switch (Config.Connection.mode.toLowerCase()) {
            case "blacklist":
                logger.info(String.format(log, "黑名单", Config.Connection.list.size()));
                break;
            case "whitelist":
                logger.info(String.format(log, "白名单", Config.Connection.list.size()));
                break;
            default:
                logger.info("验证：未开启验证，所有用户均可进入服务器。修改配置文件后名单会自动刷新，无需手动重载。");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(Main::shutdown));
        scheduler.scheduleAtFixedRate(() -> {
            try {
                Config.load(file);
            } catch (IOException e) {
                Logger.getLogger("reload-task").severe("配置文件重载失败。");
                e.printStackTrace();
            }
        }, 5000L, 5000L, TimeUnit.MILLISECONDS);
    }

    public static void shutdown() {
        logger.info("正在关闭服务器...");
        IOUtils.closeQuietly(server);
    }

    public static ScheduledThreadPoolExecutor getScheduler() {
        return scheduler;
    }

    public static ProxyServer getServer() {
        return server;
    }

    //    public static void main(String[] args) throws IOException {
//        ServerSocketChannel server = ServerSocketChannel.open();
//        server.bind(new InetSocketAddress(25565));
//        ByteBuffer lastRead = ByteBuffer.allocate(1024);
//        while (true) {
//            SocketChannel socket = server.accept();
//            Label1: while (true) {
//                ByteBuffer bb = ByteBuffer.allocate(1024);
//                lastRead.flip();
//                bb.put(lastRead);
//                lastRead.clear();
//                int len;
//                if (bb.position() == 0) {
//                    if (socket.read(bb) == -1) {
//                        break;
//                    }
//                }
//                len = bb.get(0);
//                while (bb.position() < len) { // 没读够，继续读
//                    if (socket.read(bb) == -1) {
//                        break Label1;
//                    }
//                }
//                bb.flip();
//                bb.position(1);
//                ByteBuffer current = ByteBuffer.allocate(1024);
//                for (int i = 0; i < len; i++) {
//                    current.put(bb.get());
//                }
//
//                printBytes(ByteBuffer.wrap(current.array()));
//                handlePacket(current);
//
//                // 准备读取下一次
//                lastRead.put(bb);
//                System.out.println("\nEnd");
//            }
//            System.out.println(socket.getRemoteAddress() + " disconnected.");
//            IOUtils.closeQuietly(socket);
//        }
//    }
//
//    private static void printBytes(ByteBuffer bb) {
//        int i = 0;
//        while (bb.hasRemaining()) {
//            byte b = bb.get();
//            DWord dWord = HexUtils.hex(b);
//            System.out.print(dWord.get1());
//            System.out.print(dWord.get2());
//            if (++i % 8 == 0) {
//                System.out.println();
//            } else {
//                System.out.print(" ");
//            }
//        }
//    }
//
//    private static void handlePacket(ByteBuffer bb) {
//        bb.flip();
//        if (bb.limit() < 2) {
//            return;
//        }
//        byte type = bb.get();
//        if (type == 0x00) {
//            switch (bb.get()) {
//                case 0x2F: { // IP
//                    bb.position(bb.position() + 1);
//                    @SuppressWarnings("resource")
//                    ByteArrayWriteStream baw = new ByteArrayWriteStream();
//                    byte b;
//                    while ((b = bb.get()) != 0x00 && bb.position() < bb.limit()) {
//                        baw.write(b);
//                    }
//                    joinIP = new String(baw.toByteArray());
//                    break;
//                }
//                case 0x0C: { // ID
//                    @SuppressWarnings("resource")
//                    ByteArrayWriteStream baw = new ByteArrayWriteStream();
//                    byte b;
//                    while ((b = bb.get()) != 0x00 && bb.position() < bb.limit()) {
//                        baw.write(b);
//                    }
//                    System.out.println(new String(baw.toByteArray()) + " 通过IP " + joinIP + " 加入了。");
//                    joinIP = null;
//                }
//            }
//        }
//    }
}
