import java.io.*;
import java.net.Socket;
import java.util.Random;

/**
 * Thread to handle a single client connection
 */
public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;
    private GeminiAPI geminiAPI;
    private DatabaseManager databaseManager;
    private Random random;
    private String currentObject; // Store current object name
    private boolean gameWon = false; // Track if current game was won (guessed correctly)
    
    // Object list - only store object names, "Draw a/an" will be added automatically
    private static final String[] OBJECTS = {
        // Animals
        "cat", "dog", "bird", "fish", "rabbit", "mouse", "elephant", "lion", "tiger", "bear",
        "horse", "cow", "pig", "sheep", "duck", "chicken", "owl", "eagle", "butterfly", "bee",
        "spider", "snake", "turtle", "frog", "whale", "dolphin", "shark", "octopus", "crab", "lobster",
        
        // Vehicles
        "car", "bicycle", "motorcycle", "bus", "truck", "train", "airplane", "helicopter", "boat", "ship",
        "submarine", "rocket", "tractor", "scooter", "skateboard",
        
        // Buildings & Structures
        "house", "building", "castle", "bridge", "tower", "church", "school", "hospital", "library", "museum",
        
        // Nature
        "tree", "flower", "sun", "star", "moon", "cloud", "rainbow", "mountain", "river", "ocean",
        "lake", "forest", "grass", "leaf", "rock", "cactus", "volcano", "island",
        
        // Food & Drinks
        "apple", "banana", "orange", "strawberry", "grape", "watermelon", "pizza", "hamburger", "cake", "cookie",
        "ice cream", "coffee", "tea", "cup", "bottle", "bowl", "plate", "fork", "knife", "spoon",
        
        // Household Items
        "chair", "table", "bed", "lamp", "clock", "phone", "computer", "television", "refrigerator", "microwave",
        "book", "pen", "pencil", "eraser", "notebook", "backpack", "umbrella", "key", "lock",
        
        // Clothing
        "hat", "shirt", "dress", "shoe", "sock", "glove", "scarf", "jacket", "pants", "skirt",
        
        // Sports & Games
        "ball", "basketball", "football", "soccer ball", "tennis racket", "baseball bat", "frisbee", "kite", "dice", "chess piece",
        
        // Musical Instruments
        "guitar", "piano", "violin", "drum", "trumpet", "flute", "saxophone",
        
        // Tools
        "hammer", "saw", "screwdriver", "wrench", "pliers", "drill", "ladder", "toolbox",
        
        // Other Common Objects
        "camera", "glasses", "watch", "ring", "necklace", "wallet", "purse", "mirror", "brush", "comb",
        "toothbrush", "soap", "towel", "pillow", "blanket", "toy", "doll", "teddy bear", "robot", "alien",
        "ghost", "witch", "wizard", "knight", "princess", "crown", "sword", "shield", "arrow", "bow"
    };
    
    /**
     * Get the list of all possible objects (for Gemini API)
     */
    public static String[] getPossibleObjects() {
        return OBJECTS;
    }
    
    /**
     * Generate full prompt with "Draw a" or "Draw an" prefix
     */
    private static String generatePrompt(String object) {
        // Check if object starts with a vowel sound (a, e, i, o, u)
        String lowerObject = object.toLowerCase().trim();
        if (lowerObject.startsWith("a") || lowerObject.startsWith("e") || 
            lowerObject.startsWith("i") || lowerObject.startsWith("o") || 
            lowerObject.startsWith("u")) {
            return "Draw an " + object;
        } else {
            return "Draw a " + object;
        }
    }
    
    public ClientHandler(Socket socket, GeminiAPI geminiAPI, DatabaseManager databaseManager) {
        this.socket = socket;
        this.geminiAPI = geminiAPI;
        this.databaseManager = databaseManager;
        this.random = new Random();
    }
    
    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            
            // Receive username
            Object obj = in.readObject();
            if (obj instanceof String) {
                username = (String) obj;
                System.out.println("Client connected: " + username);
                
                // Send initial statistics
                String stats = databaseManager.getUserStats(username);
                sendMessage("STATS:" + stats);
            } else {
                username = "Unknown";
            }
            
            // Send first prompt
            currentObject = OBJECTS[random.nextInt(OBJECTS.length)];
            gameWon = false; // Reset game win status for new game
            String prompt = generatePrompt(currentObject);
            sendMessage("PROMPT:" + prompt);
            System.out.println("Sent prompt to " + username + ": " + prompt);
            
            // Handle client messages
            while (true) {
                obj = in.readObject();
                
                if (obj instanceof String) {
                    String message = (String) obj;
                    
                    if (message.equals("NEWGAME")) {
                        // Client requested a new game
                        // Start new game (score will be updated when GAMEEND is received)
                        currentObject = OBJECTS[random.nextInt(OBJECTS.length)];
                        gameWon = false; // Reset for new game
                        prompt = generatePrompt(currentObject);
                        sendMessage("PROMPT:" + prompt);
                        
                        System.out.println("Sent new prompt to " + username + ": " + prompt);
                    } else if (message.startsWith("GAMEEND:")) {
                        // Game ended - update database with final score
                        String scoreStr = message.substring(8);
                        boolean won = "1".equals(scoreStr);
                        databaseManager.updateUserScore(username, won);
                        
                        // Send updated statistics
                        String stats = databaseManager.getUserStats(username);
                        sendMessage("STATS:" + stats);
                        
                        System.out.println("Game ended for " + username + ": " + (won ? "Won (1 point)" : "Lost (0 points)"));
                    } else if (message.startsWith("DRAWING:")) {
                        // Received drawing data
                        String imageBase64 = message.substring(8);
                        prompt = generatePrompt(currentObject);
                        handleDrawing(imageBase64, prompt);
                    }
                }
            }
            
        } catch (EOFException e) {
            System.out.println("Client " + username + " disconnected");
        } catch (Exception e) {
            System.err.println("Error handling client " + username + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void handleDrawing(String imageBase64, String prompt) {
        // Handle AI judgment in background thread to avoid blocking
        new Thread(() -> {
            try {
                // Call Gemini API
                String aiResponse = geminiAPI.analyzeDrawing(imageBase64, prompt);
                
                // Parse AI response to check if correct
                String identifiedObject = "unknown";
                int objectIdx = aiResponse.indexOf("\"object\"");
                if (objectIdx != -1) {
                    int objectValStart = aiResponse.indexOf("\"", objectIdx + 8) + 1;
                    int objectValEnd = aiResponse.indexOf("\"", objectValStart);
                    if (objectValEnd != -1) {
                        identifiedObject = aiResponse.substring(objectValStart, objectValEnd).toLowerCase().trim();
                    }
                }
                
                // Check if correct
                boolean isCorrect = identifiedObject.equals(currentObject.toLowerCase());
                
                // Mark game as won if correct (database will be updated when game ends)
                if (isCorrect) {
                    gameWon = true;
                }
                
                // Send result to client (include win status)
                sendMessage("RESULT:" + aiResponse + "|WON:" + gameWon);
                
            } catch (Exception e) {
                sendMessage("RESULT:{\"object\":\"unknown\",\"comment\":\"Error occurred. Please try again.\"}");
            }
        }).start();
    }
    
    private void sendMessage(String message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            System.err.println("Failed to send message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

