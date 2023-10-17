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

    public PaintApp() {
        super("Paint App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);

        canvas = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (image != null) {
                    g.drawImage(image, 0, 0, null);
                }
            }
        };
        canvas.setBackground(Color.WHITE);
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastX = e.getX();
                lastY = e.getY();
            }
        });
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                graphics.setColor(brushColor);
                graphics.setStroke(new BasicStroke(brushThickness));
                graphics.drawLine(lastX, lastY, e.getX(), e.getY());
                lastX = e.getX();
                lastY = e.getY();
                canvas.repaint();
            }
        });

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

        add(canvas, BorderLayout.CENTER);
        add(controls, BorderLayout.SOUTH);
        setVisible(true);

        image = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
        graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        saveImage();
    }

    private void saveImage() {
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics2D copyGraphics = copy.createGraphics();
        copyGraphics.drawImage(image, 0, 0, null);
        undoStack.push(copy);
        redoStack.clear();
    }

    private class ThicknessChangeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            brushThickness = ((JSlider) e.getSource()).getValue();
        }
    }

    public static void main(String[] args) {
        new PaintApp();
    }
}