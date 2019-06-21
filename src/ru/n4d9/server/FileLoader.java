package ru.n4d9.server;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileLoader {

    /**
     * Читает файл
     * @param filename имя файла
     * @return содержимое в виде строки
     * @throws IOException если что-то пойдет не так
     */
    public static String getFileContent(String filename) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)))) {
            StringBuilder fileContent = new StringBuilder();

            String line;
            while ((line = bufferedReader.readLine()) != null)
                fileContent.append(line);

            return fileContent.toString();
        }
    }
}
