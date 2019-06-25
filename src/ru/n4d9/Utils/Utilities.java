package ru.n4d9.Utils;

import ru.n4d9.client.Room;
import ru.n4d9.json.*;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utilities {

    /**
     * преобразует json-строку в лист объектов {@Room}
     * @param jsonString json-строка
     * @return лист объектов {@Room}
     * @throws Exception если что-то пойдет не по плану
     */
    public static ArrayList<Room> getRoomsFromJSON(String jsonString) {

        ArrayList<Room> building = new ArrayList<>();
        try {
            JSONEntity entity = JSONParser.parse(jsonString);
            JSONObject object = entity.toObject("Вместо ожидаемого объекта получен элемент типа " + entity.getType().toString().toLowerCase());
            JSONEntity createdEntity = object.getItem("created");

            JSONEntity collectionEntity = object.getItem("collection");
            if (collectionEntity != null) {
                JSONArray collectionArray = collectionEntity.toArray("Вместо ожидаемого массива имеет тип " + collectionEntity.getType().toString().toLowerCase());
//            building.getCollection().clear();

                for (JSONEntity room : collectionArray.getItems()) {

                    JSONObject roomObject = room.toObject("Элементы collection должны быть объектами");
                    int width = roomObject.getItem("width").toNumber("Поле width элементов коллекции должно быть числом").toInt(),
                            height = roomObject.getItem("height").toNumber("Поле height элементов коллекции должно быть числом, но это").toInt();

                    int x = roomObject.getItem("x").toNumber("Координата x элементов коллекции должна быть числом").toInt();
                    int y = roomObject.getItem("y").toNumber("Координата y элементов коллекции должна быть числом").toInt();

                    String roomName = "";
                    JSONEntity roomNameEntity = roomObject.getItem("name");
                    if (roomNameEntity != null) {
                        roomName = roomNameEntity.toString("Поле name элементов массива collection должно быть строкой").getContent();
                    }

                    Room.Thing[] things = new Room.Thing[0];

                    JSONEntity JSONshelf = roomObject.getItem("shelf");

                    if (JSONshelf != null) {
                        JSONArray shelfArray = JSONshelf.toArray("Поле shelf элементов массива collection должно быть массивом");
                        things = new Room.Thing[shelfArray.getItems().size()];

                        ArrayList<JSONEntity> items = shelfArray.getItems();
                        for (int i = 0; i < items.size(); i++) {
                            JSONObject thingObject = items.get(i).toObject("Элементы поля shelf должны быть объектами");

                            int size;
                            String name = "";

                            JSONEntity nameEntity = thingObject.getItem("name");

                            if (nameEntity != null)
                                name = nameEntity.toString(("Поле name элементов поля shelf должно быть строкой")).getContent();

                            JSONEntity sizeEntity = thingObject.getItem("size");

                            if (sizeEntity == null)
                                throw new IllegalArgumentException("У элементов поля shelf должно быть поле size");

                            size = sizeEntity.toNumber("Элементы size элементов поля shelf должны быть числами").toInt();

                            things[i] = new Room.Thing(size, name);
                        }

                    }

                    building.add(new Room(width, height, x, y, roomName, things));
                }
            }

            return building;
        } catch (JSONParseException ignored) {
            return null;
        }

    }

    private static String PASSWORD_SALT = "EDfvcpoi456GESChgfgv10";

    /**
     * Раскрашивает текст, заменяя подсроки-маркеры специальными ASCII-кодами
     * Подстрокой-маркером считается текст, заключенный в двойные прямоугольные скобки.
     * Например: [[RED]], [[BG_BLUE]]
     *
     * Информация о цветах получается из {@link Constants.ANSI_COLORS}
     * @param source исходная строка с кодами
     * @return раскрашенный текст
     */
    public static String colorize(String source) {
        try {
            StringBuilder result = new StringBuilder(source);
            HashMap<String, String> colorsMap = new HashMap<>();
            Field[] colorFields = Constants.ANSI_COLORS.class.getDeclaredFields();
            for (Field field : colorFields) {
                if (field.getType() == String.class)
                    colorsMap.put(field.getName(), (String)field.get(Constants.ANSI_COLORS.class));
            }
            String regex = "\\[\\[\\w+]]";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(result);
            while (matcher.find()) {
                String colorName = result.substring(matcher.start()+2, matcher.end()-2).toUpperCase();
                String color = colorsMap.get(colorName);
                if (color != null) {
                    result.replace(matcher.start(), matcher.end(), color);
                    matcher.region(matcher.start(), result.length());
                }
            }
            return result.toString();
        } catch (IllegalAccessException e) {
            return source;
        }
    }

    /**
     * проверяет корректность введенного пользователем e-mail адреса
     * @param email введенный пользователем e-mail
     * @return <i>true</i>, если адрес кооректен, иначе <i>false</i>
     */
    public static boolean isValidEmailAddress(String email) {
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {
            result = false;
        }
        return result;
    }

    /**
     * приводит название к необходимому для дальнейшего распознавания виду
     * @param s первоначальное название команды
     * @return преобразованное название
     */
    public static String toCamelCase(String s){
        String[] parts = s.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts){
            result.append(part.substring(0, 1).toUpperCase() +
                    part.substring(1).toLowerCase());
        }
        return result.toString();
    }

    /**
     * хэштрует строку алгоритмом MD5
     * @param password строка до применения операции
     * @return хэш пароля
     */
    public static byte[] hashPassword(String password) {
        try {
            byte[] pswdBytes = password.getBytes("UTF-8");
            java.security.MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.reset();
            byte[] array = digest.digest(pswdBytes);

            return array;
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static SecureRandom rnd = new SecureRandom();

    public static String randomString( int len ){
        StringBuilder sb = new StringBuilder( len );
        for( int i = 0; i < len; i++ )
            sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
        return sb.toString();
    }

    public static String getPasswordSalt() {
        return PASSWORD_SALT;
    }



}
