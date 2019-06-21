package ru.n4d9.Utils;

import java.io.Serializable;

/**
 * класс для сериализации части сообщения
 */
public class StringEntity implements Serializable {
    private String string;
    private String[] arguments;

    public String getString() { return string; }
    public void setString(String string) { this.string = string; }

    public String[] getArguments() {
        return arguments;
    }

    public void setArguments(String[] arguments) {
        this.arguments = arguments;
    }

    public StringEntity set(String string) {setString(string); return this;}

    public StringEntity set(String[] string) {setArguments(string); return this;}
}