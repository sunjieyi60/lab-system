package xyz.jasenon.rsocket.server.config;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.plugins.RSocketInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class RSocketSetupInterceptor implements RSocketInterceptor {

    @Override
    public RSocket apply(RSocket source) {
        return new RSocket() {
            @Override
            public Mono<Payload> requestResponse(Payload payload) {
                // 打印收到的元数据
                String metadata = payload.getMetadataUtf8();
                String data = payload.getDataUtf8();
                log.info("收到请求 - 元数据: {}, 数据: {}", metadata, data);
                return source.requestResponse(payload);
            }

            @Override
            public Mono<Void> metadataPush(Payload payload) {
                log.info("收到 metadataPush: {}", payload.getMetadataUtf8());
                return source.metadataPush(payload);
            }
        };
    }
}