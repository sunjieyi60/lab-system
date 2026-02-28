package xyz.jasenon.lab.core.packets;

public enum Command{
  /**
   * <code>COMMAND_UNKNOW = 0;</code>
   */
  COMMAND_UNKNOW(0),
  /**
   * <pre>
   *握手请求，含http的websocket握手请求
   * </pre>
   *
   * <code>COMMAND_HANDSHAKE_REQ = 1;</code>
   */
  COMMAND_HANDSHAKE_REQ(1),
  /**
   * <pre>
   *握手响应，含http的websocket握手响应
   * </pre>
   *
   * <code>COMMAND_HANDSHAKE_RESP = 2;</code>
   */
  COMMAND_HANDSHAKE_RESP(2),
  /**
   * <pre>
   *鉴权请求
   * </pre>
   *
   * <code>COMMAND_AUTH_REQ = 3;</code>
   */
  COMMAND_AUTH_REQ(3),
  /**
   * <pre>
   * 鉴权响应
   * </pre>
   *
   * <code>COMMAND_AUTH_RESP = 4;</code>
   */
  COMMAND_AUTH_RESP(4),
  /**
   * <pre>
   * 班牌数据请求
   * </pre>
   */
  COMMAND_GET_CLASS_TIME_TABLE_REQ(5),
  /**
   * <pre>
   * 班牌数据响应
   * </pre>
   */
  COMMAND_GET_CLASS_TIME_TABLE_RESP(6),
  /**
   * <pre>
   *心跳请求
   * </pre>
   *
   * <code>COMMAND_HEARTBEAT_REQ = 13;</code>
   */
  COMMAND_HEARTBEAT_REQ(13),
  /**
   * <pre>
   *关闭请求
   * </pre>
   *
   * <code>COMMAND_CLOSE_REQ = 14;</code>
   */
  COMMAND_CLOSE_REQ(14),

  ;

  public final int getNumber() {
    return value;
  }

  public static Command valueOf(int value) {
    return forNumber(value);
  }

  public static Command forNumber(int value) {
	  for(Command command : Command.values()){
	   	   if(command.getNumber() == value){
	   		   return command;
	   	   }
      }
	  return null;
  }

  private final int value;

  private Command(int value) {
    this.value = value;
  }
}

