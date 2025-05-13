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
}
