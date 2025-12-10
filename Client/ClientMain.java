import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

/**
 * Client main program
 */
public class ClientMain extends JFrame {
    private DrawingCanvas canvas;
    private JTextField usernameField;
    private JButton connectButton;
    private JButton submitButton;
    private JButton clearButton;
    private JButton newGameButton;
    private JTextArea statusArea;
    private JLabel promptLabel;
    private JLabel timerLabel;
    private JLabel roundLabel;
    private JLabel statsLabel; // User statistics label
    
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean connected = false;
    private String currentPrompt = "";
    private int currentRound = 0;
    private Timer gameTimer;
    private int remainingSeconds = 0;
    private boolean isFirstRound = true;
    private boolean currentGameWon = false; // Track if current game was won
    
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    
    public ClientMain() {
        initializeGUI();
    }
    
    private void initializeGUI() {
        setTitle("AI-Pictionary - Draw & Guess Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        
        // Color scheme
        Color bgColor = new Color(245, 245, 250);
        Color headerColor = new Color(50, 100, 150); // Darker blue for better contrast
        Color accentColor = new Color(100, 149, 237);
        
        // Set background
        getContentPane().setBackground(bgColor);
        
        // Top panel - connection and prompt with better styling
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBackground(bgColor);
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        
        // Connection panel with better styling
        JPanel connectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        connectPanel.setBackground(bgColor);
        connectPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(accentColor, 2, true),
            "Connection", 0, 0, new Font("Segoe UI", Font.BOLD, 13), accentColor));
        
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        connectPanel.add(usernameLabel);
        
        usernameField = new JTextField(15);
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        connectPanel.add(usernameField);
        
        connectButton = createStyledButton("Connect to Server", headerColor, new Color(255, 255, 255));
        connectButton.addActionListener(e -> connectToServer());
        connectPanel.add(connectButton);
        
