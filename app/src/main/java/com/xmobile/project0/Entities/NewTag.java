package com.xmobile.project0.Entities;

import java.io.Serializable;

public class NewTag implements Serializable {
    private String name;

    public NewTag(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
