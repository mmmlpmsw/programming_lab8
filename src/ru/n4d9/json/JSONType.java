package ru.n4d9.json;

/**
 * Инкапсулирует тип json-сущности
 */
public enum JSONType {
    OBJECT("объект"),
    ARRAY("массив"),
    STRING("строка"),
    NUMBER("число"),
    BOOLEAN("логический");

    String name;

    JSONType(String name) {
        this.name = name;
    }

    /**
     * @return Строка, содержащая user-friendly название типа
     */
    public String getName() {
        return name;
    }
}
