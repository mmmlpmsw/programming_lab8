package ru.n4d9.server.commands;

import ru.n4d9.Message;
import ru.n4d9.client.Room;
import ru.n4d9.server.Context;
import ru.n4d9.server.Controller;

import java.sql.*;
import java.time.ZoneOffset;

public class AddRoomCommand implements RequiresAuthorization, Command {
    @Override
    public Message resolve(Message message) throws SQLException {
        Controller controller = (Controller)Context.get("controller");
        Connection connection = controller.getConnection();
        Room room = (Room) message.getAttachment();

        PreparedStatement statement = connection.prepareStatement("insert into rooms values (?, ?, ?, ?, ?, ?, ?)");
        statement.setString(1, room.getName());
        statement.setInt(2, room.getHeight());
        statement.setInt(3, room.getWidth());
        statement.setInt(4, room.getX());
        statement.setInt(5, room.getY());
        statement.setTimestamp(6, new Timestamp(room.getCreationDate().toEpochSecond(ZoneOffset.UTC) * 1000L));
        statement.setInt(7, message.getUserid());

        ResultSet resultSet = statement.executeQuery();
        resultSet.next();
        int room_id = resultSet.getInt("id");

        for (Room.Thing thing : room.getShelf()) {
            statement = connection.prepareStatement("insert into things values (?, ?, ?)");
            statement.setInt(1, room_id);
            statement.setString(2, thing.getName());
            statement.setInt(3, thing.getThingcount());

            statement.execute();
        }

        return new Message("Комната " + room.getName() + " успешно добавлена.");
    }
}
