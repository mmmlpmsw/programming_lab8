package ru.n4d9.server;

import ru.n4d9.Message;
import ru.n4d9.json.JSONParseException;
import ru.n4d9.transmitter.Receiver;
import ru.n4d9.transmitter.ReceiverListener;
import ru.n4d9.transmitter.Sender;
import ru.n4d9.transmitter.SenderAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;

public class Server {
    private static Receiver receiver;
    private static Logger logger;
    private static ClientPool clientPool;

    public static void main(String[] args) {
        ServerConfig config;

        ClientPool clientPool = new ClientPool();
        Context.set("clientpool", clientPool);

        Controller controller = new Controller();
        Context.set("controller", controller);

        RequestResolver requestResolver = new RequestResolver();
        Context.set("request_resolver", requestResolver);

        try {
            if (args.length != 0) {

                config = ServerConfig.fromFile(args[0]);
                Context.set("config", config);

                Logger logger = initLogger(config);
                logger.log("Сервер слушает порт " + config.getPort() + "...");
                Context.set("logger", logger);

                receiver = new Receiver(config.getPort(), false);
                receiver.setListener(generateListener());
                receiver.startListening();

                clientPool.onContextReady();
                controller.onContextReady();


            } else {
                Controller.sendDown("Не указан файл настроек.");
            }
        } catch (IOException e) {
            logger.err("Не получилось запустить сервер: " + e.toString());
        } catch (JSONParseException e) {
            logger.err("Проблема с JSON:");
            logger.err(e.getMessage());
        }
    }

/*
    private static void processRequest(int requestID, byte[] data, InetAddress address) {
        Message message;
        try {
            message = Message.deserialize(data);
            String text = message.getText();
            System.out.println("Пришёл запрос: " + text);
            System.out.println("Порт клиента: " + message.getSourcePort());
            Message response = new Message("Я получил сообщение: " + text + "\nВаш request id = " + requestID);
            respond(response.serialize(), message.getSourcePort(), address);
        } catch (ClassNotFoundException | IOException e) {
            logger.err("Не получилось обработать запрос: " + e.toString());
        }
    }
*/

    private static void respond(byte[] data, int port, InetAddress address) {
        Sender.send(data, address, port, false, new SenderAdapter() {
            @Override
            public void onSuccess() {
                System.out.println("Ответ отправился");
            }

            @Override
            public void onError(String message) {
                System.out.println("Не получилось ответить на запрос: " + message);
            }
        });
    }

    private static ReceiverListener generateListener() {
        return new ReceiverListener() {
            @Override
            public void received(int requestID, byte[] data, InetAddress address, int port) {
                clientPool = (ClientPool) Context.get("clientpool");

                Message message = null;
                try {
                    message = Message.deserialize(data);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                clientPool.process(requestID, message, address, port);
            }

            @Override
            public void received(int requestID, byte[] data, InetAddress address) {
            }

            @Override
            public void exceptionThrown(Exception e) {
                logger.log("Не получилось принять запрос: " + e.toString());
            }
        };
    }

    /**
     * Инициализирует логгер для сервера.
     * @param config файл настроек сервера
     * @return логгер
     */
    private static Logger initLogger(ServerConfig config) {
        try {
            if (new File(config.getOutLogFile()).getParentFile().mkdirs())
                logger.log("Автоматически созданы директории для создания файла стандартного вывода: " + config.getOutLogFile());
            if (new File(config.getErrLogFile()).getParentFile().mkdirs())
                logger.log("Автоматически созданы директории для создания файла вывода ошибок: " + config.getErrLogFile());

            return new Logger(
                    new PrintStream(System.out, true, "UTF-8"),
                    new PrintStream(new FileOutputStream(config.getOutLogFile()), true, "UTF-8"),
                    new PrintStream(System.err, true, "UTF-8"),
                    new PrintStream(new FileOutputStream(config.getErrLogFile()), true, "UTF-8")
            );
        } catch (SecurityException e) {
            Controller.sendDown("Ошибка безопасности: " + e);
        } catch (IOException e) {
            Controller.sendDown("Ошибка записи логов: " + e.getMessage());
        }
        return null;
    }

}
