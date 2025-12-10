import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom drawing canvas component
 */
public class DrawingCanvas extends JPanel {
    /**
     * Class to store path information including points, color, and brush size
     */
    private static class PathInfo {
        List<Point> points;
        Color color;
        int brushSize;
        
        PathInfo(Color color, int brushSize) {
            this.points = new ArrayList<>();
            this.color = color;
            this.brushSize = brushSize;
        }
    }
    
    private PathInfo currentPath;
    private List<PathInfo> allPaths;
    private int brushSize = 5;
    private Color currentColor = Color.BLACK;
    private BufferedImage canvasImage;
    
    public DrawingCanvas(int width, int height) {
        setPreferredSize(new Dimension(width, height));
        setBackground(new Color(255, 255, 255));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        
        allPaths = new ArrayList<>();
        currentPath = null;
        
        // Create canvas image
        canvasImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = canvasImage.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Create new path with current color and brush size
                currentPath = new PathInfo(currentColor, brushSize);
                currentPath.points.add(e.getPoint());
                allPaths.add(currentPath);
                repaint();
            }
        });
        
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (currentPath != null) {
                    currentPath.points.add(e.getPoint());
                    repaint();
                }
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw to panel - use each path's own color and brush size
        for (PathInfo pathInfo : allPaths) {
            List<Point> path = pathInfo.points;
            if (path.size() > 1) {
                g2d.setColor(pathInfo.color);
                g2d.setStroke(new BasicStroke(pathInfo.brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < path.size() - 1; i++) {
                    Point p1 = path.get(i);
                    Point p2 = path.get(i + 1);
                    g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }
        
        // Also draw to BufferedImage - redraw everything to keep image in sync
        Graphics2D imgG2d = canvasImage.createGraphics();
        imgG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Clear image first
        imgG2d.setColor(Color.WHITE);
        imgG2d.fillRect(0, 0, getWidth(), getHeight());
        // Redraw all paths with their original colors
        for (PathInfo pathInfo : allPaths) {
            List<Point> path = pathInfo.points;
            if (path.size() > 1) {
                imgG2d.setColor(pathInfo.color);
                imgG2d.setStroke(new BasicStroke(pathInfo.brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < path.size() - 1; i++) {
                    Point p1 = path.get(i);
                    Point p2 = path.get(i + 1);
                    imgG2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }
        imgG2d.dispose();
    }
    
    public void clear() {
        allPaths.clear();
        currentPath = null;
        
        // Clear image
        Graphics2D g2d = canvasImage.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.dispose();
        
        repaint();
    }
    
    public void setBrushSize(int size) {
        this.brushSize = size;
    }
    
    public void setColor(Color color) {
        this.currentColor = color;
    }
    
    /**
     * Get the current canvas image (for sending to server)
     */
    public BufferedImage getImage() {
        return canvasImage;
    }
    
    /**
     * Get the image as Base64 encoded string
     */
    public String getImageAsBase64() {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(canvasImage, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();
            return java.util.Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

