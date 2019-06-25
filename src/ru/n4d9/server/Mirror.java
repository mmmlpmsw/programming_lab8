package ru.n4d9.server;

import ru.n4d9.client.Room;

import java.util.ArrayList;
import java.util.List;

public class Mirror implements ContextFriendly {

    private Controller controller;

    private ArrayList<Room> rooms;

    Mirror() {
        rooms = new ArrayList<>();
    }

    public List<Room> getRooms() {
        return null;
    }

    public void roomAdded(Room room) {
        rooms.add(room);
    }

    public void roomRemoved(Room room) {
        rooms.remove(room);
    }

    public void collectionReloaded() {

    }


    @Override
    public void onContextReady() {
        controller = (Controller) Context.get("controller");
    }
}
