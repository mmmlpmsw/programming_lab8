package ru.n4d9.client;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ResourceBundle;

public class CreaturePropertiesPane extends VBox {
    private Room selected;

    private RoomDeletingListener deletingListener = creatureId -> {};
    private RoomApplyingListener applyingListener = model -> {};

    private Label idLabel, ownerIdLabel, createdLabel;

    private TextField nameInput;
    private Slider xInput, yInput, widthInput, heightInput;
    private Button applyButton, resetButton, deleteButton;
    private CheckBox autoApplyCheckbox;

    private boolean autoApplyingEnabled = true;

    private Thread debounceThread = new Thread();

    public CreaturePropertiesPane() {
        ResourceBundle bundle = MainWindow.currentResourceBundle();
        setPadding(new Insets(20));
        setFillWidth(true);

        idLabel = new Label();
        ownerIdLabel = new Label();
        createdLabel = new Label();

        nameInput = new TextField();
        xInput = new Slider(0, 1000, 0);
        yInput = new Slider(0, 1000, 0);
        heightInput = new Slider(100, 500, 0);
        widthInput = new Slider(100, 500, 0);

        applyButton = new Button(bundle.getString("main.apply"));
        resetButton = new Button(bundle.getString("main.reset"));
        deleteButton = new Button(bundle.getString("main.delete"));

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

        HBox autoApplyPane = new HBox(autoApplyCheckbox);

        HBox.setMargin(nameInput, new Insets(5));
        HBox.setMargin(applyButton, new Insets(5));
        HBox.setMargin(resetButton, new Insets(5));
        HBox.setMargin(deleteButton, new Insets(5));
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
                new HBox(new Label("main.rooms-table.width-column-text"), widthInput),
                buttonsPane,
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

    private void onEdited() {
        applyButton.setDisable(false);
        resetButton.setDisable(false);

        if (autoApplyCheckbox.isSelected() && autoApplyingEnabled)
            onApply();
    }

    private void onApply() {
        if (nameInput.getText().length() == 0 || nameInput.getText().length() > 32)
            return; // TODO: Show warning
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

    private void resetProperties() {
        if (selected != null) {
            idLabel.setText("ID " + selected.getId());
            ownerIdLabel.setText(MainWindow.currentResourceBundle().getString("main.owner-id") + " " + selected.getOwnerId());
            createdLabel.setText(MainWindow.currentResourceBundle().getString("main.created") + " " + selected.getCreationDate().toString());

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
