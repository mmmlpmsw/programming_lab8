package ru.n4d9.client;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ResourceBundle;

public class RoomPropertiesPane extends VBox {
    private Room selected;

    ResourceBundle bundle = Client.currentResourceBundle();

    private RoomDeletingListener deletingListener = roomId -> {};
    private RoomApplyingListener applyingListener = model -> {};
    private RoomRemovingGreaterListener roomRemovingGreaterListener = model -> {};
    private RoomRemovingLowerListener roomRemovingLowerListener = model -> {};

    private Label idLabel, ownerIdLabel, createdLabel;

    private TextField nameInput;
    private Slider xInput, yInput, widthInput, heightInput;
    private Button applyButton, resetButton, deleteButton, removeGreaterButton, removeLowerButton;
    private CheckBox autoApplyCheckbox;

    private boolean autoApplyingEnabled = true;

    private Thread debounceThread = new Thread();

    public RoomPropertiesPane() {

        setPadding(new Insets(20));
        setFillWidth(true);

        idLabel = new Label();
        ownerIdLabel = new Label();
        createdLabel = new Label();

        nameInput = new TextField();
        xInput = new Slider(20, 800, 0);
        yInput = new Slider(0, 800, 0);
        heightInput = new Slider(100, 500, 0);
        widthInput = new Slider(100, 500, 0);

        applyButton = new Button(bundle.getString("main.apply"));
        resetButton = new Button(bundle.getString("main.reset"));
        deleteButton = new Button(bundle.getString("main.delete"));
        removeGreaterButton = new Button(bundle.getString("main.remove-greater"));
        removeLowerButton = new Button(bundle.getString("main.remove-lower"));

        autoApplyCheckbox = new CheckBox(bundle.getString("main.auto-apply"));

        xInput.setBlockIncrement(1);
        xInput.setShowTickLabels(true);
        xInput.setShowTickMarks(true);

        yInput.setBlockIncrement(1);
        yInput.setShowTickLabels(true);
        yInput.setShowTickMarks(true);

        heightInput.setBlockIncrement(1);
        heightInput.setShowTickLabels(true);
        heightInput.setShowTickMarks(true);

        widthInput.setBlockIncrement(1);
        widthInput.setShowTickLabels(true);
        widthInput.setShowTickMarks(true);

        autoApplyCheckbox.setSelected(true);

        HBox nameInputPane = new HBox(new Label(bundle.getString("main.name")), nameInput);
        nameInputPane.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(nameInput, new Insets(0, 0, 0, 20));
        HBox.setHgrow(nameInput, Priority.ALWAYS);

        HBox.setHgrow(xInput, Priority.ALWAYS);
        HBox.setHgrow(yInput, Priority.ALWAYS);
        HBox.setHgrow(heightInput, Priority.ALWAYS);
        HBox.setHgrow(widthInput, Priority.ALWAYS);

        HBox buttonsPane = new HBox(applyButton, resetButton, deleteButton);
        buttonsPane.setAlignment(Pos.CENTER);

        HBox buttons2Pane = new HBox(removeGreaterButton, removeLowerButton);
        buttons2Pane.setAlignment(Pos.CENTER);

        HBox autoApplyPane = new HBox(autoApplyCheckbox);

        HBox.setMargin(nameInput, new Insets(5));
        HBox.setMargin(applyButton, new Insets(5));
        HBox.setMargin(resetButton, new Insets(5));
        HBox.setMargin(deleteButton, new Insets(5));
        HBox.setMargin(removeGreaterButton, new Insets(5));
        HBox.setMargin(removeLowerButton, new Insets(5));
        HBox.setMargin(xInput, new Insets(5));
        HBox.setMargin(yInput, new Insets(5));
        HBox.setMargin(heightInput, new Insets(5));
        HBox.setMargin(widthInput, new Insets(5));
        HBox.setMargin(autoApplyCheckbox, new Insets(5));

        getChildren().addAll(
                nameInputPane,
                new HBox(new Label("X"), xInput),
                new HBox(new Label("Y"), yInput),
                new HBox(new Label(bundle.getString("main.rooms-table.height-column-text")), heightInput),
                new HBox(new Label(bundle.getString("main.rooms-table.width-column-text")), widthInput),
                buttonsPane,
                buttons2Pane,
                autoApplyPane
        );

        nameInput.textProperty().addListener((observable, oldValue, newValue) -> onEdited());
        xInput.valueProperty().addListener((observable, oldValue, newValue) -> onEdited());
        yInput.valueProperty().addListener((observable, oldValue, newValue) -> onEdited());
        heightInput.valueProperty().addListener((observable, oldValue, newValue) -> onEdited());
        widthInput.valueProperty().addListener((observable, oldValue, newValue) -> onEdited());

        applyButton.setOnAction((e) -> onApply());
        resetButton.setOnAction((e) -> onReset());
        deleteButton.setOnAction((e) -> onDelete());
        removeGreaterButton.setOnAction(e -> onRemoveGreater());
        removeLowerButton.setOnAction(e -> onRemoveLower());
    }

