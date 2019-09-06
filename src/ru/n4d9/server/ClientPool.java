package ru.n4d9.server;

import ru.n4d9.Utils.Message;
import ru.n4d9.transmitter.Sender;
import ru.n4d9.transmitter.SenderAdapter;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ClientPool implements ContextFriendly {

    private ArrayList<ClientConnector> connectors;
    private ArrayList<ClientConnector> subscribedConnectors;
    private Logger logger;
    private RequestResolver resolver;

    ClientPool() {
        connectors = new ArrayList<>();
        subscribedConnectors = new ArrayList<>();
    }

    ArrayList<ClientConnector> getConnectors() {
        return connectors;
    }

    public ArrayList<ClientConnector> getSubscribedConnectors() {
        return subscribedConnectors;
    }

    @Override
    public void onContextReady() {
        logger = (Logger) Context.get("logger");
        resolver = (RequestResolver) Context.get("request_resolver");
    }

    /**
     * Отправляет сообщение всем подключенным клиентам
     */
    public synchronized void sendAll(Message message) {
        if (!message.getText().equals("collection_state"))
            logger.verbose("Рассылка сообщения " + message + " подписанным коннекторам (" + connectors.size() + ")");
//        for (int i = 0; i < connectors.size(); i ++) {
        for (int i = 0; i < subscribedConnectors.size(); i ++) {
            connectors.get(i).send(message);

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
//        connectors.add(connector);
        if (message.getUserid() == null)
            logger.log("Пришел запрос от неавторизованного клиента " + message.getText());
        logger.log("Пришел запрос от клиента " + address.getHostAddress() + ":" + port + " "  + message.getText());

        switch (message.getText()) {
            case "disconnect":
                logger.verbose("Отсоединение коннектора: " + connector);
                boolean a = connectors.remove(connector);
                if (a) {
                    logger.verbose("yes");
                }

                else logger.verbose("no");
                logger.log("Клиент " + address.getHostAddress() + ":" + port + " отсоединился.");

                subscribedConnectors.remove(connector);
                break;

            case "subscribe": {
                subscribedConnectors.add(connector);
            }
            default:
                logger.verbose("Вызов ресолвера для сообщения " + message + ", ответ коннектору " + connector);
                new Thread(() -> resolver.resolve(connector, message)).start();
//                resolver.resolve(connector, message);
                break;
        }

                try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        connectors.add(connector);
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
        public synchronized void send(Message m) {
            if (!m.getText().equals("collection_state"))
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
