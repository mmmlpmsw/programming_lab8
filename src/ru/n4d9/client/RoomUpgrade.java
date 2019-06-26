package ru.n4d9.client;

import java.io.Serializable;

/**
 * класс, позволяющий передавать комнаты вместе с информацией о добавившем их пользователе
 */
public class RoomUpgrade implements Serializable {

    private int user_id;
    private Room room;

    public RoomUpgrade (Room room, int user_id){
        setUser_id(user_id);
        setRoom(room);
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

}
