package cn.kennylee.banyan.cache.support;

import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * 内嵌 Redis 测试基座。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class EmbeddedRedisSupport {

    private static RedisServer server;

    private static int port;

    private EmbeddedRedisSupport() {
    }

    /**
     * 启动内嵌 Redis（幂等）。
     */
    public static synchronized void start() {
        if (server != null) {
            return;
        }
        try {
            port = findFreePort();
            server = RedisServer.newRedisServer().port(port).build();
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(EmbeddedRedisSupport::stop, "embedded-redis-stop"));
        } catch (IOException e) {
            throw new IllegalStateException("启动内嵌 Redis 失败", e);
        }
    }

    /**
     * 获取端口。
     *
     * @return 端口
     */
    public static int port() {
        return port;
    }

    /**
     * 停止内嵌 Redis。
     */
    public static synchronized void stop() {
        if (server == null) {
            return;
        }
        try {
            server.stop();
        } catch (IOException e) {
            throw new IllegalStateException("停止内嵌 Redis 失败", e);
        } finally {
            server = null;
        }
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
