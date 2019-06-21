package ru.n4d9.server;

import ru.n4d9.json.*;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * Этот класс используется для управления настройками сервера
 */
public class ServerConfig {

    private int port;

    private String outLogFile;
    private String errLogFile;

    private String databaseHost;
    private int databasePort;

    private String databaseName;

    private String databaseUser;
    private String databasePassword;

    private String jdbcDriver;
    private String jdbcLangProtocol;

    private String smtpHost;
    private int smtpPort;
    private boolean smtpSSLEnabled;

    private String mailUsername;
    private String mailPassword;

    private String emailFrom;

    /**
     * Возвращает новый экземпляр {@link ServerConfig}, в который загружены
     * настройки из указанного файла. Файл должен быть в формате JSON.
     * @param filename файл, из которого следует загрузить настройки
     * @return экземпляр {@link ServerConfig} с настройками из файла
     * @throws IOException В случае ошибки ввода/вывода
     * @throws JSONParseException В случае синтаксической ошибки json
     * @throws NoSuchElementException Когда отсутствует какой-то обязательный параметр
     * @throws IllegalArgumentException Когда какой-то параметр имеет неверное значение
     * @throws IllegalStateException Когда какой-то параметр имеет неверный тип
     */
    static ServerConfig fromFile(String filename) throws IOException, JSONParseException, NoSuchElementException, IllegalArgumentException, IllegalStateException {
        ServerConfig result = new ServerConfig();

        String configContent = FileLoader.getFileContent(filename);

        JSONObject object = JSONParser.parse(configContent).toObject(
                "Файл должен содержать объект в формате JSON"
        );

        // Getting entities
        JSONEntity portEntity = object.getItemNotNull(
                "port",
                "Порт не указан, укажите его в параметре 'port'"
        );

        JSONEntity outLogFileEntity = object.getItemNotNull(
                "out_log_file",
                "Файл вывода сервера не указан, укажите его имя в параметре 'out_log_file'"
        );
        JSONEntity errLogFileEntity = object.getItemNotNull(
                "err_log_file",
                "Файл вывода ошибок сервера не указан, укажите его имя в параметре 'err_log_file'"
        );

        JSONEntity databaseHostEntity = object.getItemNotNull(
                "db_host",
                "Хост базы данных не указан, укажите его в параметре 'db_host'"
        );
        JSONEntity databasePortEntity = object.getItemNotNull(
                "db_port",
                "Порт базы данных не указан, укажите его в параметре 'db_port'"
        );

        JSONEntity databaseNameEntity = object.getItemNotNull(
                "db_name",
                "Имя базы данных не указано, укажите его в параметре 'db_name'"
        );

        JSONEntity databaseUserEntity = object.getItemNotNull(
                "db_user",
                "Имя пользователя базы данных не указано, укажите его в параметре 'db_user'"
        );
        JSONEntity databasePasswordEntity = object.getItemNotNull(
                "db_password",
                "Пароль базы данных не указан, укажите его в параметре 'db_password'. " +
                        "Если пароль не требуется для входа, укажите пустую строку."
        );

        JSONEntity jdbcDriverEntity = object.getItemNotNull(
                "jdbc_driver",
                "Драйвер базы данных не указан, укажите его в параметре 'jdbc_driver'"
        );
        JSONEntity jdbcLangProtocolEntity = object.getItemNotNull(
                "jdbc_lang_protocol",
                "Протокол языка базы данных не указан, укажите его в параметре 'jdbc_lang_protocol'"
        );

        JSONEntity smtpHostEntity = object.getItemNotNull(
                "smtp_host",
                "Хост SMTP не указан, укажите его в параметре 'smtp_host'"
        );
        JSONEntity smtpPortEntity = object.getItemNotNull(
                "smtp_port",
                "Порт SMTP не указан, укажите его в параметре 'smtp_port'"
        );
        JSONEntity smtpSSLEnabledEntity = object.getItemNotNull(
                "smtp_ssl_enabled",
                "Флаг режима SSL не указан, укажите его в параметре 'smtp_ssl_enabled'"
        );

        JSONEntity mailUsernameEntity = object.getItemNotNull(
                "mail_username",
                "Имя пользователя электронной почты не указано, укажите его в параметре 'mail_username'"
        );
        JSONEntity mailPasswordEntity = object.getItemNotNull(
                "mail_password",
                "Пароль электронной почты не указан, укажите его в параметре 'mail_password'"
        );

        JSONEntity emailFromEntity = object.getItemNotNull(
                "email_from",
                "Отправитель писем не указан, укажите его в параметре 'email_from'"
        );

        // Extracting Java-type variables
        int port = (int)portEntity.toNumber(
                "Порт должен быть числом, но это " + portEntity.getTypeName() + ".\n" +
                        "Проверьте значение параметра 'port'"
        ).getValue();

        String outLogFile = outLogFileEntity.toString(
                "Имя файла журнала сервера должно быть строкой, но это " + outLogFileEntity.getTypeName() + ".\n" +
                        "Проверьте значение параметра 'out_log_file'"
        ).getContent();

        String errLogFile = errLogFileEntity.toString(
                "Имя файла ошибок сервера должно быть строкой, но это " + errLogFileEntity.getTypeName() + ".\n" +
                        "Проверье значение параметра 'err_log_file'"
        ).getContent();

        String databaseHost = databaseHostEntity.toString(
                "Хост базы данных должен быть строкой, но это " + databaseHostEntity.getTypeName() + ".\n" +
                        "Проверьте значение параметра 'db_host'"
        ).getContent();

        int databasePort = (int)databasePortEntity.toNumber(
                "Порт базы данных должен быть числом, но это " + databasePasswordEntity.getTypeName() + ".\n" +
                        "Проверьте значение параметра 'db_port'"
        ).getValue();

        String databaseName = databaseNameEntity.toString(
                "Имя базы данных должно быть строкой, но это " + databaseNameEntity.getTypeName() + ".\n" +
                        "Проверьте значение параметра 'db_name'"
        ).getContent();

        String databaseUser = databaseUserEntity.toString(
                "Имя пользователя базы данных должно быть строкой, но это " + databaseUserEntity.getTypeName() + ".\n" +
                        "Проверьте значение параметра 'db_user'"
        ).getContent();

        String databasePassword = databasePasswordEntity.toString(
                "Пароль пользователя базы данных должен быть строкой, но это " + databasePasswordEntity.getTypeName() + ".\n" +
                        "Проверьте значение парамтра 'db_password'"
        ).getContent();

        String jdbcDriver = jdbcDriverEntity.toString(
                "Драйвер базы данных должен быть строкой, но это " + jdbcDriverEntity.getTypeName() + ".\n" +
                        "Проверье значение параметра 'jdbc_driver'"
        ).getContent();

        String jdbcLangProtocol = jdbcLangProtocolEntity.toString(
                "Язык протокола базы данных должен быть строкой, но это " + jdbcLangProtocolEntity.getTypeName() + ".\n" +
                        "Проверье значение параметра 'jdbc_lang_protocol'"
        ).getContent();

        String smtpHost = smtpHostEntity.toString(
                "Хост SMTP должен быть строкой, но это " + smtpHostEntity.getTypeName() + "\n" +
                        "Проверьте значение параметра 'smtp_host'"
        ).getContent();

        int smtpPort = (int)smtpPortEntity.toNumber(
                "Порт SMTP должен быть числом, но это " + smtpPortEntity.getTypeName() + "\n" +
                        "Проверьте значение параметра 'smtp_port'"
        ).getValue();

        boolean smtpSSLEnabled = smtpSSLEnabledEntity.toBoolean(
                "Флаг режиме SSL должен быть логическим типом, но это " + smtpSSLEnabledEntity.getTypeName() + "\n" +
                        "Проверьте значение параметра 'smtp_ssl_enabled"
        ).getValue();

        String mailUsername = mailUsernameEntity.toString(
                "Имя пользователя электронной почты должно быть строкой, но это " + mailUsernameEntity.getTypeName() + "\n" +
                        "Проверьте значение параметра 'mail_username'"
        ).getContent();

        String mailPassword = mailPasswordEntity.toString(
                "Пароль электронной почты должен быть строкой, но это " + mailPasswordEntity.getTypeName() + "\n" +
                        "Проверьте значение параметра 'mail_password'"
        ).getContent();

        String emailFrom = emailFromEntity.toString(
                "Отправитель писем должен быть строкой, но это " + emailFromEntity.getTypeName() + "\n" +
                        "Проверьте значение параметра 'email_from'"
        ).getContent();

        // Setting variables
        result.setPort(port);
        result.setErrLogFile(errLogFile);
        result.setOutLogFile(outLogFile);
        result.setDatabaseHost(databaseHost);
        result.setDatabasePort(databasePort);
        result.setDatabaseName(databaseName);
        result.setDatabaseUser(databaseUser);
        result.setDatabasePassword(databasePassword);
        result.setJdbcDriver(jdbcDriver);
        result.setJdbcLangProtocol(jdbcLangProtocol);
        result.setSmtpHost(smtpHost);
        result.setSmtpPort(smtpPort);
        result.setSmtpSSLEnabled(smtpSSLEnabled);
        result.setMailUsername(mailUsername);
        result.setMailPassword(mailPassword);
        result.setEmailFrom(emailFrom);

        return result;
    }

