package ru.n4d9.transmitter;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Приёмник данных. Используется для получения данных, полученных от {@link Sender}.
 */
public class Receiver {
    private static final int HEAD_SIZE = 16;
    private static final int CONTENT_SIZE = 512;

    private static final int PIECE_SIZE = HEAD_SIZE + CONTENT_SIZE;

    private DatagramSocket socket;
    private DatagramChannel channel;
    private boolean useChannel;
    private int localPort;

    private ReceiverListener listener;
    private Thread listeningThread;
    private HashMap<Integer, PieceCollector> collectors = new HashMap<>();

    /**
     * Создаёт приёмник данных с указанным портом
     * @param port порт, который нужно прослушивать
     * @throws SocketException если что-то пойдёт не так
     */
    public Receiver(int port, boolean useChannel) throws IOException {
        this.useChannel = useChannel;
        if (useChannel) {
            channel = DatagramChannel.open();
            channel.connect(new InetSocketAddress(port));
        }
        else
            socket = new DatagramSocket(port);
        localPort = port;
    }

    /**
     * Создаёт приёмник данных с указанным портом
     * @throws SocketException если что-то пойдёт не так
     */
    public Receiver(boolean useChannel) throws IOException {
        this.useChannel = useChannel;
        if (useChannel) {
            channel = DatagramChannel.open();
            channel.socket().bind(new InetSocketAddress(0));
            localPort = channel.socket().getLocalPort();
        }
        else {
            socket = new DatagramSocket();
            localPort = socket.getLocalPort();
        }
    }

    public int getLocalPort() {
        return localPort;
    }

    private Runnable listeningRunnable = () -> {
        while (!Thread.interrupted()) {
            try {
                DatagramPacket packet = new DatagramPacket(new byte[PIECE_SIZE], PIECE_SIZE);
                ByteBuffer buffer = ByteBuffer.allocate(PIECE_SIZE);
                if (useChannel) {
                    SocketAddress address = channel.receive(buffer);
                    buffer.rewind();
                    channel.send(buffer, address);
                    buffer.rewind();
                    processIncomingPacket(new DatagramPacket(buffer.array(), PIECE_SIZE, address));
                } else {
                    socket.receive(packet);
                    socket.send(packet);
                    processIncomingPacket(packet);
                }
            } catch (IOException e) {
                if (listener != null)
                    listener.exceptionThrown(e);
            }
        }
    };

    /**
     * Устанавливает слушатель, который будет принимать все события приёмника
     * @param listener слушатель событий
     */
    public void setListener(ReceiverListener listener) {
        this.listener = listener;
    }

    /**
     * Запускает приём пакетов
     */
    public void startListening() {
        stopListening();
        listeningThread = new Thread(listeningRunnable);
        listeningThread.start();
    }

    /**
     * Останавливает приём пакетов
     * @deprecated
     */
    public void stopListening() {
        if (listeningThread != null && (!listeningThread.isInterrupted() || listeningThread.isAlive()))
            listeningThread.interrupt();
    }

    /**
     * Обрабатывает каждый входящий пакет
     * @param packet входящий пакет
     */
    private void processIncomingPacket(DatagramPacket packet) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(packet.getData());
        byteBuffer.position(12);
        int requestID = byteBuffer.getInt();
        if (!collectors.containsKey(requestID))
            collectors.put(requestID, new PieceCollector(requestID, packet.getAddress(), packet.getPort()));
        collectors.get(requestID).collect(packet);
    }

    /**
     * Коллектор кусочков сообщений (датаграмм), собирает сообщения от одного запроса
     */
    private class PieceCollector {
        private int requestID;
        private InetAddress address;
        private int port;
        private SortedSet<MarkedUpPacket> packets = new TreeSet<>();

        PieceCollector(int requestID, InetAddress address, int port) {
            this.requestID = requestID;
            this.address = address;
            this.port = port;
        }

        /**
         * Используется для сбора частей одного запроса
         * @param packet датаграмма, которую нужно собрать
         */
        void collect(DatagramPacket packet) {
            ByteBuffer buffer = ByteBuffer.wrap(packet.getData());

            MarkedUpPacket markedUpPacket = new MarkedUpPacket(
                    buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getInt()
            );
            buffer.get(markedUpPacket.data, 0, CONTENT_SIZE);
            packets.add(markedUpPacket);

            if (packets.size() >= markedUpPacket.all)
                collected();
        }

        /**
         * Вызывается, когда сборка частей запроса завершена (пришли все части запроса)
         */
        private void collected() {
            collectors.remove(requestID);

            byte[] data = new byte[packets.size() * CONTENT_SIZE];
            int length = 0;

            int offset = 0;
            for (MarkedUpPacket packet : packets) {
                System.arraycopy(packet.data, 0, data, offset, CONTENT_SIZE);
                offset += CONTENT_SIZE;
                length += packet.len;
            }

            byte[] result = new byte[length];
            System.arraycopy(data, 0, result, 0, length);

            if (listener != null)
                listener.received(requestID, result, address, port);
        }

        /**
         * Класс-обёртка размеченного кусочка сообщения
         */
        private class MarkedUpPacket implements Comparable<MarkedUpPacket> {
            int num, all, len, req_id;
            byte[] data = new byte[CONTENT_SIZE];

            MarkedUpPacket(int n, int a, int l, int r) {
                num = n;
                all = a;
                len = l;
                req_id = r;
            }

            @Override
            public int compareTo(MarkedUpPacket o) {
                return num - o.num;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == null || obj.getClass() != this.getClass())
                    return false;
                MarkedUpPacket packet = (MarkedUpPacket)obj;
                return  packet.num == this.num &&
                        packet.all == this.all &&
                        packet.len == this.len &&
                        packet.req_id == this.req_id &&
                        Arrays.equals(packet.data, this.data);
            }
        }
    }
}
