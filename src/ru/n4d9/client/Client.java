package ru.n4d9.client;

import ru.n4d9.Message;
import ru.n4d9.transmitter.Receiver;
import ru.n4d9.transmitter.ReceiverListener;
import ru.n4d9.transmitter.Sender;
import ru.n4d9.transmitter.SenderAdapter;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Scanner;

public class Client {
    private static final int SENDING_PORT = 6666;
    private static Scanner scanner;
    private static Receiver receiver;

    public static void main(String[] args) {
        scanner = new Scanner(System.in);
        try {
            receiver = new Receiver(true);
            receiver.startListening();
        } catch (IOException e) {
            System.out.println("Не получилось запустить клиент: " + e.toString());
        }

        System.out.println("Локальный порт: " + receiver.getLocalPort());
        IMMA_CHARGIN_MAH_LAZER();
    }

    // TODO: Нормально назвать функцию
    private static void IMMA_CHARGIN_MAH_LAZER() {
//        System.out.print("> ");
//        String nextLine = scanner.nextLine();
        String nextLine = "myCommand";
        processCommand(nextLine);
    }

    private static void processCommand(String command) {
        Message message = new Message(command);
        message.setSourcePort(receiver.getLocalPort());
        try {
            Sender.send(message.serialize(), InetAddress.getByName("localhost"), SENDING_PORT, true, new SenderAdapter() {
                @Override
                public void onSuccess() {
                    System.out.println("Команда отправилась, жду ответ...");
                    waitForServerResponse();
                }

                @Override
                public void onError(String message) {
                    System.out.println("Не получилось отправить запрос: " + message);
                    IMMA_CHARGIN_MAH_LAZER();
                }
            });
        } catch (IOException e) {
            System.out.println("Не получилось сформировать запрос: " + e.getMessage());
        }
    }

    private static void waitForServerResponse() {
        Thread outer = Thread.currentThread();
        receiver.setListener(new ReceiverListener() {
            @Override
            public void received(int requestID, byte[] data, InetAddress address, int port) {
            }

            @Override
            public void received(int requestID, byte[] data, InetAddress address) {
                try {
                    Message message = Message.deserialize(data);
                    System.out.println("Вот что ответил сервер: " + message.getText());
                    Thread.sleep(30);
                    outer.interrupt();
                } catch (ClassNotFoundException | IOException e) {
                    System.out.println("Не получилось обработать ответ сервера");
                } catch (InterruptedException ignored) {
                }
            }

            @Override
            public void exceptionThrown(Exception e) {
                e.printStackTrace();
                System.out.println("Не получилось получить ответ сервера: " + e.toString());
                outer.interrupt();
            }
        });

        try {
            Thread.sleep(3000);
            System.out.println("Сервер ничего не ответил");
        } catch (InterruptedException ignored) {
        } finally {
            IMMA_CHARGIN_MAH_LAZER();
        }
    }
}
