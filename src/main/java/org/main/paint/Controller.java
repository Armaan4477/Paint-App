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
    @FXML private ColorPicker colorPicker;
    @FXML private Label coordinatesLabel;
    @FXML private Button undoButton;
    @FXML private Button redoButton;
    @FXML private StackPane canvasContainer;
    @FXML private HBox textControlsBox;
    @FXML private TextField textInput;
    @FXML private ComboBox<String> fontFamilyComboBox;
    @FXML private ComboBox<Integer> fontSizeComboBox;
    @FXML private CheckBox boldCheckBox;
    @FXML private CheckBox italicCheckBox;
    @FXML private CheckBox textModeCheckBox;

    private GraphicsContext gc;
    private Color currentColor = Color.BLACK;
    private Brush currentBrush;
    private boolean textMode = false;
    private List<TextBox> textBoxes = new ArrayList<>();
    private TextBox activeTextBox = null;
    private TextBox selectedTextBox = null;
    private double dragStartX, dragStartY;
    private boolean isDraggingTextBox = false;
    private Stack<Image> undoStack = new Stack<>();
    private Stack<Image> redoStack = new Stack<>();
    private Stack<List<TextBox>> textBoxUndoStack = new Stack<>();
    private Stack<List<TextBox>> textBoxRedoStack = new Stack<>();
    private boolean isDrawing = false;

    @FXML
    public void initialize() {
        gc = canvas.getGraphicsContext2D();
        clearCanvas();
        saveBrushState();         // initial brush snapshot
        saveTextState();          // initial empty text snapshot
        brushTypeComboBox.getItems().addAll("Circle","Square","Pencil","Spray","Line","Triangle","Star");
        brushTypeComboBox.setValue("Pencil");
        colorPicker.setValue(currentColor);
        colorPicker.setOnAction(e -> {
            currentColor = colorPicker.getValue();
            updateBrush();
            if (selectedTextBox != null) {
                selectedTextBox.setColor(currentColor);
                redrawCanvas();
            }
        });
        initializeTextControls();
        updateBrush();
        brushTypeComboBox.setOnAction(e -> updateBrush());
        brushSizeSlider.valueProperty().addListener((obs, o, n) -> updateBrush());
        textModeCheckBox.selectedProperty().addListener((obs, o, n) -> {
            textMode = n;
            updateBrush();
            toggleTextControls();
            if (!textMode && activeTextBox != null) finalizeActiveTextBox();
            saveBrushState();
            updateUndoRedoButtons();  // <-- ensure buttons reflect the active stack
        });
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, this::handleMouseMoved);
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleMouseClicked);
        canvas.setOnKeyTyped(this::handleKeyTyped);
        canvas.setFocusTraversable(true);
        updateUndoRedoButtons();
        canvas.setOnKeyPressed(this::handleKeyPressed);
    }

    private void handleKeyPressed(KeyEvent event) {
        if (selectedTextBox != null && (event.getCode().toString().equals("DELETE") ||
                                        event.getCode().toString().equals("BACK_SPACE"))) {
            if (selectedTextBox.isEditing()) {
                if (event.getCode().toString().equals("BACK_SPACE")) return;
            }
            saveTextState();   // text-only
            textBoxes.remove(selectedTextBox);
            selectedTextBox = null;
            redrawCanvas();
        }
    }

    private void handleKeyTyped(KeyEvent event) {
        if (activeTextBox != null && activeTextBox.isEditing()) {
            String character = event.getCharacter();
            String currentText = activeTextBox.getText();
            if (character.equals("\b")) {
                if (currentText.length() > 0) activeTextBox.setText(currentText.substring(0, currentText.length() - 1));
            } else if (character.equals("\r") || character.equals("\n")) {
                finalizeActiveTextBox(); return;
            } else if (!character.equals("\t")) {
                activeTextBox.setText(currentText + character);
            }
            if (textInput != null) textInput.setText(activeTextBox.getText());
            redrawCanvas();
        }
    }

    private void initializeTextControls() {
        List<String> fontFamilies = Font.getFamilies();
        fontFamilyComboBox.getItems().addAll(fontFamilies);
        fontFamilyComboBox.setValue("Arial");
        Integer[] fontSizes = {8,10,12,14,16,18,20,24,28,32,36,42,48,56,64,72};
        fontSizeComboBox.getItems().addAll(fontSizes);
        fontSizeComboBox.setValue(20);
        textInput.textProperty().addListener((obs, o, n) -> {
            if (activeTextBox != null) {
                activeTextBox.setText(n);
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
        boldCheckBox.selectedProperty().addListener((obs, o, n) -> {
            if (selectedTextBox != null) {
                selectedTextBox.setBold(n);
                redrawCanvas();
            }
        });
        italicCheckBox.selectedProperty().addListener((obs, o, n) -> {
            if (selectedTextBox != null) {
                selectedTextBox.setItalic(n);
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
        if (textMode) currentBrush = null;
        else switch (brushTypeComboBox.getValue()) {
            case "Circle":    currentBrush = new Brush.CircleBrush(size, currentColor); break;
            case "Square":    currentBrush = new Brush.SquareBrush(size, currentColor); break;
            case "Spray":     currentBrush = new Brush.SprayBrush(size, currentColor); break;
            case "Line":      currentBrush = new Brush.LineBrush(size, currentColor); break;
            case "Triangle":  currentBrush = new Brush.TriangleBrush(size, currentColor); break;
            case "Star":      currentBrush = new Brush.StarBrush(size, currentColor); break;
            case "Pencil":
            default:          currentBrush = new Brush.PencilBrush(size, currentColor); break;
        }
    }

    private void handleMouseClicked(MouseEvent event) {
        if (textMode) {
            TextBox clickedBox = findTextBoxAt(event.getX(), event.getY());
            if (clickedBox == null && activeTextBox != null) { finalizeActiveTextBox(); return; }
            if (event.getClickCount() == 2 && clickedBox != null) { startEditingTextBox(clickedBox); return; }
            if (activeTextBox == null && clickedBox == null) {
                // saveTextState();  // removed: createNewTextBox already saves text state
                createNewTextBox(event.getX(), event.getY());
            }
        }
    }

    private void handleMousePressed(MouseEvent event) {
        if (textMode) {
            TextBox clickedBox = findTextBoxAt(event.getX(), event.getY());
            if (clickedBox != null) {
                if (selectedTextBox != clickedBox) selectTextBox(clickedBox);
                dragStartX = event.getX(); dragStartY = event.getY(); isDraggingTextBox = true;
            } else deselectTextBox();
        } else {
            saveBrushState();    // start of a new stroke
            isDrawing = true;
            if (currentBrush != null) {
                if (!isDrawing) saveBrushState();
                currentBrush.draw(gc, event.getX(), event.getY());
            }
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        coordinatesLabel.setText(String.format("Coordinates: %.0f, %.0f", event.getX(), event.getY()));
        if (textMode) {
            if (isDraggingTextBox && selectedTextBox != null) {
                double dx = event.getX() - dragStartX, dy = event.getY() - dragStartY;
                selectedTextBox.setX(selectedTextBox.getX() + dx);
                selectedTextBox.setY(selectedTextBox.getY() + dy);
                dragStartX = event.getX(); dragStartY = event.getY();
                redrawCanvas();
            }
        } else {
            if (isDrawing && currentBrush != null) currentBrush.draw(gc, event.getX(), event.getY());
        }
    }

    private void handleMouseReleased(MouseEvent event) {
        if (textMode) {
            isDraggingTextBox = false;
            if (selectedTextBox != null && !selectedTextBox.isEditing()) {
                saveTextState();  // after move
            }
        } else if (isDrawing) {
            isDrawing = false;
            if (currentBrush instanceof Brush.PencilBrush)
                ((Brush.PencilBrush) currentBrush).resetLastPosition();
                saveBrushState();
        }
    }

    private void handleMouseMoved(MouseEvent event) {
        coordinatesLabel.setText(String.format("Coordinates: %.0f, %.0f", event.getX(), event.getY()));
        if (textMode) {
            TextBox hoveredBox = findTextBoxAt(event.getX(), event.getY());
            canvas.setCursor(hoveredBox != null ? javafx.scene.Cursor.HAND : javafx.scene.Cursor.TEXT);
        } else canvas.setCursor(javafx.scene.Cursor.DEFAULT);
    }

    private TextBox findTextBoxAt(double x, double y) {
        for (int i = textBoxes.size() - 1; i >= 0; i--) {
            TextBox box = textBoxes.get(i);
            if (box.contains(x, y)) {
                return box;
            }
        }
        return null;
    }

    private void createNewTextBox(double x, double y) {
        saveTextState();  // text-only
        String initialText = "Enter text...";
        int fontSize = fontSizeComboBox.getValue();
        String fontFamily = fontFamilyComboBox.getValue();
        boolean isBold = boldCheckBox.isSelected();
        boolean isItalic = italicCheckBox.isSelected();
        double baselineY = y + fontSize * 0.7;
        TextBox newTextBox = new TextBox(initialText, x, baselineY, currentColor, 
                                        fontFamily, fontSize, isBold, isItalic);
        textBoxes.add(newTextBox);
        startEditingTextBox(newTextBox);
        redrawCanvas();
    }

    private void startEditingTextBox(TextBox box) {
        if (activeTextBox != null && activeTextBox != box) {
            finalizeActiveTextBox();
        }
        activeTextBox = box;
        selectedTextBox = box;
        box.setEditing(true);
        if (textInput != null) {
            textInput.setText(box.getText());
        }
        fontFamilyComboBox.setValue(box.getFontFamily());
        fontSizeComboBox.setValue((int)box.getFontSize());
        boldCheckBox.setSelected(box.isBold());
        italicCheckBox.setSelected(box.isItalic());
        textInput.selectAll();
        textInput.requestFocus();
        redrawCanvas();
    }

    private void finalizeActiveTextBox() {
        if (activeTextBox != null) {
            if (activeTextBox.getText().trim().isEmpty()) {
                textBoxes.remove(activeTextBox);
            } else {
                activeTextBox.setEditing(false);
            }
            activeTextBox = null;
            saveTextState(); // text-only
            redrawCanvas();
        }
    }

    private void selectTextBox(TextBox box) {
        deselectTextBox();
        selectedTextBox = box;
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

    private void redrawCanvas() {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (!undoStack.isEmpty()) {
            gc.drawImage(undoStack.peek(), 0, 0);
        }
        for (TextBox box : textBoxes) {
            drawTextBox(box);
        }
    }

    private void drawTextBox(TextBox box) {
        gc.setFill(box.getColor());
        gc.setFont(box.getFont());
        gc.fillText(box.getText(), box.getX(), box.getBaselineY());
        if (box == selectedTextBox) {
            double width = box.getWidth();
            double height = box.getFontSize();
            gc.setStroke(Color.BLUE);
            gc.setLineDashes(2);
            gc.strokeRect(box.getX() - 2, box.getTopY() - 2, width + 4, height + 4);
            gc.setLineDashes(null);
            if (box.isEditing()) {
                double cursorX = box.getX() + box.getWidth();
                gc.setStroke(Color.BLACK);
                gc.strokeLine(cursorX, box.getTopY(), cursorX, box.getBaselineY());
            }
        }
    }

    @FXML private void handleClearCanvas() {
        saveBrushState();    // clear canvas undo
        clearCanvas();
        saveTextState();     // clear text undo
        textBoxes.clear();
        activeTextBox = null;
        selectedTextBox = null;
        updateUndoRedoButtons();
    }

    private void clearCanvas() {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void saveBrushState() {
        WritableImage snap = new WritableImage((int)canvas.getWidth(), (int)canvas.getHeight());
        canvas.snapshot(null, snap);
        undoStack.push(snap);
        redoStack.clear();
        updateUndoRedoButtons();
    }

    private void saveTextState() {
        textBoxUndoStack.push(new ArrayList<>(textBoxes));
        textBoxRedoStack.clear();
        updateUndoRedoButtons();
    }

    @FXML private void handleUndo() {
        if (textMode) {
            if (textBoxUndoStack.size() > 1) {
                textBoxRedoStack.push(textBoxUndoStack.pop());
                List<TextBox> prev = textBoxUndoStack.peek();
                textBoxes.clear();
                if (prev!=null) prev.forEach(b->
                    textBoxes.add(new TextBox(b.getText(),b.getX(),b.getY(),
                                              b.getColor(),b.getFontFamily(),
                                              b.getFontSize(),b.isBold(),b.isItalic()))
                );
                activeTextBox = selectedTextBox = null;
                redrawCanvas();
            }
        } else {
            if (undoStack.size() > 1) {
                redoStack.push(undoStack.pop());
                Image img = undoStack.peek();
                gc.setFill(Color.WHITE);
                gc.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
                gc.drawImage(img,0,0);
                redrawCanvas();
            }
        }
        updateUndoRedoButtons();
    }

    @FXML private void handleRedo() {
        if (textMode) {
            if (!textBoxRedoStack.isEmpty()) {
                textBoxUndoStack.push(textBoxRedoStack.pop());
                List<TextBox> next = textBoxUndoStack.peek();
                textBoxes.clear();
                if (next!=null) next.forEach(b->
                    textBoxes.add(new TextBox(b.getText(),b.getX(),b.getY(),
                                              b.getColor(),b.getFontFamily(),
                                              b.getFontSize(),b.isBold(),b.isItalic()))
                );
                activeTextBox = selectedTextBox = null;
                redrawCanvas();
            }
        } else {
            if (!redoStack.isEmpty()) {
                undoStack.push(redoStack.pop());
                Image img = undoStack.peek();
                gc.setFill(Color.WHITE);
                gc.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
                gc.drawImage(img,0,0);
                redrawCanvas();
            }
        }
        updateUndoRedoButtons();
    }

    private void updateUndoRedoButtons() {
        if (textMode) {
            undoButton.setDisable(textBoxUndoStack.size() <= 1);
            redoButton.setDisable(textBoxRedoStack.isEmpty());
        } else {
            undoButton.setDisable(undoStack.size() <= 1);
            redoButton.setDisable(redoStack.isEmpty());
        }
    }
}
