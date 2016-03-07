package server;

import java.net.Socket;

import Common.RequestHandler;

public class WelcomeServer extends Server {
	private ServiceThread[] threads;

	@Override
	public void start() {
		super.start();
		try {
			threads = new ServiceThread[threadNum];
			for (int i = 0; i < threads.length; i++) {
				threads[i] = new ServiceThread();
				threads[i].start();
			}
			for (int i = 0; i < threads.length; i++) {
				threads[i].join();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	class ServiceThread extends Thread {

		public void run() {
			while (true) {
				// get a new request connection
				Socket s = null;

				synchronized (listenSocket) {
					try {
						s = listenSocket.accept();
						// process a request
						new RequestHandler(s, WelcomeServer.this).processRequest();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
