package online.flowerinsnow.minecraftproxy.config;

import online.flowerinsnow.saussureautils.io.CopyOption;
import online.flowerinsnow.saussureautils.io.IOUtils;
import online.flowerinsnow.xml.XMLFactory;
import online.flowerinsnow.xml.node.XMLNodeDocument;
import online.flowerinsnow.xml.node.XMLNodeElement;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class Config {
    private Config() {
    }

    public static class Server {
        public static String ip;
        public static int port;
    }

    public static class Connection {
        public static String mode;
        public static List<String> list;
        public static long handshakeMaxWait;
        public static class Target {
            public static String accept;
            public static int acceptPort;
            public static boolean sendThis;
            public static String deny;
            public static int denyPort;
        }
    }

    public static void load(Path path) throws IOException, ConfigurationEntryMissedException {
        // 如果文件不存在则复制默认
        if (!path.toFile().isFile()) {
            IOUtils.createFileIfNotExists(path);
            //noinspection DataFlowIssue
            IOUtils.copy(
                    Thread.currentThread().getContextClassLoader().getResourceAsStream("config.xml"),
                    path, CopyOption.CLOSE_INPUT
            );
        }

        XMLNodeDocument document = XMLFactory.parse(path.toFile());
        XMLNodeElement configuration = document.getElement("configuration");

        XMLNodeElement server = configuration.getElement("server");
        Server.ip = server.getString("ip");
        Server.port = server.getIntValue("port");

        XMLNodeElement connection = configuration.getElement("connection");
        Connection.mode = connection.getString("mode");
        XMLNodeElement list = connection.getElement("list");
        Connection.list = list.getStringList("user");
        Connection.handshakeMaxWait = list.getLongValue("handshake_max_wait");

        XMLNodeElement target = connection.getElement("target");
        XMLNodeElement accept = target.getElement("accept");
        XMLNodeElement deny = target.getElement("deny");

        Connection.Target.accept = accept.getTextList().get(0);
        Connection.Target.acceptPort = Integer.parseInt(accept.getAttribute("port"));
        Connection.Target.sendThis = Boolean.parseBoolean(accept.getAttribute("sendthis"));
        Connection.Target.deny = deny.getTextList().get(0);
        Connection.Target.denyPort = Integer.parseInt(deny.getAttribute("port"));

        checkEntry(Server.ip, "server.ip");
        checkEntry(Connection.mode, "connection.mode");
        checkEntry(Connection.list, "connection.list");
        checkEntry(Connection.Target.accept, "connection.target.accept");
        checkEntry(Connection.Target.deny, "connection.target.deny");
    }

    /**
     * 检查一个条目是否为null，如果是，抛出异常
     *
     * @param object 条目值
     * @param name 条目名
     * @throws ConfigurationEntryMissedException 当条目为null时抛出
     */
    private static void checkEntry(Object object, String name) throws ConfigurationEntryMissedException {
        if (object == null) {
            throw new ConfigurationEntryMissedException(name);
        }
    }
}