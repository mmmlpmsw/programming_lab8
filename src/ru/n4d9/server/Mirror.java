package ru.n4d9.server;

import ru.n4d9.client.Room;

import java.util.ArrayList;
import java.util.List;

public class Mirror implements ContextFriendly {
    //todo Mirror
    private Controller controller = (Controller) Context.get("controller");
    private ClientPool clientPool = (ClientPool) Context.get("clientpool");

    private ArrayList<Room> rooms;

    Mirror() {
        rooms = new ArrayList<>();
    }

    /**
     * метод для получения текущего состояния коллекции
     * @return список содержимого базы данных
     */
    public List<Room> getRooms() {
        if (rooms.size() == 0) {
            rooms = controller.getAllRoomsToMirror();
            return rooms;
        } else return rooms;
    }

    public void roomAdded(Room room, int user_id) {
        rooms.add(room);
//        clientPool.sendAll(new Message("room_added", new RoomUpgrade(room, user_id)));

    }

    public void roomRemoved(Room room, int user_id) {
        rooms.remove(room);
//        clientPool.sendAll(new Message("room_removed", new RoomUpgrade(room, user_id)));

    }


    @Override
    public void onContextReady() {
        controller = (Controller) Context.get("controller");
    }
}
