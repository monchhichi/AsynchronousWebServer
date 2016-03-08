package server;

import java.io.IOException;
import java.net.Socket;

import Common.RequestHandler;

public class PerThreadServer extends Server {
	@Override
	public void start() {
		super.start();
		try {
			while (true) {
				// take a ready connection from the accepted queue
				final Socket connectionSocket;
				connectionSocket = listenSocket.accept(); // accept is a block function
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						// process a request
						RequestHandler wrh;
						try {
							wrh = new RequestHandler(connectionSocket, PerThreadServer.this);
							wrh.processRequest();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}).start();
			} 
		} catch (IOException e) {
				e.printStackTrace();
		} 
	} 
}
