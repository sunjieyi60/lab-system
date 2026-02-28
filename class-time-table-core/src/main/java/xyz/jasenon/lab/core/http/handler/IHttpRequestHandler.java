package xyz.jasenon.lab.core.http.handler;

import xyz.jasenon.lab.core.exception.ImException;
import xyz.jasenon.lab.core.http.HttpRequest;
import xyz.jasenon.lab.core.http.HttpResponse;
import xyz.jasenon.lab.core.http.RequestLine;

/**
 *
 * @author wchao
 *
 */
public interface IHttpRequestHandler {
	/**
	 *
	 * @param packet
	 * @param requestLine
	 * @return
	 * @throws ImException
	 * @author wchao
	 */
	public HttpResponse handler(HttpRequest packet, RequestLine requestLine) throws ImException;

	/**
	 *
	 * @param request
	 * @param requestLine
	 * @return
	 * @author wchao
	 */
	public HttpResponse resp404(HttpRequest request, RequestLine requestLine);

	/**
	 *
	 * @param request
	 * @param requestLine
	 * @param throwable
	 * @return
	 * @author wchao
	 */
	public HttpResponse resp500(HttpRequest request, RequestLine requestLine, Throwable throwable);
	
	/**
	 * 清空静态资源缓存，如果没有缓存，可以不处理
	 * @param request
	 * @author: wchao
	 */
	public void clearStaticResCache(HttpRequest request);
}
