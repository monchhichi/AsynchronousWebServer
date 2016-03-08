package async;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;

import Common.HTTPRequest;
import load.DumbBalancer;
import load.LoadBalancer;

public class RequestHandler implements IReadWriteHandler {

	private ByteBuffer inBuffer;
	private ByteBuffer outBuffer;

	private boolean requestComplete;
	private boolean responseReady;
	private boolean responseSent;
	private boolean channelClosed;

	private StringBuffer request;
	HTTPRequest httpRequest;

	File requestedFile;
	String root = "./";

	public RequestHandler() {
		inBuffer = ByteBuffer.allocate(4096);
		outBuffer = ByteBuffer.allocate(1048576);

		// initial state
		requestComplete = false;
		responseReady = false;
		responseSent = false;
		channelClosed = false;

		request = new StringBuffer(4096);
	}

	public int getInitOps() {
		return SelectionKey.OP_READ;
	}

	public void handleException() {
	}

	public void handleRead(SelectionKey key) throws IOException {

		// a connection is ready to be read
		Debug.DEBUG("->handleRead");

		if (requestComplete) { // this call should not happen, ignore
			return;
		}

		// process data
		processInBuffer(key);

		// update state
		updateState(key);

		Debug.DEBUG("handleRead->");

	} // end of handleRead

	private void updateState(SelectionKey key) throws IOException {

		Debug.DEBUG("->Update dispatcher.");

		if (channelClosed)
			return;

		/*
		 * if (responseSent) { Debug.DEBUG(
		 * "***Response sent; shutdown connection"); client.close();
		 * dispatcher.deregisterSelection(sk); channelClosed = true; return; }
		 */

		int nextState = key.interestOps();
		if (requestComplete) {
			nextState = nextState & ~SelectionKey.OP_READ;
			Debug.DEBUG("New state: -Read since request parsed complete");
		} else {
			nextState = nextState | SelectionKey.OP_READ;
			Debug.DEBUG("New state: +Read to continue to read");
		}

		if (responseReady) {

			if (!responseSent) {
				nextState = nextState | SelectionKey.OP_WRITE;
				Debug.DEBUG("New state: +Write since response ready but not done sent");
			} else {
				nextState = nextState & ~SelectionKey.OP_WRITE;
				Debug.DEBUG("New state: -Write since response ready and sent");
			}
		}

		key.interestOps(nextState);

	}

	public void handleWrite(SelectionKey key) throws IOException {
		Debug.DEBUG("->handleWrite");

		// process data
		SocketChannel client = (SocketChannel) key.channel();
		Debug.DEBUG("handleWrite: Write data to connection " + client + "; from buffer " + outBuffer);
		int writeBytes = client.write(outBuffer);
		Debug.DEBUG("handleWrite: write " + writeBytes + " bytes; after write " + outBuffer);

		if (responseReady && (outBuffer.remaining() == 0)) {
			responseSent = true;
			Debug.DEBUG("handleWrite: responseSent");
		}

		// update state
		updateState(key);

		// try {Thread.sleep(5000);} catch (InterruptedException e) {}
		Debug.DEBUG("handleWrite->");
	} // end of handleWrite

	private void processInBuffer(SelectionKey key) throws IOException {
		Debug.DEBUG("processInBuffer");
		SocketChannel client = (SocketChannel) key.channel();
		int readBytes = client.read(inBuffer);
		Debug.DEBUG("handleRead: Read data from connection " + client + " for " + readBytes + " byte(s); to buffer "
				+ inBuffer);

		if (readBytes == -1) { // end of stream
			requestComplete = true;

			Debug.DEBUG("handleRead: readBytes == -1");
		} else {
			inBuffer.flip(); // read input
			// outBuffer = ByteBuffer.allocate( inBuffer.remaining() );
			while (!requestComplete && inBuffer.hasRemaining() && request.length() < request.capacity()) {
				char ch = (char) inBuffer.get();
				request.append(ch);
			} // end of while
		}
		System.out.println(request.toString());
		inBuffer.clear(); // we do not keep things in the inBuffer

		if (requestComplete) {
			generateResponse();
		}
	} // end of process input

