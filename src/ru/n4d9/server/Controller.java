package ru.n4d9.server;

import ru.n4d9.Utils.Message;
import ru.n4d9.Utils.Utilities;
import ru.n4d9.client.Room;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.sql.*;
import java.time.ZoneOffset;
import java.util.*;

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

//        mirror.getRooms().clear();
//        mirror.getRooms().addAll(getAllRooms());

        initDatabaseConnection();
        initTables();
        initEmail();

    }

    public void removeRoomFromMirror(Room room, int user_id) {
//        mirror = (Mirror)Context.get("mirror");
        mirror.roomRemoved(room);
    }

    public void modifyRoomInMirror(Room room) {
//        mirror = (Mirror)Context.get("mirror");
        mirror.roomModified(room);
    }

    public ArrayList<Room> getAllRoomsToMirror() {

        ArrayList<Room> result = new ArrayList<>();

        try {
            PreparedStatement statement = connection.prepareStatement("select * from rooms");
            ResultSet roomsResultSet = statement.executeQuery();
            if (roomsResultSet == null) return null;
            while (roomsResultSet.next()) {
                int room_id = roomsResultSet.getInt("id");
                int user_id = roomsResultSet.getInt("user_id");

                Room room = new Room(roomsResultSet.getInt("width"),
                        roomsResultSet.getInt("height"),
                        roomsResultSet.getInt("x"),
                        roomsResultSet.getInt("y"),
                        roomsResultSet.getString("name")
                        );
                room.setId(room_id);
                room.setOwnerId(user_id);
                result.add(room);
            }

        } catch (SQLException e) {
            logger.err("Ошибка при работе с SQL: " + e.getMessage());
        }
        return result;
    }

    /**
     * добавляет объект в базу данных
     * @param room объект, который нужно добавить
     * @param ownerid уникальный номер пользователя, добавляющего объект
     * @return название объекта
     * @throws SQLException при ошибке при работе с базой данных
     */
    public Room addRoom(Room room, int ownerid) throws SQLException {

        PreparedStatement statement = connection.prepareStatement("insert into rooms (name, height, width, x, y, creationdate, user_id) values (?, ?, ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
        statement.setString(1, room.getName());
        statement.setDouble(2, room.getHeight());
        statement.setDouble(3, room.getWidth());
        statement.setDouble(4, room.getX());
        statement.setDouble(5, room.getY());
        statement.setTimestamp(6, new Timestamp(room.getCreationDate().toEpochSecond(ZoneOffset.UTC) * 1000L));
        statement.setInt(7, ownerid);

        statement.execute();
        ResultSet resultSet = statement.getGeneratedKeys();
        resultSet.next();
        int room_id = resultSet.getInt("id");

        for (Room.Thing thing : room.getShelf()) {
            statement = connection.prepareStatement("insert into things (room_id, name, size) values (?, ?, ?)");
            statement.setInt(1, room_id);
            statement.setString(2, thing.getName());
            statement.setInt(3, thing.getThingcount());

            statement.execute();
        }
        room.setId(room_id);
        room.setOwnerId(ownerid);

//        mirror = (Mirror)Context.get("mirror");
        mirror.roomAdded(room);

        return room;
    }

    public static Room removeRoomById(int id, Connection connection) throws SQLException {
        if (connection == null)
            return null;

        PreparedStatement statement = connection.prepareStatement(
                "select * from rooms where id = ?"
        );
        statement.setInt(1, id);
        ResultSet set = statement.executeQuery();

        set.next();
        Room room = Room.fromResultSet(set);

        statement = connection.prepareStatement(
                "delete from rooms where id = ?"
        );
        statement.setInt(1, id);
        statement.execute();

        statement = connection.prepareStatement(
                "delete from things where room_id = ?"
        );
        statement.setInt(1, id);
        statement.execute();
        return room;
    }



    /**
     * удаляет комнату по id
     *
     * @param id id комнаты, которую нужно удалить
     * @return true, если комната удалилась, во всех остальных случаях false
     */
//    public static boolean removeRoomById(int id, Connection connection) throws SQLException {
//        if (connection == null)
//            return false;
//
//        PreparedStatement statement = connection.prepareStatement(
//                "delete from rooms where id = ?"
//        );
//        statement.setInt(1, id);
//        int removed = statement.executeUpdate();
//        statement = connection.prepareStatement(
//                "delete from things where room_id = ?"
//        );
//        statement.setInt(1, id);
//        statement.execute();
//        return removed != 0;
//    }

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
                    "(id serial primary key not null, name text, height double precision, width double precision, x double precision, y double precision," +
                    "creationdate timestamp, rotation double precision, user_id integer)"
            );
            statement.execute("create table if not exists users (" +
                    "id serial primary key not null, name text, email text unique, password_hash bytea, color integer)"
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
