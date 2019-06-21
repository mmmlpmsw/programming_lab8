package ru.n4d9.server.commands;

import ru.n4d9.Message;
import ru.n4d9.server.Controller;

import javax.mail.MessagingException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;

public interface Command {
    Message resolve(Message message) throws SQLException, GeneralSecurityException, MessagingException;
}
