package ru.n4d9.server.commands;

import ru.n4d9.Utils.Message;
import ru.n4d9.client.Room;
import ru.n4d9.server.ClientPool;
import ru.n4d9.server.Context;
import ru.n4d9.server.Controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RemoveCommand implements RequiresAuthorization, Command {
    @Override
    public Message resolve(Message message) throws SQLException {
        Controller controller = (Controller) Context.get("controller");
        ClientPool clientPool = (ClientPool)Context.get("clientpool");
        Connection connection = controller.getConnection();
        int room_id = (int)message.getAttachment();

        //controller.removeRoomFromMirror(room, message.getUserid());

        if (room_id == 0)
            return new Message("BAD_REQUEST");

        PreparedStatement statement = connection.prepareStatement(
                "select * from rooms where id = ? and user_id = ?"
        );
        statement.setInt(1, room_id);
        statement.setInt(2, message.getUserid());
        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            Room model = Room.fromResultSet(resultSet);

            statement = connection.prepareStatement("delete from rooms where id = ?;");
            statement.setInt(1, room_id);
            statement.execute();

            clientPool.sendAll(new Message("room_removed", model));
        }
        return null;
    }

}
