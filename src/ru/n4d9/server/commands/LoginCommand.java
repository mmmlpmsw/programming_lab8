package ru.n4d9.server.commands;

import ru.n4d9.Message;
import ru.n4d9.Utils.StringEntity;
import ru.n4d9.Utils.Utilities;
import ru.n4d9.client.Room;
import ru.n4d9.server.Context;
import ru.n4d9.server.Controller;
import ru.n4d9.server.Server;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static ru.n4d9.Utils.Utilities.colorize;

public class LoginCommand implements Command {

    static Session mailSession;

    @Override
    public Message resolve(Message message) throws SQLException, MessagingException {

        Controller controller = (Controller)Context.get("controller");
        Connection connection = controller.getConnection();

        if (!(message.getAttachment() instanceof StringEntity)) {
            return new Message("Клиент отправил данные в неверном формате");
        }
        StringEntity entityAttachment = (StringEntity) message.getAttachment();
        String[] attachment = entityAttachment.getArguments();
        if (attachment.length < 2) {
            return new Message("Клиент отправил неполные данные");
        }
        String name = attachment[0];
        String email = attachment[1];
        if (name.length() < 2) {
            return new Message("Вы ввели неверное имя.");
        }
        if (!Utilities.isValidEmailAddress(email)) {
            return new Message("Вы ввели некорректный e-mail.");
        }

        try {
            PreparedStatement statement = connection.prepareStatement("select email from users WHERE email = ?");
            statement.setString(1, email);

            if (statement.executeQuery().next())
                return new Message("Этот email уже зарегистрирован.");

            String password = Utilities.randomString(9);
            System.out.println(password);

            sendEMail(email, "Подтверждение регистрации",
                    "Ваш пароль " +
                            password
            );
            PreparedStatement statement1 = connection.prepareStatement("insert into users " +
                    "(name, email, password_hash) values (?, ?, ?)");

            statement1.setString(1, name);
            statement1.setString(2, email);
            statement1.setBytes(3, Utilities.hashPassword(password + Utilities.getPasswordSalt()));

            statement1.execute();
            statement.execute();

            return new Message(colorize("[[bright_green]]На указанный адрес электронной почты отправлен пароль.\n" +
                    "Введите login, чтобы выполнить вход.\n [[RESET]]"));
        }  catch (AddressException e) {
//                e.printStackTrace();
            return new Message("Вы ввели некорректный e-mail.");
        }

    }

    static void sendEMail(String to, String subject, String content) throws MessagingException {

        MimeMessage message = new MimeMessage(mailSession);
        message.setFrom("mmmlpmsw@protonmail.com");
        message.setRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject);
        message.setContent(content, "text/html; charset=utf-8");

        Transport.send(message);
    }
}
