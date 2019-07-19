package ru.n4d9.server;

import ru.n4d9.Utils.Message;
import ru.n4d9.client.Room;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;

public class Mirror implements ContextFriendly {
    private Controller controller;
    private ClientPool clientPool;
    private Logger logger;

//    private ArrayList<Room> rooms;
    private HashSet<Room> rooms;

    Mirror(Controller controller, Logger logger) {
        this.controller = controller;
        this.logger = logger;
//        rooms = new ArrayList<>();
        rooms = new HashSet<>();
        rooms = getRooms();
        new Thread(this::sendState).start();
    }

    /**
     * метод для получения текущего состояния коллекции
     * @return список содержимого базы данных
     */
//    public ArrayList<Room> getRooms() {
//        if (rooms.size() == 0) {
//            rooms = controller.getAllRoomsToMirror();
//            logger.verbose("Сейчас в коллекции есть " + rooms.size() + " комнат: " + rooms.toString());
//            return rooms;
//        } else return rooms;
//    }

    public HashSet<Room> getRooms() {
        try {
            ResultSet resultSet = controller.getConnection().createStatement().executeQuery("select * from rooms");
            HashSet<Room> creatureModels = new HashSet<>();

            while (resultSet.next())
                creatureModels.add(Room.fromResultSet(resultSet));

            return creatureModels;
        } catch (SQLException e) {e.getMessage();}
        return null;
    }

    void roomAdded(Room room) {
        rooms.add(room);
        logger.verbose("Добавлена комната: " + room.toString());
        logger.verbose("Сейчас в коллекции " + rooms.size() + " комнат.");
    }

    void roomRemoved(Room room) {
        rooms.remove(room);
        logger.verbose("Удалена комната: " + room.toString());
        logger.verbose("Сейчас в коллекции " + rooms.size() + " комнат.");
    }

    void roomModified (Room room) {
        logger.verbose("Внесены изменения в комнату " + room.getId());
        for (Room r : rooms) {
            if (room.getId() == r.getId()) {

                logger.verbose("Было: " + r.toString());
                r.setFromRoomModel(room);
                logger.verbose("Стало: " + r.toString());
                logger.verbose("Текущее состояние коллекции: " + rooms.toString());
                break;
            }

        }

    }

    private void sendState() {
        while (true) {
            clientPool.sendAll(new Message("collection_state", rooms));
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {}

        }
    }

    @Override
    public void onContextReady() {
        clientPool = (ClientPool) Context.get("clientpool");
        logger = (Logger) Context.get("logger");
//        controller = (Controller) Context.get("controller");
    }
}
