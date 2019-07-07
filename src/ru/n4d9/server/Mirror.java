package ru.n4d9.server;

import ru.n4d9.Utils.Message;
import ru.n4d9.client.Room;
import ru.n4d9.server.commands.Command;

import java.security.acl.LastOwnerException;
import java.util.ArrayList;
import java.util.List;

public class Mirror implements ContextFriendly {
    private Controller controller;
    private ClientPool clientPool;
    private Logger logger;

    private ArrayList<Room> rooms;

    Mirror(Controller controller, Logger logger) {
        this.controller = controller;
        this.logger = logger;
        rooms = new ArrayList<>();
        rooms = getRooms();
        new Thread(this::sendState).start();
    }

    /**
     * метод для получения текущего состояния коллекции
     * @return список содержимого базы данных
     */
    public ArrayList<Room> getRooms() {
        if (rooms.size() == 0) {
            rooms = controller.getAllRoomsToMirror();
            logger.verbose("Сейчас в коллекции есть " + rooms.size() + " комнат: " + rooms.toString());
            return rooms;
        } else return rooms;
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
            if (r.getId() == room.getId()) {
                logger.verbose("Было: " + r.toString());
                r.setFromRoomModel(room);
                break;
            }
            break;
        }
        logger.verbose("Стало: " + room.toString());
    }

    private void sendState() {
        Thread thread = Thread.currentThread();
        while (true) {
            clientPool.sendAll(new Message("collection_state", rooms));
            try {
                thread.sleep(2000);
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
