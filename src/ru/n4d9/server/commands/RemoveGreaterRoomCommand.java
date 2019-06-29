package ru.n4d9.server.commands;

import ru.n4d9.Utils.Message;
import ru.n4d9.Utils.Utilities;
import ru.n4d9.client.Room;
import ru.n4d9.server.Context;
import ru.n4d9.server.Controller;

import javax.mail.MessagingException;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class RemoveGreaterRoomCommand implements RequiresAuthorization, Command{

    @Override
    public Message resolve(Message message) throws SQLException, GeneralSecurityException, MessagingException {
        Controller controller = (Controller) Context.get("controller");
        Connection connection = controller.getConnection();
        Room room = (Room) message.getAttachment();

        PreparedStatement statement = connection.prepareStatement(
                "select count(*) from " + "users where id = ? and email = ? and password_hash = ?"
        );
        statement.setInt(1, message.getUserid());
        statement.setString(2, message.getLogin());
        statement.setBytes(3, Utilities.hashPassword(message.getPassword() + Utilities.getPasswordSalt()));

        ResultSet resultSet = statement.executeQuery();
        if (!resultSet.next()) return new Message("Вы не авторизованы. Чтобы войти, введите login.");

        statement = connection.prepareStatement(
                "select id from rooms where user_id = ? and name > ?"
        );
        statement.setInt(1, message.getUserid());
        statement.setString(2, room.getName());

        resultSet = statement.executeQuery();

        int removed = 0;
        while (resultSet.next()) {
            if (Controller.removeRoomById(resultSet.getInt("id"), connection))
                removed++;
        }

        return new Message("Удалено " + removed + " комнат.");
    }


}
