package ru.n4d9.server.commands;

import ru.n4d9.Message;
import ru.n4d9.Utils.Utilities;
import ru.n4d9.server.Context;
import ru.n4d9.server.Controller;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

public class RegisterCommand implements Command {

    @Override
    public Message resolve(Message message) throws SQLException, MessagingException {

        Controller controller = (Controller)Context.get("controller");
        Connection connection = controller.getConnection();

        Properties properties = (Properties)message.getAttachment();

        String name = properties.getProperty("name", "");
        String email = properties.getProperty("email", "");
        if (!Utilities.isValidEmailAddress(email)) {
            return new Message("WRONG_EMAIL");
        }

        try {
            PreparedStatement statement = connection.prepareStatement("select email from users WHERE email = ?");
            statement.setString(1, email);

            if (statement.executeQuery().next())
                return new Message("ALREADY_REGISTERED");

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

            return new Message("OK");
        }  catch (AddressException e) {
            return new Message("INTERNAL_ERROR");
        }

    }

}
