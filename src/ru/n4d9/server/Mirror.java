package ru.n4d9.server;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.sun.xml.internal.ws.encoding.RootOnlyCodec;
import ru.n4d9.Utils.Message;
import ru.n4d9.client.Room;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

public class Mirror implements ContextFriendly {
    private Controller controller;
    private ClientPool clientPool;
    private Logger logger;
    private World world;

//    private ArrayList<Room> rooms;
    private HashSet<Room> rooms;
    private ArrayList<RoomShell> roomShells;

    Mirror(Controller controller, Logger logger) {
        this.controller = controller;
        this.logger = logger;
        rooms = getRooms();
        roomShells = new ArrayList<>();
        world = new World(new Vector2(0, 10), true);
        createWorld();
        new Thread(() -> {
            sendState();
        }).start();

    }


    void createWorld() {
        for (Room room : rooms)
            createRoom(room);
    }


    Body createRoom(Room room) {
        BodyDef roomDef = new BodyDef();
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



        return roomBody;
    }




    /**
     * метод для получения текущего состояния коллекции
     * @return список содержимого базы данных
     */
//    public ArrayList<Room> getRooms() {
//        if (rooms.size() == 0) {
//            rooms = controller.getAllRoomsToMirror();
//            logger.verbose("Сейчас в коллекции есть " + rooms.size() + " комнат: " + rooms.toString());
//            return rooms;
//        } else return rooms;
//    }

    public HashSet<Room> getRooms() {
        try {
            ResultSet resultSet = controller.getConnection().createStatement().executeQuery("select * from rooms");
            HashSet<Room> roomModels = new HashSet<>();

            while (resultSet.next())
                roomModels.add(Room.fromResultSet(resultSet));

            return roomModels;
        } catch (SQLException e) {e.getMessage();}
        return null;
    }

    void roomAdded(Room room) {
        rooms.add(room);
        roomShells.add(new RoomShell(room, createRoom(room)));
//        roomBodies.add(createRoom(room));
        logger.verbose("Добавлена комната: " + room.toString());
        logger.verbose("Сейчас в коллекции " + rooms.size() + " комнат.");
    }

    void roomRemoved(Room room) {
        rooms.remove(room);
        boolean a = roomShells.remove(new RoomShell(room, createRoom(room)));

        logger.verbose("Удалена комната: " + room.toString());
        logger.verbose("Сейчас в коллекции " + rooms.size() + " комнат.");
    }

    void roomModified (Room room) {
        logger.verbose("Внесены изменения в комнату " + room.getId());
        for (Room r : rooms) {
            if (room.getId() == r.getId()) {

                logger.verbose("Было: " + r.toString());
                r.setFromRoomModel(room);
                logger.verbose("Стало: " + r.toString());
                logger.verbose("Текущее состояние коллекции: " + rooms.toString());
                break;
            }
        }
        for (RoomShell r : roomShells) {
            if (room.getId() == r.getRoom().getId()) {

                logger.verbose("Было: " + r.toString());
                Room changed = r.getRoom().setFromRoomModel(room);
                r.setRoom(changed);
                logger.verbose("Стало: " + r.toString());
                logger.verbose("Текущее состояние коллекции: " + rooms.toString());
                break;
            }
        }
    }

    private void sendState() {
        while (true) {
            world.step(4F, 4, 4);
            //TODO : change collection state
            clientPool.sendAll(new Message("collection_state", rooms));
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {}
        }
    }

    @Override
    public void onContextReady() {
        clientPool = (ClientPool) Context.get("clientpool");
        logger = (Logger) Context.get("logger");
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