    /**
     * @return Порт, который следует слушать серверу
     */
    public int getPort() {
        return port;
    }

    /**
     * Устанавливает порт, который следует слушать серверу.
     * Порт должен находиться в пределах от 1 до 65535.
     * @param port номер порта
     */
    public void setPort(int port) {
        if (port < 1 || port > 65535)
            throw new IllegalArgumentException("Порт должен быть в пределах от 1 до 65535");
        this.port = port;
    }

    /**
     * @return хост базы данных к которой следует подключаться серверу
     */
    public String getDatabaseHost() {
        return databaseHost;
    }

    public void setDatabaseHost(String databaseHost) {
        this.databaseHost = databaseHost;
    }

    /**
     * @return порт базы данных, к которой следует подключаться серверу
     */
    public int getDatabasePort() {
        return databasePort;
    }

    /**
     * Устанавливает порт базы данных, к которой следует подключиться серверу
     * @param databasePort номер порта
     */
    public void setDatabasePort(int databasePort) {
        if (port < 1 || port > 65535)
            throw new IllegalArgumentException("Порт должен быть в пределах от 1 до 65535");
        this.databasePort = databasePort;
    }

    /**
     * @return Имя базы данных, к которой следует подключиться серверу
     */
    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    /**
     * @return Имя пользователя базы данных, с которым работает сервер
     */
    public String getDatabaseUser() {
        return databaseUser;
    }

