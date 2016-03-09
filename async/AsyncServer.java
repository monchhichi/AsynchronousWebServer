package async;

import java.nio.channels.*;
import java.net.*;
import java.io.IOException;

import server.Server;

public class AsyncServer extends Server {

	public static int DEFAULT_PORT = 8000;
	public static int DEFAULT_TIMEOUT = 3000;

	@Override
	public void start() {

		// get dispatcher/selector
		Dispatcher dispatcher = new Dispatcher();

		// open server socket channel
		ServerSocketChannel sch = openServerChannel(port);

		// create server acceptor for Echo Line ReadWrite Handler
		ISocketReadWriteHandlerFactory echoFactory = new EchoLineReadWriteHandlerFactory();
		Acceptor acceptor = new Acceptor(echoFactory);

		Thread dispatcherThread;
		// register the server channel to a selector
		try {
			SelectionKey key = sch.register(dispatcher.selector(), SelectionKey.OP_ACCEPT);
			key.attach(acceptor);

			// start dispatcher
			dispatcherThread = new Thread(dispatcher);
			dispatcherThread.start();
		} catch (IOException ex) {
			System.out.println("Cannot register and start server");
			System.exit(1);
		}
	}

	public static ServerSocketChannel openServerChannel(int port) {
		ServerSocketChannel serverChannel = null;
		try {

			// open server socket for accept
			serverChannel = ServerSocketChannel.open();

			// extract server socket of the server channel and bind the port
			ServerSocket ss = serverChannel.socket();
			InetSocketAddress address = new InetSocketAddress(port);
			ss.bind(address);

			// configure it to be non blocking
			serverChannel.configureBlocking(false);

			Debug.DEBUG("Server listening for connections on port " + port);

		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
		} // end of catch

		return serverChannel;
	} // end of open serverChannel

} // end of class
