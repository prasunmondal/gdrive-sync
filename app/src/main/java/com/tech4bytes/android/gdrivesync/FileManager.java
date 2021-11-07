package com.tech4bytes.android.gdrivesync;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class FileManager {
    List<Directory> folders_to_sync = new ArrayList<>();
    List<File> files_available = new ArrayList<>();

    void writeFileData(Context context) {
        IOObjectToFile file_operation = new IOObjectToFile();
        file_operation.WriteObjectToFile(context, "folders_to_sync", folders_to_sync);
        file_operation.WriteObjectToFile(context, "files_available", files_available);
    }

    void readFileData(Context context) {
        IOObjectToFile file_operation = new IOObjectToFile();
        file_operation.ReadObjectFromFile(context, "folders_to_sync");
        file_operation.ReadObjectFromFile(context, "files_available");
    }

    void add_file(Context context, String full_path) {
        files_available.add(new File(full_path));
        writeFileData(context);
    }

    void add_folder(Context context, String directory_path) {
        folders_to_sync.add(new Directory(directory_path));
        writeFileData(context);
        listFilesInDirectory(context, directory_path);
    }

    void listFilesInDirectory(Context context, String directoryPath) {
        String path = directoryPath;
        String actual_path = path.split(":")[1];
        path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + actual_path;
        Log.d("Files", "Path: " + path);
        java.io.File directory = new java.io.File(path);
        java.io.File[] files = directory.listFiles();
        Log.d("Files", "Size: " + files.length);

        for (int i = 0; i < files.length; i++) {
            Log.d("Files", "FileName:" + files[i].getName() + "-" + files[i].isFile() + "-" + files[i].length() + "-" + files[i].lastModified());
            add_file(context, files[i].getAbsolutePath());
        }
    }
}

class File {
    java.io.File file;
    Sync_Status sync_status;

    public File(String full_path) {
        file = new java.io.File(full_path);
    }
}

class Directory {
    String directory_path;

    public Directory(String directory_path) {
        this.directory_path = directory_path;
    }
}

enum Sync_Status {
    PENDING, UPLOADING, DOWNLOADING, UPLOAD_COMPLETE
}