    void selectCreature(Room model, boolean editable) {
        if (model == null) {
            setManaged(false);
            setVisible(false);
            return;
        }

        setManaged(true);
        setVisible(true);

        autoApplyingEnabled = false;
        selected = model;

        applyButton.setDisable(true);
        resetButton.setDisable(true);
        deleteButton.setDisable(!editable);
        removeGreaterButton.setDisable(!editable);
        removeLowerButton.setDisable(!editable);
        autoApplyCheckbox.setDisable(!editable);
        nameInput.setDisable(!editable);
        xInput.setDisable(!editable);
        yInput.setDisable(!editable);
        heightInput.setDisable(!editable);
        widthInput.setDisable(!editable);

        if (model == null)
            setDisable(true);
        else {
            resetProperties();
            setDisable(false);
        }
        autoApplyingEnabled = true;
    }

    void setApplyingListener(RoomApplyingListener applyingListener) {
        this.applyingListener = applyingListener;
    }

    void setDeletingListener(RoomDeletingListener deletingListener) {
        this.deletingListener = deletingListener;
    }

    void setRemovingGreaterListener(RoomRemovingGreaterListener removingListener) {
        this.roomRemovingGreaterListener = removingListener;
    }

    void setRemovingLowerListener(RoomRemovingLowerListener removingListener) {
        this.roomRemovingLowerListener = removingListener;
    }

    private void onEdited() {
        applyButton.setDisable(false);
        resetButton.setDisable(false);

        if (autoApplyCheckbox.isSelected() && autoApplyingEnabled) //todo
            onApply();
    }

    private void onApply() {
        if (nameInput.getText().length() == 0 || nameInput.getText().length() > 32)
            return;  // todo check name and show warning
        if (!debounceThread.isAlive()) {
            debounceThread = new Thread(() -> {
                try {
                    Thread.sleep(300);
                    Room room = new Room(
                            (double) widthInput.getValue(),
                            (double) heightInput.getValue(),
                            (double)xInput.getValue(),
                            (double) yInput.getValue(),
                            nameInput.getText()
                    );
                    room.setId(selected.getId());
                    room.setOwnerId(selected.getOwnerId());
                    applyingListener.applyRequested(room);
                } catch (InterruptedException ignored) {
                }
            });
            debounceThread.start();
        }
    }

    private void onReset() {
        resetProperties();
    }

    private void onDelete() {
        deletingListener.deleteRequested(selected.getId());
    }

    private void onRemoveGreater() {
        roomRemovingGreaterListener.removeGreaterRequested(selected);
    }

    private void onRemoveLower() {
        roomRemovingLowerListener.removeLowerRequested(selected);
    }

    private void resetProperties() {
        if (selected != null) {
            idLabel.setText("ID " + selected.getId());
            ownerIdLabel.setText(bundle.getString("main.owner-id") + " " + selected.getOwnerId());
            createdLabel.setText(bundle.getString("main.created") + " " + selected.getCreationDate().toString());

            nameInput.setText(selected.getName());
            xInput.setValue(selected.getX());
            yInput.setValue(selected.getY());
            heightInput.setValue(selected.getHeight());
            widthInput.setValue(selected.getWidth());

            applyButton.setDisable(true);
            resetButton.setDisable(true);
        }
    }
}
