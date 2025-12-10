import java.sql.*;

/**
 * Database manager for SQLite database
 * Stores user statistics: username, total_games, total_score
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:game_database.db";
    private Connection connection;
    
    public DatabaseManager() {
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            createTables();
            System.out.println("Database initialized successfully");
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createTables() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS users (" +
                "username TEXT PRIMARY KEY, " +
                "total_games INTEGER DEFAULT 0, " +
                "total_score INTEGER DEFAULT 0" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("Database table created/verified");
        } catch (SQLException e) {
            System.err.println("Failed to create table: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Update user score after a game round
     * @param username The username
     * @param correct Whether the user guessed correctly
     */
    public void updateUserScore(String username, boolean correct) {
        try {
            // Insert user if not exists
            String insertSQL = "INSERT OR IGNORE INTO users (username, total_games, total_score) VALUES (?, 0, 0)";
            try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
                pstmt.setString(1, username);
                pstmt.executeUpdate();
            }
            
            // Update games count and score
            String updateSQL = "UPDATE users SET total_games = total_games + 1, " +
                    "total_score = total_score + ? WHERE username = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {
                pstmt.setInt(1, correct ? 1 : 0);
                pstmt.setString(2, username);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Failed to update user score: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get user's total score
     */
    public int getUserScore(String username) {
        String sql = "SELECT total_score FROM users WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("total_score");
            }
        } catch (SQLException e) {
            System.err.println("Failed to get user score: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }
    
    /**
     * Get user's total games
     */
    public int getUserGames(String username) {
        String sql = "SELECT total_games FROM users WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("total_games");
            }
        } catch (SQLException e) {
            System.err.println("Failed to get user games: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }
    
    /**
     * Get user statistics as a formatted string
     */
    public String getUserStats(String username) {
        int games = getUserGames(username);
        int score = getUserScore(username);
        return "Games: " + games + " | Score: " + score;
    }
    
    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

