/**
 * 
 */
package xyz.jasenon.lab.server.config;

import xyz.jasenon.lab.core.http.HttpConfig;
import xyz.jasenon.lab.core.ws.WsConfig;

/**
 * @author WChao
 *
 */
public class DefaultImConfigBuilder extends ImServerConfigBuilder {

	@Override
	public ImServerConfigBuilder configHttp(HttpConfig httpConfig) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public ImServerConfigBuilder configWs(WsConfig wsServerConfig) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	protected ImServerConfigBuilder getThis() {
		return this;
	}

}
