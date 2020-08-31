package com.vinson.addev.model.local;

import com.otaliastudios.cameraview.size.Size;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.IndexType;
import io.objectbox.annotation.Unique;

@Entity
public class LoopVideoFile {
    @Id
    public long id;
    @Unique
    public String fileName;
    @Index(type = IndexType.HASH64)
    public String path;
    public long fileSize;
    public long createdAt;

    public LoopVideoFile(String fileName, String path, long fileSize) {
        this.fileName = fileName;
        this.path = path;
        this.fileSize = fileSize;
        this.createdAt = System.currentTimeMillis();
    }

    public LoopVideoFile(String fileName, String path, long fileSize, long createdAt) {
        this.fileName = fileName;
        this.path = path;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
    }

    public LoopVideoFile(long id, String fileName, String path, long fileSize, long createdAt) {
        this.id = id;
        this.fileName = fileName;
        this.path = path;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "LoopRecordFile{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", path='" + path + '\'' +
                ", fileSize=" + fileSize +
                ", createdAt=" + createdAt +
                '}';
    }
}
