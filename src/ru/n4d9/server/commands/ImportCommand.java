package ru.n4d9.server.commands;

import ru.n4d9.Utils.Message;
import ru.n4d9.Utils.StringEntity;
import ru.n4d9.Utils.Utilities;
import ru.n4d9.client.Room;
import ru.n4d9.server.ClientPool;
import ru.n4d9.server.Context;
import ru.n4d9.server.Controller;

import java.sql.SQLException;
import java.util.ArrayList;

public class ImportCommand implements RequiresAuthorization, Command {

    @Override
    public Message resolve(Message message) throws SQLException {
        Controller controller = (Controller) Context.get("controller");
        ClientPool clientPool = (ClientPool)Context.get("clientpool");

        if (!message.hasAttachment()) {
            return new Message("WRONG");
        }
        if (!(message.getAttachment() instanceof StringEntity)) {
            return new Message("WRONG");
        }
        ArrayList<Room> rooms = Utilities.getRoomsFromJSON(((StringEntity) message.getAttachment()).getString());
        try {
            for (Room room : rooms) {
                controller.addRoom(room, message.getUserid());
            }

            Message response = new Message("rooms_import", rooms);
            response.setSourcePort(message.getSourcePort());
            response.setLogin(message.getLogin());
            response.setPassword(message.getPassword());
            response.setUserid(message.getUserid());
            clientPool.sendAll(response);
            return null;
        } catch (NullPointerException e) {
            return new Message("file_empty");
        }

    }
}
