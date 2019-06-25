package ru.n4d9.server.commands;

import ru.n4d9.Message;
import ru.n4d9.Utils.StringEntity;
import ru.n4d9.Utils.Utilities;
import ru.n4d9.client.Room;
import ru.n4d9.server.Context;
import ru.n4d9.server.Controller;

import javax.mail.MessagingException;
import java.security.GeneralSecurityException;
import java.sql.*;
import java.time.ZoneOffset;
import java.util.ArrayList;

public class ImportRoomCommand implements RequiresAuthorization, Command {

    @Override
    public Message resolve(Message message) throws SQLException, GeneralSecurityException, MessagingException {
        Controller controller = (Controller) Context.get("controller");
        Connection connection = controller.getConnection();

        if (!message.hasAttachment()) {
            return new Message("Имя файла не указано.\n" +
                    "Введите help для получения справки.");
        }
        if (!(message.getAttachment() instanceof StringEntity)) {
            return new Message("Клиент отправил запрос в неверном формате : аргумент сообщения должен быть строкой.");
        }
        ArrayList<Room> rooms = Utilities.getRoomsFromJSON(((StringEntity) message.getAttachment()).getString());
        try {
            for (Room room : rooms) {
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

            }
            return new Message("Загрузка " + rooms.size() + " комнат прошла успешно.");
        } catch (NullPointerException e) {
            return new Message("Файл пуст.");
        }

    }
}
