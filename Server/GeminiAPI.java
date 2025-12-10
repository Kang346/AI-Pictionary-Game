import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Google Gemini API integration class
 */
public class GeminiAPI {
    // API key hardcoded here
    private static final String API_KEY = "YOU_GEMINI_API";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;
    
    private HttpClient httpClient;
    
    public GeminiAPI() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    
    /**
     * Analyze drawing and return AI judgment result
     */
    public String analyzeDrawing(String imageBase64, String prompt) throws Exception {
        // Build JSON request body
        String jsonBody = buildRequestJson(imageBase64, prompt);
        
        // Create HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(30))
                .build();
        
        // Send request and get response
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        // Handle different response status codes
        if (response.statusCode() == 429) {
            // Rate limit exceeded
            return "{\"object\":\"unknown\",\"comment\":\"API rate limit exceeded. Please wait a moment and try again.\"}";
        } else if (response.statusCode() != 200) {
            // Other API errors
            return "{\"object\":\"unknown\",\"comment\":\"API error occurred. Please try again.\"}";
        }
        
        // Parse response
        return parseResponse(response.body());
    }
    
    /**
     * Build JSON request body for Gemini API
     */
    private String buildRequestJson(String imageBase64, String prompt) {
        // Get all possible objects from ClientHandler
        String[] possibleObjects = ClientHandler.getPossibleObjects();
        
        // Build the list of possible objects as a comma-separated string
        // Escape each object name properly
        StringBuilder objectsList = new StringBuilder();
        for (int i = 0; i < possibleObjects.length; i++) {
            if (i > 0) {
                objectsList.append(", ");
            }
            // Escape the object name for JSON
            String escapedObject = escapeJson(possibleObjects[i]);
            objectsList.append("\"").append(escapedObject).append("\"");
        }
        
        // AI should not know what the user was asked to draw
        // It should only identify what it sees in the drawing from the given list
        // Gemini API request format
        // text and inline_data must be in separate parts
        String instruction = "Look at this drawing. The user has drawn a simple, common object. " +
                "The object must be one of the following: " + objectsList.toString() + ". " +
                "Identify which object from this list you see in the drawing. " +
                "Respond ONLY with a valid JSON object in this exact format: " +
                "{\"object\": \"the exact name from the list above (must match exactly, lowercase)\", " +
                "\"comment\": \"a brief, humorous comment about the drawing (max 50 words)\"}. " +
                "The object name MUST be one of the items from the list above. " +
                "If the drawing is unclear or doesn't match any item in the list, use \"unknown\" as the object name. " +
                "Make the comment witty and concise.";
        
        String json = "{"
                + "\"contents\":[{"
                + "\"parts\":["
                + "{" + "\"text\":\"" + escapeJson(instruction) + "\"" + "},"
                + "{" + "\"inline_data\":{"
                + "\"mime_type\":\"image/png\","
                + "\"data\":\"" + imageBase64 + "\""
                + "}}"
                + "]"
                + "}]"
                + "}";
        return json;
    }
    
    /**
     * Escape special characters in JSON string
     */
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
    
    /**
     * Parse Gemini API response and extract JSON with guess and comment
     * Returns JSON string with format: {"guess":"yes/no","comment":"..."}
     */
    private String parseResponse(String responseBody) {
        try {
            // Response format: {"candidates":[{"content":{"parts":[{"text":"..."}]}}]}
            // Extract text field
            int textStart = -1;
            
            // Try "text":" pattern
            textStart = responseBody.indexOf("\"text\":\"");
            if (textStart == -1) {
                // Try "text": " pattern (with space)
                textStart = responseBody.indexOf("\"text\": \"");
                if (textStart != -1) {
                    textStart += 9; // Skip "text": "
                }
            } else {
                textStart += 8; // Skip "text":"
            }
            
            if (textStart == -1 || textStart < 8) {
                // Check for error response
                if (responseBody.contains("\"error\"")) {
                    int errorStart = responseBody.indexOf("\"message\":\"");
                    if (errorStart != -1) {
                        errorStart += 10; // Skip "message":" 
                        int errorEnd = responseBody.indexOf("\"", errorStart);
                    if (errorEnd != -1) {
                        return "{\"object\":\"unknown\",\"comment\":\"API Error: " + responseBody.substring(errorStart, errorEnd) + "\"}";
                    }
                }
                return "{\"object\":\"unknown\",\"comment\":\"API Error occurred\"}";
            }
            return "{\"object\":\"unknown\",\"comment\":\"Failed to parse AI response\"}";
            }
            
            // Find the end of the text (handle escaped quotes and newlines)
            int textEnd = textStart;
            boolean escaped = false;
            int braceCount = 0;
            boolean inJson = false;
            
            for (int i = textStart; i < responseBody.length(); i++) {
                char c = responseBody.charAt(i);
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                if (c == '{') {
                    inJson = true;
                    braceCount++;
                }
                if (c == '}') {
                    braceCount--;
                    if (braceCount == 0 && inJson) {
                        textEnd = i + 1;
                        break;
                    }
                }
                if (c == '"' && !inJson) {
                    // If we haven't found JSON yet, look for the end of the string
                    textEnd = i;
                    break;
                }
            }
            
            if (textEnd <= textStart) {
                return "{\"object\":\"unknown\",\"comment\":\"Failed to extract response text\"}";
            }
            
            String text = responseBody.substring(textStart, textEnd);
            // Handle escape characters
            text = text.replace("\\n", "\n")
                      .replace("\\\"", "\"")
                      .replace("\\\\", "\\")
                      .replace("\\r", "\r")
                      .replace("\\t", "\t");
            
            // Try to extract JSON from the text
            // Look for JSON object in the response
            int jsonStart = text.indexOf("{");
            int jsonEnd = text.lastIndexOf("}");
            
            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                String jsonStr = text.substring(jsonStart, jsonEnd + 1);
                // Validate it's a JSON object with object and comment
                if (jsonStr.contains("\"object\"") && jsonStr.contains("\"comment\"")) {
                    return jsonStr;
                }
            }
            
            // If no JSON found, try to parse the text as is
            // Maybe AI returned JSON but it's wrapped in markdown or other text
            // Look for JSON-like structure
            if (text.contains("object") && text.contains("comment")) {
                // Try to extract values
                String object = "unknown";
                String comment = text;
                
                int objectIdx = text.indexOf("\"object\"");
                if (objectIdx != -1) {
                    int objectValStart = text.indexOf("\"", objectIdx + 8) + 1;
                    int objectValEnd = text.indexOf("\"", objectValStart);
                    if (objectValEnd != -1) {
                        object = text.substring(objectValStart, objectValEnd);
                    }
                }
                
                int commentIdx = text.indexOf("\"comment\"");
                if (commentIdx != -1) {
                    int commentValStart = text.indexOf("\"", commentIdx + 9) + 1;
                    int commentValEnd = text.indexOf("\"", commentValStart);
                    if (commentValEnd != -1) {
                        comment = text.substring(commentValStart, commentValEnd);
                    }
                }
                
                return "{\"object\":\"" + object + "\",\"comment\":\"" + escapeJson(comment) + "\"}";
            }
            
            // Fallback: return unknown object
            return "{\"object\":\"unknown\",\"comment\":\"" + escapeJson(text.substring(0, Math.min(100, text.length()))) + "\"}";
            
        } catch (Exception e) {
            return "{\"object\":\"unknown\",\"comment\":\"Parsing error occurred\"}";
        }
    }
}

