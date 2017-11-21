
/**
 * Concurrent Server example
 *
 * RC - 2017/2018 (MIEI - FCT/UNL)
 *
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;



public class ConcurrentEchoServer {
	
	static final int PORT = 8000 ;
	
	/**
	 * MAIN - accept and handle client connections using one thread for each client
	 * 
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		ServerSocket ss = new ServerSocket( PORT );
		
		for (;;) {
			Socket clientS = ss.accept();
			Thread t = new ClientHandler(clientS);
			t.start();  // handle the new client in a new thread
		}
	}
}


class ClientHandler extends Thread {
	Socket localSock;

	/**
	 * initialize the new object to handle one client in a new thread
	 */
	public ClientHandler( Socket s) {
		localSock = s;
	}

	/**
	 * handleClient - handle one client using in and out streams
	 * 
	 * @param in - stream from client
	 * @param out - stream to client
	 */
	private void handleClient(InputStream in, OutputStream out) {
		int n;
		byte buf[] = new byte[1024];
		try {
			while ( (n=in.read(buf))>0 ) {  // works as an EchoServer
				System.out.println("recebi: "+new String(buf, 0, n));
				out.write(buf, 0, n);
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * start method for each new server thread
	 * handle one client 
	 */
	public void run() {
		try {	
			InputStream in = localSock.getInputStream();
			OutputStream out = localSock.getOutputStream();
			handleClient( in, out );
			localSock.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
