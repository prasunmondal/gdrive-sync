package com.tech4bytes.android.gdrivesync;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FileManager {
    List<Directory> folders_to_sync = new ArrayList<>();
    List<LocalFile> files_available = new ArrayList<>();

    void writeFileData(Context context) throws IOException {
        try {
            Log.d("Memory", "Write sync data: Start");
            IOObjectToFile file_operation = new IOObjectToFile();
            file_operation.WriteObjectToFile(context, "folders_to_sync", folders_to_sync);
            file_operation.WriteObjectToFile(context, "files_available", files_available);
            Log.d("Memory", "Write sync data: Complete");
        } catch (Exception e) {
            Log.d("Memory", "Write sync data: Failed");
            throw e;
        }
    }

    void readFileData(Context context) {
        try {
            Log.d("Memory", "Fetch Existing sync data: Start");
            IOObjectToFile file_operation = new IOObjectToFile();
            folders_to_sync = (List<Directory>) file_operation.ReadObjectFromFile(context, "folders_to_sync");
            files_available = (List<LocalFile>) file_operation.ReadObjectFromFile(context, "files_available");
            Log.d("Memory", "Fetch Existing sync data: Complete");
        } catch (Exception e) {
            Log.d("Memory", "Fetch Existing sync data: Failed");
        }
    }

    void add_file(Context context, java.io.File full_path) {
        boolean doesExistInRecords = false;
        for(LocalFile f: files_available) {
            if(is_same_file(f, full_path)) {
                doesExistInRecords = true;
            }
        }
        if(!doesExistInRecords) {
            files_available.add(new LocalFile(full_path));
        }
    }

    boolean is_same_file(LocalFile fileRef1, java.io.File fileRef2) {
        return fileRef1.file.getName().equals(fileRef2.getName());
    }

    void add_folder(Context context, String directory_path) throws IOException {
        folders_to_sync.add(new Directory(directory_path));
        writeFileData(context);
        listFilesInDirectory(context, directory_path);
    }

    void listFilesInDirectory(Context context, String directoryPath) throws IOException {
        String path = directoryPath;
        String actual_path = path.split(":")[1];
        path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + actual_path;
        Log.d("Files", "Path: " + path);
        java.io.File directory = new java.io.File(path);
        java.io.File[] files = directory.listFiles();
        Log.d("Files", "Size: " + files.length);
        for (int i = 0; i < files.length; i++) {
            Log.d("Files", "FileName:" + files[i].getName() + "-" + files[i].isFile() + "-" + files[i].length() + "-" + files[i].lastModified());
            add_file(context, files[i]);
        }
        writeFileData(context);
    }
}

class LocalFile implements Serializable {
    java.io.File file;
    Sync_Status sync_status;

    public LocalFile(java.io.File file) {
        this.file = file;
        sync_status = Sync_Status.PENDING;
    }
}

class Directory implements Serializable {
    String directory_path;

    public Directory(String directory_path) {
        this.directory_path = directory_path;
    }
}

enum Sync_Status implements Serializable {
    PENDING, UPLOADING, DOWNLOADING, UPLOAD_COMPLETE
}
