import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Main {
	public static void main(String[] args){
		ServerSocket server;
		ExecutorService pool = Executors.newFixedThreadPool(10);
		try {
			server = new ServerSocket(3000);
			System.out.println("Server up and running on " + System.getProperty("user.dir"));
			while(true) {
				SimpleHTTP answerer = new SimpleHTTP(server.accept());
				System.out.println("new connection");
				pool.execute(answerer);
//				Thread thread = new Thread(answerer);
//				thread.start();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Main cycle error: " + e.getMessage());
		}
		
	}
}
