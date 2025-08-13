package org.zemo.omninet.notes.enums;

public enum TodoStatus {

    NOT_STARTED(1, "Not Started"), IN_PROGRESS(2, "In progess"), COMPLETED(3, "Complted");

    private Integer id;

    private String name;

    TodoStatus(Integer i, String string) {
        this.id = i;
        this.name = string;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
