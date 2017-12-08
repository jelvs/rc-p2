import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.io.*;

public class FilePlayer extends Thread{

	private static String url;
	private static Map<Integer, ArrayList<Segment>> segments = new HashMap<Integer, ArrayList<Segment>>();
	private static Map<Integer, ArrayList<Integer>> avgBand = new HashMap<Integer, ArrayList<Integer>>();
	private static ConcurrentLinkedDeque<byte[]> segmentsQueue = new ConcurrentLinkedDeque<>();
	private static ArrayList<Integer> avgB = new ArrayList<Integer>(); // average bandWidth
	private static String TXT = "descriptor.txt";
	private static ArrayList<String> allcontent = new ArrayList<String>();
	private static long playoutDelay;
	private static double rTime;
	private static double sTime;
	private static int bestQuality = 1;
	private static double averageBand = 0;

	private static final String MP4 = ".mp4";
	private static final String M4S = ".m4s";

	public static void main(String[] args) throws Exception {

		//structure to browser 

		//proxySocket
		ServerSocket proxySocket = new ServerSocket(1234);
		System.out.println("ProxyServer ready at "+ 1234);
		Socket browserSock= proxySocket.accept();
		InputStream inputFromBrowser = browserSock.getInputStream();
		OutputStream outToBrowser = browserSock.getOutputStream();
		System.out.println("\n========================================\n");
		System.out.println("Connected to browser");

		String requestS = readLine(inputFromBrowser);
		String request = requestS;
		System.out.println( "received: "+requestS );
		while ( !requestS.equals("") ) {
			requestS = readLine(inputFromBrowser);
			System.out.println("Header line:\t" + requestS);

		}

		int quality = 1;
		int index = 0;
		if(request.contains("start")) {

			getMyFilm(request, args);
			readDescription();
			new Thread(() -> {

				getMySegments();

			}).start();
			System.out.println(segmentsQueue.size());
			boolean canI = false;
			while(!canI) {
				if(segmentsQueue.size() >=2) {
					System.out.println(segmentsQueue.size());
					System.out.println(segments.get(quality).get(index).getSeg());
					if(segments.get(quality).get(index).getSeg() != null) {

						byte[] initBuf = segmentsQueue.poll();
						byte[] buffer = segmentsQueue.poll();
						int totallength = initBuf.length + buffer.length; 
						System.out.println(initBuf+ "e" +buffer);
						StringBuilder reply = new StringBuilder("HTTP/1.1 200 OK\r\n");
						reply.append("Date: "+new Date().toString()+"\r\n");
						reply.append("Server: The proxy server bitch (v0.9) \r\n" );
						reply.append("Access-Control-Allow-Origin: * \r\n");
						reply.append("Content-Length: "+ totallength +"\r\n");
						reply.append(allcontent.get(quality) + "\r\n\r\n");
						outToBrowser.write(reply.toString().getBytes());
						outToBrowser.write(initBuf,0,initBuf.length);
						outToBrowser.write(buffer,0, buffer.length);
						index +=2;
						canI = true;
					}
				}
			}
		}
		int indexS = 1;
		while(indexS < (segments.get(quality).size()-1)) {
			System.out.println(indexS);
			requestS = readLine(inputFromBrowser);
			request = requestS;
			System.out.println( "received: "+ requestS );
			while ( !requestS.equals("") ) {
				requestS = readLine(inputFromBrowser);
				System.out.println("Header line:\t" + requestS);
			}
			if(request.contains("next")) {
				byte[] buffer = segmentsQueue.poll(); 
				if(segments.get(quality).get(indexS).getSeg() != null) {

					StringBuilder reply = new StringBuilder("HTTP/1.1 200 OK\r\n");
					reply.append("Date: "+new Date().toString()+"\r\n");
					reply.append("Server: The proxy server bitch (v0.9) \r\n" );
					reply.append("Access-Control-Allow-Origin: * \r\n");
					reply.append("Content-Length: "+ buffer.length +"\r\n");
					reply.append(allcontent.get(quality) + "\r\n\r\n");
					outToBrowser.write(reply.toString().getBytes());
					outToBrowser.write(buffer,0, buffer.length);
					indexS++;
					System.out.println(segments.get(quality).get(indexS).getSeg());
				}
			}
		}
		System.out.println("badjoraz");
		requestS = readLine(inputFromBrowser);
		request = requestS;
		System.out.println( "received: "+ requestS );
		while ( !requestS.equals("") ) {
			requestS = readLine(inputFromBrowser);
			System.out.println("Header line:\t" + requestS);
		}

		byte[] buffer = new byte[0];


		StringBuilder reply = new StringBuilder("HTTP/1.1 200 OK\r\n");
		reply.append("Date: "+new Date().toString()+"\r\n");
		reply.append("Server: The proxy server bitch (v0.9) \r\n" );
		reply.append("Access-Control-Allow-Origin: * \r\n");
		reply.append("Content-Length: "+ buffer.length +"\r\n");
		reply.append(allcontent.get(quality) + "\r\n\r\n");
		outToBrowser.write(reply.toString().getBytes());
		//outToBrowser.write(buffer,0, buffer.length);



		browserSock.close();
		proxySocket.close();
		System.out.println(proxySocket.isClosed());
	}







	public static void getMyFilm(String cmd, String[] args) {
		if(cmd.contains("coco")) {
			url = args.length == 1 ? args[0] : "http://localhost:8080/coco/" ; 

		}
		else {
			url = args.length == 1 ? args[0] : "http://localhost:8080/dante/" ;
		}
		playoutDelay = args.length == 2 ? Integer.parseInt(args[1]) : 10;

	}

