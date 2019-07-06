package ru.n4d9.server.commands;

import ru.n4d9.Utils.Message;
import ru.n4d9.client.Room;
import ru.n4d9.server.ClientPool;
import ru.n4d9.server.Context;
import ru.n4d9.server.Controller;

import javax.mail.MessagingException;
import java.security.GeneralSecurityException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ModifyCommand implements Command, RequiresAuthorization {
    @Override
    public Message resolve(Message message) throws SQLException, GeneralSecurityException, MessagingException {
        Controller controller = (Controller) Context.get("controller");
        ClientPool clientPool = (ClientPool)Context.get("clientpool");

        Room model = (Room)message.getAttachment();

        if (model.getName().length() == 0 || model.getName().length() > 32)
            return new Message("BAD_REQUEST");
        if (model.getX() < 0 || model.getY() < 0 || model.getX() > 1000 || model.getY() > 1000)
            return new Message("BAD_REQUEST");

        PreparedStatement statement = controller.getConnection().prepareStatement(
                "update rooms set name = ?, x = ?, y = ?, height = ?, width = ?, creationdate = creationdate where id = ? and user_id = ?"
        );
        statement.setString(1, model.getName());
        statement.setDouble(2, model.getX());
        statement.setDouble(3, model.getY());
        statement.setDouble(4, model.getHeight());
        statement.setDouble(5, model.getWidth());
        statement.setInt(6, model.getId());
        statement.setInt(7, message.getUserid());
        statement.execute();

        statement = controller.getConnection().prepareStatement(
                "select * from rooms where id = ? and user_id = ?"
        );
        statement.setInt(1, model.getId());
        statement.setInt(2, message.getUserid());
        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            // Создаётся ещё одна модель, чтобы не использовать id владельца
            // и не модифицировать время создания
            Room room = Room.fromResultSet(resultSet);

            controller.modifyRoomInMirror(room);

            Message response = new Message("room_modified", room);
            response.setSourcePort(message.getSourcePort());
            response.setLogin(message.getLogin());
            response.setPassword(message.getPassword());
            response.setUserid(message.getUserid());

            clientPool.sendAll(response);
        }

        return null;
    }

}
