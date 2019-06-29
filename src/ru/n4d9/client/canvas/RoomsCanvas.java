package ru.n4d9.client.canvas;

import javafx.animation.AnimationTimer;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import ru.n4d9.client.Room;

import java.util.*;

public class RoomsCanvas extends Canvas {
    private static final int AREA_SIZE = 1000;
    private static final int PADDING = 50;

    private ObservableList<Room> target;
    private final Set<RoomVisualBuffer> proxy = new HashSet<>();
    private Thread updatingThread = new Thread();
    private HashMap<Integer, Color> userColors = new HashMap<>();
    private RoomVisualBuffer selected = null;

    private RoomSelectingListener listener = model -> {};

    private AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            draw();
        }
    };

    public void clearProxy() {
        proxy.clear();
    }

    public void setTarget(ObservableList<Room> target) {
        this.target = target;

        selected = null;
        proxy.clear();

        updatingThread.interrupt();
        updatingThread = new Thread(() -> {
            try {
                long lastMillis = System.currentTimeMillis();
                while (true) {
                    Thread.sleep(1000 / 60);
                    synchronized (proxy) {
                        update(System.currentTimeMillis() - lastMillis);
                    }
                    lastMillis = System.currentTimeMillis();
                }
            } catch (InterruptedException ignored) {
                System.out.println("WARNING: Updating thread has been interrupted");
            }
        });
        updatingThread.setDaemon(true);
        updatingThread.start();

        timer.start();

        setOnMouseClicked(e -> onClicked(e.getX(), e.getY()));
    }

    public HashMap<Integer, Color> getUserColors() {
        return userColors;
    }

    public void selectRoom(Room model) {
        if (model == null) {
            selected = null;
            return;
        }

        for (RoomVisualBuffer current : proxy) {
            if (current.origin.getId() == model.getId()) {
                selected = current;
                return;
            }
        }
    }

    public void setSelectingListener(RoomSelectingListener listener) {
        this.listener = listener;
    }

    private Point2D unproject(Point2D point) {
        double x = point.getX();
        double y = point.getY();

        double scale = getScale();
        Point2D translate = getTranslate();

        // Матрица преобразований
        double  a = 1, b = 0,
                c = 0, d = 1,
                e = 0, f = 0;

        // Перенос масшабирования и смещения на матрицу
        a /= scale;
        d /= scale;

        e -= translate.getX();
        f -= translate.getY();

        // Преобразование координат матрицей
        x = a*x + c*y + e;
        y = b*c + d*y + f;

        return new Point2D(x, y);
    }

    private void onClicked(double x, double y) {

        Point2D unprojected = unproject(new Point2D(x, y));

        x = unprojected.getX();
        y = unprojected.getY();

        for (RoomVisualBuffer current : proxy) {
            if (x > current.visualX && x < current.visualX + current.origin.getWidth() &&
            y > current.visualY && y < current.visualY + current.origin.getHeight()) {
                listener.selected(current.origin);
                return;
            }
        }
        listener.selected(null);
        selected = null;
    }

    private void update(long delta) {
        synchronized (proxy) {
            for (Room model : target) {
                Optional<RoomVisualBuffer> creatureBuffer = proxy.stream().filter((b) -> b.origin.getId() == model.getId()).findAny();
                if (creatureBuffer.isPresent())
                   creatureBuffer.get().update(delta);
                else
                    proxy.add(new RoomVisualBuffer(model));
            }

            Set<RoomVisualBuffer> toRemove = new HashSet<>();

            for (RoomVisualBuffer buffer : proxy) {
                Optional<Room> model = target.stream().filter((m) -> m.getId() == buffer.origin.getId()).findAny();
                if (!model.isPresent())
                    toRemove.add(buffer);
            }
            toRemove.forEach(proxy::remove);
        }
        // TODO: remove from proxy
    }

    private void draw() {
        GraphicsContext context = getGraphicsContext2D();
        context.clearRect(0, 0, getWidth(), getHeight());
        context.save();

        double scale = getScale();
        Point2D translate = getTranslate();

        context.scale(scale, scale);
        context.translate(translate.getX(), translate.getY());

        proxy.forEach((b) -> b.draw(context));
        context.setStroke(Color.BLACK);
        context.setLineWidth(2);
        context.strokePolygon(
                new double[] {
                        0, AREA_SIZE, AREA_SIZE, 0, 0
                },
                new double[] {
                        0, 0, AREA_SIZE, AREA_SIZE, 0
                }, 5
        );

        if (selected != null)
            selected.drawSelectionOutline(context);
        context.restore();
    }

    /**
     * @return масштаб для вписывания в размер
     */

    private double getScale() {
        if (getWidth() < getHeight())
            return getWidth()/(AREA_SIZE + PADDING*2);
        else
            return getHeight()/(AREA_SIZE + PADDING*2);
    }

    /**
     * @return смещение для вписывания в размер
     */

    private Point2D getTranslate() {
        double scale = getScale();
        if (getWidth() < getHeight())
            return new Point2D(PADDING, (getHeight() - AREA_SIZE*scale)/2 + PADDING);
        else
            return new Point2D((getWidth() - AREA_SIZE*scale)/2 + PADDING, PADDING);
    }

    /**
     * Специальный класс-обёртка для {@link Room}, предоставляющий
     * функционал для отображения и анимации
     */

    private class RoomVisualBuffer {
        private Room origin;

        private double visualX;
        private double visualY;
        private double visualRotation;

        private RoomVisualBuffer(Room origin) {
            this.origin = origin;

            visualX = origin.getX();
            visualY = origin.getY();
            visualRotation = origin.getRotation();
        }

        /**
         * Рисует целевое существо
         * @param context контекст холста
         */

        private void draw(GraphicsContext context) {
            context.save();

            Color color = Color.rgb(128, 128, 128, .5);

            if (userColors.containsKey(origin.getOwnerId()))
                color = userColors.get(origin.getOwnerId()).deriveColor(0, 1, 1, .5);

            context.setFill(color);
            context.setStroke(color.darker());

            context.translate(origin.getX()+0.5*origin.getWidth(), origin.getY()+0.5*origin.getHeight());
            context.rotate(origin.getRotation());
            context.strokeRect(-origin.getWidth()/2, -origin.getHeight()/2, origin.getWidth(), origin.getHeight());

            context.restore();
        }

        /**
         * Рисует рамку выбора целевого существа
         * @param context контекст золста
         */

        private void drawSelectionOutline(GraphicsContext context) {
            context.save();

            Color color = Color.rgb(255, 154, 0, .75);

            context.setLineWidth(4);
            context.setStroke(color);
            context.setLineDashes(10);
            context.setLineDashOffset(10);

            context.strokeRect(
                    visualX,
                    visualY,
                    origin.getWidth(),
                    origin.getHeight()
            );

            context.restore();
        }

        /**
         * Выполняет обновление буфера
         *
         */
        private void update(long delta) {
            visualX += (origin.getX() - visualX)/25f;
            visualY += (origin.getY() - visualY)/25f;
            visualRotation += (origin.getRotation() - visualRotation)/25f;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || obj.getClass() != getClass())
                return false;
            return origin.getId() == (((RoomVisualBuffer)obj).origin.getId());
        }

        @Override
        public int hashCode() {
            return origin.hashCode();
        }
    }
}