	public static void getMySegments() {

		List<Double> averageBands = new ArrayList<>();
		int quality = 1;
		int nextSegment= 0;

		//String ineedTHISTOO = "" + quality + "/";

		try {
			segmentsQueue = new ConcurrentLinkedDeque<>();
			URL urls = new URL(url);
			System.out.println("\n========================================\n");
			for (;;) {
				while(segmentsQueue.size() < 5 && nextSegment < segments.get(quality).size()) {

					InetAddress serverAddr = InetAddress.getByName(urls.getHost());
					int port = urls.getPort();
					if ( port == -1 ) port = 80;
					Socket sock = new Socket( serverAddr, port );
					OutputStream toServer = sock.getOutputStream();
					InputStream fromServer = sock.getInputStream();
					String format = "";

					if(segments.get(quality).get(nextSegment).getSeg().equals("init")) {
						format = MP4;
					}
					else {
						format = M4S;
					}
					bestQuality = getBestQuality(averageBand);
					System.out.println("bestQuality: " + bestQuality);
					//mudar para bestQuality
					String ineedTHISTOO = "" + quality + "/";
					String segment = urls.getPath().concat("video/" + ineedTHISTOO + segments.get(quality).get(nextSegment).getSeg());
					String request = String.format("GET %s HTTP/1.0\r\n" + "User-Agent: X-RC2017\r\n\r\n", segment + format );
					toServer.write(request.getBytes());

					sTime = System.currentTimeMillis();

					System.out.println("\n========================================\n");
					System.out.println("Sent request: "+ request);
					System.out.println("========================================");

					String answerLine = readLine(fromServer);

					System.out.println("Got answer: "+ answerLine +"\n");
					while ( !answerLine.equals("") ) {
						answerLine = readLine(fromServer);
						//System.out.println("Header line:\t" + answerLine);

					}
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					int nRead;
					byte[] buf = new byte[16384];
					while ((nRead = fromServer.read(buf, 0, buf.length)) != -1) {
						buffer.write(buf, 0, nRead);
					}
					buffer.flush();
					rTime = System.currentTimeMillis();
					segmentsQueue.addLast(buffer.toByteArray());
					double dimension = segments.get(bestQuality).get(nextSegment).getDimension();
					averageBand = calcAverageBand(averageBands, rTime, sTime, dimension);
					System.out.println("averageBand: " + averageBand );
					nextSegment++;
					sock.close();

				}
			}


		}catch(IOException e) {
			e.printStackTrace();
		}


	}
	public static void readDescription() throws Exception {
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
		allcontent = new ArrayList<>();
		while ((content = in.readLine()) != null) {
			if(content.startsWith("Content-type:")) {
				allcontent.add(content);
			}
			String [] tmp = content.split("/|\\s|\\.");
			String [] tmp2 = content.split(" ");
			if(!content.equals("")) {
				if(content.startsWith("movie")) {
					int qualityNumber = Integer.parseInt(tmp[1]);
					for(int i=1; i<= qualityNumber; i++) {
						segments.put(i, new ArrayList<Segment>());
						//avgBand.put(i, new ArrayList<Integer>());
					}	
				}
				if(content.startsWith("Average")) {
					avgB.add(Integer.parseInt(tmp2[1]));
				}
				if(content.startsWith("video") ) {
					ArrayList<Segment> l = segments.get(Integer.parseInt(tmp[1]));
					l.add(new Segment(Integer.parseInt(tmp[1]), tmp[2], Integer.parseInt(tmp[4])));
					segments.put(Integer.parseInt(tmp[1]), l);
					//avgBand.put(Integer.parseInt(tmp[1]), avgB);
				}	
			}

		}
		in.close();
		sock.close();
		

		System.out.println("avgBand quality 1 : " + avgB.get(0).intValue());
		System.out.println("avgBand quality 2 : " + avgB.get(1).intValue());
		System.out.println("avgBand quality 3 : " + avgB.get(2).intValue());
		System.out.println("avgBand quality 4 : " + avgB.get(3).intValue());
		System.out.println("avgBand quality 5 : " + avgB.get(4).intValue());
		System.out.println(segments.get(1).size());
	}

	/*
	 * Finds best Quality to download next Segment
	 */
	private static int getBestQuality(double averageBand) {
		int quality = 1;
		int sum = 0;
		
		if(averageBand <= avgB.get(0)) {
			quality = 1;
			sum++;
		}else if(averageBand <= avgB.get(1) && averageBand > avgB.get(0) ) {
			quality = 2;
			sum++;

		}else if(averageBand <= avgB.get(2) && averageBand > avgB.get(1)) {
			quality = 3;
			sum ++;

		}else if(averageBand <= avgB.get(3) && averageBand > avgB.get(2) ) {
			quality = 4;
			sum ++;

		}else if(averageBand <= avgB.get(4) && averageBand > avgB.get(3) && sum > 5) {
			quality = 5;
			sum --;
		}
		return quality;

	}

	
	/*
	 * Calculate avgBand
	 */
	private static double calcAverageBand(List<Double> list, double rTime, double sTime, double dimension) {
		double sum = 0;
		double band = dimension / ((rTime - sTime) / 1000);

		list.add(band);
		//da QUEUE 5 ??
		if(list.size() > 5) {
			list.remove(0);
		}
		for (Double l : list) {
			sum +=l;
		}

		return sum/list.size();


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


}
