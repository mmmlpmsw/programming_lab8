package ru.n4d9.server;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Поток, который пишет вводимые в него данные в 2 других потока
 */
public class TeeOutputStream extends OutputStream {

    private OutputStream stream1, stream2;

    public TeeOutputStream(OutputStream stream1, OutputStream stream2) {
        if (stream1 == null || stream2 == null)
            throw new IllegalArgumentException("Оба потока должны быть не null");
        this.stream1 = stream1;
        this.stream2 = stream2;
    }

    @Override
    public void write(int b) throws IOException {
        stream1.write(b);
        stream2.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        stream1.write(b);
        stream2.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        stream1.write(b, off, len);
        stream2.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        stream1.flush();
        stream2.flush();
    }
}
