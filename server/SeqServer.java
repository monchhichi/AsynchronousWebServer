package server;

import java.io.IOException;
import java.net.Socket;

import Common.RequestHandler;

public class SeqServer extends Server{

	@Override
	public void start() {
		super.start();
		while (true) {
		    // take a ready connection from the accepted queue
			Socket connectionSocket;
			try {
				connectionSocket = listenSocket.accept(); // accept is a block function
				// process a request
				RequestHandler wrh = new RequestHandler(connectionSocket, this);
				wrh.processRequest();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} 
	} 
} 