package xyz.jasenon.lab.class_time_table.rsocket.client;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.RSocketServer;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpServer;
import reactor.util.retry.Retry;
import xyz.jasenon.lab.class_time_table.rsocket.protocol.PeerProtocol;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * RSocket P2P 连接器
 * 支持作为客户端连接远程，或作为服务端监听连接
 * 实现 CS 角色可切换
 */
@Slf4j
public class RSocketPeerConnector {
    
    private RSocket socket;
    private io.rsocket.Closeable server;
    private PeerProtocol protocolHandler;
    private String localUuid;
    
    // 连接状态
    private volatile ConnectionState state = ConnectionState.DISCONNECTED;
    
    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED_AS_CLIENT,    // 作为客户端连接到远程
        LISTENING_AS_SERVER,    // 作为服务端监听
        CONNECTED_AS_SERVER     // 有客户端连接到我
    }
    
    /**
     * 作为客户端连接到远程服务端
     * 
     * @param host 远程主机
     * @param port 远程端口
     * @param uuid 本机 UUID（用于标识）
     * @param protocolHandler 协议处理器（处理来自远程的请求）
     * @return 连接成功的 RSocket
     */
    public Mono<RSocket> connectAsClient(String host, int port, String uuid, 
                                          PeerProtocol protocolHandler) {
        this.localUuid = uuid;
        this.protocolHandler = protocolHandler;
        this.state = ConnectionState.CONNECTING;
        
        log.info("[{}] 正在连接到 {}:{}...", uuid, host, port);
        
        // 创建 SocketAcceptor 处理远程调用
        SocketAcceptor acceptor = createSocketAcceptor(protocolHandler, uuid);
        
        return RSocketConnector.create()
                .acceptor(acceptor)  // 允许远程调用本地方法（双工）
                .reconnect(Retry.fixedDelay(3, Duration.ofSeconds(5)))
                .connect(() -> TcpClientTransport.create(
                        TcpClient.create()
                                .host(host)
                                .port(port)
                                .doOnConnected(conn -> {
                                    log.info("[{}] TCP 连接已建立", uuid);
                                    state = ConnectionState.CONNECTED_AS_CLIENT;
                                })
                                .doOnDisconnected(conn -> {
                                    log.warn("[{}] TCP 连接已断开", uuid);
                                    state = ConnectionState.DISCONNECTED;
                                })
                ))
                .doOnSuccess(s -> {
                    this.socket = s;
                    log.info("[{}] RSocket 连接成功（作为客户端）", uuid);
                })
                .doOnError(e -> {
                    log.error("[{}] 连接失败: {}", uuid, e.getMessage());
                    state = ConnectionState.DISCONNECTED;
                });
    }
    
    /**
     * 作为服务端监听连接
     * 
     * @param port 监听端口
     * @param uuid 本机 UUID
     * @param protocolHandler 协议处理器
     * @return 服务端实例
     */
    public Mono<io.rsocket.Closeable> listenAsServer(int port, String uuid,
                                                      PeerProtocol protocolHandler) {
        this.localUuid = uuid;
        this.protocolHandler = protocolHandler;
        
        log.info("[{}] 正在启动服务端，监听端口 {}...", uuid, port);
        
        SocketAcceptor acceptor = createSocketAcceptor(protocolHandler, uuid);
        
        return RSocketServer.create(acceptor)
                .bind(TcpServerTransport.create(
                        TcpServer.create()
                                .port(port)
                                .doOnBound(conn -> {
                                    log.info("[{}] 服务端已绑定端口 {}", uuid, port);
                                    state = ConnectionState.LISTENING_AS_SERVER;
                                })
                                .doOnConnection(conn -> {
                                    log.info("[{}] 有客户端连接", uuid);
                                    state = ConnectionState.CONNECTED_AS_SERVER;
                                })
                ))
                .doOnSuccess(s -> {
                    this.server = s;
                    log.info("[{}] 服务端启动成功", uuid);
                });
    }
    
    /**
     * 创建 SocketAcceptor（处理远程调用）
     */
    private SocketAcceptor createSocketAcceptor(PeerProtocol handler, String uuid) {
        return SocketAcceptor.forRequestChannel(payloads -> {
            // 默认实现：创建 RSocketPeerHandler 处理请求
            RSocketPeerHandler peerHandler = new RSocketPeerHandler(handler, uuid);
            return Mono.just(peerHandler);
        });
    }
    
    /**
     * 获取当前连接状态
     */
    public ConnectionState getState() {
        return state;
    }
    
    /**
     * 是否已连接（作为客户端或服务器）
     */
    public boolean isConnected() {
        return state == ConnectionState.CONNECTED_AS_CLIENT 
                || state == ConnectionState.CONNECTED_AS_SERVER;
    }
    
    /**
     * 获取 RSocket 实例（用于发送请求）
     */
    public RSocket getSocket() {
        if (socket == null) {
            throw new IllegalStateException("尚未建立连接");
        }
        return socket;
    }
    
    /**
     * 关闭连接
     */
    public Mono<Void> close() {
        return Mono.fromRunnable(() -> {
            if (socket != null) {
                socket.dispose();
            }
            if (server != null) {
                server.dispose();
            }
            state = ConnectionState.DISCONNECTED;
            log.info("[{}] 连接已关闭", localUuid);
        });
    }
}
