package ru.n4d9.client;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

public class Room implements Comparable<Room>, Serializable {

    private String name /*= "Безымянная";*/;
    private double height, width; //width - ширина, height - высота
    private double x, y; //только по длине и высоте
    private LocalDateTime creationDate = LocalDateTime.now();
    private Date d;
    private ArrayList<Thing> shelf = new ArrayList<>();
    private double size;
    private double rotation;

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    private int ownerId = 0;
    private int id = 0;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    Room () {}

    Room(double width, double height, double x, double y) {
        setBounds(x, y, width, height);
    }

    Room(double width, double height, double x, double y, String name) {
        setBounds(x, y, width, height);
        setName(name);
    }

    public Date getD() {
        return d;
    }

    public void setD(Date d) {
        this.d = d;
    }

    public Room(double width, double height, double x, double y, String name, Thing... things) {
        setBounds(x, y, width, height);
        size = width*height;
        setName(name);
        shelf = new ArrayList<Thing>(Arrays.asList(things));
    }

    public Room(double width, double height, double x, double y, String name, ArrayList<Thing> things) {
        setBounds(x, y, width, height);
        size = width*height;
        setName(name);
        this.shelf = things;
    }

    public ArrayList<Thing> getShelf() { return shelf; }


    public void setBounds(double x, double y, double width, double height) {
        setPosition(x, y);
        setSize(width, height);
    }

    public void setPosition(double x, double y) {
        setX(x);
        setY(y);
    }

    public void setSize(double width, double height) {
        setWidth(width);
        setHeight(height);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }

    public double getWidth() { return width; }
    public double getHeight() { return height; }

    public void setWidth(double width) {
        if (width < 0)
            throw new IllegalArgumentException("Ширина не может быть отрицательной");
        this.width = width;
    }

    public void setHeight(double height) {
        if (height < 0)
            throw new IllegalArgumentException("Высота не может быть отрицательной");
        this.height = height;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getCreationDate() {return creationDate; }

    /**
     *осуществляет сортировку комнат по имени
     */
    @Override
    public int compareTo(Room o) {
        return this.getName().compareTo(o.getName());
    }

    /**
     * читабельное представление об объекте
     * @return String - информация о комнате
     */
    @Override
    public String toString() {

        StringBuilder roominfo = new StringBuilder("Комната");
        if (name.isEmpty()) {
            roominfo.append("без названия");
        } else {
            roominfo.append("-" + this.getName());
        }
        roominfo.append(", имеющая размеры: " + width + " x " + height + ", ")
                .append("и координаты левой нижней точки: x: " + getX() + ", y: " + getY() );
        if (shelf.size() == 0) {roominfo.append(" , пустая.");}
        else {
            roominfo.append(", содержащая " + shelf.size() + " предметов.");
        }
        return roominfo.toString();

    }

    public static Room fromResultSet(ResultSet set) throws SQLException {
        Room result = new Room();
        result.id = set.getInt("id");
        result.x = set.getDouble("x");
        result.y = set.getDouble("y");
        result.height = set.getFloat("height");
        result.width = set.getFloat("width");
        result.name = set.getString("name");
        result.ownerId = set.getInt("user_id");
        result.creationDate = LocalDateTime.ofInstant(set.getTimestamp("creationdate").toInstant(), ZoneId.systemDefault());
        result.rotation = set.getDouble("rotation");
        return result;
    }

    public void setFromRoomModel(Room model) {
        id = model.id;
        x = model.x;
        y = model.y;
        width = model.width;
        height = model.height;
        ownerId = model.ownerId;
        name = model.name;
        creationDate = model.creationDate;
    }


    public static class Thing implements Serializable {
        private int thingcount;
        private String name;

        public Thing(String name) {
            setName(name);
        }

        public Thing() {}

        public Thing(int thingcount, String name) {
            setName(name);
            setThingcount(thingcount);
        }

        public void setThingcount(int thingcount) {this.thingcount = thingcount;}
        public int getThingcount() {return thingcount;}

        public String getName() {return name;}
        public void setName(String name) {this.name = name;}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Thing shelf = (Thing) o;
            return thingcount == shelf.thingcount &&
                    Objects.equals(name, shelf.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(thingcount, name);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Room room = (Room) o;
        return height == room.height &&
                width == room.width &&
                x == room.x &&
                y == room.y &&
                Objects.equals(name, room.name) &&
                Objects.equals(shelf, room.shelf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, height, width, x, y, creationDate, shelf);
    }
}
