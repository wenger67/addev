package com.vinson.addev.model;

import javax.security.auth.Subject;

public class Category {
    private int ID;
    private String categoryName;
    private Subject categorySubject;

    public Category(int ID, String categoryName, Subject categorySubject) {
        this.ID = ID;
        this.categoryName = categoryName;
        this.categorySubject = categorySubject;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Subject getCategorySubject() {
        return categorySubject;
    }

    public void setCategorySubject(Subject categorySubject) {
        this.categorySubject = categorySubject;
    }
}
