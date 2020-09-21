package com.vinson.addev.model.local;

import androidx.annotation.NonNull;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.IndexType;
import io.objectbox.annotation.Unique;

@Entity
public class FrameSavedImage {
    @Id
    public long id;
    @Unique
    public String fileName;
    @Index(type = IndexType.HASH64)
    public String path;
    public long fileSize;
    public long createdAt;

    public FrameSavedImage() {
    }

    public FrameSavedImage(String fileName, String path, long fileSize) {
        this.fileName = fileName;
        this.path = path;
        this.fileSize = fileSize;
        this.createdAt = System.currentTimeMillis();
    }

    public FrameSavedImage(String fileName, String path, long fileSize, long createdAt) {
        this.fileName = fileName;
        this.path = path;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
    }

    @NonNull
    @Override
    public String toString() {
        return "FrameSavedImage{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", path='" + path + '\'' +
                ", fileSize=" + fileSize +
                ", createdAt=" + createdAt +
                '}';
    }
}
