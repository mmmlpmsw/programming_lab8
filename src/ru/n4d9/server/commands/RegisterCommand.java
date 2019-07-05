package ru.n4d9.server.commands;

import ru.n4d9.Utils.Message;
import ru.n4d9.Utils.Utilities;
import ru.n4d9.server.Context;
import ru.n4d9.server.Controller;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

public class RegisterCommand implements Command {

    @Override
    public Message resolve(Message message) throws SQLException, MessagingException {

        Controller controller = (Controller)Context.get("controller");
        Connection connection = controller.getConnection();

        Message response = new Message(null);
        response.setSourcePort(message.getSourcePort());
        response.setLogin(message.getLogin());
        response.setPassword(message.getPassword());
        response.setUserid(message.getUserid());

        Properties properties = (Properties)message.getAttachment();

        String name = properties.getProperty("name", "");
        String email = properties.getProperty("email", "");
        if (!Utilities.isValidEmailAddress(email)) {
            response.setText("WRONG_EMAIL");
            return response;
        }

        try {
            PreparedStatement statement = connection.prepareStatement("select email from users WHERE email = ?");
            statement.setString(1, email);

            if (statement.executeQuery().next()) {
                response.setText("ALREADY_REGISTERED");
                return response;
            }

            String password = Utilities.randomString(9);

            controller.sendEMail(email, "Подтверждение регистрации",
                    "Ваш пароль " +
                            password
            );
            PreparedStatement statement1 = connection.prepareStatement("insert into users " +
                    "(name, email, password_hash) values (?, ?, ?)");

            statement1.setString(1, name);
            statement1.setString(2, email);
            statement1.setBytes(3, Utilities.hashPassword(password + Utilities.getPasswordSalt()));

            statement1.execute();

            response.setText("OK");
            return response;

        }  catch (AddressException e) {
            response.setText("INTERNAL_ERROR");
            return response;
        }

    }

}
