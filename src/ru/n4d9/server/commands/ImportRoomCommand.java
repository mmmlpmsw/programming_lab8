package ru.n4d9.server.commands;

import ru.n4d9.Message;
import ru.n4d9.Utils.StringEntity;
import ru.n4d9.Utils.Utilities;
import ru.n4d9.client.Room;
import ru.n4d9.server.Context;
import ru.n4d9.server.Controller;

import java.sql.*;
import java.util.ArrayList;

public class ImportRoomCommand implements RequiresAuthorization, Command {

    @Override
    public Message resolve(Message message) throws SQLException {
        Controller controller = (Controller) Context.get("controller");

        if (!message.hasAttachment()) {
            return new Message("Имя файла не указано.\n" +
                    "Введите help для получения справки.");
        }
        if (!(message.getAttachment() instanceof StringEntity)) {
            return new Message("Клиент отправил запрос в неверном формате : аргумент сообщения должен быть строкой.");
        }
        ArrayList<Room> rooms = Utilities.getRoomsFromJSON(((StringEntity) message.getAttachment()).getString());
        try {
            for (Room room : rooms) {
                controller.addRoom(room, message.getUserid());
            }
            return new Message("Загрузка " + rooms.size() + " комнат прошла успешно.");
        } catch (NullPointerException e) {
            return new Message("Файл пуст.");
        }

    }
}
