import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.Stack;

public class PaintApp extends JFrame {
    private JPanel canvas;
    private int lastX, lastY;
    private Color brushColor = Color.BLACK;
    private int brushThickness = 5;
    private BufferedImage image;
    private Graphics2D graphics;
    private Stack<BufferedImage> undoStack = new Stack<>();
    private Stack<BufferedImage> redoStack = new Stack<>();
    private Rectangle selectionRect;
    private boolean isSelecting = false;
    private JToggleButton selectToggleButton;
    private String currentBrush = "Basic"; // Default brush type

    public PaintApp() {
        super("Paint App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);

        canvas = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (image != null) {
                    g.drawImage(image, 0, 0, null);
                }
                if (selectionRect != null) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setColor(Color.BLACK);
                    g2d.setStroke(new BasicStroke(1));
                    g2d.draw(selectionRect);
                }
            }
        };
        canvas.setBackground(Color.WHITE);
        canvas.addMouseListener(new CustomMouseAdapter());

        canvas.addMouseMotionListener(new CustomMouseAdapter());

        JPanel controls = new JPanel();
        JButton colorButton = new JButton("Choose Color");
        colorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                brushColor = JColorChooser.showDialog(PaintApp.this, "Choose Brush Color", brushColor);
            }
        });
        controls.add(colorButton);

        JSlider thicknessSlider = new JSlider(1, 20, brushThickness);
        thicknessSlider.addChangeListener(new ThicknessChangeListener());
        controls.add(thicknessSlider);

        JButton textButton = new JButton("Add Text");
        textButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = JOptionPane.showInputDialog(PaintApp.this, "Enter text:");
                if (text != null && !text.isEmpty()) {
                    graphics.setColor(brushColor);
                    graphics.setFont(new Font("Arial", Font.PLAIN, brushThickness * 5));
                    FontMetrics fontMetrics = graphics.getFontMetrics();
                    int textWidth = fontMetrics.stringWidth(text);
                    int textHeight = fontMetrics.getHeight();
                    BufferedImage textImage = new BufferedImage(textWidth, textHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D textGraphics = (Graphics2D) textImage.getGraphics();
                    textGraphics.setColor(brushColor);
                    textGraphics.setFont(graphics.getFont());
                    textGraphics.drawString(text, 0, fontMetrics.getAscent());
                    graphics.drawImage(textImage, lastX, lastY, null);
                    canvas.repaint();
                    saveImage();
                }
            }
        });
        controls.add(textButton);

        selectToggleButton = new JToggleButton("Select");
        selectToggleButton.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (selectToggleButton.isSelected()) {
                    canvas.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                } else {
                    canvas.setCursor(Cursor.getDefaultCursor());
                }
            }
        });
        controls.add(selectToggleButton);

        JButton undoButton = new JButton("Undo");
        undoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!undoStack.isEmpty()) {
                    redoStack.push(image);
                    image = undoStack.pop();
                    graphics = image.createGraphics();
                    canvas.repaint();
                }
            }
        });
        controls.add(undoButton);

        JButton redoButton = new JButton("Redo");
        redoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!redoStack.isEmpty()) {
                    undoStack.push(image);
                    image = redoStack.pop();
                    graphics = image.createGraphics();
                    canvas.repaint();
                }
            }
        });
        controls.add(redoButton);

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                canvas.repaint();
                saveImage();
            }
        });
        controls.add(clearButton);

        // Create a popup menu for brush selection
        JPopupMenu brushMenu = new JPopupMenu();
        JMenuItem basicBrushItem = new JMenuItem("Basic Brush");
        basicBrushItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeBrush("Basic");
            }
        });
        brushMenu.add(basicBrushItem);

        JMenuItem squareBrushItem = new JMenuItem("Square Brush");
        squareBrushItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeBrush("Square");
            }
        });
        brushMenu.add(squareBrushItem);

        JMenuItem circleBrushItem = new JMenuItem("Circle Brush");
        circleBrushItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeBrush("Circle");
            }
        });
        brushMenu.add(circleBrushItem);

        JMenuItem sprayBrushItem = new JMenuItem("Spray Brush");
        sprayBrushItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeBrush("Spray");
            }
        });
        brushMenu.add(sprayBrushItem);

        JMenuItem starBrushItem = new JMenuItem("Star Brush");
        starBrushItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeBrush("Star");
            }
        });
        brushMenu.add(starBrushItem);

        JButton brushButton = new JButton("Select Brush");
        brushButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                brushMenu.show(brushButton, 0, brushButton.getHeight());
            }
        });
        controls.add(brushButton);

        add(canvas, BorderLayout.CENTER);
        add(controls, BorderLayout.SOUTH);
        setVisible(true);

        image = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
        graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        saveImage();
    }

    private class ThicknessChangeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            brushThickness = ((JSlider) e.getSource()).getValue();
        }
    }

    private void saveImage() {
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics2D copyGraphics = copy.createGraphics();
        copyGraphics.drawImage(image, 0, 0, null);
        undoStack.push(copy);
        redoStack.clear();
    }

    private class CustomMouseAdapter extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            lastX = e.getX();
            lastY = e.getY();
            if (selectToggleButton.isSelected()) {
                isSelecting = true;
                selectionRect = new Rectangle(lastX, lastY, 0, 0);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (isSelecting) {
                isSelecting = false;
                selectionRect = new Rectangle(
                        Math.min(lastX, e.getX()),
                        Math.min(lastY, e.getY()),
                        Math.abs(e.getX() - lastX),
                        Math.abs(e.getY() - lastY)
                );
                canvas.repaint();
            } else {
                selectionRect = null; // clear selection rectangle
                canvas.repaint(); // repaint canvas to clear selection rectangle
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (isSelecting) {
                int width = e.getX() - lastX;
                int height = e.getY() - lastY;
                selectionRect.setSize(width, height);
                canvas.repaint();
            } else {
                drawWithBrush(e.getX(), e.getY());
                lastX = e.getX();
                lastY = e.getY();
                canvas.repaint();
            }
        }
    }

    private void drawWithBrush(int x, int y) {
        if (currentBrush.equals("Basic")) {
            drawBasicBrush(x, y);
        } else if (currentBrush.equals("Square")) {
            drawSquareBrush(x, y);
        } else if (currentBrush.equals("Circle")) {
            drawCircleBrush(x, y);
        } else if (currentBrush.equals("Spray")) {
            drawSprayBrush(x, y);
        } else if (currentBrush.equals("Star")) {
            drawStarBrush(x, y);
        }
    }

    private void drawBasicBrush(int x, int y) {
        graphics.setColor(brushColor);
        graphics.setStroke(new BasicStroke(brushThickness));
        graphics.drawLine(lastX, lastY, x, y);
    }

    private void drawSquareBrush(int x, int y) {
        graphics.setColor(brushColor);
        graphics.setStroke(new BasicStroke(brushThickness));
        int size = brushThickness * 2;
        graphics.fillRect(x - size / 2, y - size / 2, size, size);
    }

    private void drawCircleBrush(int x, int y) {
        graphics.setColor(brushColor);
        graphics.setStroke(new BasicStroke(brushThickness));
        int size = brushThickness * 2;
        graphics.fillOval(x - size / 2, y - size / 2, size, size);
    }

    private void drawSprayBrush(int x, int y) {
        graphics.setColor(brushColor);
        int numSpray = 20;
        for (int i = 0; i < numSpray; i++) {
            int offsetX = (int) (Math.random() * brushThickness * 8) - brushThickness * 5;
            int offsetY = (int) (Math.random() * brushThickness * 8) - brushThickness * 5;
            graphics.fillRect(x + offsetX, y + offsetY, 1, 1);
        }
    }

    private void drawStarBrush(int x, int y) {
        graphics.setColor(brushColor);
        int size = brushThickness * 2;
        int[] xPoints = {x, x + size, x + size / 2, x, x - size / 2};
        int[] yPoints = {y - size, y - size / 2, y, y + size / 2, y};
        graphics.fillPolygon(xPoints, yPoints, 5);
    }


    private void changeBrush(String newBrush) {
        currentBrush = newBrush;
    }

    public static void main(String[] args) {
        new PaintApp();
    }
}
