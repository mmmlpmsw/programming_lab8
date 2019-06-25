package ru.n4d9.client.login;

import java.io.Serializable;

public interface LoginListener {
    public void onLogin(int id, String login, String password);
}
