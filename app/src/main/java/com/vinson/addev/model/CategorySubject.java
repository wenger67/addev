package com.vinson.addev.model;

public class CategorySubject {
    private int ID;
    private String subjectName;

    public CategorySubject(int ID, String subjectName) {
        this.ID = ID;
        this.subjectName = subjectName;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }
}
