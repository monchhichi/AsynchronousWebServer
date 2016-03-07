package Common;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import server.Server;

public class RequestHandler {

    static boolean _DEBUG = false;
    static int reqCount = 0;

    String root;
    Socket connSocket;
    BufferedReader inFromClient;
    DataOutputStream outToClient;

    File requestedFile;
    HTTPRequest request;
    Server server;
    
    public RequestHandler(Socket connectionSocket, Server server) throws IOException {
        reqCount ++;

	    this.connSocket = connectionSocket;
	    this.server = server;
	    this.root = server.getRoot();
	    
	    inFromClient = new BufferedReader(new InputStreamReader(connSocket.getInputStream()));
	    outToClient = new DataOutputStream(connSocket.getOutputStream());
    }

    public void processRequest() {
		try {
			request = parse();
			
			// mobile
			String url = request.getUrl();
			if(request.getHeaderUserAgent() != null && request.getHeaderUserAgent().toLowerCase().contains("iphone")) {
				if(url.equals("/")) {
					requestedFile = new File("index_m.html");
					if(!requestedFile.exists()) {
						requestedFile = new File(root + "index.html");
					}
				}
				else {
					requestedFile = new File(root + url);
				}
			}
			// normal
			else {
				if(url.equals("/")) {
					requestedFile = new File(root + "index.html");
				}
				else {
					requestedFile = new File(root + url);
				}
			}
			
			DEBUG("Map to File name: " + requestedFile);
			if (!requestedFile.isFile()) {
			    outputError(404,  "Not Found");
			    requestedFile = null;
		    } 
			else {
				if(requestedFile.canExecute()) {
					outputExecuteResult();
				}
				else {
					outputFile();
				}
			}
			
		    connSocket.close();
		} catch (Exception e) {
		    outputError(400, "Server error");
		}

    } 
    
    private void outputFile() throws IOException {
    	//Format
    	/*
    	 	HTTP/1.0 <StatusCode> <message>
			Date: <date>
			Server: <your server name>
			Content-Type: text/html
			Last-Modified: <date>
			Content-Length: <LengthOfFile>
			CRLF
			<file content>
    	 */
    	String url = request.getUrl();
    	
    	// Message line
    	outToClient.writeBytes("HTTP/1.0 200 Document Follows\r\n");
    	
    	// Date
    	SimpleDateFormat rfc1123format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
		rfc1123format.setTimeZone(TimeZone.getTimeZone("GMT"));
		Calendar calendar = Calendar.getInstance();		
	    outToClient.writeBytes("Date: " + rfc1123format.format(calendar.getTime()) + "\r\n");
	    
	    // Server
	    outToClient.writeBytes("Server: " + request.getServer() + "\r\n");
	    // Content-Type
	    if (url.endsWith(".jpg")) {
	        outToClient.writeBytes("Content-Type: image/jpeg\r\n");
	    }
	    else if (url.endsWith(".gif")) {
	        outToClient.writeBytes("Content-Type: image/gif\r\n");
	    }
	    else if (url.endsWith(".html") || url.endsWith(".htm")) {
	        outToClient.writeBytes("Content-Type: text/html\r\n");
	    }
	    else {
	        outToClient.writeBytes("Content-Type: text/plain\r\n");
	    }
	    // Last-Modified
	    outToClient.writeBytes("Last-Modified: " + rfc1123format.format(requestedFile.lastModified()) + "\r\n");
	    
	    // Content-Length + content	    
	    
	    String cached = server.getFromCache(url);

	    // If in cache
	    if(cached != null) {
	    	int numOfBytes = cached.length();
	    	outToClient.writeBytes("Content-Length: " + numOfBytes + "\r\n");
	    	outToClient.writeBytes("\r\n");
	        outToClient.writeBytes(cached);
	        return;
	    }
	    // If not in cache
	    int numOfBytes = (int)requestedFile.length();
	    outToClient.writeBytes("Content-Length: " + numOfBytes + "\r\n");
    	outToClient.writeBytes("\r\n");
    	// <file content>
	    FileInputStream fis  = new FileInputStream (requestedFile);
	    byte[] fileInBytes = new byte[numOfBytes];
	    fis.read(fileInBytes);
	    outToClient.write(fileInBytes, 0, numOfBytes);
	    server.updateCache(url, new String(fileInBytes));
	    fis.close();
	}

	private void outputExecuteResult() throws IOException {
		ProcessBuilder pb = new ProcessBuilder(requestedFile.getPath());
		// set variables for CGI
		Map<String, String> env = pb.environment();
		env.put("REQUEST_METHOD", request.getMethod());
//	    env.put("QUERY_STRING", request.getQuery());
	    env.put("Content_Type", "application/x-www-form-urlencode");
	    
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
	    outToClient.writeBytes("HTTP/1.0 200 OK\r\n");
	    outToClient.writeBytes(sb.toString());
	}

	// Find the corresponding file of URL
    private HTTPRequest parse() throws Exception {
		HTTPRequest rt = new HTTPRequest();
		
	    String requestMessageLine = inFromClient.readLine();
	    DEBUG("Request " + reqCount + ": " + requestMessageLine); // GET file3.html HTTP/1.0

	    // process the message line to get the method and url
	    String[] request = requestMessageLine.split("\\s");

	    if (request.length < 2 || !request[0].equals("GET")) {
		    outputError(500, "Bad request");
		    return null;
	    }
	    rt.setMethod(request[0]);
	    rt.setUrl(request[1]);
	    
	    // process the header to get If-Modified-Since and User-Agent
	    String header = inFromClient.readLine();
        while (!header.equals("")) {
           header.trim();
           String val = header.substring(header.indexOf(' ') + 1);
           if(header.startsWith("If-Modified-Since")) {
        	   rt.setHeaderIfModifiedSince(val);
           }
           else if(header.startsWith("User-Agent")) {
        	   rt.setHeaderUserAgent(val);
           }
           else if (header.startsWith("Content-Length")) {
        	   rt.setHeaderContentLength(val);
           } 
           else if (header.startsWith("Content-Type")) {
        	   rt.setHeaderContentType(val);
           }
           else if (header.startsWith("Server")) {
        	   rt.setServer(val);
           }
           header = inFromClient.readLine();
        }
        return rt;
    } 


    void outputError(int errCode, String errMsg) {
	    try {
	        outToClient.writeBytes("HTTP/1.0 " + errCode + " " + errMsg + "\r\n");
	    } catch (Exception e) {}
    }

    static void DEBUG(String s) {
       if (_DEBUG)
          System.out.println( s );
    }
}