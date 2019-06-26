package ru.n4d9.server.commands;

import ru.n4d9.Message;
import ru.n4d9.client.Room;
import ru.n4d9.server.Context;
import ru.n4d9.server.Controller;

import java.sql.*;

public class AddRoomCommand implements RequiresAuthorization, Command {
    @Override
    public Message resolve(Message message) throws SQLException {
        Controller controller = (Controller)Context.get("controller");
        Connection connection = controller.getConnection();
        Room room = (Room) message.getAttachment();
        controller.addRoomToMirror(room, message.getUserid());

        controller.addRoom(room, message.getUserid());

        return new Message("Комната " + room.getName() + " успешно добавлена.");
    }
}
