package ru.n4d9.server.commands;

import ru.n4d9.Utils.Message;

import javax.mail.MessagingException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;

/**
 *
 */
public interface Command {
    Message resolve(Message message) throws SQLException, GeneralSecurityException, MessagingException;
}
