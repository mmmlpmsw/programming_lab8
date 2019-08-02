package ru.n4d9.server;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import ru.n4d9.client.Room;

import java.util.HashSet;

public class MyWorld {

    World world;
    HashSet<Room> roomsSet;

    MyWorld(HashSet<Room> rooms, float delta) {
        this.roomsSet = rooms;
        world = new World(new Vector2(0, 10), true);
//        createWorld();
        while (true) {
            world.step(delta, 4, 4);
        }
    }

//    void createWorld() {
//        for (Room room : roomsSet)
//            createRoom(room);
//    }
//
//    void createRoom(Room room) {
//        BodyDef roomDef = new BodyDef();
//        roomDef.type = BodyDef.BodyType.KinematicBody;
//        Body roomBody = world.createBody(roomDef);
//
//        PolygonShape roofShape = new PolygonShape();
//        Vector2[] roofCoords = {
//                new Vector2((float)(room.getX() - 17), (float)room.getY()),
//                new Vector2((float)(room.getX() + 0.5 * room.getWidth()), (float)(((-30*room.getHeight())/room.getWidth()) - room.getHeight())),
//                new Vector2((float)(room.getX() + room.getWidth() + 17), (float)room.getY())
//        };
//        roofShape.set(roofCoords);
//
//        PolygonShape roomShape = new PolygonShape();
//        Vector2[] roomCoords = {
//                new Vector2((float)room.getX(), (float)room.getY()),
//                new Vector2((float)room.getX() + (float)room.getWidth(), (float)room.getY()),
//                new Vector2((float)room.getX() + (float)room.getWidth(), (float)room.getY() + (float)room.getHeight()),
//                new Vector2((float)room.getX(), (float)room.getY() + (float)room.getHeight())};
//        roomShape.set(roomCoords);
//
//        roomBody.createFixture(roofShape, 1f);
//        roomBody.createFixture(roomShape, 1f);
//    }
}
