/**
 * HTTP Server example
 *
 * RC - 2017/2018 (MIEI - FCT/UNL)
 *
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import com.sun.xml.internal.txw2.output.DumpSerializer;

/**
 * Super simple and incomplete HTTP Server
 */
public class TinyHTTPServer {
	
	static final int PORT = 1234 ;
	
	
	/**
	 * Copies data from an input stream to an output stream
	 */
	static void dumpStream( InputStream in, OutputStream out)
			throws IOException {
		byte[] arr = new byte[1024];
		for( ;;) {
			int n = in.read( arr);
			if( n == -1)
				break;
			out.write( arr, 0, n);
		}
	}
	
	/**
	 * Reads one message from the HTTP header
	 */
	public static String readLine( InputStream is ) throws IOException {
		StringBuffer sb = new StringBuffer() ;
		
		int c ;
		while( (c = is.read() ) >= 0 ) {
			if( c == '\r' ) continue ;
			if( c == '\n' ) break ;
			sb.append( new Character( (char)c) ) ;
		}
		return sb.toString() ;
	} 
	
	
	/**
	 * Parses the first line of the HTTP request and returns an array
	 * of three strings: reply[0] = method, reply[1] = object and reply[2] = version
	 * Example: input "GET /index.html HTTP/1.0"
	 * output reply[0] = "GET", reply[1] = "/index.html" and reply[2] = "HTTP/1.0"
	 * 
	 * If the input is malformed, it returns something unpredictable
	 */


	public static String[] parseHttpRequest( String request) {
		String[] error = { "ERROR", "", "" };
		String[] result = { "", "", "" };
		int pos0 = request.indexOf( ' ');
		if( pos0 == -1) return error;
		result[0] = request.substring( 0, pos0).trim();
		pos0++;
		int pos1 = request.indexOf( ' ', pos0);
		if( pos1 == -1) return error;
		result[1] = request.substring( pos0, pos1).trim();
		result[2] = request.substring( pos1 + 1).trim();
		if(! result[1].startsWith("/")) return error;
		if(! result[2].startsWith("HTTP")) return error;
		return result;
	}

	/**
	 * Parses the first line of the HTTP reply and returns an array
	 * of three strings: reply[0] = version, reply[1] = number and reply[2] = result message
	 * Example: input "HTTP/1.0 501 Not Implemented"
	 * output reply[0] = "HTTP/1.0", reply[1] = "501" and reply[2] = "Not Implemented"
	 * 
	 * If the input is malformed, it returns something unpredictable
	 */

	public static String[] parseHttpReply (String reply) {
		String[] result = { "", "", "" };
		int pos0 = reply.indexOf(' ');
		if( pos0 == -1) return result;
		result[0] = reply.substring( 0, pos0).trim();
		pos0++;
		int pos1 = reply.indexOf(' ', pos0);
		if( pos1 == -1) return result;
		result[1] = reply.substring( pos0, pos1).trim();
		result[2] = reply.substring( pos1 + 1).trim();
		return result;
	}

	
	/**
	 * Returns an input stream with an error message "Not Implemented"
	 */
	static InputStream notSupportedPageStream() {
		final String page = 
				"<HTML><BODY>Request Not Supported</BODY></HTML>" ;
		int length = page.length();
		StringBuilder reply = new StringBuilder("HTTP/1.0 501 Not Implemented\r\n");
		reply.append("Date: "+new Date().toString()+"\r\n");
		reply.append("Server: "+"The tiny server (v0.9)"+"\r\n");
		reply.append("Content-Length: "+String.valueOf(length)+"\r\n\r\n");
		reply.append( page );
		return new ByteArrayInputStream( reply.toString().getBytes());
	}
	
	/**
	 * Returns an input stream with a very simple valid page with the text of the input
	 */
	static InputStream simplePageStream(String somePage) {
		String page = "<HTML><BODY>" + somePage + "</BODY></HTML>";
		int length = page.length();
		StringBuilder reply = new StringBuilder("HTTP/1.0 200 OK\r\n");
		reply.append("Date: "+new Date().toString()+"\r\n");
		reply.append("Server: "+"The tiny server (v0.9)"+"\r\n");
		reply.append("Content-Length: "+String.valueOf(length)+"\r\n\r\n");
		reply.append( page );
		return new ByteArrayInputStream( reply.toString().getBytes());
	}
	
	
	
	/**
	 * sendFile: when available, sends the file in the URL to the client
	 * 
	 */
	static void sendFile (String fileName, OutputStream out)
			throws IOException {
		
		File file = new File(fileName.substring(1));
		
		if(file.exists()) {
		System.out.println(file.length());
		StringBuilder reply = new StringBuilder("HTTP/1.0 200 OK\r\n");
		reply.append("Date: "+new Date().toString()+"\r\n");
		reply.append("Server: The tiny server (v0.9) \r\n" );
		reply.append("Content-Length: "+ file.length() +"\r\n\r\n");
		
		out.write(reply.toString().getBytes());
		
		FileInputStream f = new FileInputStream(file);
		dumpStream(f, out);
		}
		
		else {
			StringBuilder reply = new StringBuilder("HTTP/1.0 404 NOT FOUND\r\n");
			reply.append("\r\n");
			//reply.append("Date: "+new Date().toString()+"\r\n");
			//reply.append("Server: "+"The tiny server (v0.9)"+"\r\n");
			//reply.append("Content-Length: "+String.valueOf(length)+"\r\n\r\n");
			out.write(reply.toString().getBytes());
		}			
}
	

	

	
	
	/**
	 * processHTTPrequest - handle one HTTP request
	 * 
	 * @param in - stream from client
	 * @param out - stream to client
	 */
	private static void processHTTPrequest(InputStream in, OutputStream out) {
		try {
			
			String request = readLine(in);
		 	System.out.println( "received: "+request );
			String[] requestParts = parseHttpRequest(request);
			// ignora-se o resto da mensagem HTTP
			while ( ! readLine( in ).equals("") );
			// tratamento do pedido
			if( requestParts[0].equalsIgnoreCase("GET") ) {
				 sendFile(requestParts[1] , out);
				//dumpStream (simplePageStream("Logo que conseguir passo a"+
				//	" enviar-lhe o ficheiro pedido!"),out);
			} else { // ignore other requests
				dumpStream (notSupportedPageStream(),out);
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}

	
	/**
	 * MAIN - accept and handle client connections
	 */
	
	public static void main(String[] args) throws IOException {
		ServerSocket ss = new ServerSocket( PORT );
		System.out.println("Server ready at "+PORT);
			new Thread(() -> {
				try {
					for (;;) {
						
						Socket clientS = ss.accept();
						InputStream in = clientS.getInputStream();
						OutputStream out = clientS.getOutputStream();
						processHTTPrequest( in, out );
				
						clientS.close();
					}
				} catch(IOException e) {
				}
			}).start();
	}
		
}
