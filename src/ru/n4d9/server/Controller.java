package ru.n4d9.server;

import ru.n4d9.Message;
import ru.n4d9.Utils.Utilities;

import java.sql.*;

public class Controller implements ContextFriendly {

    private Connection connection;
    private Logger logger;


    public Connection getConnection() {
        return connection;
    }

    @Override
    public void onContextReady() {
        logger = (Logger)Context.get("logger");
        initDatabaseConnection();
        initTables();

    }

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
     * Отправляет приказ на отчисление в ближайший принтер
     * @param message Прощальное сообщение
     */
    static void sendDown(String message) {
        System.err.println("Пиши ПСЖ: " + message);
        System.exit(-1);
    }

}
