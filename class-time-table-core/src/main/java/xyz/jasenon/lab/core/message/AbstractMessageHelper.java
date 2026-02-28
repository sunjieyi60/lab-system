/**
 * 
 */
package xyz.jasenon.lab.core.message;

import xyz.jasenon.lab.core.ImConst;
import xyz.jasenon.lab.core.config.ImConfig;
import xyz.jasenon.lab.core.message.MessageHelper;

/**
 * @author HP
 *
 */
public abstract class AbstractMessageHelper implements MessageHelper,ImConst {

	protected ImConfig imConfig;

	public ImConfig getImConfig() {
		return imConfig;
	}

	public void setImConfig(ImConfig imConfig) {
		this.imConfig = imConfig;
	}
}
