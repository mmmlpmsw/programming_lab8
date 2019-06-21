package ru.n4d9.server.commands;

import ru.n4d9.Message;
import ru.n4d9.Utils.StringEntity;
import ru.n4d9.Utils.Utilities;
import ru.n4d9.server.Context;
import ru.n4d9.server.Controller;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static ru.n4d9.Utils.Utilities.colorize;

public class RegisterCommand implements Command {
    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static SecureRandom rnd = new SecureRandom();

    @Override
    public Message resolve(Message message) throws SQLException, GeneralSecurityException {
        Controller controller = (Controller) Context.get("controller");
        Connection connection = controller.getConnection();

        if (!(message.getAttachment() instanceof StringEntity)) {
            return new Message("Клиент отправил данные в неверном формате.");
        }
        StringEntity entityAttachment = (StringEntity) message.getAttachment();
        String[] attachment = entityAttachment.getArguments();
        if (attachment.length < 2) {
            return new Message("Клиент отправил неполные данные.");
        }
        String email = attachment[0];
        String password = attachment[1];

        PreparedStatement statement = connection.prepareStatement("select * from users " +
                "where email = ? and password_hash = ?");
        byte[] bytes = Utilities.hashPassword(password + Utilities.getPasswordSalt());

        statement.setString(1, email);
        statement.setBytes(2, bytes);
        statement.execute();
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            int userid = resultSet.getInt("id");
            String name = resultSet.getString("name");
            Message result = new Message(name + " , вход успешно выполнен.", new String[]{Integer.toString(userid), email, password});
            return result;
        } else {
            return new Message(colorize("[[RED]]Вход не выполнен: неверная пара email/пароль.[[RESET]]"));
        }

    }
}
