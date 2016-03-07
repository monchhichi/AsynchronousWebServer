package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;


public abstract class Server{
	protected int port;
	protected String root;
	protected int threadNum;
	protected int cacheSize;
	private int currentCacheSize;
	protected ServerSocket listenSocket;
	
	protected Map<String, String> cache = new HashMap<String, String>();
	
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}

	public String getRoot() {
		return root;
	}
	public void setRoot(String root) {
		this.root = root;
	}


	public int getThreadNum() {
		return threadNum;
	}
	public void setThreadNum(int threadNum) {
		this.threadNum = threadNum;
	}
	
	public boolean inCache(String filename) {
		return cache.containsKey(filename);
	}
	public String getFromCache(String filename) {
		return cache.get(filename);
	}
	public void setCache(int cacheSize) {
		this.cacheSize = cacheSize;
		this.currentCacheSize = 0;
	}

	public void updateCache(String filename, String content) {
		if(currentCacheSize + content.length() > cacheSize) {
			return;
		}
		cache.put(filename, content);
		this.currentCacheSize += content.length();
	}

	public void start() {
		try {
			listenSocket = new ServerSocket(port);
//			System.out.println("\nServer started; listening at " + port);
//			System.out.println("Server root: " + root);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
} 