    public void setDatabaseUser(String databaseUser) {
        this.databaseUser = databaseUser;
    }

    /**
     * @return Пароль базы данных, с которым работает сервер
     */
    public String getDatabasePassword() {
        return databasePassword;
    }

    public void setDatabasePassword(String databasePassword) {
        this.databasePassword = databasePassword;
    }

    /**
     * @return Драйвер базы данных
     */
    public String getJdbcDriver() {
        return jdbcDriver;
    }

    public void setJdbcDriver(String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
    }

    /**
     * @return Протокол языка базы данных, который следует использовать при подключении
     */
    public String getJdbcLangProtocol() {
        return jdbcLangProtocol;
    }

    public void setJdbcLangProtocol(String jdbcLangProtocol) {
        this.jdbcLangProtocol = jdbcLangProtocol;
    }

    /**
     * @return Хост SMTP для отправки писем
     */
    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    /**
     * @return Порт SMTP для отправки писем
     */
    public int getSmtpPort() {
        return smtpPort;
    }

    /**
     * Устанавливает порт SMTP для отправки писем.
     * Номер порта должен быть от 1 до 65535
     *
     * @param smtpPort номер порта
     */
    public void setSmtpPort(int smtpPort) {
        if (port < 1 || port > 65535)
            throw new IllegalArgumentException("Порт должен быть в пределах от 1 до 65535");
        this.smtpPort = smtpPort;
    }

    /**
     * @return Флаг, укажиывающий, следует ли использовать SSL
     */
    public boolean isSmtpSSLEnabled() {
        return smtpSSLEnabled;
    }

    public void setSmtpSSLEnabled(boolean smtpSSLEnabled) {
        this.smtpSSLEnabled = smtpSSLEnabled;
    }

    /**
     * @return Имя пользователя адреса электронной почты
     */
    public String getMailUsername() {
        return mailUsername;
    }

    public void setMailUsername(String mailUsername) {
        this.mailUsername = mailUsername;
    }

    /**
     * @return Пароль адреса электронной почты
     */
    public String getMailPassword() {
        return mailPassword;
    }

    public void setMailPassword(String mailPassword) {
        this.mailPassword = mailPassword;
    }

    /**
     * @return Отправитель, от имени которого следует отправлять электронные письма
     */
    public String getEmailFrom() {
        return emailFrom;
    }

    public void setEmailFrom(String emailFrom) {
        this.emailFrom = emailFrom;
    }

    public String getOutLogFile() {
        return outLogFile;
    }

    public void setOutLogFile(String outLogFile) {
        this.outLogFile = outLogFile;
    }

    /**
     * @return Имя файла, в который будут записаны ошибки сервера
     */
    public String getErrLogFile() {
        return errLogFile;
    }

    public void setErrLogFile(String errLogFile) {
        this.errLogFile = errLogFile;
    }
}
