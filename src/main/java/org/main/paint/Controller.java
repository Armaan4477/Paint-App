package org.main.paint;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Controller {
    @FXML private Canvas canvas;
    @FXML private ComboBox<String> brushTypeComboBox;
    @FXML private Slider brushSizeSlider;
    @FXML private ColorPicker colorPicker; // Add ColorPicker
    @FXML private Label coordinatesLabel;
    @FXML private Button undoButton;
    @FXML private Button redoButton;
    @FXML private StackPane canvasContainer;
    
    // Text controls
    @FXML private HBox textControlsBox;
    @FXML private TextField textInput;
    @FXML private ComboBox<String> fontFamilyComboBox;
    @FXML private ComboBox<Integer> fontSizeComboBox;
    @FXML private CheckBox boldCheckBox;
    @FXML private CheckBox italicCheckBox;
    @FXML private CheckBox textModeCheckBox; // <-- Add this line
    
    private GraphicsContext gc;
    private Color currentColor = Color.BLACK;
    private Brush currentBrush;
    
    // Text mode variables
    private boolean textMode = false;
    private List<TextBox> textBoxes = new ArrayList<>();
    private TextBox activeTextBox = null;
    private TextBox selectedTextBox = null;
    private double dragStartX, dragStartY;
    private boolean isDraggingTextBox = false;
    
    // For undo/redo functionality
    private Stack<Image> undoStack = new Stack<>();
    private Stack<Image> redoStack = new Stack<>();
    private Stack<List<TextBox>> textBoxUndoStack = new Stack<>();
    private Stack<List<TextBox>> textBoxRedoStack = new Stack<>();
    private boolean isDrawing = false;

    @FXML
    public void initialize() {
        // Initialize canvas and graphics context
        gc = canvas.getGraphicsContext2D();
        clearCanvas();
        saveState(); // Save initial blank canvas state
        
        // Initialize brush types (remove "Text")
        brushTypeComboBox.getItems().addAll(
            "Circle", "Square", "Pencil", "Spray", 
            "Line", "Triangle", "Star" // New brushes
        );
        brushTypeComboBox.setValue("Pencil");
        
        // Initialize ColorPicker
        colorPicker.setValue(currentColor);
        colorPicker.setOnAction(e -> {
            currentColor = colorPicker.getValue();
            updateBrush();
            // Update selected text box color if one is selected
            if (selectedTextBox != null) {
                selectedTextBox.setColor(currentColor);
                redrawCanvas();
            }
        });
        
        // Initialize text controls
        initializeTextControls();
        
        // Set initial brush
        updateBrush();
        
        // Add listeners
        brushTypeComboBox.setOnAction(e -> {
            updateBrush();
        });
        brushSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateBrush());
        
        // Text mode toggle
        textModeCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            textMode = newVal;
            updateBrush();
            toggleTextControls();
            
            // Finalize any active text when switching modes
            if (!textMode && activeTextBox != null) {
                finalizeActiveTextBox();
            }
        });
        
        // Add canvas event handlers
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, this::handleMouseMoved);
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleMouseClicked);
        
        // Add keyboard event handler for text editing
        canvas.setOnKeyTyped(this::handleKeyTyped);
        canvas.setFocusTraversable(true);
        
        // Initialize undo/redo buttons as disabled
        updateUndoRedoButtons();
        
        // Add a new delete key handler
        canvas.setOnKeyPressed(this::handleKeyPressed);
    }

    private void handleKeyPressed(KeyEvent event) {
        if (selectedTextBox != null && (event.getCode().toString().equals("DELETE") || 
                                        event.getCode().toString().equals("BACK_SPACE"))) {
            if (selectedTextBox.isEditing()) {
                // If editing, let the handleKeyTyped method handle backspace
                if (event.getCode().toString().equals("BACK_SPACE")) {
                    return;
                }
            }
            // Otherwise, delete the entire text box
            saveState();
            textBoxes.remove(selectedTextBox);
            selectedTextBox = null;
            redrawCanvas();
        }
    }
    
    private void handleKeyTyped(KeyEvent event) {
        if (activeTextBox != null && activeTextBox.isEditing()) {
            String character = event.getCharacter();
            String currentText = activeTextBox.getText();
            
            if (character.equals("\b")) { // Backspace
                if (currentText.length() > 0) {
                    activeTextBox.setText(currentText.substring(0, currentText.length() - 1));
                }
            } else if (character.equals("\r") || character.equals("\n")) { // Enter key
                finalizeActiveTextBox();
                return;
            } else if (!character.equals("\t")) { // Ignore tab key
                activeTextBox.setText(currentText + character);
            }
            
            // Update text input field to match text box content
            if (textInput != null) {
                textInput.setText(activeTextBox.getText());
            }
            
            redrawCanvas();
        }
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
        textInput.textProperty().addListener((obs, oldVal, newVal) -> {
            if (activeTextBox != null) {
                activeTextBox.setText(newVal);
                redrawCanvas();
            }
        });
        
        fontFamilyComboBox.setOnAction(e -> {
            if (selectedTextBox != null) {
                selectedTextBox.setFontFamily(fontFamilyComboBox.getValue());
                redrawCanvas();
            }
        });
        
        fontSizeComboBox.setOnAction(e -> {
            if (selectedTextBox != null) {
                selectedTextBox.setFontSize(fontSizeComboBox.getValue());
                redrawCanvas();
            }
        });
        
        boldCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedTextBox != null) {
                selectedTextBox.setBold(newVal);
                redrawCanvas();
            }
        });
        
        italicCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedTextBox != null) {
                selectedTextBox.setItalic(newVal);
                redrawCanvas();
            }
        });
    }
    
    private void toggleTextControls() {
        textControlsBox.setVisible(textMode);
        textControlsBox.setManaged(textMode);
    }

    private void updateBrush() {
        double size = brushSizeSlider.getValue();
        
        if (textMode) {
            // Don't set a brush in text mode
            currentBrush = null;
        } else {
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
                case "Line":
                    currentBrush = new Brush.LineBrush(size, currentColor);
                    break;
                case "Triangle":
                    currentBrush = new Brush.TriangleBrush(size, currentColor);
                    break;
                case "Star":
                    currentBrush = new Brush.StarBrush(size, currentColor);
                    break;
                case "Pencil":
                default:
                    currentBrush = new Brush.PencilBrush(size, currentColor);
                    break;
            }
        }
    }

    private void handleMouseClicked(MouseEvent event) {
        if (textMode) {
            // Check if clicked on existing text box
            TextBox clickedBox = findTextBoxAt(event.getX(), event.getY());
            
            // If clicked outside any text box but there's an active one, finalize it
            if (clickedBox == null && activeTextBox != null) {
                finalizeActiveTextBox();
                return;
            }
            
            // Double-click to edit existing text box
            if (event.getClickCount() == 2 && clickedBox != null) {
                startEditingTextBox(clickedBox);
                return;
            }
            
            // First click creates new text box if no active one and not clicked on existing
            if (activeTextBox == null && clickedBox == null) {
                saveState();
                // Create text box with y coordinate representing baseline, not top
                createNewTextBox(event.getX(), event.getY());
            }
        }
    }

    private void handleMousePressed(MouseEvent event) {
        if (textMode) {
            // In text mode, check if clicking on existing text
            TextBox clickedBox = findTextBoxAt(event.getX(), event.getY());
            if (clickedBox != null) {
                if (selectedTextBox != clickedBox) {
                    // Select this text box
                    selectTextBox(clickedBox);
                }
                
                // Prepare for potential drag operation
                dragStartX = event.getX();
                dragStartY = event.getY();
                isDraggingTextBox = true;
            } else {
                // Clicking elsewhere deselects current text
                deselectTextBox();
            }
        } else {
            // Normal drawing mode
            isDrawing = true;
            if (currentBrush != null) {
                // Take snapshot before starting new stroke but don't clear the canvas
                if (!isDrawing) { // Only save state when starting a new stroke
                    saveState();
                }
                currentBrush.draw(gc, event.getX(), event.getY());
            }
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        coordinatesLabel.setText(String.format("Coordinates: %.0f, %.0f", event.getX(), event.getY()));
        
        if (textMode) {
            // Move selected text box
            if (isDraggingTextBox && selectedTextBox != null) {
                double deltaX = event.getX() - dragStartX;
                double deltaY = event.getY() - dragStartY;
                selectedTextBox.setX(selectedTextBox.getX() + deltaX);
                selectedTextBox.setY(selectedTextBox.getY() + deltaY);
                dragStartX = event.getX();
                dragStartY = event.getY();
                redrawCanvas();
            }
        } else {
            // Normal drawing - draw on top of saved state
            if (isDrawing && currentBrush != null) {
                currentBrush.draw(gc, event.getX(), event.getY());
            }
        }
    }
    
    private void handleMouseReleased(MouseEvent event) {
        if (textMode) {
            isDraggingTextBox = false;
            if (selectedTextBox != null && !selectedTextBox.isEditing()) {
                saveState();  // Save state after moving text box
            }
        } else if (isDrawing) {
            isDrawing = false;
            
            if (currentBrush instanceof Brush.PencilBrush) {
                ((Brush.PencilBrush) currentBrush).resetLastPosition();
            }
        }
    }
    
    private void handleMouseMoved(MouseEvent event) {
        coordinatesLabel.setText(String.format("Coordinates: %.0f, %.0f", event.getX(), event.getY()));
        
        // Change cursor if hovering over text box in text mode
        if (textMode) {
            TextBox hoveredBox = findTextBoxAt(event.getX(), event.getY());
            if (hoveredBox != null) {
                canvas.setCursor(javafx.scene.Cursor.HAND);
            } else {
                canvas.setCursor(javafx.scene.Cursor.TEXT);
            }
        } else {
            canvas.setCursor(javafx.scene.Cursor.DEFAULT);
        }
    }

    private TextBox findTextBoxAt(double x, double y) {
        // Search in reverse to get top-most text box first (last added)
        for (int i = textBoxes.size() - 1; i >= 0; i--) {
            TextBox box = textBoxes.get(i);
            if (box.contains(x, y)) {
                return box;
            }
        }
        return null;
    }
    
    private void createNewTextBox(double x, double y) {
        // Save state before adding new text box to preserve existing drawings
        saveState();
        
        String initialText = "Enter text...";
        int fontSize = fontSizeComboBox.getValue();
        String fontFamily = fontFamilyComboBox.getValue();
        boolean isBold = boldCheckBox.isSelected();
        boolean isItalic = italicCheckBox.isSelected();
        
        // Adjust Y position to account for text baseline
        // This converts the clicked Y position (which would be the top of where user clicked)
        // to proper baseline Y coordinate (which is what TextBox expects)
        double baselineY = y + fontSize * 0.7; // Approximate baseline offset
        
        TextBox newTextBox = new TextBox(initialText, x, baselineY, currentColor, 
                                        fontFamily, fontSize, isBold, isItalic);
        
        textBoxes.add(newTextBox);
        startEditingTextBox(newTextBox);
        redrawCanvas();
    }
    
    private void startEditingTextBox(TextBox box) {
        // Finalize any currently active text box
        if (activeTextBox != null && activeTextBox != box) {
            finalizeActiveTextBox();
        }
        
        // Start editing this box
        activeTextBox = box;
        selectedTextBox = box;
        box.setEditing(true);
        
        // Update UI controls to match text box properties
        if (textInput != null) {
            textInput.setText(box.getText());
        }
        fontFamilyComboBox.setValue(box.getFontFamily());
        fontSizeComboBox.setValue((int)box.getFontSize());
        boldCheckBox.setSelected(box.isBold());
        italicCheckBox.setSelected(box.isItalic());
        
        // Select all text for immediate editing
        textInput.selectAll();
        textInput.requestFocus();
        
        redrawCanvas();
    }
    
    private void finalizeActiveTextBox() {
        if (activeTextBox != null) {
            if (activeTextBox.getText().trim().isEmpty()) {
                // Remove empty text boxes
                textBoxes.remove(activeTextBox);
            } else {
                activeTextBox.setEditing(false);
            }
            activeTextBox = null;
            saveState();
            redrawCanvas();
        }
    }
    
    private void selectTextBox(TextBox box) {
        deselectTextBox();
        selectedTextBox = box;
        
        // Update UI controls to match text box properties
        fontFamilyComboBox.setValue(box.getFontFamily());
        fontSizeComboBox.setValue((int)box.getFontSize());
        boldCheckBox.setSelected(box.isBold());
        italicCheckBox.setSelected(box.isItalic());
        textInput.setText(box.getText());
        
        redrawCanvas();
    }
    
    private void deselectTextBox() {
        selectedTextBox = null;
        redrawCanvas();
    }

    // Helper method to redraw the entire canvas with all objects
    private void redrawCanvas() {
        // First draw the white background
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Then draw the latest canvas state from undo stack
        if (!undoStack.isEmpty()) {
            gc.drawImage(undoStack.peek(), 0, 0);
        }

        // Finally draw all text boxes
        for (TextBox box : textBoxes) {
            drawTextBox(box);
        }
    }

    private void drawTextBox(TextBox box) {
        gc.setFill(box.getColor());
        gc.setFont(box.getFont());
        
        // Draw the text at the baseline position
        gc.fillText(box.getText(), box.getX(), box.getBaselineY());
        
        // Draw selection indicator or editing cursor
        if (box == selectedTextBox) {
            double width = box.getWidth();
            double height = box.getFontSize();
            
            // Draw selection rectangle - now positioned correctly relative to text
            gc.setStroke(Color.BLUE);
            gc.setLineDashes(2);
            gc.strokeRect(box.getX() - 2, box.getTopY() - 2, width + 4, height + 4);
            gc.setLineDashes(null);
            
            // Draw editing indicator
            if (box.isEditing()) {
                // Draw text cursor
                double cursorX = box.getX() + box.getWidth();
                gc.setStroke(Color.BLACK);
                gc.strokeLine(cursorX, box.getTopY(), cursorX, box.getBaselineY());
            }
        }
    }

    @FXML
    private void handleClearCanvas() {
        saveState(); // Save state before clearing
        clearCanvas();
        textBoxes.clear();
        activeTextBox = null;
        selectedTextBox = null;
        updateUndoRedoButtons();
    }
    
    private void clearCanvas() {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }
    
    // Save the current canvas (drawing + text) as the new background
    private void saveState() {
        // Take clean snapshot of the canvas without text boxes
        WritableImage currentDrawing = new WritableImage(
            (int)canvas.getWidth(), 
            (int)canvas.getHeight()
        );
        
        // Store current text boxes temporarily
        List<TextBox> tempTextBoxes = new ArrayList<>(textBoxes);
        textBoxes.clear();
        
        // Redraw only the canvas content
        redrawCanvas();
        canvas.snapshot(null, currentDrawing);
        
        // Restore text boxes
        textBoxes = new ArrayList<>(tempTextBoxes);
        
        // Push states to undo stack
        undoStack.push(currentDrawing);
        textBoxUndoStack.push(new ArrayList<>(tempTextBoxes));
        
        // Clear redo stacks
        redoStack.clear();
        textBoxRedoStack.clear();
        
        // Redraw everything
        redrawCanvas();
        updateUndoRedoButtons();
    }

    @FXML
    private void handleUndo() {
        if (undoStack.size() > 1 && !textBoxUndoStack.isEmpty()) { // Keep at least the initial state
            // Push current states to redo stacks
            redoStack.push(undoStack.pop());
            textBoxRedoStack.push(textBoxUndoStack.pop());

            // Restore previous states
            Image lastCanvasState = undoStack.peek();
            List<TextBox> lastTextBoxState = textBoxUndoStack.peek();

            // Restore canvas background
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            gc.drawImage(lastCanvasState, 0, 0);

            // Restore text boxes
            textBoxes.clear();
            if (lastTextBoxState != null) {
                for (TextBox box : lastTextBoxState) {
                    textBoxes.add(new TextBox(box.getText(), box.getX(), box.getY(),
                                             box.getColor(), box.getFontFamily(),
                                             box.getFontSize(), box.isBold(),
                                             box.isItalic()));
                }
            }

            // Deselect any text box
            activeTextBox = null;
            selectedTextBox = null;

            redrawCanvas();
            updateUndoRedoButtons();
        }
    }
    
    @FXML
    private void handleRedo() {
        if (!redoStack.isEmpty() && !textBoxRedoStack.isEmpty()) {
            // Move the redone state to the undo stack
            undoStack.push(redoStack.pop());
            textBoxUndoStack.push(textBoxRedoStack.pop());

            // Restore canvas background
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            gc.drawImage(undoStack.peek(), 0, 0);

            // Restore text boxes
            textBoxes.clear();
            List<TextBox> textBoxState = textBoxUndoStack.peek();
            if (textBoxState != null) {
                for (TextBox box : textBoxState) {
                    textBoxes.add(new TextBox(box.getText(), box.getX(), box.getY(),
                                             box.getColor(), box.getFontFamily(), 
                                             box.getFontSize(), box.isBold(),
                                             box.isItalic()));
                }
            }

            // Deselect any text box
            activeTextBox = null;
            selectedTextBox = null;

            redrawCanvas();
            updateUndoRedoButtons();
        }
    }
    
    private void updateUndoRedoButtons() {
        undoButton.setDisable(undoStack.size() <= 1); // Disable if only initial state
        redoButton.setDisable(redoStack.isEmpty());
    }
}
