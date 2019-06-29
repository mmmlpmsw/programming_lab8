package ru.n4d9.server;

import ru.n4d9.Utils.Message;
import ru.n4d9.Utils.Utilities;
import ru.n4d9.server.commands.Command;
import ru.n4d9.server.commands.RequiresAuthorization;

import javax.mail.MessagingException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;

//todo answers COMMAND_NOT_FOUND, AUTH_FAILED, WRONG, file_empty

/**
 * класс, выполняющий дальнейшую обработку команды
 */
public class RequestResolver extends Thread implements ContextFriendly {

    private Controller controller;
    private Logger logger;

    @Override
    public void onContextReady() {
        controller = (Controller)Context.get("controller");
        logger = (Logger) Context.get("logger");
    }

    /**
     * Делает вторичную обработку
     * @param connector коннектор отправителя
     * @param message сообщение отправителя
     */
    public void resolve(ClientPool.ClientConnector connector, Message message) {
        try {
            Class clazz = Class.forName("ru.n4d9.server.commands." + Utilities.toCamelCase(message.getText()) + "Command");
            Object object = clazz.newInstance();
            Command command = (Command) object;
            Message response = new Message("COMMAND_NOT_FOUND");

            if (command instanceof RequiresAuthorization) {
                if (controller.isUserAuthorized(message)) {
                    response = command.resolve(message);
                    if (response != null)
                        connector.send(response);
                } else {
                    response = new Message("AUTH_FAILED");
                    connector.send(response);
                }
            } else
                response = command.resolve(message);
            if (response != null)
                connector.send(response);
        } catch (ClassCastException e) {
            logger.warn("В пакете находится класс неверного формата");
            connector.send(new Message("INTERNAL_ERROR"));
        } catch (ClassNotFoundException e) {
            connector.send(new Message("COMMAND_NOT_FOUND"));
        } catch (InstantiationException | IllegalAccessException e) {
            logger.warn("В пакете нашелся класс неверного формата " + e.toString());
        } catch (SQLException | MessagingException| GeneralSecurityException e) {
            logger.err(e.toString());
            e.printStackTrace();
            connector.send(new Message("INTERNAL_ERROR"));
        }

    }

}