        // Game info panel with better styling
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        infoPanel.setBackground(bgColor);
        infoPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(accentColor, 2, true),
            "Game Info", 0, 0, new Font("Segoe UI", Font.BOLD, 13), accentColor));
        
        promptLabel = createInfoLabel("Prompt: Waiting for connection...", new Font("Segoe UI", Font.BOLD, 13));
        roundLabel = createInfoLabel("Round: -", new Font("Segoe UI", Font.PLAIN, 13));
        timerLabel = createInfoLabel("Time: --:--", new Font("Segoe UI", Font.BOLD, 14));
        timerLabel.setForeground(new Color(220, 20, 60));
        statsLabel = createInfoLabel("Stats: Games: 0 | Score: 0", new Font("Segoe UI", Font.PLAIN, 12));
        statsLabel.setForeground(new Color(0, 100, 0));
        
        infoPanel.add(promptLabel);
        infoPanel.add(Box.createHorizontalStrut(20));
        infoPanel.add(roundLabel);
        infoPanel.add(Box.createHorizontalStrut(20));
        infoPanel.add(timerLabel);
        infoPanel.add(Box.createHorizontalStrut(20));
        infoPanel.add(statsLabel);
        
        topPanel.add(connectPanel, BorderLayout.NORTH);
        topPanel.add(infoPanel, BorderLayout.SOUTH);
        
        // Center - drawing canvas with better styling
        canvas = new DrawingCanvas(800, 600);
        JScrollPane canvasScroll = new JScrollPane(canvas);
        canvasScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(accentColor, 2, true),
            "Drawing Canvas", 0, 0, new Font("Segoe UI", Font.BOLD, 13), accentColor));
        canvasScroll.setBackground(bgColor);
        
        // Right side - control panel with better styling
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBackground(bgColor);
        controlPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(accentColor, 2, true),
            "Drawing Tools", 0, 0, new Font("Segoe UI", Font.BOLD, 13), accentColor));
        controlPanel.setPreferredSize(new Dimension(200, 0));
        
        // Add spacing
        controlPanel.add(Box.createVerticalStrut(10));
        
        // Brush size with better styling
        JPanel brushPanel = new JPanel(new BorderLayout(5, 5));
        brushPanel.setBackground(bgColor);
        JLabel brushLabel = new JLabel("Brush Size:");
        brushLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        brushPanel.add(brushLabel, BorderLayout.NORTH);
        JSlider brushSlider = new JSlider(1, 20, 5);
        brushSlider.setMajorTickSpacing(5);
        brushSlider.setPaintTicks(true);
        brushSlider.setPaintLabels(true);
        brushSlider.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        brushSlider.addChangeListener(e -> canvas.setBrushSize(brushSlider.getValue()));
        brushPanel.add(brushSlider, BorderLayout.CENTER);
        controlPanel.add(brushPanel);
        controlPanel.add(Box.createVerticalStrut(15));
        
        // Color selection with better styling
        JPanel colorPanel = new JPanel(new BorderLayout(5, 5));
        colorPanel.setBackground(bgColor);
        JLabel colorLabel = new JLabel("Color:");
        colorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        colorPanel.add(colorLabel, BorderLayout.NORTH);
        
        JPanel colorButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        colorButtonsPanel.setBackground(bgColor);
        
        JButton blackBtn = createColorButton("Black", new Color(200, 200, 200), Color.BLACK);
        blackBtn.addActionListener(e -> canvas.setColor(Color.BLACK));
        JButton redBtn = createColorButton("Red", new Color(255, 200, 200), Color.BLACK);
        redBtn.addActionListener(e -> canvas.setColor(Color.RED));
        JButton blueBtn = createColorButton("Blue", new Color(200, 200, 255), Color.BLACK);
        blueBtn.addActionListener(e -> canvas.setColor(Color.BLUE));
        
        colorButtonsPanel.add(blackBtn);
        colorButtonsPanel.add(redBtn);
        colorButtonsPanel.add(blueBtn);
        colorPanel.add(colorButtonsPanel, BorderLayout.CENTER);
        controlPanel.add(colorPanel);
        controlPanel.add(Box.createVerticalStrut(15));
        
        // Action buttons with better styling
        clearButton = createStyledButton("Clear Canvas", new Color(180, 0, 40), Color.WHITE); // Darker red
        clearButton.addActionListener(e -> canvas.clear());
        controlPanel.add(clearButton);
        controlPanel.add(Box.createVerticalStrut(10));
        
        submitButton = createStyledButton("Submit Drawing", new Color(34, 139, 34), Color.WHITE);
        submitButton.setEnabled(false);
        submitButton.addActionListener(e -> submitDrawing());
        controlPanel.add(submitButton);
        controlPanel.add(Box.createVerticalStrut(10));
        
        newGameButton = createStyledButton("New Game", new Color(255, 140, 0), Color.WHITE);
        newGameButton.setEnabled(false);
        newGameButton.addActionListener(e -> requestNewGame());
        controlPanel.add(newGameButton);
        
        // Bottom - status display with better styling
        statusArea = new JTextArea(6, 40);
        statusArea.setEditable(false);
        statusArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        statusArea.setBackground(new Color(250, 250, 255));
        statusArea.setForeground(new Color(50, 50, 50));
        statusArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        
        JScrollPane statusScroll = new JScrollPane(statusArea);
        statusScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(accentColor, 2, true),
            "Game Status", 0, 0, new Font("Segoe UI", Font.BOLD, 13), accentColor));
        statusScroll.setBackground(bgColor);
        
        // Layout
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBackground(bgColor);
        centerPanel.add(canvasScroll, BorderLayout.CENTER);
        centerPanel.add(controlPanel, BorderLayout.EAST);
        
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(statusScroll, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(null);
        setSize(1100, 850);
    }
    
    private JButton createStyledButton(String text, Color bgColor, Color textColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(bgColor);
        button.setForeground(new Color(0, 0, 0)); // Use black text for better visibility
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect - make background slightly brighter but keep text black
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor.brighter());
                    button.setForeground(new Color(0, 0, 0));
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor);
                    button.setForeground(new Color(0, 0, 0));
                }
            }
        });
        
        return button;
    }
    
    private JButton createColorButton(String text, Color bgColor, Color textColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(bgColor);
        button.setForeground(new Color(0, 0, 0)); // Use black text for better visibility
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 60), 2),
            BorderFactory.createEmptyBorder(10, 18, 10, 18)));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0, 0, 0), 3),
                    BorderFactory.createEmptyBorder(9, 17, 9, 17)));
                button.setForeground(new Color(0, 0, 0));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(60, 60, 60), 2),
                    BorderFactory.createEmptyBorder(10, 18, 10, 18)));
                button.setForeground(new Color(0, 0, 0));
            }
        });
        
        return button;
    }
    
    private JLabel createInfoLabel(String text, Font font) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(new Color(50, 50, 50));
        return label;
    }
    
    private void connectToServer() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            showMessage("Please enter a username!");
            return;
        }
        
        if (connected) {
            showMessage("Already connected to server!");
            return;
        }
        
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            
            // Send username
            out.writeObject(username);
            out.flush();
            
            connected = true;
            connectButton.setEnabled(false);
            usernameField.setEnabled(false);
            submitButton.setEnabled(true);
            newGameButton.setEnabled(true);
            
            showMessage("Successfully connected to server!");
            
            // Start thread to receive messages
            new Thread(this::receiveMessages).start();
            
        } catch (Exception e) {
            showMessage("Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void receiveMessages() {
        try {
            while (connected) {
                Object obj = in.readObject();
                
                if (obj instanceof String) {
                    String message = (String) obj;
                    SwingUtilities.invokeLater(() -> handleServerMessage(message));
                }
            }
        } catch (Exception e) {
            if (connected) {
                SwingUtilities.invokeLater(() -> {
                    showMessage("Disconnected from server: " + e.getMessage());
                    connected = false;
                    connectButton.setEnabled(true);
                    usernameField.setEnabled(true);
                    submitButton.setEnabled(false);
                    newGameButton.setEnabled(false);
                });
            }
        }
    }
    
    private void handleServerMessage(String message) {
        if (message.startsWith("PROMPT:")) {
            // Received new prompt
            // End previous game if it wasn't won (user clicked New Game before winning)
            if (currentRound > 0 && !currentGameWon) {
                endGame(false);
            }
            
            currentPrompt = message.substring(7);
            promptLabel.setText("Prompt: " + currentPrompt);
            currentRound = 0;
            isFirstRound = true;
            currentGameWon = false; // Reset for new game
            canvas.clear();
            startNewRound();
            showMessage("New game started! Prompt: " + currentPrompt);
        } else if (message.startsWith("RESULT:")) {
            // Received AI judgment result (JSON format)
            // Format: "RESULT:{...json...}|WON:true/false"
            int wonIndex = message.indexOf("|WON:");
            String jsonResult;
            if (wonIndex != -1) {
                jsonResult = message.substring(7, wonIndex);
                String wonStr = message.substring(wonIndex + 5);
                currentGameWon = "true".equals(wonStr);
            } else {
                jsonResult = message.substring(7);
            }
            
            boolean wasCorrect = handleAIResult(jsonResult);
            
            // Stop timer
            if (gameTimer != null) {
                gameTimer.stop();
            }
            
            if (wasCorrect) {
                // If correct, pause the game and wait for user to click New Game
                submitButton.setEnabled(false);
                showMessage("Game paused! Click 'New Game' to start a new round.");
            } else {
                // If incorrect, continue to next round normally
                if (currentRound < 4) {
                    currentRound++;
                    isFirstRound = false;
                    startNewRound();
                } else {
                    // Game ended without winning - notify server to record 0 points
                    endGame(false);
                    showMessage("Game over! You have completed all 5 rounds of drawing.");
                    submitButton.setEnabled(false);
                }
            }
        } else if (message.startsWith("STATS:")) {
            // Received updated statistics
            String stats = message.substring(6);
            statsLabel.setText("Stats: " + stats);
        } else {
            showMessage("Server: " + message);
        }
    }
    
    private void startNewRound() {
        if (isFirstRound) {
            remainingSeconds = 60; // First round: 1 minute
        } else {
            remainingSeconds = 15; // Subsequent rounds: 15 seconds each
        }
        
        roundLabel.setText("Round: " + (currentRound + 1) + "/5");
        updateTimer();
        
        // Re-enable submit button
        submitButton.setEnabled(true);
        
        if (gameTimer != null) {
            gameTimer.stop();
        }
        
        gameTimer = new Timer(1000, e -> {
            remainingSeconds--;
            updateTimer();
            if (remainingSeconds <= 0) {
                gameTimer.stop();
                // Auto submit
                submitDrawing();
            }
        });
        gameTimer.start();
    }
    
    private void updateTimer() {
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        timerLabel.setText(String.format("Time: %02d:%02d", minutes, seconds));
    }
    
    private void submitDrawing() {
        if (!connected) {
            showMessage("Not connected to server!");
            return;
        }
        
        if (gameTimer != null) {
            gameTimer.stop();
        }
        
        try {
            String imageBase64 = canvas.getImageAsBase64();
            if (imageBase64 == null) {
                showMessage("Failed to get image data!");
                return;
            }
            
            // Send image data
            out.writeObject("DRAWING:" + imageBase64);
            out.flush();
            
            showMessage("Submitted round " + (currentRound + 1) + " drawing, waiting for AI judgment...");
            submitButton.setEnabled(false);
            
        } catch (Exception e) {
            showMessage("Submission failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Request a new game from the server
     */
    private void requestNewGame() {
        if (!connected) {
            showMessage("Not connected to server!");
            return;
        }
        
        try {
            // Stop current timer if running
            if (gameTimer != null) {
                gameTimer.stop();
            }
            
            // If game was won, notify server to record 1 point
            if (currentGameWon) {
                endGame(true);
            }
            
            // Send new game request
            out.writeObject("NEWGAME");
            out.flush();
            
            showMessage("Requesting new game...");
            
        } catch (Exception e) {
            showMessage("Failed to request new game: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Notify server that game ended and update score
     */
    private void endGame(boolean won) {
        try {
            out.writeObject("GAMEEND:" + (won ? "1" : "0"));
            out.flush();
        } catch (Exception e) {
            System.err.println("Failed to send game end: " + e.getMessage());
        }
    }
    
    /**
     * Handle AI result in JSON format: {"object":"识别出的物体","comment":"..."}
     * @return true if the guess was correct, false otherwise
     */
    private boolean handleAIResult(String jsonResult) {
        try {
            // Parse JSON
            String identifiedObject = "unknown";
            String comment = "No comment available";
            
            // Extract identified object
            int objectIdx = jsonResult.indexOf("\"object\"");
            if (objectIdx != -1) {
                int objectValStart = jsonResult.indexOf("\"", objectIdx + 8) + 1;
                int objectValEnd = jsonResult.indexOf("\"", objectValStart);
                if (objectValEnd != -1) {
                    identifiedObject = jsonResult.substring(objectValStart, objectValEnd).toLowerCase().trim();
                }
            }
            
            // Extract comment
            int commentIdx = jsonResult.indexOf("\"comment\"");
            if (commentIdx != -1) {
                int commentValStart = jsonResult.indexOf("\"", commentIdx + 9) + 1;
                int commentValEnd = jsonResult.indexOf("\"", commentValStart);
                if (commentValEnd != -1) {
                    comment = jsonResult.substring(commentValStart, commentValEnd);
                    // Unescape JSON strings
                    comment = comment.replace("\\\"", "\"")
                                     .replace("\\n", "\n")
                                     .replace("\\\\", "\\");
                }
            }
            
            // Extract target object from prompt (e.g., "Draw a cat" -> "cat")
            String targetObject = currentPrompt.replace("Draw a ", "")
                                               .replace("Draw an ", "")
                                               .toLowerCase()
                                               .trim();
            
            // Compare identified object with target object
            boolean isCorrect = identifiedObject.equals(targetObject);
            
            // Display result
            if (isCorrect) {
                showMessage("[CORRECT] The AI identified it as: " + identifiedObject + " (target: " + targetObject + ")");
                showMessage("Congratulations! You got it right! +1 point!");
            } else {
                showMessage("[INCORRECT] The AI identified it as: " + identifiedObject + " (target: " + targetObject + ")");
            }
            
            // Always show the comment
            showMessage("AI Comment: " + comment);
            
            return isCorrect;
            
        } catch (Exception e) {
            showMessage("Error parsing AI result: " + jsonResult);
            e.printStackTrace();
            return false;
        }
    }
    
    private void showMessage(String message) {
        statusArea.append("[" + java.time.LocalTime.now().toString() + "] " + message + "\n");
        statusArea.setCaretPosition(statusArea.getDocument().getLength());
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                String lookAndFeel = UIManager.getSystemLookAndFeelClassName();
                UIManager.setLookAndFeel(lookAndFeel);
            } catch (Exception e) {
                e.printStackTrace();
            }
            new ClientMain().setVisible(true);
        });
    }
}

