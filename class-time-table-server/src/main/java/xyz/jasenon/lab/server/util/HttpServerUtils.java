package xyz.jasenon.lab.server.util;

import xyz.jasenon.lab.core.http.HttpConfig;
import xyz.jasenon.lab.core.http.HttpRequest;
import xyz.jasenon.lab.server.config.ImServerConfig;
/**
 * @author WChao
 * 2017年8月18日 下午5:47:00
 */
public class HttpServerUtils {
	/**
	 *
	 * @param request
	 * @return
	 * @author WChao
	 */
	public static HttpConfig getHttpConfig(HttpRequest request) {
		ImServerConfig imServerConfig = (ImServerConfig)request.getImChannelContext().getImConfig();
		return imServerConfig.getHttpConfig();
	}

	/**
	 * @param args
	 * @author WChao
	 */
	public static void main(String[] args) {

	}

	/**
	 *
	 * @author WChao
	 */
	public HttpServerUtils() {
	}
}
