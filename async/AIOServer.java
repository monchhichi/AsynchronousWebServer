package async;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import load.DumbBalancer;

import Common.HTTPRequest;

public class AIOServer {

	public final static int PORT = 8000;
	public final static long TIMEOUT = 1000;

	HTTPRequest httpRequest;
	File requestedFile;
	String root = "./";

	ByteBuffer request;

	public static void main(String[] args) {
		new AIOServer().start();
	}

	public void start() {
		try (AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open()
				.bind(new InetSocketAddress(PORT))) {
			while (true) {
				ByteBuffer buffer = ByteBuffer.allocate(4096);
				Future<AsynchronousSocketChannel> client = server.accept();

				// block with timeout
				AsynchronousSocketChannel channel = client.get();
				Future<Integer> future = channel.read(buffer);

				// async
				future.get();
				buffer.flip();

				StringBuffer sb = new StringBuffer();
				while (buffer.hasRemaining()) {
					char ch = (char) buffer.get();
					sb.append(ch);
				}
				System.out.println("Received: " + sb.toString());

				request = ByteBuffer.allocate(1048576);
				generateResponse(sb);

				channel.write(request);
				channel.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	private void generateResponse(StringBuffer sb) {
		try {
			httpRequest = parse(sb);
			// check virtual URL
			if (httpRequest.getUrl().equals("/load")) {
				DumbBalancer balancer = DumbBalancer.getInstance();
				if (balancer.checkLoad()) {
					request.put(new String("HTTP/1.0 200 OK\r\n").getBytes());
					request.put(new String("Content-Length: 0\r\n").getBytes());
					request.put(new String("\r\n").getBytes());
				} else {
					request.put(new String("HTTP/1.0 503 Service Unavailable\r\n").getBytes());
					request.put(new String("Content-Length: 0\r\n").getBytes());
					request.put(new String("\r\n").getBytes());
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
			request.flip();
		} catch (Exception e) {
			e.printStackTrace();
			// outputError(400, "Server error");
		}
	} // end of generate response

	private HTTPRequest parse(StringBuffer request) throws IOException {
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
		request.put(new String("HTTP/1.0 200 OK\r\n").getBytes());
		request.put(sb.toString().getBytes());
	}

	private void outputFile() throws Exception {
		String filePath = requestedFile.getPath();
		request.put(new String("HTTP/1.0 200 OK\r\n").getBytes());

		if (filePath.endsWith(".jpg"))
			request.put(new String("Content-Type: image/jpeg\r\n").getBytes());
		else if (filePath.endsWith(".gif"))
			request.put(new String("Content-Type: image/gif\r\n").getBytes());
		else if (filePath.endsWith(".html") || filePath.endsWith(".htm"))
			request.put(new String("Content-Type: text/html\r\n").getBytes());
		else
			request.put(new String("Content-Type: text/plain\r\n").getBytes());

		int numOfBytes = (int) requestedFile.length();
		request.put(new String("Content-Length: " + numOfBytes + "\r\n\r\n").getBytes());

		// send file content
		FileInputStream fis = new FileInputStream(requestedFile);

		byte[] fileInBytes = new byte[numOfBytes];
		fis.read(fileInBytes);

		request.put(fileInBytes, 0, numOfBytes);

		fis.close();
	}

}
