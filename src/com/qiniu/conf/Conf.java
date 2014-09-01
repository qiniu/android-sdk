package com.qiniu.conf;

public class Conf {
    public static final String VERSION = "6.1.0";
    public static String UP_HOST = "http://upload.qiniu.com";
    public static String UP_HOST2 = "http://up.qiniu.com";
    
    public static final String CHARSET = "utf-8";
    
	/**
	 * HTTP连接超时的时间毫秒(ms)
     * Determines the timeout in milliseconds until a connection is established.
     * A timeout value of zero is interpreted as an infinite timeout.
     *
     * Please note this parameter can only be applied to connections that
     * are bound to a particular local address.
     */
	public static int CONNECTION_TIMEOUT = 30 * 1000;
	
	/**
	 * 读取response超时的时间毫秒(ms)
     * Defines the socket timeout (<code>SO_TIMEOUT</code>) in milliseconds,
     * which is the timeout for waiting for data  or, put differently,
     * a maximum period inactivity between two consecutive data packets).
     * A timeout value of zero is interpreted as an infinite timeout.
     * @see java.net.SocketOptions#SO_TIMEOUT
     */
	public static int SO_TIMEOUT = 30 * 1000;
	
	public static final int BLOCK_SIZE = 1024 * 1024 * 4;
	public static int CHUNK_SIZE = 1024 * 256;
    public static int FIRST_CHUNK = 1024 * 256;
    public static int ONCE_WRITE_SIZE = 1024 * 32;
    
	public static int BLOCK_TRY_TIMES = 2;
	public static int CHUNK_TRY_TIMES = 3;
	
	public static String USER_AGENT = null;
	
	public static final int ERROR_CODE = 0;
	public static final int CANCEL_CODE = -1;
	public static String PROCESS_MSG = "upload alread in process or procssed or canceled.";
	
}
