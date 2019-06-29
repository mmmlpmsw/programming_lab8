package ru.n4d9.server.commands;

import ru.n4d9.Utils.Message;
import ru.n4d9.Utils.StringEntity;
import ru.n4d9.Utils.Utilities;
import ru.n4d9.client.Room;
import ru.n4d9.server.ClientPool;
import ru.n4d9.server.Context;
import ru.n4d9.server.Controller;
import ru.n4d9.server.FileLoader;

import javax.mail.MessagingException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.ArrayList;

public class LoadCommand implements RequiresAuthorization, Command {
    @Override
    public Message resolve(Message message) throws SQLException, GeneralSecurityException, MessagingException {
        Controller controller = (Controller) Context.get("controller");
        ClientPool clientPool = (ClientPool)Context.get("clientpool");

        if (!message.hasAttachment()) {
            return new Message("WRONG");
        }
        try {
            if (!(message.getAttachment() instanceof StringEntity)) {
                return new Message("WRONG");
            }
            ArrayList<Room> rooms = Utilities.getRoomsFromJSON(FileLoader.getFileContent(((StringEntity) message.getAttachment()).getString()));
            for (Room room : rooms) {
                controller.addRoom(room, message.getUserid());
            }

            Message response = new Message("room_load", rooms);
            response.setSourcePort(message.getSourcePort());
            response.setLogin(message.getLogin());
            response.setPassword(message.getPassword());
            response.setUserid(message.getUserid());

            clientPool.sendAll(response);
            return null;

        } catch (Exception e) {
            e.getMessage();
            return new Message("INTERNAL_ERROR");
        }

    }
}
