package ru.n4d9.server.commands;

import ru.n4d9.Utils.Message;
import ru.n4d9.Utils.Utilities;
import ru.n4d9.client.Room;
import ru.n4d9.server.Context;
import ru.n4d9.server.Controller;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import static ru.n4d9.Utils.Utilities.colorize;

public class LoginCommand implements Command {
    //todo вернуть клиенту id
    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static SecureRandom rnd = new SecureRandom();

    @Override
    public Message resolve(Message message) throws SQLException, GeneralSecurityException {
        Controller controller = (Controller) Context.get("controller");
        Connection connection = controller.getConnection();

        Properties properties = (Properties)message.getAttachment();

        String email = properties.getProperty("email", "");
        String password = properties.getProperty("password", "");

        PreparedStatement statement = connection.prepareStatement("select * from users " +
                "where email = ? and password_hash = ?");
        byte[] bytes = Utilities.hashPassword(password + Utilities.getPasswordSalt());

        statement.setString(1, email);
        statement.setBytes(2, bytes);
        statement.execute();
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            int userid = resultSet.getInt("id");
            String username = resultSet.getString("name");

            statement = connection.prepareStatement("select * from rooms");
            ResultSet set = statement.executeQuery();
            //set.next();
            ArrayList<Room> rooms = new ArrayList<>();
            while (set.next()) {
                int roomid = set.getInt("id");
                String name = set.getString("name");
                double height = set.getDouble("height");
                double width = set.getDouble("width");
                double x = set.getDouble("x");
                double y = set.getDouble("y");
                Date creationdate = set.getDate("creationdate");
                Room room = new Room(width, height, x, y, name);
                room.setId(roomid);
                room.setD(creationdate);  //todo
                room.setOwnerId(set.getInt("user_id"));
                rooms.add(room);

            }
            Message result = new Message("OK", rooms);
            result.setUserid(userid);
            result.setLogin(email);
            result.setPassword(password);
            result.setUsername(username);
            return result;
        } else {
            return new Message(colorize("WRONG"));
        }

    }
}
