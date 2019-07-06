package ru.n4d9.server.commands;

import ru.n4d9.Utils.Message;
import ru.n4d9.Utils.Utilities;
import ru.n4d9.client.Room;
import ru.n4d9.server.ClientPool;
import ru.n4d9.server.Context;
import ru.n4d9.server.Controller;

import javax.mail.MessagingException;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;


public class RemoveGreaterCommand implements RequiresAuthorization, Command{

    @Override
    public Message resolve(Message message) throws SQLException, GeneralSecurityException, MessagingException {
        Controller controller = (Controller) Context.get("controller");
        Connection connection = controller.getConnection();
        ClientPool clientPool = (ClientPool)Context.get("clientpool");

        Room room = (Room) message.getAttachment();

        PreparedStatement statement = connection.prepareStatement(
                "select id from rooms where user_id = ? and name > ?"
        );
        statement.setInt(1, message.getUserid());
        statement.setString(2, room.getName());

        ResultSet resultSet = statement.executeQuery();
        ArrayList<Room> rooms = new ArrayList<>();

        while (resultSet.next()) {
            rooms.add(Controller.removeRoomById(resultSet.getInt("id"), connection));

        }

        for (Room r : rooms) {
            controller.removeRoomFromMirror(r, message.getUserid());
        }

        clientPool.sendAll(new Message("rooms_removed", rooms));

        return null;
    }


}
