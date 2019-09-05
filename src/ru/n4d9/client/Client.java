package ru.n4d9.client;

import javafx.application.Application;

import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

public class Client {

    private static Locale currentLocale = Locale.getDefault();
    private static HashMap<Locale, ResourceBundle> resourceBundles = new HashMap<>();

    public static void main(String[] args) {
        if (args.length == 2) {
            MainWindow.autofillLogin = args[0];
            MainWindow.autofillPassword = args[1];
        }
        initResourceBundles();
        Application.launch(MainWindow.class);

    }

    private static void initResourceBundles() {
        resourceBundles.clear();
        resourceBundles.put(
                currentLocale,
                ResourceBundle.getBundle("i18n/text", currentLocale)
        );
        resourceBundles.put(
                new Locale("es", "NI"),
                ResourceBundle.getBundle("i18n/text", new Locale("es", "NI"), new UTF8BundleControl())
        );
        resourceBundles.put(
                new Locale("et", "EE"),
                ResourceBundle.getBundle("i18n/text", new Locale("et", "EE"), new UTF8BundleControl())
        );
        resourceBundles.put(
                new Locale("fr", "FR"),
                ResourceBundle.getBundle("i18n/text", new Locale("fr", "FR"), new UTF8BundleControl())
        );
        resourceBundles.put(
                new Locale("ru", "RU"),
                ResourceBundle.getBundle("i18n/text", new Locale("ru", "RU"), new UTF8BundleControl())
        );
    }

    public static HashMap<Locale, ResourceBundle> getResourceBundles() {
        return resourceBundles;
    }

    public static ResourceBundle currentResourceBundle() {
        return resourceBundles.get(currentLocale);
    }

    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    public static void setCurrentLocale(Locale currentLocale) {
        Client.currentLocale = currentLocale;
    }
}
