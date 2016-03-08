package Common;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class SHTTPTestClient {
	// test client property 
	private String server;
	private String servname;
	private int port;
	private int parallel;
	private List<String> filesList;
	private double testTime;
	
	// count variable
	private int fileCnt = 0;
	private int byteCnt = 0;
	private int wasteTime = 0;
	
	// Debug or not
	private static boolean DEBUG = false;
	
	class RequestThread extends Thread {
		@Override
		public void run() {
			long startTime = System.currentTimeMillis();
			while(true) {
				// The client stops after <time of test in seconds>.
				/*
				 * The client should print out the total transaction throughput (# files finished downloading by all threads, averaged over per second), 
				 * data rate throughput (number bytes received, averaged over per second), 
				 * and the average of wait time (i.e., time from issuing request to getting first data).
				 */
				if(System.currentTimeMillis() - startTime >= testTime * 1000) {
					break;
				}
				for(String filename: filesList) {
					try {
						if(System.currentTimeMillis() - startTime >= testTime * 1000) {
							break;
						}
						// send request
						Socket socket = new Socket(InetAddress.getByName(server), port);
						DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
						/*
						 * GET <URL> HTTP/1.0
						 * Host: <ServerName>
						 * User-Agent: 
						 * If-Modified-Since: Sat, 29 Oct 1994 19:43:31 GMT	
						 * CRLF
						 */
						Calendar calendar = Calendar.getInstance();
						SimpleDateFormat rfc1123format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
						rfc1123format.setTimeZone(TimeZone.getTimeZone("GMT"));
//						String httpString =
//				                "GET " + filename + " HTTP/1.0\r\n" + "Host: " + servname + "\r\n"
//				                    + "If-Modified-Since: " + rfc1123format.format(calendar.getTime()) + "\r\n"
//				                    + "User-Agent: SHTTPTestClient" + "\r\n\r\n";	
						// iphone
//						System.out.println(servname);
						filename = "/classes/cs433/web/www-root/html-big/" + filename;
						
//						System.out.println(filename);

						String httpString =
				                "GET " + filename + " HTTP/1.0\r\n" + "Host: " + servname + "\r\n"
				                    + "If-Modified-Since: " + rfc1123format.format(calendar.getTime()) + "\r\n"
				                    + "User-Agent: iPhone" + "\r\n\r\n";	
						System.out.println(httpString);
						
						// Is this the right place to count the start time?
						long timeIssued = System.currentTimeMillis();
						outToServer.writeBytes(httpString);
						
						// block until getting first data 
						InputStream is = socket.getInputStream();

			            // block until getting first data (collect waste time)
						StringBuffer sb = new StringBuffer();
			            sb.append((char) is.read());
			            timeIssued = System.currentTimeMillis() - timeIssued;

			            byte[] buf = new byte[4096];
			            int total = 0, cnt;
			            while ((cnt = is.read(buf)) > 0) {
			              total += cnt;
			            }
						
						socket.close();
						
//						 update count variable
						synchronized(SHTTPTestClient.this) {
							wasteTime += timeIssued;
							fileCnt++;
							byteCnt += total;
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
				}
			}
		}
	}
	

	public SHTTPTestClient(String server, String servname, int port, int parallel, List<String> filesList,
			double testTime) {
		this.server = server;
		this.servname = servname;
		this.port = port;
		this.parallel = parallel;
		this.filesList = filesList;
		this.testTime = testTime;
	}

	public static void main(String[] args) throws Exception {
		/*
		 *  Format: 
		 *  	%java SHTTPTestClient -server <server> -servname <server name> 
		 *  				-port <server port> -parallel <# of threads> -files <file name> 
		 *  				-T <time of test in seconds>
		 */
		// get parameter
		Map<String, String> options = new HashMap<String, String>();
		for(int i = 0; i < args.length - 1; i += 2) {
			if(args[i].startsWith("-")) {
				options.put(args[i].substring(1), args[i + 1]);
			}
		}
		
		
		
		// check parameter complete or not
		boolean flag = false;
		for(String str: new String[]{"server", "servname", "port", "parallel", "files", "T"}) {
			if(!options.containsKey(str)) {
				System.err.println("Error: Option " + str + " not given");
				flag = true;
			}
		}
		if(flag) {
			System.err.println("Input format:");
			System.err.println("java SHTTPTestClient -server <server> -servname <server name> -port <server port> -parallel <# of threads> -files <file name> -T <time of test in seconds>");
			return;
		}
		
		String server = options.get("server");
		String servname = options.get("servname");
		int port = Integer.parseInt(options.get("port"));
		int parallel = Integer.parseInt(options.get("parallel"));
		List<String> filesList = new ArrayList<String>();
		FileInputStream fis = new FileInputStream(options.get("files"));	 
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		String line = null;
		while ((line = br.readLine()) != null) {
			filesList.add(line);
		}
		br.close();
		double testTime = Double.parseDouble(options.get("T"));
		
		// create testClient class
	    SHTTPTestClient testClient = new SHTTPTestClient(server, servname, port, parallel, filesList, testTime);
	    
	    if(DEBUG) {
	    	System.out.println(testClient.server);
	    	System.out.println(testClient.servname);
	    	System.out.println(testClient.port);
	    	System.out.println(testClient.parallel);
	    	System.out.println(testClient.filesList);
	    	System.out.println(testClient.testTime);
	    }
	    
	    // start testClient
	    testClient.makeRequest();
	    System.out.println(testClient.fileCnt + " files downloaded");
	    System.out.println(testClient.fileCnt / (double) testTime + " files per second");
	    System.out.println(testClient.byteCnt + " bytes received");
	    System.out.println(testClient.byteCnt / (double) testTime + " bytes per second");
	    System.out.println(testClient.wasteTime + " miliseconds wasted");
	    System.out.println(testClient.wasteTime / (double) testTime + " miliseconds wasted per second");
	    
	}
	
	public void makeRequest() {
		
		Thread[] threads = new Thread[parallel];
		for(int i = 0; i < parallel; i++) {
			threads[i] = new RequestThread();
			threads[i].start();
		}
		
		// wait until all the threads to die
		for(Thread thread: threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
