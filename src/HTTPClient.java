import java.net.*;
import java.io.*;

public class HTTPClient extends Thread{

	/**
	 * Reads one message from the HTTP header
	 */
	public static String readLine( InputStream is ) throws IOException {
		StringBuffer sb = new StringBuffer();
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



	public static void main(String[] args) throws Exception {
		String url = args.length == 1 ? args[0] : "http://asc.di.fct.unl.pt/index.html";
		URL u = new URL(url);

		System.out.println("\n========================================\n");
		System.out.println("Processing url: "+url+"\n");
		System.out.println("========================================\n");
		
		System.out.println("protocol = " + u.getProtocol());
		System.out.println("authority = " + u.getAuthority());
		System.out.println("host = " + u.getHost());
		System.out.println("port = " + u.getPort());
		System.out.println("path = " + u.getPath());
		System.out.println("query = " + u.getQuery());
		System.out.println("filename = " + u.getFile());
		System.out.println("ref = " + u.getRef());
		System.out.println(u);

		// Assuming URL of the form http:// ....

		InetAddress serverAddr = InetAddress.getByName(u.getHost());
		int port = u.getPort();
		if ( port == -1 ) port = 80;
		String fileName = u.getPath();
		Socket sock = new Socket( serverAddr, port );
		OutputStream toServer = sock.getOutputStream();
		InputStream fromServer = sock.getInputStream();

		System.out.println("\n========================================\n");
		System.out.println("Connected to server");
		String request = String.format("GET %s HTTP/1.0\r\n\r\n", fileName);
		toServer.write(request.getBytes());
		System.out.println("Sent request: "+request);
		System.out.println("========================================");
		String answerLine = readLine(fromServer);
		System.out.println("Got answer: "+answerLine+"\n");
		String[] result = parseHttpReply(answerLine);
		answerLine = readLine(fromServer);
		while ( !answerLine.equals("") ) {
			System.out.println("Header line:\t"+answerLine);
			answerLine = readLine(fromServer);
		}
		System.out.println("\n========================================");
		System.out.println("\nGot an empty line, showing body \n");
		System.out.println("========================================");
		int c ;
		while( (c = fromServer.read() ) > 0 ) {
			System.out.print((char) c);
		}
		System.out.println();
		sock.close();
	}

}

