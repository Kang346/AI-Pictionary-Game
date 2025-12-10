import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server main program
 */
public class ServerMain {
    private static final int PORT = 8888;
    private ExecutorService threadPool;
    private GeminiAPI geminiAPI;
    private DatabaseManager databaseManager;
    
    public ServerMain() {
        threadPool = Executors.newCachedThreadPool();
        geminiAPI = new GeminiAPI();
        databaseManager = new DatabaseManager();
    }
    
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("AI-Pictionary Server started, listening on port: " + PORT);
            System.out.println("Waiting for client connections...");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());
                
                // Create independent handler thread for each client
                threadPool.submit(new ClientHandler(clientSocket, geminiAPI, databaseManager));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        ServerMain server = new ServerMain();
        server.start();
    }
}

