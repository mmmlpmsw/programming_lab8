package ru.n4d9.transmitter;

import java.net.InetAddress;

/**
 * Слушатель приёмника данных. Используется, чтобы регистрироваться на события приёмника.
 */
public interface ReceiverListener {
    /**
     * Вызывается, когда получены данные от клиента
     * @param requestID уникальный идентификатор запроса
     * @param data данные
     * @param address адрес отправителя
     * @param port порт отправителя
     */
    void received(int requestID, byte[] data, InetAddress address, int port);

    /**
     * Вызывается, когда происходит исключение. Вызов при любом исключении не гарантирован.
     * @param e объект исключения
     */
    void exceptionThrown(Exception e);
}