	private void generateResponse() {
		try {

			httpRequest = parse();
			// check virtual URL
			if (httpRequest.getUrl().equals("/load")) {
				DumbBalancer balancer = DumbBalancer.getInstance();
				if (balancer.checkLoad()) {
					outBuffer.put(new String("HTTP/1.0 200 OK\r\n").getBytes());
					outBuffer.put(new String("Content-Length: 0\r\n").getBytes());
					outBuffer.put(new String("\r\n").getBytes());
				} else {
					outBuffer.put(new String("HTTP/1.0 503 Service Unavailable\r\n").getBytes());
					outBuffer.put(new String("Content-Length: 0\r\n").getBytes());
					outBuffer.put(new String("\r\n").getBytes());
				}
			} else {
				// map URL to File
				// iPhone
				if (httpRequest.getHeaderUserAgent() != null && httpRequest.getHeaderUserAgent().contains("iPhone"))
					requestedFile = mobileFile(httpRequest.getUrl());
				else {
					// normal
					String url = httpRequest.getUrl();
					if (url.equals("/")) {
						requestedFile = new File("index.html");
					} else {
						if (url.startsWith("/"))
							url = url.substring(1);
						requestedFile = new File(root + httpRequest.getUrl());
					}
				}

				System.out.println("Map to File name: " + requestedFile);
				if (!requestedFile.isFile()) {
					// outputError(404, "Not Found");
					requestedFile = null;
				} else {
					if (requestedFile.canExecute())
						outputExecResult();
					else
						outputFile();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			// outputError(400, "Server error");
		}
		outBuffer.flip();
		responseReady = true;
	} // end of generate response

	private void outputExecResult() throws Exception {
		ProcessBuilder pb = new ProcessBuilder(requestedFile.getPath());

		// set variables for CGI
		Map<String, String> env = pb.environment();
		env.put("REQUEST_METHOD", httpRequest.getMethod());
		env.put("QUERY_STRING", httpRequest.getQuery());
		env.put("Content_type", "application/x-www-form-urlencode");
		// invoke script
		Process b = pb.start();

		// get result
		InputStream is = b.getInputStream();

		StringBuilder sb = new StringBuilder();
		byte[] buf = new byte[8 * 1024];
		while ((is.read(buf)) != -1) {
			sb.append(new String(buf));
		}
		is.close();

		// output
		outBuffer.put(new String("HTTP/1.0 200 OK\r\n").getBytes());
		outBuffer.put(sb.toString().getBytes());
	}

	private void outputFile() throws Exception {
		String filePath = requestedFile.getPath();
		outBuffer.put(new String("HTTP/1.0 200 OK\r\n").getBytes());
		outBuffer.put(new String("Set-Cookie: MyCool433Seq12345\r\n").getBytes());

		if (filePath.endsWith(".jpg"))
			outBuffer.put(new String("Content-Type: image/jpeg\r\n").getBytes());
		else if (filePath.endsWith(".gif"))
			outBuffer.put(new String("Content-Type: image/gif\r\n").getBytes());
		else if (filePath.endsWith(".html") || filePath.endsWith(".htm"))
			outBuffer.put(new String("Content-Type: text/html\r\n").getBytes());
		else
			outBuffer.put(new String("Content-Type: text/plain\r\n").getBytes());

		int numOfBytes = (int) requestedFile.length();
		outBuffer.put(new String("Content-Length: " + numOfBytes + "\r\n\r\n").getBytes());

		// send file content
		FileInputStream fis = new FileInputStream(requestedFile);

		byte[] fileInBytes = new byte[numOfBytes];
		fis.read(fileInBytes);

		outBuffer.put(fileInBytes, 0, numOfBytes);

		fis.close();
	}

	private HTTPRequest parse() throws IOException {
		HTTPRequest rt = new HTTPRequest();
		InputStream is = new ByteArrayInputStream(new String(request).getBytes());
		BufferedReader br = new BufferedReader(new InputStreamReader(is));

		// first line
		String requestMessageLine = br.readLine();
		// DEBUG("Request " + reqCount + ": " + requestMessageLine);
		String[] firstLine = requestMessageLine.split("\\s");

		// if (firstLine.length < 3) {
		// outputError(500, "Bad request");
		// return null;
		// }

		rt.setMethod(firstLine[0]);
		int tmp;
		if ((tmp = firstLine[1].indexOf('?')) != -1) {
			rt.setUrl(firstLine[1].substring(0, tmp));
			rt.setQuery(firstLine[1].substring(tmp));
		} else
			rt.setUrl(firstLine[1]);
		rt.setQuery("");

		// headers
		// TODO reflection
		String headerLine = br.readLine();
		while (headerLine != null && !headerLine.equals("")) {
			String val = headerLine.substring(headerLine.indexOf(' ') + 1);
			if (headerLine.startsWith("If-Modified-Since")) {
				rt.setHeaderIfModifiedSince(val);
			} else if (headerLine.startsWith("User-Agent")) {
				rt.setHeaderUserAgent(val);
			} else if (headerLine.startsWith("Content-Length")) {
				rt.setHeaderContentLength(val);
			} else if (headerLine.startsWith("Content-Type")) {
				rt.setHeaderContentType(val);
			}
			headerLine = br.readLine();
		}

		// POST body
		try {
			char[] buf = new char[Integer.parseInt(rt.getHeaderContentLength())];
			br.read(buf);
			rt.setMsgBody(new String(buf));
		} catch (Exception e) {
			//
		}

		return rt;
	}

	private File mobileFile(String path) {
		File rt;
		if (path.equals("/")) {
			rt = new File("index_m.html");
			if (!rt.exists())
				rt = new File("index.html");
		} else {
			rt = new File(path);
		}
		return rt;
	}
}
