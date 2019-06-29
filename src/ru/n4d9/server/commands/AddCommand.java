package ru.n4d9.server.commands;

import ru.n4d9.Utils.Message;
import ru.n4d9.client.Room;
import ru.n4d9.server.ClientPool;
import ru.n4d9.server.Context;
import ru.n4d9.server.Controller;

import java.sql.*;

public class AddCommand implements RequiresAuthorization, Command {
    @Override
    public Message resolve(Message message) throws SQLException {
        Controller controller = (Controller)Context.get("controller");
        ClientPool clientPool = (ClientPool)Context.get("clientpool");
        Room room = (Room) message.getAttachment();

        room = controller.addRoom(room, message.getUserid());
        Message response = new Message("room_added", room);
        response.setSourcePort(message.getSourcePort());
        response.setLogin(message.getLogin());
        response.setPassword(message.getPassword());
        response.setUserid(message.getUserid());

        clientPool.sendAll(response);

        return null;
    }
}
