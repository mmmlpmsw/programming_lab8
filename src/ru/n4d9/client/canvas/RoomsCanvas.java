package ru.n4d9.client.canvas;

import com.badlogic.gdx.math.Vector2;
import javafx.animation.AnimationTimer;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import ru.n4d9.client.Room;
import static java.lang.Math.*;

import java.awt.*;
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
                        for (RoomVisualBuffer b : proxy)
                            if (!target.contains(b.origin))
                                proxy.remove(b);

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

    int product(int Px, int Py, int Ax, int Ay, int Bx, int By)
    {
        return (Bx - Ax) * (Py - Ay) - (By - Ay) * (Px - Ax);
    }

    private boolean check (double x, double y, double w, double h, double rot, double x0, double y0) {

        Point2D c = new Point2D(x+w/2, y+h/2);

        //p - массив вершин повернутого прямоугольника
        Point2D[] points = {
                new Point2D((x-c.getX())*cos(rot) - (y-c.getY())*sin(rot) + c.getX(),
                        (x-c.getX())*sin(rot)+(y-c.getY())*cos(rot) + c.getY()),

                new Point2D((x+w-c.getX())*cos(rot) - (y-c.getY())*sin(rot) + c.getX(),
                        (x+w-c.getX())*sin(rot)+(y-c.getY())*cos(rot) + c.getY()),

                new Point2D((x+w-c.getX())*cos(rot) - (y+h-c.getY())*sin(rot)+c.getX(),
                        (x+w-c.getX())*sin(rot)+(y+h-c.getY())*cos(rot) + c.getY()),

                new Point2D((x-c.getX())*cos(rot) - (y+h-c.getY())*sin(rot)+c.getX(),
                        (x-c.getX())*sin(rot)+(y+h-c.getX())*cos(rot) + c.getY())
        };

        //point - точка, которую надо проверить
        Point2D point = new Point2D(x0, y0);

        boolean result = false;

        int x1 = (int)points[0].getX();
        int y1 = (int)points[0].getY();
        int x2 = (int)points[1].getX();
        int y2 = (int)points[1].getY();
        int x3 = (int)points[2].getX();
        int y3 = (int)points[2].getY();
        int x4 = (int)points[3].getX();
        int y4 = (int)points[3].getY();


        int
                p1 = product((int)x0, (int)y0, x1, y1, x2, y2),
                p2 = product((int)x0, (int)y0, x2, y2, x3, y3),
                p3 = product((int)x0, (int)y0, x3, y3, x4, y4),
                p4 = product((int)x0, (int)y0, x4, y4, x1, y1);

        if ((p1 < 0 && p2 < 0 && p3 < 0 && p4 < 0) ||
                (p1 > 0 && p2 > 0 && p3 > 0 && p4 > 0))
            result = true;
        else result = false;

        return result;
    }

    private void onClicked(double x, double y) {

        Point2D unprojected = unproject(new Point2D(x, y));

        x = unprojected.getX();
        y = unprojected.getY();


        for (RoomVisualBuffer current : proxy) {
            double r = (current.origin.getHeight() + current.origin.getWidth())/2;
            Point2D center = new Point2D(current.visualX + current.origin.getWidth()/2, current.visualY + current.origin.getHeight()/2);
            //прямоугольник
//            if (x > (current.visualX * current.visualRotation) && x < current.visualX + current.origin.getWidth() &&
//            y > current.visualY && y < current.visualY + current.origin.getHeight()) {
            //условие, которое не смогло
//            if (check(current.visualX, current.visualY, current.origin.getWidth(), current.origin.getHeight(), current.visualRotation, x, y)) {
            if (r*r >= pow(center.getX() - x, 2) + pow(center.getY() - y, 2)) {
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

            context.translate(origin.getX(), origin.getY());
            context.rotate(origin.getRotation());

            //room
            context.fillRect(-origin.getWidth()/2, -origin.getHeight()/2, origin.getWidth(), origin.getHeight());

           //roof
            double[] xPoints = {
                    -origin.getWidth()/1.8,
                    0 ,
                    origin.getWidth()/1.8
            },
                    yPoints = {
                            -origin.getHeight()/2 ,
                            -origin.getHeight() ,
                            -origin.getHeight()/2
            };
            context.strokePolygon(xPoints, yPoints, 3);
            context.fillPolygon(xPoints, yPoints, 3);

            //door
            context.strokeRect(-3*origin.getWidth()/8, -origin.getHeight()/4,
                                origin.getWidth()/4, 3*origin.getHeight()/4);
            context.setFill(Color.rgb(255, 255, 255));
            context.fillRect(-3*origin.getWidth()/8, -origin.getHeight()/4,
                    origin.getWidth()/4, 3*origin.getHeight()/4);
            context.setFill(Color.rgb(118, 57, 0, .7));
            context.fillRect(-3*origin.getWidth()/8, -origin.getHeight()/4,
                    origin.getWidth()/4, 3*origin.getHeight()/4);

            //window
            context.strokeRect(origin.getWidth()/8, -3*origin.getHeight()/8,
                                    origin.getWidth()/4, origin.getHeight()/4);
            context.setFill(Color.rgb(255, 255, 255));
            context.fillRect(origin.getWidth()/8, -3*origin.getHeight()/8,
                    origin.getWidth()/4, origin.getHeight()/4);
            context.setFill(Color.rgb(186, 218, 255));
            context.fillRect(origin.getWidth()/8, -3*origin.getHeight()/8,
                    origin.getWidth()/4, origin.getHeight()/4);

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

            context.translate(origin.getX(), origin.getY());
            context.rotate(origin.getRotation());
            context.strokeRect(
                    /*visualX*/ - origin.getWidth()/2,
                    /*visualY */- origin.getHeight()/2,
                    origin.getWidth(),
                    origin.getHeight()
            );



            //roof
            double[] xPoints = {
                    -origin.getWidth()/1.8 /*+ origin.getX()*/,
                    0 /*+ origin.getX()*/,
                    origin.getWidth()/1.8 /*+ origin.getX()*/
            },
            yPoints = {
                    -origin.getHeight()/2 /*+ origin.getY() */,
                    -origin.getHeight() /*+ origin.getY()*/,
                    -origin.getHeight()/2 /*+ origin.getY()*/
            };
            context.strokePolygon(xPoints, yPoints, 3);

            context.restore();

        }

        /**
         * Выполняет обновление буфера
         *
         */
        private void update(long delta) {
            visualX += (origin.getX() - visualX)/60f;
            visualY += (origin.getY() - visualY)/60f;
            visualRotation += (origin.getRotation() - visualRotation)/60f;
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
