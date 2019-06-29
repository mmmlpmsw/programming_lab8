package ru.n4d9.server;

import ru.n4d9.Utils.Message;
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

        Mirror mirror = new Mirror();
        Context.set("mirror", mirror);

        try {
            if (args.length != 0) {
                config = ServerConfig.fromFile(args[0]);
                Context.set("config", config);

                logger = initLogger(config);
                Context.set("logger", logger);

                logger.setShowVerbose(args.length == 2 && args[1].equals("dev"));

                receiver = new Receiver(config.getPort(), false);
                receiver.setListener(generateListener());
                receiver.startListening();

                logger.log("Сервер слушает порт " + config.getPort() + "...");

                clientPool.onContextReady();
                controller.onContextReady();
                requestResolver.onContextReady();
                mirror.onContextReady();
            } else {
                Controller.sendDown("Не указан файл настроек.");
            }
        } catch (IOException e) {
//            e.printStackTrace();
            logger.err("Не получилось запустить сервер: " + e.toString());
        } catch (JSONParseException e) {
            logger.err("Проблема с JSON:" + e.getMessage());
        }
    }

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

    /**
     * Создает слушатель приёмника, выполняющий запросы клиентов
     *
     * @return объект-слушатель
     */
    private static ReceiverListener generateListener() {
        logger.verbose("Создаётся слушатель запросов");
        return new ReceiverListener() {
            @Override
            public void received(int requestID, byte[] data, InetAddress address, int port) {
                clientPool = (ClientPool) Context.get("clientpool");

                logger.verbose("Пришли данные от " + address + ":" + port);
                try {
                    Message message = Message.deserialize(data);
                    logger.verbose("Пришло сообщение: " + message);
                    clientPool.process(requestID, message, address, port);
                } catch (IOException e) {
                    e.printStackTrace(); // todo handling
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void exceptionThrown(Exception e) {
                logger.warn("Не получилось принять запрос: " + e.toString());
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
                    new PrintStream(
                            new TeeOutputStream(
                                System.out,
                                new FileOutputStream(config.getOutLogFile(), true)),
                            true, "UTF-8"),
                    new PrintStream(
                            new TeeOutputStream(
                                System.err,
                                new FileOutputStream(config.getErrLogFile(), true)),
                            true, "UTF-8"
            ));
        } catch (SecurityException e) {
            Controller.sendDown("Ошибка безопасности: " + e);
        } catch (IOException e) {
            Controller.sendDown("Ошибка записи логов: " + e.getMessage());
        }
        return null;
    }

}
