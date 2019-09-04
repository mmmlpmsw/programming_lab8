package ru.n4d9.client;




import ru.n4d9.json.*;

import java.util.ArrayList;

/**
 * Советский завод по производству комнат для хрущёвок
 */
class RoomFactory {

    /**
     * создает объект-комнату из json-представления
     * @param json - json-представление объекта
     * @return - объект класса Room
     * @throws JSONParseException
     * @throws IllegalArgumentException
     */
    static Room makeRoomFromJSON(String json) throws JSONParseException, IllegalArgumentException {
        JSONEntity entity = JSONParser.parse(json);

        if (!entity.isObject()) {
            throw new IllegalArgumentException("Данный json должен быть объектом, но имеет тип " + entity.getType().toString().toLowerCase());
        }

        JSONObject object = (JSONObject)entity;

        JSONEntity coordXEntity = object.getItem("x");
        JSONEntity coordYEntity = object.getItem("y");

        if (coordXEntity == null || coordYEntity == null) {
            throw new IllegalArgumentException("Координаты должны быть обязательно указаны.");
        }

        if (!(coordXEntity.isNumber()&&coordYEntity.isNumber())) {
            throw new IllegalArgumentException("Координаты должны быть целыми числами.");
        }

        int coordX = (int)((JSONNumber)coordXEntity).getValue();
        int coordY = (int)((JSONNumber)coordYEntity).getValue();

        JSONEntity widthEntity = object.getItem("width");
        JSONEntity heightEntity = object.getItem("height");
        //JSONEntity lengthEntity = object.getItem("length");

        if (widthEntity == null || heightEntity == null /*|| lengthEntity == null*/) {
            throw new IllegalArgumentException("width, height должны быть обязательно указаны.");
        }

        if (!(widthEntity.isNumber() && heightEntity.isNumber()/* && lengthEntity.isNumber()*/)) {
            throw new IllegalArgumentException("width, height должны быть числами.");
        }

        int width = (int)((JSONNumber)widthEntity).getValue();
        int height = (int)((JSONNumber)heightEntity).getValue();

        String name = "";
        JSONEntity nameEntity = object.getItem("name");
        if (nameEntity != null) {
            if (nameEntity.isString()) {
                name = ((JSONString)nameEntity).getContent();
            }
            else {
                throw new IllegalArgumentException("name должен быть строкой, но имеет тип " + nameEntity.getType().toString().toLowerCase());
            }
        }

        Room.Thing[] shelfArray = new Room.Thing[0];
        JSONEntity thingsEntity = object.getItem("shelf");

        if (thingsEntity != null) {
            if (!thingsEntity.isArray()) {
                throw new IllegalArgumentException("shelf должен быть массивом, но имеет тип" + thingsEntity.getType().toString().toLowerCase());
            }

            ArrayList<JSONEntity> entities = ((JSONArray)thingsEntity).getItems();
            shelfArray = new Room.Thing[entities.size()];

            for (int i = 0; i < entities.size(); i++) {
                if (!entities.get(i).isObject()) {
                    throw new IllegalArgumentException("Все элементы массива shelf должны быть объектами");
                }
                JSONObject thingObject = (JSONObject) entities.get(i);
                String nameObj = "";
                int size;

                nameEntity = thingObject.getItem("name");
                if (nameEntity != null) {
                    if (nameEntity.isString()) {
                        nameObj = ((JSONString) nameEntity).getContent();
                    }
                    else {
                        throw new IllegalArgumentException("Поля name в элементах массива shelf должны быть строками, но одно из них имеет тип " + nameEntity.getType().toString().toLowerCase());
                    }
                }

                JSONEntity sizeEntity = thingObject.getItem("size");
                if (sizeEntity == null)
                    throw new IllegalArgumentException("Поле size в элементах массива shelf являются обязательными");

                if (sizeEntity.isNumber()) { size = (int)((JSONNumber) sizeEntity).getValue();}
                else {
                    throw new IllegalArgumentException("Поля size в элементах массива shelf должны быть числами, но одно из них имеет тип " + sizeEntity.getType().toString().toLowerCase());
                }

                shelfArray[i] = new Room.Thing(size, nameObj);
            }
        }

        return new Room(width, height, coordX, coordY,  name, shelfArray);
    }

    /**
     * Создаёт комнаты из их json-представления. Если получен json-объект, сгенерируется одна комната.
     * Если получен json-массив объектов, будет прочтён каждый объект внутри массива и возвращён
     * массив комнат, сгенерированных для каждого объекта
     * @param json json-представление
     * @return массив комнат
     * @throws Exception если что-то пошло не по плану
     */
    static Room[] makeRoomsFromJSON (String json) throws Exception {
        JSONEntity entity = JSONParser.parse(json);

        if (entity == null) {throw new IllegalArgumentException("Требуется json-объект, но получен null"); }
        if (entity.isObject()) { return new Room[]{makeRoomFromJSON(entity.toString())}; }
        else {
            if (entity.isArray()) {
                JSONArray roomArray = entity.toArray();
                Room[] rooms = new Room[roomArray.size()];
                for (int i = 0; i < roomArray.size(); i++) {
                    rooms[i] = makeRoomFromJSON(String.valueOf(roomArray.getItem(i).toObject()));
                }
                return rooms;
            }
            else {
                throw new IllegalArgumentException("Ошибка: не все элементы массива являются объектами.");
            }
        }
    }
}
