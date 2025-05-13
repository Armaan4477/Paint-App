package org.main.paint;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class Controller {
    @FXML private Canvas canvas;
    @FXML private ComboBox<String> brushTypeComboBox;
    @FXML private Slider brushSizeSlider;
    @FXML private HBox colorPalette;
    @FXML private Label coordinatesLabel;
    @FXML private Button undoButton;
    @FXML private Button redoButton;
    
    // New text controls
    @FXML private HBox textControlsBox;
    @FXML private TextField textInput;
    @FXML private ComboBox<String> fontFamilyComboBox;
    @FXML private ComboBox<Integer> fontSizeComboBox;
    @FXML private CheckBox boldCheckBox;
    @FXML private CheckBox italicCheckBox;
    
    private GraphicsContext gc;
    private Color currentColor = Color.BLACK;
    private Brush currentBrush;
    private final List<Color> colors = Arrays.asList(
        Color.BLACK, Color.WHITE, Color.RED, Color.GREEN, Color.BLUE, 
        Color.YELLOW, Color.PURPLE, Color.ORANGE, Color.PINK, Color.BROWN
    );
    
    // For undo/redo functionality
    private Stack<Image> undoStack = new Stack<>();
    private Stack<Image> redoStack = new Stack<>();
    private boolean isDrawing = false;

    @FXML
    public void initialize() {
        // Initialize canvas and graphics context
        gc = canvas.getGraphicsContext2D();
        clearCanvas();
        saveCanvasState(); // Save initial blank canvas state
        
        // Initialize brush types
        brushTypeComboBox.getItems().addAll("Circle", "Square", "Pencil", "Spray", "Text");
        brushTypeComboBox.setValue("Pencil");
        
        // Create color palette
        createColorPalette();
        
        // Initialize text controls
        initializeTextControls();
        
        // Set initial brush
        updateBrush();
        
        // Add listeners
        brushTypeComboBox.setOnAction(e -> {
            updateBrush();
            toggleTextControls();
        });
        brushSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateBrush());
        
        // Add canvas event handlers
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, this::handleMouseMoved);
        
        // Initialize undo/redo buttons as disabled
        updateUndoRedoButtons();
    }

    private void initializeTextControls() {
        // Populate font families
        List<String> fontFamilies = Font.getFamilies();
        fontFamilyComboBox.getItems().addAll(fontFamilies);
        fontFamilyComboBox.setValue("Arial");
        
        // Populate font sizes
        Integer[] fontSizes = {8, 10, 12, 14, 16, 18, 20, 24, 28, 32, 36, 42, 48, 56, 64, 72};
        fontSizeComboBox.getItems().addAll(fontSizes);
        fontSizeComboBox.setValue(20);
        
        // Add listeners for text controls
        textInput.textProperty().addListener((obs, oldVal, newVal) -> updateBrush());
        fontFamilyComboBox.setOnAction(e -> updateBrush());
        fontSizeComboBox.setOnAction(e -> updateBrush());
        boldCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> updateBrush());
        italicCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> updateBrush());
    }
    
    private void toggleTextControls() {
        boolean isTextBrush = "Text".equals(brushTypeComboBox.getValue());
        textControlsBox.setVisible(isTextBrush);
        textControlsBox.setManaged(isTextBrush);
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
            case "Text":
                String text = textInput.getText();
                if (text == null || text.isEmpty()) {
                    text = "Sample Text";
                }
                String fontFamily = fontFamilyComboBox.getValue();
                boolean isBold = boldCheckBox.isSelected();
                boolean isItalic = italicCheckBox.isSelected();
                
                // Use font size from combobox if Text brush is selected
                if (fontSizeComboBox.getValue() != null) {
                    size = fontSizeComboBox.getValue();
                }
                
                currentBrush = new Brush.TextBrush(size, currentColor, text, fontFamily, isBold, isItalic);
                break;
            case "Pencil":
            default:
                currentBrush = new Brush.PencilBrush(size, currentColor);
                break;
        }
    }

    private void handleMousePressed(MouseEvent event) {
        isDrawing = true;
        currentBrush.draw(gc, event.getX(), event.getY());
    }

    private void handleMouseDragged(MouseEvent event) {
        // Only continue drawing for non-text brushes
        if (!(currentBrush instanceof Brush.TextBrush)) {
            currentBrush.draw(gc, event.getX(), event.getY());
        }
        coordinatesLabel.setText(String.format("Coordinates: %.0f, %.0f", event.getX(), event.getY()));
    }
    
    private void handleMouseReleased(MouseEvent event) {
        if (isDrawing) {
            saveCanvasState(); // Save state after drawing
            isDrawing = false;
            redoStack.clear(); // Clear redo stack after action
        }
        
        if (currentBrush instanceof Brush.PencilBrush) {
            ((Brush.PencilBrush) currentBrush).resetLastPosition();
        }
        
        updateUndoRedoButtons();
    }
    
    private void handleMouseMoved(MouseEvent event) {
        coordinatesLabel.setText(String.format("Coordinates: %.0f, %.0f", event.getX(), event.getY()));
    }

    @FXML
    private void handleClearCanvas() {
        saveCanvasState(); // Save state before clearing
        clearCanvas();
        redoStack.clear(); // Clear redo stack after action
        updateUndoRedoButtons();
    }
    
    private void clearCanvas() {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }
    
    // New methods for undo/redo functionality
    private void saveCanvasState() {
        WritableImage snapshot = new WritableImage((int)canvas.getWidth(), (int)canvas.getHeight());
        canvas.snapshot(null, snapshot);
        undoStack.push(snapshot);
        updateUndoRedoButtons();
    }
    
    @FXML
    private void handleUndo() {
        if (undoStack.size() > 1) { // Keep at least the initial state
            redoStack.push(undoStack.pop());
            Image lastState = undoStack.peek();
            gc.drawImage(lastState, 0, 0);
            updateUndoRedoButtons();
        }
    }
    
    @FXML
    private void handleRedo() {
        if (!redoStack.isEmpty()) {
            Image redoState = redoStack.pop();
            undoStack.push(redoState);
            gc.drawImage(redoState, 0, 0);
            updateUndoRedoButtons();
        }
    }
    
    private void updateUndoRedoButtons() {
        undoButton.setDisable(undoStack.size() <= 1); // Disable if only initial state
        redoButton.setDisable(redoStack.isEmpty());
    }
}
