package ru.n4d9.server;

import ru.n4d9.Message;
import ru.n4d9.Utils.Utilities;
import ru.n4d9.client.Room;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.sql.*;
import java.util.Properties;

public class Controller implements ContextFriendly {

    private Connection connection;
    private Logger logger;
    private Mirror mirror;
    private Session mailSession;

    public Connection getConnection() {
        return connection;
    }

    @Override
    public void onContextReady() {
        logger = (Logger)Context.get("logger");
        mirror = (Mirror)Context.get("mirror");
        initDatabaseConnection();
        initTables();
        initEmail();

    }

    /**
     * Удаляет комнату по id
     *
     * @param id id комнаты, которую нужно удалить
     * @return true, если комната удалилась, во всех остальных случаях false
     */
    public static boolean removeRoomById(int id, Connection connection) throws SQLException {
        if (connection == null)
            return false;

        PreparedStatement statement = connection.prepareStatement(
                "delete from rooms where id = ?"
        );
        statement.setInt(1, id);
        int removed = statement.executeUpdate();
        statement = connection.prepareStatement(
                "delete from things where room_id = ?"
        );
        statement.setInt(1, id);
        statement.execute();
        return removed != 0;
    }

    /**
     * проверяет, авторизован ли пользователь, отправивший запрос
     * @param message запрос и информация он нем
     * @return <i>true</i>, если установлено, что пользователь авторизован, иначе <i>false</i>
     * @throws SQLException если произошла ошибка при обращении к БД
     */
    public boolean isUserAuthorized(Message message) throws SQLException {
        Controller controller = (Controller)Context.get("controller");
        Connection connection = controller.getConnection();
        PreparedStatement statement = connection.prepareStatement(
                "select * from users where email = ? and password_hash = ?"
        );
        statement.setString(1, message.getLogin());
        statement.setBytes(2, Utilities.hashPassword(message.getPassword() + Utilities.getPasswordSalt()));

        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            int userid = resultSet.getInt("id");
            if (userid != 0)
                    return true;
            return false;
        }
        return false;
    }


    public void addRoom(Room room){
        mirror.roomAdded(room);
    }

    public void removeRoom(Room room) {
        mirror.roomRemoved(room);
    }




    /**
     * Подключается к базе данных
     */
    private void initDatabaseConnection() {
        ServerConfig config = (ServerConfig) Context.get("config");
        logger.log("Соединяемся с базой данных...");

        try {
            Class.forName(config.getJdbcDriver());
        } catch (ClassNotFoundException e) {
            sendDown("Чтобы подключиться к базе данных, нужен драйвер: " + config.getJdbcDriver());
        }

        String databaseUrl = String.format(
                "jdbc:%s://%s:%s/%s",
                config.getJdbcLangProtocol(),
                config.getDatabaseHost(),
                config.getDatabasePort(),
                config.getDatabaseName()
        );
        try {
            connection = DriverManager.getConnection(databaseUrl, config.getDatabaseUser(), config.getDatabasePassword());
        } catch (SQLException e) {
            sendDown("Не получилось соединиться с базой данных: " + e.toString());
        }

        logger.log("Соединились с базой данных!");
    }

    /**
     * Создаёт необходимые таблицы, если их нет
     */
    private void initTables() {
        try {
            Statement statement = connection.createStatement();
            statement.execute("create table if not exists rooms " +
                    "(id serial primary key not null, name text, height integer, width integer, x integer, y integer," +
                    "creationdate timestamp, user_id integer)"
            );
            statement.execute("create table if not exists users (" +
                    "id serial primary key not null, name text, email text unique, password_hash bytea)"
            );
            statement.execute("create table if not exists things (" +
                    "id serial primary key not null, name text, size integer, room_id integer)"
            );
        } catch (SQLException e) {
            sendDown("Не получилось создать таблицы: " + e.toString());
        }
    }

    /**
     * Инициализирует соединение с JavaMail API
     */
    private void initEmail() {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", "in-v3.mailjet.com");
        properties.put("mail.smtp.port", 587);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.ssl.enable", "false");
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        Authenticator mailAuth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("e06513d48ba28a105caff1c08c4f0031", "bd95b9f237746446436ce4cb8420ef32");
            }
        };
        mailSession = Session.getDefaultInstance(properties, mailAuth);


    }

    public void sendEMail(String to, String subject, String content) throws MessagingException {

        MimeMessage message = new MimeMessage(mailSession);
        message.setFrom("mmmlpmsw@protonmail.com");
        message.setRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject);
        message.setContent(content, "text/html; charset=utf-8");

        Transport.send(message);
    }

    /**
     * Отправляет приказ на отчисление в ближайший принтер
     * @param message Прощальное сообщение
     */
    static void sendDown(String message) {
        System.err.println("Пиши ПСЖ: " + message);
        System.exit(-1);
    }

}
