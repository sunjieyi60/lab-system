/**
 * 
 */
package xyz.jasenon.lab.server;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.server.ServerTioConfig;
import org.tio.server.TioServer;
import xyz.jasenon.lab.core.ImConst;
import xyz.jasenon.lab.core.cache.redis.RedissonTemplate;
import xyz.jasenon.lab.server.cluster.redis.RedisCluster;
import xyz.jasenon.lab.server.cluster.redis.RedisClusterConfig;
import xyz.jasenon.lab.server.config.ImServerConfig;
import xyz.jasenon.lab.server.helper.redis.RedisMessageHelper;
import xyz.jasenon.lab.server.protocol.ProtocolManager;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * J-IM服务端启动类
 * @author WChao
 *
 */
public class JimServer {

	private static Logger log = LoggerFactory.getLogger(JimServer.class);
	private TioServer tioServer = null;
	private ImServerConfig imServerConfig;

	public JimServer(ImServerConfig imServerConfig){
		this.imServerConfig = imServerConfig;
	}
	
	public void init(ImServerConfig imServerConfig){
		System.setProperty("tio.default.read.buffer.size", String.valueOf(imServerConfig.getReadBufferSize()));
		if(imServerConfig.getMessageHelper() == null){
			imServerConfig.setMessageHelper(new RedisMessageHelper());
		}
		if(ImServerConfig.ON.equals(imServerConfig.getIsCluster())){
			imServerConfig.setIsStore(ImServerConfig.ON);
			if(imServerConfig.getCluster() == null){
				try{
					imServerConfig.setCluster(new RedisCluster(RedisClusterConfig.newInstance(ImConst.Topic.REDIS_CLUSTER_TOPIC_SUFFIX, RedissonTemplate.me().getRedissonClient())));
				}catch(Exception e){
					log.error("Connection cluster configuration is abnormal, please check",e);
				}
			}
		}
		ProtocolManager.init();
		tioServer = new TioServer((ServerTioConfig)imServerConfig.getTioConfig());
	}
	
	public void start() throws IOException {
		Stopwatch timeWatch = Stopwatch.createStarted();
		log.warn("J-IM Server start");
		init(imServerConfig);
		tioServer.start(this.imServerConfig.getBindIp(), this.imServerConfig.getBindPort());
		log.warn("J-IM Server started at address: {} time:{}ms", imServerConfig.getBindIp()+":"+imServerConfig.getBindPort(), timeWatch.elapsed(TimeUnit.MILLISECONDS));
	}
	
	public void stop(){
		tioServer.stop();
	}
}
