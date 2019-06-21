package ru.n4d9.Utils;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utilities {

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

    public static String toCamelCase(String s){
        String[] parts = s.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts){
            result.append(part.substring(0, 1).toUpperCase() +
                    part.substring(1).toLowerCase());
        }
        return result.toString();
    }

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
