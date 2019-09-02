package ru.n4d9.server;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import ru.n4d9.Utils.Message;
import ru.n4d9.client.Room;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

public class Mirror implements ContextFriendly {
    //TODO обновление базы данных
    private static final float RATIO = 1f;

    private Controller controller;
    private ClientPool clientPool;
    private Logger logger;
    private World world;

    private ArrayList<RoomShell> roomShells;

    @Override
    public void onContextReady() {
        clientPool = (ClientPool) Context.get("clientpool");
        logger = (Logger) Context.get("logger");
        controller = (Controller) Context.get("controller");

        roomShells = new ArrayList<>();
        for (Room r : getRooms()) {
            roomShells.add(new RoomShell(r));
        }
        world = new World(new Vector2(0,  10), true);
        createWorld();
        new Thread(() -> {
            long iteration = 0;
            while (true) {
                world.step(1/30f, 3, 4);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
                updateState();
                if (++iteration%4 == 0)
                    sendState();
            }
        }).start();
    }

    /**
     * вызывается после очередного изменения-шага
     * в мире (world) и меняет состояние во всех объектах
     * room и связанных с ними телами в world
     */
    void updateState() {
        Array<Body> bodies = new Array<>();
        world.getBodies(bodies);
        for (Body b : bodies) {
//            for (RoomShell roomShell : roomShells) {
//                if (b.getUserData() == null) continue;
//                if (b.getUserData().equals(roomShell.getRoom())) {
//                    Vector2 position = b.getPosition();
//                    float rotation = (float)Math.toDegrees(b.getAngle());
//
//                    System.out.println(roomShell.getRoom().getName() + " :: x " + position.x + ", y " + position.y + ", rot " + rotation);
//
//                    //обновление room в оболочке
//                    roomShell.getRoom().setX(position.x/RATIO);
//                    roomShell.getRoom().setY(position.y/RATIO);
//                    roomShell.getRoom().setRotation(rotation);
//
//                    //обновление body в оболочке
//                    roomShell.setBody(b);
//
//                    //обновление room в body из мира
//                    b.setUserData(roomShell.getRoom());
//
//                }
            if (b.getUserData() != null) {
                Room target = (Room)b.getUserData();

                Vector2 position = b.getPosition();
                float rotation = (float)Math.toDegrees(b.getAngle());

                //обновление room в оболочке
                target.setX(position.x/RATIO);
                target.setY(position.y/RATIO);
                target.setRotation(rotation);
            }
        }

    }


    void createWorld() {
        Body body;
        for (RoomShell shell : roomShells) {
            body = createRoom(shell.getRoom());
            shell.setBody(body);
        }

        makeBox(-5000, 1000, 100000, 1000); // пол
        makeBox(-100, 0, 100, 1000); // стена
        makeBox(1000, 0, 100, 1000); // стена
    }

    private void makeBox(float x, float y, float w, float h) {
        BodyDef bdef = new BodyDef();

        bdef.position.set(x, y);

        bdef.type = BodyDef.BodyType.StaticBody;
        Body body = world.createBody(bdef);
        PolygonShape roomShape = new PolygonShape();
        Vector2[] roomCoords = {
                new Vector2(0, 0),
                new Vector2(w, 0),
                new Vector2(w, h),
                new Vector2(0, h)};
        roomShape.set(roomCoords);

        FixtureDef fDef = new FixtureDef();
        fDef.shape = roomShape;

        body.createFixture(fDef);
    }

    /**
     * создает (Body)-соответствие комнаты room и
     * добавляет ее в world
     * @param room объект, на основе которого создается
     * @return созданный Body-объект
     */
    private Body createRoom(Room room) {
        BodyDef roomDef = new BodyDef();

        roomDef.angle = (float)Math.toRadians(room.getRotation());
        roomDef.position.set((float) room.getX(), (float) room.getY());
        roomDef.fixedRotation = false;
        roomDef.type = BodyDef.BodyType.DynamicBody;
        roomDef.gravityScale = 10;

        Body roomBody = world.createBody(roomDef);

        PolygonShape roomShape = new PolygonShape();
        roomShape.setAsBox((float)room.getWidth()/2, (float)room.getHeight()/2);

        roomBody.createFixture(roomShape, 1);

        PolygonShape roofShape = new PolygonShape();
        Vector2[] roofCoords = {
                new Vector2((float)(- room.getWidth()/1.8), (float)( - room.getHeight()/2)),
                new Vector2((float)(0), (float)( - room.getHeight())),
                new Vector2((float)(+ room.getWidth()/1.8), (float)( - room.getHeight()/2))
        };
        roofShape.set(roofCoords);
        roomBody.createFixture(roofShape, 1);

        roomBody.setUserData(room);

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

        Array<Body> bodies = new Array<>();
        world.getBodies(bodies);
        for (Body b : bodies) {
            if (b.getUserData() != null && ((Room)b.getUserData()).getId() == room.getId()) {
                world.destroyBody(b);
                break;
            }
        }

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
                ArrayList<Room> roomShellsRooms = new ArrayList<>();
                for (RoomShell shell : roomShells) {
                    roomShellsRooms.add(shell.getRoom());
                }


                clientPool.sendAll(new Message("collection_state", roomShellsRooms));
    }

    private class RoomShell {
        private Room room;
        private Body body;

        public Room getRoom() { return room;}

        public void setRoom(Room room) { this.room = room; }

        public Body getBody() { return body; }

        public void setBody(Body body) { this.body = body; }

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