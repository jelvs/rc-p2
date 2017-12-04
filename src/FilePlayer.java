import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;
import java.io.*;

public class FilePlayer extends Thread{

	private static String url;
	private static final String COCO = "C";
	private static final String DANTE = "D";
	private static TreeMap<Integer, ArrayList<String>> segments = new TreeMap<Integer, ArrayList<String>>();
	private static Map<Integer, ArrayList<Integer>> avgBand = new HashMap<Integer, ArrayList<Integer>>();
	private static ArrayList<String> seg = new ArrayList<String>();
	private static ArrayList<Integer> avgB = new ArrayList<Integer>(); // average bandWidth
	private static long playoutDelay;
	private static String TXT = "descriptor.txt";



	public static void main(String[] args) throws Exception {

		Scanner inn = new Scanner(System.in);
		System.out.println("Iniciar coco = C || Iniciar dante = D");
		String cmd = getCommand(inn);
		switch(cmd){
		case COCO:
			url = args.length == 1 ? args[0] : "http://localhost:8080/coco/" ; 	
			break;
		case DANTE:
			url = args.length == 1 ? args[0] : "http://localhost:8080/dante/" ;
			break;
		default:
			break;
		}
		playoutDelay = args.length == 2 ? Integer.parseInt(args[1]) : 10;
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
		Socket sock = new Socket( serverAddr, port );
		String descriptor = u.getPath().concat(TXT);
		OutputStream toServer = sock.getOutputStream();
		InputStream fromServer = sock.getInputStream();

		System.out.println("\n========================================\n");
		System.out.println("Connected to server");

		String request = String.format("GET %s HTTP/1.0\r\n" + "User-Agent: X-RC2017\r\n\r\n", descriptor);
		toServer.write(request.getBytes());

		System.out.println("Sent request: "+ request);
		System.out.println("========================================");

		String answerLine = readLine(fromServer);

		System.out.println("Got answer: "+ answerLine +"\n");



		String[] result = parseHttpReply(answerLine);
		answerLine = readLine(fromServer);
		while ( !answerLine.equals("") ) {
			System.out.println("Header line:\t" + answerLine);
			answerLine = readLine(fromServer);
		}


		//Structure to read descriptor.txt


		BufferedReader in = new BufferedReader (new InputStreamReader(fromServer));
		String content = "";


		while ((content = in.readLine()) != null) {
			String[] tmp = content.split(" ");
			String[] aux;
			if(!content.equals("")) {
				if(content.startsWith("video") ) {
						
					tmp[0].split("/");
					aux = tmp[0].substring(8).split("\\.m4s");
					seg.add(aux[0]);

					segments.put(Integer.parseInt(tmp[1]), seg );

				}	
			}

		}



		System.out.println("\n========================================");
		System.out.println("\nGot an empty line, showing body \n");
		System.out.println("========================================");

		System.out.println(seg.get(5));

		int c ;
		while( (c = fromServer.read() ) > 0 ) {
			System.out.print((char) c);
		}

		System.out.println();
		in.close();
		sock.close();







	}




	//supostamente pede segmento a segmento vê o que achas da ideia
	private static void processHTTPrequest(InputStream in, OutputStream out) {
		try {

			String request = readLine(in);
			System.out.println( "received: "+request );
			String[] requestParts = parseHttpRequest(request);
			// ignora-se o resto da mensagem HTTP
			while ( ! readLine( in ).equals("") );
			// tratamento do pedido
			if( requestParts[0].equalsIgnoreCase("GET") ) {
				sendSegment(requestParts[1] , out);
			}

			else { // ignore other requests
				dumpStream (notSupportedPageStream(),out);
			}

		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}

	//Enviar o segmento em https
	private static void sendSegment(String segment, OutputStream out) throws IOException{
		boolean sended = false;
		int i = 0;

		while(i < seg.size()) {
			if(seg.get(i).equals(segment)) {
				//falta escolher a avgband
				System.out.println(segment.length());
				StringBuilder reply = new StringBuilder("HTTP/1.0 200 OK\r\n");
				reply.append("Date: "+new Date().toString()+"\r\n");
				reply.append("Server: The tiny server (v0.9) \r\n" );
				reply.append("Content-Length: "+ segment.length() +"\r\n\r\n");

				out.write(reply.toString().getBytes());

				out.write( segment.getBytes(), 0, segment.length());
				sended = true;
			}
		}
		if(!sended) {
			StringBuilder reply = new StringBuilder("HTTP/1.0 404 NOT FOUND\r\n");
			reply.append("\r\n");
			//reply.append("Date: "+new Date().toString()+"\r\n");
			//reply.append("Server: "+"The tiny server (v0.9)"+"\r\n");
			//reply.append("Content-Length: "+String.valueOf(length)+"\r\n\r\n");
			out.write(reply.toString().getBytes());
		}
	}



	private static String getCommand(Scanner inn) {
		String input; 
		input = inn.next().toUpperCase();
		return input;
	}

	private static void getAvgBand() {
		if(segments.containsKey(1)) {
			//position 0 = coco
			//position 1 = dante
			avgB.add(59314);
			avgB.add(474213);	
			avgBand.put(1, avgB);	
		}
		else if(segments.containsKey(2)) {
			//position 0 = coco
			//position 1 = dante
			avgB.add(983096);
			avgB.add(1110624);	
			avgBand.put(2, avgB);
		}
		else if(segments.containsKey(3)) {
			//position 0 = coco
			//position 1 = dante
			avgB.add(1363130);
			avgB.add(1684559);	
			avgBand.put(3, avgB);
		}
		else if(segments.containsKey(4)) {
			//position 0 = coco
			//position 1 = dante
			avgB.add(1763706);
			avgB.add(2302036);	
			avgBand.put(4, avgB);
		} else {
			//position 0 = coco
			//position 1 = dante
			avgB.add(2125252);

			avgBand.put(5, avgB);
		}

	}



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


}




