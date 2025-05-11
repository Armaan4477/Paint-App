package org.main.paint;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import java.util.Arrays;
import java.util.List;

public class Controller {
    @FXML private Canvas canvas;
    @FXML private ComboBox<String> brushTypeComboBox;
    @FXML private Slider brushSizeSlider;
    @FXML private HBox colorPalette;
    @FXML private Label coordinatesLabel;
    
    private GraphicsContext gc;
    private Color currentColor = Color.BLACK;
    private Brush currentBrush;
    private final List<Color> colors = Arrays.asList(
        Color.BLACK, Color.WHITE, Color.RED, Color.GREEN, Color.BLUE, 
        Color.YELLOW, Color.PURPLE, Color.ORANGE, Color.PINK, Color.BROWN
    );

    @FXML
    public void initialize() {
        // Initialize canvas and graphics context
        gc = canvas.getGraphicsContext2D();
        clearCanvas();
        
        // Initialize brush types
        brushTypeComboBox.getItems().addAll("Circle", "Square", "Pencil", "Spray");
        brushTypeComboBox.setValue("Pencil");
        
        // Create color palette
        createColorPalette();
        
        // Set initial brush
        updateBrush();
        
        // Add listeners
        brushTypeComboBox.setOnAction(e -> updateBrush());
        brushSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateBrush());
        
        // Add canvas event handlers
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, this::handleMouseMoved);
    }

    private void createColorPalette() {
        for (Color color : colors) {
            Rectangle rect = new Rectangle(20, 20, color);
            rect.getStyleClass().add("color-rect");
            
            if (color == currentColor) {
                rect.getStyleClass().add("color-rect-selected");
            }
            
            rect.setOnMouseClicked(e -> {
                currentColor = color;
                updateBrush();
                
                // Update selection styling
                colorPalette.getChildren().forEach(node -> {
                    if (node instanceof Rectangle) {
                        ((Rectangle) node).getStyleClass().remove("color-rect-selected");
                    }
                });
                rect.getStyleClass().add("color-rect-selected");
            });
            
            colorPalette.getChildren().add(rect);
        }
    }
    
    private void updateBrush() {
        double size = brushSizeSlider.getValue();
        
        switch (brushTypeComboBox.getValue()) {
            case "Circle":
                currentBrush = new Brush.CircleBrush(size, currentColor);
                break;
            case "Square":
                currentBrush = new Brush.SquareBrush(size, currentColor);
                break;
            case "Spray":
                currentBrush = new Brush.SprayBrush(size, currentColor);
                break;
            case "Pencil":
            default:
                currentBrush = new Brush.PencilBrush(size, currentColor);
                break;
        }
    }

    private void handleMousePressed(MouseEvent event) {
        currentBrush.draw(gc, event.getX(), event.getY());
    }

    private void handleMouseDragged(MouseEvent event) {
        currentBrush.draw(gc, event.getX(), event.getY());
        coordinatesLabel.setText(String.format("Coordinates: %.0f, %.0f", event.getX(), event.getY()));
    }
    
    private void handleMouseReleased(MouseEvent event) {
        if (currentBrush instanceof Brush.PencilBrush) {
            ((Brush.PencilBrush) currentBrush).resetLastPosition();
        }
    }
    
    private void handleMouseMoved(MouseEvent event) {
        coordinatesLabel.setText(String.format("Coordinates: %.0f, %.0f", event.getX(), event.getY()));
    }

    @FXML
    private void handleClearCanvas() {
        clearCanvas();
    }
    
    private void clearCanvas() {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }
}
