package Common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import server.*;

public class Main {
	
	public final static String DEFAULT_PORT = "8000";
	public final static String DEFAULT_ROOT = "./";
	public final static String DEFAULT_THREAD_NUM = "2";
	static boolean DEBUG = true;
	public static void main(String[] args) throws IOException {
		// Parse command line
		// %java <servername> -config <config_file_name>
		if(args.length < 2 || !args[0].equals("-config")) {
			System.out.println("Config file not given!");
			System.out.println("Correct input format:");
			System.out.println("java Main -config <config_file_name>");
			return;
		}
		String configFileName = args[1];
		// Parse config file
		/*
		  	Listen 6789
			ThreadPoolSize 8
			CacheSize 8096
			
			<VirtualHost *:6789>
			  DocumentRoot  /home/httpd/html/zoo/classes/cs433/web/www-root
			  ServerName cicada.cs.yale.edu
			<VirtualHost>  
			
			<VirtualHost *:6789>
			  DocumentRoot  /home/httpd/html/zoo/classes/cs433/web/mobile-root
			  ServerName mobile.cicada.cs.yale.edu
			<VirtualHost>  
		 */
		HashMap<String, String> config = new HashMap<String, String>();
		// set default val
		String defaultRoot = DEFAULT_ROOT;
		config.put("Listen", DEFAULT_PORT);
		config.put("ThreadPoolSize", DEFAULT_THREAD_NUM);
		
		File configFile = new File(configFileName);
		if(!configFile.exists()) {
			System.err.println("File " + configFileName + " not found!");
			return;
		}
		
		FileReader fr = new FileReader(configFile);
		BufferedReader br = new BufferedReader(fr);
		String line;
		while ((line = br.readLine()) != null) {
			if(DEBUG) {
				System.out.println(line);
			}
			line = line.trim();
			if(line.isEmpty()) {
				continue;
			}
			if(line.startsWith("<VirtualHost")) {
				String serverName = "localhost", docRoot = "./";
				while(!(line = br.readLine()).contains("</VirtualHost>")) { // According to Apache configuration style, should be </VirtualHost> instead of <VirtualHost>
					if(DEBUG) {
						System.out.println(line);
					}
					line = line.trim();
					String[] kv = line.split("\\s+");
					if(kv.length != 2) {
						System.err.println("Format error in line:" + line);
						return;
					}
					if(kv[0].equals("DocumentRoot")) {
						docRoot = kv[1];
						if(defaultRoot.equals(DEFAULT_ROOT)) {
							defaultRoot = docRoot;
						}
					}
					else if(kv[0].equals("ServerName")) {
						serverName = kv[1];
					}
				}
				config.put(serverName, docRoot);
				if(DEBUG) {
					System.out.println("</VirtualHost>");
				}
			}
			else {
				String[] kv = line.split("\\s+");
				if(kv.length != 2) {
					System.err.println("Format error in line:" + line);
					return;
				}
				config.put(kv[0], kv[1]);
			}
		}
		br.close();
		
		if(DEBUG) {
			System.out.println("\n### input config: ###");
			for(Entry<String, String> entry: config.entrySet()) {
				System.out.println(entry.getKey() + " : " + entry.getValue());
			}
		}
		
		// Turn to different server according to the Approach parameter
		if(!config.containsKey("Approach") || config.get("Approach") == null) {
			System.err.println("Invalid Approach for server!");
			return;
		}
		String approach = config.get("Approach");
		Server server;
		switch (approach) {
			case "sequential"		: server = new SeqServer(); 		break;
			case "perRequestThread"	: server = new PerThreadServer(); 	break;
			case "welcome"			: server = new WelcomeServer();		break;
			case "busywait"			: server = new BusyWaitServer();	break;
			case "suspension"		: server = new SuspensionServer();	break;
			default: System.err.println("Unknown approach!"); return;
		}
		server.setCache(Integer.parseInt(config.get("CacheSize")));
		server.setThreadNum(Integer.parseInt(config.get("ThreadPoolSize")));
		server.setPort(Integer.parseInt(config.get("Listen")));
		server.setRoot(defaultRoot);
		server.start();
	}
}
