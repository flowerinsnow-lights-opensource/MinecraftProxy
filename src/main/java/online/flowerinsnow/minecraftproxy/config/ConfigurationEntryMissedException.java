package online.flowerinsnow.minecraftproxy.config;

/**
 * 配置文件条目缺失时抛出
 */
public class ConfigurationEntryMissedException extends RuntimeException {
    public ConfigurationEntryMissedException() {
        super();
    }

    public ConfigurationEntryMissedException(String message) {
        super(message);
    }

    public ConfigurationEntryMissedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationEntryMissedException(Throwable cause) {
        super(cause);
    }
}
