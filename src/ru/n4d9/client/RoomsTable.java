package ru.n4d9.client;

import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ResourceBundle;

public class RoomsTable extends TableView<Room> {

    ResourceBundle bundle = Client.currentResourceBundle();
    private TableColumn<Room, String> idColumn = new TableColumn<>("Id");
    private TableColumn<Room, String> nameColumn = new TableColumn<>();
    private TableColumn<Room, String> xColumn = new TableColumn<>("x");
    private TableColumn<Room, String> yColumn = new TableColumn<>("y");
    private TableColumn<Room, String> heightColumn = new TableColumn<>();
    private TableColumn<Room, String> widthColumn = new TableColumn<>();
    private TableColumn<Room, String> ownerColumn = new TableColumn<>();
    private TableColumn<Room, String> createdColumn = new TableColumn<>();

    public RoomsTable() {
        idColumn.setPrefWidth(40);
        nameColumn.setPrefWidth(120);
        xColumn.setPrefWidth(80);
        yColumn.setPrefWidth(80);
        heightColumn.setPrefWidth(80);
        widthColumn.setPrefWidth(80);
        ownerColumn.setPrefWidth(40);
        createdColumn.setPrefWidth(250);


        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        xColumn.setCellValueFactory(new PropertyValueFactory<>("x"));
        yColumn.setCellValueFactory(new PropertyValueFactory<>("y"));
        heightColumn.setCellValueFactory(new PropertyValueFactory<>("height"));
        widthColumn.setCellValueFactory(new PropertyValueFactory<>("width"));
        ownerColumn.setCellValueFactory(new PropertyValueFactory<>("ownerId"));
        createdColumn.setCellValueFactory(new PropertyValueFactory<>("creationDate"));

        setMinWidth(600);
        setMinHeight(300);

        getColumns().add(idColumn);
        getColumns().add(nameColumn);
        getColumns().add(xColumn);
        getColumns().add(yColumn);
        getColumns().add(heightColumn);
        getColumns().add(widthColumn);
        getColumns().add(ownerColumn);
        getColumns().add(createdColumn);
        initLocalizedData();
    }

    public void initLocalizedData() {
        nameColumn.setText(bundle.getString("main.rooms-table.name-column-text"));
        heightColumn.setText(bundle.getString("main.rooms-table.height-column-text"));
        widthColumn.setText(bundle.getString("main.rooms-table.width-column-text"));
        ownerColumn.setText(bundle.getString("main.rooms-table.owner-column-text"));
        createdColumn.setText(bundle.getString("main.rooms-table.created-column-text"));
        setPlaceholder(new Label(bundle.getString("main.creatures-table.empty-table-text")));
    }
}
