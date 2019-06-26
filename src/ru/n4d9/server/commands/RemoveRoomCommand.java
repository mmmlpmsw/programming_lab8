package ru.n4d9.server.commands;

import ru.n4d9.Message;
import ru.n4d9.Utils.Utilities;
import ru.n4d9.client.Room;
import ru.n4d9.server.Context;
import ru.n4d9.server.Controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RemoveRoomCommand implements RequiresAuthorization, Command {
    @Override
    public Message resolve(Message message) throws SQLException {
        Controller controller = (Controller) Context.get("controller");
        Connection connection = controller.getConnection();
        Room room = (Room) message.getAttachment();

        controller.removeRoomFromMirror(room, message.getUserid());

        PreparedStatement statement = connection.prepareStatement(
                "select * from " + "users where id = ? and email = ? and password_hash = ?"
        );
        statement.setInt(1, message.getUserid());
        statement.setString(2, message.getLogin());
        statement.setBytes(3, Utilities.hashPassword(message.getPassword() + Utilities.getPasswordSalt()));
        ResultSet resultSet = statement.executeQuery();
        if (!resultSet.next()) return new Message("Вы не авторизованы. Чтобы войти, введите login.");

        statement = connection.prepareStatement("select id from rooms where name = ? and height = ?" +
                "and width = ? and x = ? and y = ? and user_id = ?");
        statement.setString(1, room.getName());
        statement.setInt(2, room.getHeight());
        statement.setInt(3, room.getWidth());
        statement.setInt(4, room.getX());
        statement.setInt(5, room.getY());
        statement.setInt(6, message.getUserid());
        resultSet = statement.executeQuery();

        int removed = 0;
        while (resultSet.next()) {
            if (Controller.removeRoomById(resultSet.getInt("id"), connection))
                removed++;
        }

        return new Message("Удалено " + removed + " комнат.");


    }

}
