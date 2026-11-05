/* Copyright 2026 Andesk contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */
package com.farmerbb.taskbar.backup;

import android.content.Context;

import com.farmerbb.taskbar.util.U;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Keeps a recoverable snapshot of every on-disk file overwritten by backup restore.
 * The ready marker is written last, before the caller starts changing any files.
 * A pending restore can be rolled back on the next app launch after a process crash.
 */
public final class RestoreJournal {
    private static final String DIRECTORY = "restore_rollback";
    private static final String[] DATA_FILES = {
            "PinnedBlockedApps", "Blacklist", "TopApps", "SavedWindowSizes"
    };

    private RestoreJournal() {}

    private static File directory(Context context) {
        return new File(context.getFilesDir(), DIRECTORY);
    }

    private static File[] targets(Context context) {
        File[] target = new File[DATA_FILES.length + 1 + U.getImageFilenames().length];
        for(int i = 0; i < DATA_FILES.length; i++)
            target[i] = new File(context.getFilesDir(), DATA_FILES[i]);

        target[DATA_FILES.length] = new File(
                new File(context.getFilesDir().getParentFile(), "shared_prefs"),
                context.getPackageName() + "_preferences.xml");

        File images = new File(context.getFilesDir(), "tb_images");
        String[] names = U.getImageFilenames();
        for(int i = 0; i < names.length; i++)
            target[DATA_FILES.length + 1 + i] = new File(images, names[i]);
        return target;
    }

    private static void copy(File from, File to) throws IOException {
        File parent = to.getParentFile();
        if(parent != null && !parent.exists() && !parent.mkdirs())
            throw new IOException("Cannot create backup directory");
        try(InputStream input = new BufferedInputStream(new FileInputStream(from));
                OutputStream output = new BufferedOutputStream(new FileOutputStream(to))) {
            byte[] buffer = new byte[8192];
            int count;
            while((count = input.read(buffer)) != -1)
                output.write(buffer, 0, count);
        }
    }

    /** Must be called only while there is no pending restore. */
    public static void capture(Context context) throws IOException {
        File folder = directory(context);
        if(folder.exists()) {
            if(new File(context.getFilesDir(), "restore_in_progress").exists())
                throw new IOException("Previous restore needs recovery");
            clear(context); // Stale, incomplete staging data cannot have changed settings.
        }
        if(!folder.mkdirs()) throw new IOException("Cannot prepare rollback directory");
        try {
            File[] target = targets(context);
            for(int i = 0; i < target.length; i++) {
                File destination = new File(folder, i + ".bak");
                if(target[i].isFile())
                    copy(target[i], destination);
                else if(!new File(folder, i + ".absent").createNewFile())
                    throw new IOException("Cannot record missing file");
            }
            if(!new File(folder, "ready").createNewFile())
                throw new IOException("Cannot complete rollback snapshot");
        } catch(IOException error) {
            clear(context);
            throw error;
        }
    }

    public static void rollback(Context context) throws IOException {
        File folder = directory(context);
        if(!new File(folder, "ready").isFile())
            throw new IOException("Rollback snapshot is not complete");
        File[] target = targets(context);
        // Validate the entire journal before changing even one target file.
        for(int i = 0; i < target.length; i++) {
            boolean exists = new File(folder, i + ".bak").isFile();
            boolean absent = new File(folder, i + ".absent").isFile();
            if(exists == absent) throw new IOException("Invalid rollback record " + i);
        }
        for(int i = 0; i < target.length; i++) {
            File original = new File(folder, i + ".bak");
            if(original.exists()) {
                File tmp = new File(target[i].getParentFile(), target[i].getName() + ".rollback_tmp");
                copy(original, tmp);
                if(target[i].exists() && !target[i].delete())
                    throw new IOException("Cannot replace file during rollback");
                if(!tmp.renameTo(target[i]))
                    throw new IOException("Cannot finish file rollback");
            } else if(target[i].exists() && !target[i].delete()) {
                throw new IOException("Cannot remove file during rollback");
            }
        }
    }

    /** Delete only the fixed, app-owned snapshot files; never delete user data. */
    public static void clear(Context context) {
        File folder = directory(context);
        if(!folder.exists()) return;
        int count = targets(context).length;
        for(int i = 0; i < count; i++) {
            new File(folder, i + ".bak").delete();
            new File(folder, i + ".absent").delete();
        }
        new File(folder, "ready").delete();
        folder.delete();
    }
}
