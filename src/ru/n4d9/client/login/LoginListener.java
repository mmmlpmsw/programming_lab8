package ru.n4d9.client.login;

import ru.n4d9.client.Room;

import java.util.ArrayList;

public interface LoginListener {
    public void onLogin(int id, String username, String login, String password, ArrayList<Room> rooms, String color);
}
