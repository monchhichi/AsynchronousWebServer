package server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import Common.RequestHandler;

public class SuspensionServer extends Server {

	private ServiceThread[] threads;
	private List<Socket> connSockPool = new ArrayList<Socket>();

	public void start() {
		super.start();
		try {
			// create thread pool
			threads = new ServiceThread[threadNum];

			// start all threads
			for (int i = 0; i < threads.length; i++) {
				threads[i] = new ServiceThread();
				threads[i].start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		while (true) {
			try {
				Socket s = listenSocket.accept();

				synchronized (connSockPool) {
					connSockPool.add(s);
					connSockPool.notifyAll();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	class ServiceThread extends Thread {

		public void run() {
			while (true) {
				Socket s = null;

				// busy wait until one socket is assigned to the thread
				synchronized (connSockPool) {
					// always check (notifyAll())
					while (connSockPool.isEmpty()) {
						try {
							connSockPool.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					// remove the first request
					s = connSockPool.remove(0);
				}
				try {
					new RequestHandler(s, SuspensionServer.this).processRequest();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
