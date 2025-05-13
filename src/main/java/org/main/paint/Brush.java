package org.main.paint;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public abstract class Brush {
    protected double size;
    protected Color color;
    
    public Brush(double size, Color color) {
        this.size = size;
        this.color = color;
    }
    
    public abstract void draw(GraphicsContext gc, double x, double y);
    
    public void setSize(double size) {
        this.size = size;
    }
    
    public void setColor(Color color) {
        this.color = color;
    }
    
    public double getSize() {
        return size;
    }
    
    public Color getColor() {
        return color;
    }
    
    public static class CircleBrush extends Brush {
        public CircleBrush(double size, Color color) {
            super(size, color);
        }
        
        @Override
        public void draw(GraphicsContext gc, double x, double y) {
            gc.setFill(color);
            gc.fillOval(x - size/2, y - size/2, size, size);
        }
    }
    
    public static class SquareBrush extends Brush {
        public SquareBrush(double size, Color color) {
            super(size, color);
        }
        
        @Override
        public void draw(GraphicsContext gc, double x, double y) {
            gc.setFill(color);
            gc.fillRect(x - size/2, y - size/2, size, size);
        }
    }
    
    public static class PencilBrush extends Brush {
        private double lastX = -1;
        private double lastY = -1;
        
        public PencilBrush(double size, Color color) {
            super(size, color);
        }
        
        @Override
        public void draw(GraphicsContext gc, double x, double y) {
            gc.setStroke(color);
            gc.setLineWidth(size);
            
            if (lastX != -1 && lastY != -1) {
                gc.strokeLine(lastX, lastY, x, y);
            }
            
            lastX = x;
            lastY = y;
        }
        
        public void resetLastPosition() {
            lastX = -1;
            lastY = -1;
        }
    }
    
    public static class SprayBrush extends Brush {
        public SprayBrush(double size, Color color) {
            super(size, color);
        }
        
        @Override
        public void draw(GraphicsContext gc, double x, double y) {
            gc.setFill(color);
            
            int dotCount = (int)(size * size / 10);
            double radius = size / 2;
            
            for (int i = 0; i < dotCount; i++) {
                double offsetX = (Math.random() - 0.5) * size;
                double offsetY = (Math.random() - 0.5) * size;
                
                // Check if point is within the circle
                if (offsetX * offsetX + offsetY * offsetY <= radius * radius) {
                    gc.fillOval(x + offsetX, y + offsetY, 1, 1);
                }
            }
        }
    }

    // New: LineBrush (draws a straight line segment centered at (x, y))
    public static class LineBrush extends Brush {
        public LineBrush(double size, Color color) {
            super(size, color);
        }

        @Override
        public void draw(GraphicsContext gc, double x, double y) {
            gc.setStroke(color);
            gc.setLineWidth(size / 4);
            gc.strokeLine(x - size / 2, y, x + size / 2, y);
        }
    }

    // New: TriangleBrush (draws an equilateral triangle centered at (x, y))
    public static class TriangleBrush extends Brush {
        public TriangleBrush(double size, Color color) {
            super(size, color);
        }

        @Override
        public void draw(GraphicsContext gc, double x, double y) {
            double height = Math.sqrt(3) / 2 * size;
            double[] xs = {x, x - size / 2, x + size / 2};
            double[] ys = {y - height / 3, y + height * 2 / 3, y + height * 2 / 3};
            gc.setFill(color);
            gc.fillPolygon(xs, ys, 3);
        }
    }

    // New: StarBrush (draws a 5-pointed star centered at (x, y))
    public static class StarBrush extends Brush {
        public StarBrush(double size, Color color) {
            super(size, color);
        }

        @Override
        public void draw(GraphicsContext gc, double x, double y) {
            double rOuter = size / 2;
            double rInner = rOuter * 0.5;
            double[] xs = new double[10];
            double[] ys = new double[10];
            for (int i = 0; i < 10; i++) {
                double angle = Math.PI / 2 + i * Math.PI / 5;
                double r = (i % 2 == 0) ? rOuter : rInner;
                xs[i] = x + r * Math.cos(angle);
                ys[i] = y - r * Math.sin(angle);
            }
            gc.setFill(color);
            gc.fillPolygon(xs, ys, 10);
        }
    }
}
