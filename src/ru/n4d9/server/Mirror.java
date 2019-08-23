package ru.n4d9.server;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import ru.n4d9.Utils.Message;
import ru.n4d9.client.Room;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

public class Mirror implements ContextFriendly {
    private Controller controller;
    private ClientPool clientPool;
    private Logger logger;
    private World world;

    private ArrayList<RoomShell> roomShells;

    /**
     * вызывается после очередного изменения-шага
     * в мире (world) и меняет состояние во всех объектах
     * room и связанных с ними телами в world
     */
    void updateState() {
        Array<Body> bodies = new Array<>();
        world.getBodies(bodies);
        for (Body b : bodies) {
            for (RoomShell roomShell : roomShells) {
                if (b.getUserData().equals(roomShell.getRoom())) {
                    Vector2 position = b.getPosition();
                    float rotation = b.getAngle();

                    //обновление room в оболочке
                    roomShell.getRoom().setX(position.x);
                    roomShell.getRoom().setY(position.y);
                    roomShell.getRoom().setRotation(rotation);

                    //обновление body в оболочке
                    roomShell.setBody(b);

                    //обновление room в body из мира
                    b.setUserData(roomShell.getRoom());

                }

            }
        }

    }


    void createWorld() {
        Body body;
        for (RoomShell shell : roomShells) {
            body = createRoom(shell.getRoom());
            shell.setBody(body);
        }
    }

    /**
     * создает (Body)-соответствие комнаты room и
     * добавляет ее в world
     * @param room объект, на основе которого создается
     * @return созданный Body-объект
     */
    private Body createRoom(Room room) {
        BodyDef roomDef = new BodyDef();
        roomDef.position.set((float) room.getX(), (float) room.getY());
        roomDef.position.setAngle((float)room.getRotation());


        roomDef.type = BodyDef.BodyType.DynamicBody;

        Body roomBody = world.createBody(roomDef);


        PolygonShape roofShape = new PolygonShape();
        Vector2[] roofCoords = {
                new Vector2((float)(room.getX() - 17), (float)room.getY()),
                new Vector2((float)(room.getX() + 0.5 * room.getWidth()), (float)(((-30*room.getHeight())/room.getWidth()) - room.getHeight())),
                new Vector2((float)(room.getX() + room.getWidth() + 17), (float)room.getY())
        };
        roofShape.set(roofCoords);

        PolygonShape roomShape = new PolygonShape();
        Vector2[] roomCoords = {
                new Vector2((float)room.getX(), (float)room.getY()),
                new Vector2((float)room.getX() + (float)room.getWidth(), (float)room.getY()),
                new Vector2((float)room.getX() + (float)room.getWidth(), (float)room.getY() + (float)room.getHeight()),
                new Vector2((float)room.getX(), (float)room.getY() + (float)room.getHeight())};
        roomShape.set(roomCoords);

        roomBody.createFixture(roofShape, 1f);
        roomBody.createFixture(roomShape, 1f);

        roomBody.setUserData(room);

        roomBody.setTransform((float)room.getX(), (float)room.getY(), (float)room.getRotation());
        roomBody.getPosition();
        return roomBody;
    }

    /**
     * метод для получения текущего состояния коллекции
     * @return список содержимого базы данных
     */
    public ArrayList<Room> getRooms() {
        try {
            ResultSet resultSet = controller.getConnection().createStatement().executeQuery("select * from rooms");
            ArrayList<Room> roomModels = new ArrayList<>();

            while (resultSet.next())
                roomModels.add(Room.fromResultSet(resultSet));

            return roomModels;
        } catch (SQLException e) {e.getMessage();}
        return null;
    }

    void roomAdded(Room room) {
        roomShells.add(new RoomShell(room, createRoom(room)));
        logger.verbose("Добавлена комната: " + room.toString());
        logger.verbose("Сейчас в коллекции " + roomShells.size() + " комнат.");
    }

    void roomRemoved(Room room) {
        RoomShell roomShell = new RoomShell(room, createRoom(room));
        world.destroyBody(roomShell.getBody());
        boolean a = roomShells.remove(roomShell);

        logger.verbose("Результат удаления: " + a);
        logger.verbose("Удалена комната: " + room.toString());
        logger.verbose("Сейчас в коллекции " + roomShells.size() + " комнат.");
    }

    void roomModified (Room room) {
        logger.verbose("Внесены изменения в комнату " + room.getId());

        for (RoomShell r : roomShells) {
            if (room.getId() == r.getRoom().getId()) {

                world.destroyBody(r.getBody());
                logger.verbose("Было: " + r.toString());
                Room changed = r.getRoom().setFromRoomModel(room);
                r.setRoom(changed);

                Body newBody = createRoom(changed);
                r.setBody(newBody);

                logger.verbose("Стало: " + r.toString());
                break;
            }
        }
    }

    private void sendState() {
//        while (true) {
            ArrayList<Room> roomShellsRooms = new ArrayList<>();
            for (RoomShell shell : roomShells)
                roomShellsRooms.add(shell.getRoom());

            clientPool.sendAll(new Message("collection_state", roomShellsRooms));
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {}
//        }
    }

    @Override
    public void onContextReady() {
        clientPool = (ClientPool) Context.get("clientpool");
        logger = (Logger) Context.get("logger");
        controller = (Controller) Context.get("controller");

        roomShells = new ArrayList<>();
        for (Room r : getRooms()) {
            roomShells.add(new RoomShell(r));
        }
        world = new World(new Vector2(0, 10), true);
        createWorld();
        new Thread(() -> {
            while (true) {
                world.step(1f/60, 10, 10);
                updateState();
                sendState();
            }
        }).start();
//        controller = (Controller) Context.get("controller");
    }

    private class RoomShell {
        private Room room;
        private Body body;

        public Room getRoom() { return room;}

        public void setRoom(Room room) { this.room = room; }

        public Body getBody() { return body; }

        public void setBody(Body body) { this.body = body; }

        public RoomShell() {}

        public RoomShell(Room room) {
            setRoom(room);
        }

        public RoomShell (Room room, Body body) {
            setRoom(room);
            setBody(body);
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RoomShell roomShell = (RoomShell) o;
            return roomShell.getRoom().equals(this.getRoom());
        }

        @Override
        public int hashCode() {
            return Objects.hash(room, body);
        }
    }

}