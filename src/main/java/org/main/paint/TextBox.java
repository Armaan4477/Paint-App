package org.main.paint;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

public class TextBox {
    private String text;
    private double x;
    private double y;
    private Color color;
    private String fontFamily;
    private double fontSize;
    private boolean isBold;
    private boolean isItalic;
    private boolean isEditing;

    public TextBox(String text, double x, double y, Color color, 
                String fontFamily, double fontSize, boolean isBold, boolean isItalic) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
        this.fontFamily = fontFamily;
        this.fontSize = fontSize;
        this.isBold = isBold;
        this.isItalic = isItalic;
        this.isEditing = true; // New text boxes start in editing mode
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Font getFont() {
        FontWeight weight = isBold ? FontWeight.BOLD : FontWeight.NORMAL;
        FontPosture posture = isItalic ? FontPosture.ITALIC : FontPosture.REGULAR;
        return Font.font(fontFamily, weight, posture, fontSize);
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public double getFontSize() {
        return fontSize;
    }

    public void setFontSize(double fontSize) {
        this.fontSize = fontSize;
    }

    public boolean isBold() {
        return isBold;
    }

    public void setBold(boolean bold) {
        this.isBold = bold;
    }

    public boolean isItalic() {
        return isItalic;
    }

    public void setItalic(boolean italic) {
        this.isItalic = italic;
    }

    public boolean isEditing() {
        return isEditing;
    }

    public void setEditing(boolean editing) {
        this.isEditing = editing;
    }
    
    // Calculate rough width of the text based on font
    public double getWidth() {
        return text.length() * fontSize * 0.6;  // Approximate width
    }
    
    // Check if a point is inside this text box (for selection)
    public boolean contains(double testX, double testY) {
        double width = getWidth();
        double height = fontSize;
        
        // Get the top-left corner of the text for hit testing
        double textTopY = y - fontSize * 0.85; // More precise top Y calculation
        
        return testX >= x && testX <= (x + width) &&
               testY >= textTopY && testY <= (textTopY + height);
    }
    
    // Get the baseline Y coordinate for text drawing (where text actually gets drawn)
    public double getBaselineY() {
        return y;
    }
    
    // Get the top Y coordinate for the text box (for selection rectangle)
    public double getTopY() {
        // More precise calculation of where text visually appears
        return y - fontSize * 0.85; 
    }
}
