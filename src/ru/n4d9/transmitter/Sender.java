package ru.n4d9.transmitter;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Отправитель данных. Отправляет данные, которые можно получить с помощью {@link Receiver}.
 *
 * Разметка заголовка одного сообщения (piece):
 * +---------+---------+---------+------------+
 * | num (4) | all (4) | len (4) | req_id (4) |
 * +---------+---------+---------+------------+
 * num - порядковый номер пакета
 * all - количество всех пакетов
 * len - количество данных в этом сообщении
 * req_id - уникальный идентификатор запроса
 * Остальная часть сообщения является её содержимым
 *
 */
public class Sender {
    /**
     * Размер заголовка сообщения
     */
    private static final int HEAD_SIZE = 16;

    /**
     * Размер содержимого сообщения
     */
    private static final int CONTENT_SIZE = 512;

    /**
     * Общий размер пакета
     */
    private static final int PIECE_SIZE = HEAD_SIZE + CONTENT_SIZE;


    public Sender () {

    }

    /**
     * Отправляет данные на указанный адрес и порт. Отправка выполняется в отдельном потоке.
     * @param data данные, которые нужно отправить
     * @param address адрес, на который нужно отправить данные
     * @param port порт, используемый для подключения
     * @param listener слушатель, сообщающий о событиях
     */
    public static void send(byte[] data, InetAddress address, int port, boolean useChannel, SenderListener listener) {
        if (useChannel)
            sendViaChannel(data, address, port, listener);
        else
            sendViaDatagram(data, address, port, listener);
    }

    private synchronized static void sendViaDatagram(byte[] data, InetAddress address, int port, SenderListener listener) {
        List<DatagramPacket> packets = generateDatagramPackets(data);

        try (DatagramSocket socket = new DatagramSocket()) {
            Thread mainThread = Thread.currentThread();

            Runnable receivingRunnable = () -> {
                try {
                    socket.receive(new DatagramPacket(new byte[PIECE_SIZE], PIECE_SIZE));

                    mainThread.interrupt();
                } catch (SocketException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };

            for (int i = 0; i < packets.size(); i++) {
                Thread receivingThread = new Thread(receivingRunnable);
                receivingThread.setDaemon(true);
                DatagramPacket packet = packets.get(i);
                socket.send(new DatagramPacket(packet.getData(), packet.getData().length, address, port));

                receivingThread.start();

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    listener.onProgress(1f * i / packets.size());
                    continue;
                }

                receivingThread = new Thread(receivingRunnable);
                receivingThread.start();

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    listener.onProgress(1f * i / packets.size());
                    continue;
                }

                receivingThread.interrupt();
                listener.onError("Сторона не отвечает");
                return;
            }
        } catch (Exception e) {
            listener.onError("Ошибка отправки запроса: " + e.getMessage());
            e.printStackTrace();
        }
        listener.onSuccess();
    }

    private synchronized static void sendViaChannel(byte[] data, InetAddress address, int port, SenderListener listener) {
        List<DatagramPacket> packets = generateDatagramPackets(data);

        try (DatagramChannel channel = DatagramChannel.open()) {
            Thread mainThread = Thread.currentThread();

            Runnable receivingRunnable = () -> {
                try {
                    channel.receive(ByteBuffer.allocate(PIECE_SIZE));

                    mainThread.interrupt();
                } catch (IOException ignored) {}
            };

            for (int i = 0; i < packets.size(); i++) {
                Thread receivingThread = new Thread(receivingRunnable);
                receivingThread.setDaemon(true);
                DatagramPacket packet = packets.get(i);

                channel.send(ByteBuffer.wrap(packet.getData()), new InetSocketAddress(address, port));

                receivingThread.start();

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    listener.onProgress(1f * i / packets.size());
                    continue;
                }

                receivingThread = new Thread(receivingRunnable);
                receivingThread.start();

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    listener.onProgress(1f * i / packets.size());
                    continue;
                }

                receivingThread.interrupt();
                listener.onError("Сторона не отвечает");
                return;
            }
        } catch (Exception e) {
//            listener.onError("Ошибка отправки запроса: " + e.getMessage());
            e.printStackTrace();
        }
        listener.onSuccess();
    }



    /**
     * Генерирует датаграммы, содержащие размеченные сообщения с указанными данными
     * @param data содержимое сообщений в датаграммах
     * @return список датаграмм
     */
    private static List<DatagramPacket> generateDatagramPackets(byte[] data) {
        List<DatagramPacket> result = new ArrayList<>();
        int requestID = getNextRequestID();

        for (int offset = 0; offset < data.length; offset += CONTENT_SIZE) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(PIECE_SIZE);

            int bytesLeft = data.length - offset;

            byteBuffer.putInt(offset/CONTENT_SIZE);                             // Номер пакета
            byteBuffer.putInt((int)Math.ceil(1.0*data.length/CONTENT_SIZE));    // Всего пакетов
            byteBuffer.putInt(bytesLeft > CONTENT_SIZE ? CONTENT_SIZE : bytesLeft); // Длина
            byteBuffer.putInt(requestID);                                       // Идентификатор запроса

            byteBuffer.put(data, offset, bytesLeft > CONTENT_SIZE ? CONTENT_SIZE : bytesLeft);

            DatagramPacket packet = new DatagramPacket(byteBuffer.array(), PIECE_SIZE);
            result.add(packet);
        }
        return result;
    }

    private static int getNextRequestID() {
        return (int)(Integer.MIN_VALUE + Math.random()*(1.0*Integer.MAX_VALUE - Integer.MIN_VALUE));
    }
}
