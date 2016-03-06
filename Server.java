import java.io.*;
import java.net.*;
import java.util.*;

public class Server{

    public static int serverPort = 6789;    
    public static String WWW_ROOT = "./";

    public static void main(String args[]) throws Exception  {
	
		//  get port
		if (args.length >= 1) {
		    serverPort = Integer.parseInt(args[0]);
		}
	
		// get root
		if (args.length >= 2) {
		    WWW_ROOT = args[1];
		}
		
		// create server socket
		ServerSocket listenSocket = new ServerSocket(serverPort);
		System.out.println("Server listening at: " + listenSocket);
		System.out.println("Server www root: " + WWW_ROOT);
	
		while (true) {
		    try {
			    // take a ready connection from the accepted queue
			    Socket connectionSocket = listenSocket.accept();
			    System.out.println("\nReceive request from " + connectionSocket);
		
			    // process a request
			    RequestHandler wrh = new RequestHandler(connectionSocket, WWW_ROOT);
			    wrh.processRequest();
	
		    } catch (Exception e) {
		    	
			}
		} 
    } 
} 