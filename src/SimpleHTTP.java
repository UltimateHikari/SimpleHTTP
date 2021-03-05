import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Date;

public class SimpleHTTP implements Runnable{
	private Socket client;
	private BufferedReader rd = null;
	private PrintWriter out = null;
	private OutputStream byteOut = null;
	private String request, filename;
	private Integer timeout = 5; //in seconds
	private String connection = "keep-alive";
	private String acceptString = null;
	private final String acceptable = "text/plain, text/html, image/jpeg";
	private String currentType = null;
	private boolean isPersistent = true;

	
	public SimpleHTTP(Socket c) {
		client = c;
	}

	@Override
	public void run() {
		try {
			client.setSoTimeout(timeout*1000);
			
			rd = new BufferedReader(new InputStreamReader(client.getInputStream()));
			byteOut = client.getOutputStream();
			out = new PrintWriter(byteOut);
			
			String s = null;
			while(isPersistent && (s = rd.readLine()) != null) {
				
				notifyLog("New request");
				
				fetchHeaders(s);
				
				if(request.equals("GET")) {
					//not Files.readAllBytes 'cause need that 404 exception
					
					setMIME();
					
					if(filename.equals("/")) {
						filename = "/index.html";
					}
					
					File file = new File("."  + filename);
					FileInputStream fin = new FileInputStream(file);
					byte [] readFile = fin.readAllBytes();
					
					respond200(file);
					byteOut.write(readFile);
					byteOut.flush();
					
					fin.close();
				} else {
					respondClose("405 Not Allowed");
				}
			}
			notifyLog("reading " + (s = rd.readLine()));
		} catch (IllegalArgumentException e) {
			notifyLog("bad MIME type" + e.getMessage());
			respondClose("415 Unsupported Media Type");
		} catch (FileNotFoundException e) {
			notifyLog("File not found");
			respondClose("404 Not Found");
		} catch (SocketTimeoutException e) {
			notifyLog("Connection timed out: " + e.getMessage());
			//respondClose("408 Request Timeout"); //Socket is ded anyway
		} catch (IOException e) { 
			notifyLog("IOE: " + e.getMessage());
		} finally {
			try {
				rd.close();
				byteOut.close();
				out.close();
				client.close();
				notifyLog("Connection closed");
			} catch (IOException e) {
				notifyLog("Error closing: " + e.getMessage());
			}
		}
		
	}

	private void setMIME() throws IllegalArgumentException{
		//guess from filename (for browser use)
		String category = null, type = null;
		if(filename.endsWith(".jpg")) {
			category = "image";
			type = "jpeg";
		}
		if(filename.endsWith(".htm") || filename.endsWith(".html") || filename.equals("/")) {
			category = "text";
			type = "html";
		}
		if(filename.endsWith(".txt")) {
			category = "text";
			type = "plain";
		}
		if(category == null) { throw new IllegalArgumentException("not deduced from filename"); }
		
		notifyLog(acceptString);
		
		if(
				acceptString.contains(category + "/" + type) || 
				acceptString.contains(category + "/*") || 
				acceptString.contains("*/*")
				) {
			currentType = category + "/" + type;
		}else {
			throw new IllegalArgumentException("deduced is not acceptable");
		}
	}
	
	private void fetchHeaders(String firstHeader) throws IOException{
		String s = null;
		String [] tokens;
		
		tokens = firstHeader.split("\s");
		request = tokens[0];
		filename = tokens[1];
		
		notifyLog(request + " " + filename);
		
		while((s = rd.readLine()) != null) {
			//System.out.println(s);
			if(s.equals("")) {
				//end of headers
				break;
			}
			tokens = s.split("\s");
			
			if(tokens[0].equals("Connection:") && tokens[1].toLowerCase().equals("close")) {
				//keep-alive by default
				isPersistent = false;
				connection = "close";
			}
			
			if(tokens[0].equals("Keep-Alive:")) {
				for(String i:tokens) {
					notifyLog("KA param: " + i);
				}
			}
			
			if(tokens[0].equals("Accept:")) {
				acceptString = s;
			}
		}
	}
	
	private void respond200(File file) {
		notifyLog("200 OK");
		out.println("HTTP/1.1 200 OK");
		out.println("Server: SimpleHTTPServer");
		out.println("Date: " + new Date());
		out.println("Last-Modified: " + new Date(file.lastModified()));
		out.println("Content-type: text/html");
		out.println("Content-length: " + file.length());
		out.println("Connection: " + connection);
		out.println();
		out.flush();
	}
	

	private void respondClose(String code) {
		notifyLog(code);
		out.println("HTTP/1.1 " + code);
		out.println("Server: SimpleHTTPServer");
		out.println("Date: " + new Date());
		out.println("Content-type: " + currentType);
		out.println("Content-length: 0");
		out.println("Connection: " + connection);
		out.println();
		out.flush();
	}
	
	private void notifyLog(String s) {
		System.out.println("Server: " + s);
	}

}
