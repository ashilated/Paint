package dev.ashilated.paint;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


public class PaintController {

    @FXML
    Canvas canvas;
    @FXML
    Spinner brushSize;
    @FXML
    ToggleButton paintbrush;
    @FXML
    ToggleButton eraser;
    @FXML
    ToggleButton bucket;
    @FXML
    ToggleButton eyedropper;
    @FXML
    ToggleButton rectangle;
    @FXML
    ToggleButton circle;
    @FXML
    ColorPicker colorPicker;
    @FXML
    Button undo;
    @FXML
    Button redo;
    @FXML
    MenuItem newFile;
    @FXML
    MenuItem saveFile;
    @FXML
    Pane node;

    private final ToggleGroup tools = new ToggleGroup();

    private Point lastPos;
    private boolean eraseMode = false;
    private boolean smokeMode = false;

    private ArrayList<Image> history = new ArrayList<>();
    private ArrayList<Image> redoHistory = new ArrayList<>();

    public void initialize() {
        setCanvasBackground();
        initBrush();
        setToolsGroup();
        handleTools();
        handleFiles();
    }

    private void handleFiles() {
        newFile.setOnAction(_ -> {
            setCanvasBackground();
            history.removeAll(history);
        });
        saveFile.setOnAction(_ -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialFileName("Untitled.png");
            File file = fileChooser.showSaveDialog(node.getScene().getWindow());
            if (file != null) {
                try {
                    ImageIO.write(SwingFXUtils.fromFXImage(canvas.snapshot(null, null), null), "png", file);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void setCanvasBackground() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, 600, 400);
        gc.setFill(Color.BLACK);
    }

    private void initBrush() {
        brushSize.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1,1000));
        brushSize.getValueFactory().setValue(12);
        brushSize.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                brushSize.increment(0);
            }
        });

        colorPicker.setValue(Color.BLACK);
    }


    private void handleTools() {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        colorPicker.setOnAction(_ -> {
            if (!eraseMode) {
                gc.setStroke(colorPicker.getValue());
                gc.setFill(colorPicker.getValue());
            }
        });
        brushSize.valueProperty().addListener((_) -> gc.setLineWidth((int) brushSize.getValue()));

        paintbrush.setOnAction(_ -> draw(gc));
        eraser.setOnAction(_ -> erase(gc));
        bucket.setOnAction(_ -> smoke());
        eyedropper.setOnAction(_ -> pickColor());
        rectangle.setOnAction(_ -> drawRect(gc));
        circle.setOnAction(_ -> drawCircle(gc));

        undo.setOnAction(_ -> {
            if (history.size() > 1) {
                gc.drawImage(history.get(history.size()-2), 0, 0);
                redoHistory.add(history.getLast());
                history.removeLast();
            }

        });
        redo.setOnAction(_ -> {
            if (!redoHistory.isEmpty()) {
                gc.drawImage(redoHistory.getLast(), 0, 0);
                history.add(redoHistory.getLast());
                redoHistory.removeLast();
            }
        });

        paintbrush.setSelected(true);
        draw(gc);
    }

    private void setToolsGroup() {
        paintbrush.setToggleGroup(tools);
        eraser.setToggleGroup(tools);
        //bucket.setToggleGroup(tools);
        eyedropper.setToggleGroup(tools);
        rectangle.setToggleGroup(tools);
        circle.setToggleGroup(tools);
    }

    private EventHandler<MouseEvent> addToHistory = _ -> history.add(canvas.snapshot(null, null));

    private EventHandler<MouseEvent> clearLastPos = _ -> lastPos = null;

    private EventHandler<MouseEvent> draw = _ -> {};

    private void draw(GraphicsContext gc) {
        eraseMode = false;
        removeToolHandlers();

        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineWidth((int) brushSize.getValue());
        gc.setStroke(colorPicker.getValue());

        draw = event -> {
            clearRedoHistory();
            if (lastPos != null) {
                gc.strokeLine(lastPos.x - 2, lastPos.y - 2, event.getX() - 2, event.getY() - 2);
            } else gc.strokeLine(event.getX() - 2, event.getY() - 2, event.getX() - 2, event.getY() - 2);
            lastPos = new Point((int) event.getX(), (int) event.getY());
            if (smokeMode) gc.drawImage(canvas.snapshot(null, null), 0, 0);
        };
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, draw);
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, draw);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, clearLastPos);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, addToHistory);
    }

    private EventHandler<MouseEvent> erase = _ -> {};
    private void erase(GraphicsContext gc) {
        eraseMode = true;
        clearRedoHistory();
        removeToolHandlers();

        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineWidth((int) brushSize.getValue());
        gc.setStroke(Color.WHITE);
        erase = event -> {
            if (lastPos != null) {
                gc.strokeLine(lastPos.x - 2, lastPos.y - 2, event.getX() - 2, event.getY() - 2);
            } else gc.strokeLine(event.getX() - 2, event.getY() - 2, event.getX() - 2, event.getY() - 2); //gc.clearRect(event.getX() - 2, event.getY() - 2, (int) brushSize.getValue(), (int) brushSize.getValue());
            lastPos = new Point((int) event.getX(), (int) event.getY());
        };
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, erase);
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, erase);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, clearLastPos);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, addToHistory);
    }

    private void smoke() {
        smokeMode = !smokeMode;
    }

    private EventHandler<MouseEvent> fill = _ -> {};
    private void fill(GraphicsContext gc) {
        eraseMode = false;
        removeToolHandlers();

        fill = event -> {
            clearRedoHistory();
            PixelWriter pixelWriter = gc.getPixelWriter();
            WritableImage currentCanvas = canvas.snapshot(null, null);
            PixelReader pixelReader = currentCanvas.getPixelReader();

            int xPos = (int) event.getX(), yPos = (int) event.getY();
            Color colorToReplace = pixelReader.getColor(xPos, yPos);

            doFill(xPos, yPos, colorToReplace, pixelWriter);

        };
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, fill);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, clearLastPos);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, addToHistory);
    }

    private void simpleFill(int x, int y, Color color, PixelWriter pixelWriter) {
        paint(new Point(x, y), pixelWriter);
        if (x > 0 && checkColor(new Point(x - 1, y), color)) simpleFill(x-1, y, color, pixelWriter);
        if (x < canvas.getWidth() - 1 && checkColor(new Point(x + 1, y), color)) simpleFill(x + 1, y, color, pixelWriter);
        if (y > 0 && checkColor(new Point(x, y-1), color)) simpleFill(x, y-1, color, pixelWriter);
        if (y < canvas.getHeight() - 1 && checkColor(new Point(x, y+1), color)) simpleFill(x + 1, y+1, color, pixelWriter);
    }


    private void doFill(int xPos, int yPos, Color colorToReplace, PixelWriter pixelWriter) {

        while (yPos < canvas.getHeight() - 1 && checkColor(new Point(xPos, yPos+1), colorToReplace)) yPos++;
        while (xPos < canvas.getWidth() - 1 && checkColor(new Point(xPos+1, yPos), colorToReplace)) xPos++;
        //System.out.println(xPos + " " + yPos);
        doFillCore(xPos, yPos, colorToReplace, pixelWriter);
    }
    private void doFillCore(int xPos, int yPos, Color color, PixelWriter pixelWriter) {
        int lastRowLength = 0;
        do {
            int rowLength = 0, startX = xPos;
            if (lastRowLength != 0 && !checkColor(new Point(xPos, yPos), color)) {
                do {
                    if (--lastRowLength == 0) return;
                } while (!checkColor(new Point(++xPos, yPos), color));
                startX = xPos;
            } else {
                for (; xPos > 0 && checkColor(new Point(xPos-1, yPos), color); rowLength++, lastRowLength++) {
                    System.out.println(xPos - 1 + " " + yPos);
                    paint(new Point(--xPos, yPos), pixelWriter);
                    if (yPos > 0 && checkColor(new Point(xPos, yPos-1), color)) doFill(xPos, yPos-1, color, pixelWriter);
                }
            }

            for (; startX < canvas.getWidth() - 1 && checkColor(new Point(startX, yPos), color); rowLength++, startX++) {
                paint(new Point(startX, yPos), pixelWriter);
            }

            if (rowLength < lastRowLength) {
                for (int rowEnd = xPos + lastRowLength; ++startX < rowEnd; ) {
                    if (checkColor(new Point(startX, yPos), color)) doFillCore(startX, yPos, color, pixelWriter);
                }
            } else if (rowLength > lastRowLength && yPos > 0) {
                for (int ux = xPos + lastRowLength; ++ux < startX; ) {
                    if (checkColor(new Point(ux, yPos-1), color)) doFill(ux, yPos-1, color, pixelWriter);
                }
            }
            lastRowLength = rowLength;
        } while (lastRowLength != 0 && yPos < canvas.getHeight() - 1);


    }


    private void paint(Point pos, PixelWriter pixelWriter) {
        pixelWriter.setColor(pos.x, pos.y, colorPicker.getValue());
    }

    private boolean checkColor(Point pos, Color color) {
        WritableImage currentCanvas = canvas.snapshot(null, null);
        PixelReader pixelReader = currentCanvas.getPixelReader();
        return pixelReader.getColor(pos.x, pos.y).toString().equals(color.toString());
    }


    private EventHandler<MouseEvent> pickColor = _ -> {};
    private void pickColor() {
        removeToolHandlers();
        pickColor = event -> {
            WritableImage currentCanvas = canvas.snapshot(null, null);
            PixelReader pixelReader = currentCanvas.getPixelReader();
            colorPicker.setValue(pixelReader.getColor((int) event.getX(), (int) event.getY()));
        };
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, pickColor);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, clearLastPos);

    }

    private Image shapeToolTempHistory;
    private int lastWidth;
    private int lastHeight;

    private EventHandler<MouseEvent> drawRectIntPos = _ -> {};
    private EventHandler<MouseEvent> drawRect = _ -> {};
    private void drawRect(GraphicsContext gc) {
        eraseMode = false;
        removeToolHandlers();
        drawRectIntPos = event -> {
            clearRedoHistory();
            if (lastPos == null) {
                lastPos = new Point((int) event.getX(), (int) event.getY());
                shapeToolTempHistory = canvas.snapshot(null, null);
                lastHeight = 0;
                lastWidth = 0;
            }

        };
        drawRect = event -> {
            if (lastPos != null) {
                Point pointTwo = new Point((int) event.getX(), (int) event.getY());
                Point topLeftPoint = new Point(lastPos.x, lastPos.y);
                int width = Math.abs(pointTwo.x - lastPos.x);
                int height = Math.abs(pointTwo.y - lastPos.y);
                if (pointTwo.x < lastPos.x) {
                    topLeftPoint.x = pointTwo.x;
                }
                if (pointTwo.y < lastPos.y) {
                    topLeftPoint.y = pointTwo.y;
                }
                if (width < lastWidth || height < lastHeight) {
                    gc.drawImage(shapeToolTempHistory, 0, 0);
                }
                gc.fillRect(topLeftPoint.x, topLeftPoint.y, width, height);
                lastWidth = width;
                lastHeight = height;
            }
        };
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, drawRectIntPos);
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, drawRect);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, addToHistory);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, clearLastPos);

    }

    private EventHandler<MouseEvent> drawCircleIntPos = _ -> {};
    private EventHandler<MouseEvent> drawCircle = _ -> {};
    private void drawCircle(GraphicsContext gc) {
        eraseMode = false;
        removeToolHandlers();
        drawCircleIntPos = event -> {
            clearRedoHistory();
            if (lastPos == null) {
                lastPos = new Point((int) event.getX(), (int) event.getY());
                shapeToolTempHistory = canvas.snapshot(null, null);
            }

        };
        drawCircle = event -> {
            if (lastPos != null) {
                Point pointTwo = new Point((int) event.getX(), (int) event.getY());
                Point topLeftPoint = new Point(lastPos.x, lastPos.y);
                int width = Math.abs(pointTwo.x - lastPos.x);
                int height = Math.abs(pointTwo.y - lastPos.y);
                if (pointTwo.x < lastPos.x) {
                    topLeftPoint.x = pointTwo.x;
                }
                if (pointTwo.y < lastPos.y) {
                    topLeftPoint.y = pointTwo.y;
                }
                gc.drawImage(shapeToolTempHistory, 0, 0);
                gc.fillOval(topLeftPoint.x, topLeftPoint.y, width, height);
            }
        };
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, drawCircleIntPos);
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, drawCircle);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, addToHistory);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, clearLastPos);
    }

    private void removeToolHandlers() {
        canvas.removeEventHandler(MouseEvent.MOUSE_DRAGGED, draw);
        canvas.removeEventHandler(MouseEvent.MOUSE_PRESSED, draw);
        canvas.removeEventHandler(MouseEvent.MOUSE_DRAGGED, erase);
        canvas.removeEventHandler(MouseEvent.MOUSE_PRESSED, erase);
        canvas.removeEventHandler(MouseEvent.MOUSE_PRESSED, fill);
        canvas.removeEventHandler(MouseEvent.MOUSE_PRESSED, pickColor);
        canvas.removeEventHandler(MouseEvent.MOUSE_PRESSED, drawRectIntPos);
        canvas.removeEventHandler(MouseEvent.MOUSE_DRAGGED, drawRect);
        canvas.removeEventHandler(MouseEvent.MOUSE_PRESSED, drawCircleIntPos);
        canvas.removeEventHandler(MouseEvent.MOUSE_DRAGGED, drawCircle);
        canvas.removeEventHandler(MouseEvent.MOUSE_RELEASED, clearLastPos);
        canvas.removeEventHandler(MouseEvent.MOUSE_RELEASED, addToHistory);
    }

    private void clearRedoHistory() {
        redoHistory.removeAll(redoHistory);
    }

}