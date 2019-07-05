package ru.n4d9.server;

import ru.n4d9.Utils.Message;
import ru.n4d9.transmitter.Sender;
import ru.n4d9.transmitter.SenderAdapter;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ClientPool implements ContextFriendly {

    private Set<ClientConnector> connectors;
    private Logger logger;
    private RequestResolver resolver;

    ClientPool() {
        connectors = new HashSet<>();
    }

    public Set<ClientConnector> getConnectors() {
        return connectors;
    }

    @Override
    public void onContextReady() {
        logger = (Logger) Context.get("logger");
        resolver = (RequestResolver) Context.get("request_resolver");
    }

    /**
     * Отправляет сообщение всем подключенным клиентам
     */
    public void sendAll(Message message) {
        logger.verbose("Рассылка сообщения " + message + " подписанным коннекторам (" + connectors.size() + ")");
        for (ClientConnector connector: connectors) {
            connector.send(message);
        }
    }

    /**
     * Принимает весь входящий трафик сервера.
     * Выполняет первичную обработку команды.
     * <p>
     * Если требуется, отправляет сообщение в {@link RequestResolver},
     * передавая ссылку на экземпляр {@link ClientConnector}, ассоциированный
     * с клиентом-отправителем. Если такого экземпляра нет, он создаётся.
     *
     * @param requestID идентификатор запроса
     * @param message   информация
     * @param address   адрес отправителя
     * @param port      порт отправителя
     */
    public void process(int requestID, Message message, InetAddress address, int port) {
        logger.verbose("Обработка запроса: " + message + " от " + address + ":" + port);

        ClientConnector connector = new ClientConnector(address, message.getSourcePort());
        connectors.add(connector);
        if (message.getUserid() == null)
            logger.log("Пришел запрос от неавторизованного клиента " + message.getText());
        logger.log("Пришел запрос от клиента " + address.getHostAddress() + ":" + port + " "  + message.getText());

        switch (message.getText()) {
            case "disconnect":
                logger.verbose("Отсоединение коннектора: " + connector);
                connectors.remove(connector);
                logger.log("Клиент " + address.getHostAddress() + ":" + port + " отсоединился.");
                break;

            default:
                logger.verbose("Вызов ресолвера для сообщения " + message + ", ответ коннектору " + connector);
//                new Thread(() -> resolver.resolve(connector, message)).start();
                resolver.resolve(connector, message);
                break;
        }
    }


    public class ClientConnector extends Thread {
        private InetAddress address;
        private int port;

        ClientConnector(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }

        /**
         * Отправляет сообщение клиенту, ассоциированному с этим коннектором
         *
         * @param m сообщение
         */
        public void send(Message m) {
            logger.verbose("Отправка сообщения " + m + " через коннектор " + this);
            try {
                Sender.send(m.serialize(), address, port, false, new SenderAdapter() {
                    @Override
                    public void onError(String message) {
                        logger.err("Не получилось отправить сообщение клиенту: " + message);
                        logger.verbose("Не получилось отправить сообщение " + m + " по коннектору " + this + ", ошибка: " + message);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                logger.err(e.getMessage());
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClientConnector that = (ClientConnector) o;
            return port == that.port &&
                    Arrays.equals(address.getAddress(), that.address.getAddress());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(address.getAddress()) + port;
        }

        @Override
        public String toString() {
            return String.format(
                    "ClientConnector [address = %s, port = %s]",
                    address, port
            );
        }
    }
}
