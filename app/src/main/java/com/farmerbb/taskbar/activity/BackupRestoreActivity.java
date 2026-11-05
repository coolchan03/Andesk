/* Copyright 2020 Braden Farmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.farmerbb.taskbar.activity;

import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.backup.BackupUtils;
import com.farmerbb.taskbar.backup.JSONBackupAgent;
import com.farmerbb.taskbar.backup.RestoreJournal;
import com.farmerbb.taskbar.util.U;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class BackupRestoreActivity extends AbstractProgressActivity {
    private static final long MAX_ARCHIVE_BYTES = 64L * 1024 * 1024;
    private static final int MAX_JSON_BYTES = 4 * 1024 * 1024;
    private static final int MAX_IMAGE_BYTES = 12 * 1024 * 1024;

    private static byte[] readLimited(InputStream input, int limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while((read = input.read(buffer)) != -1) {
            if(output.size() > limit - read) throw new IOException("Backup entry exceeds size limit");
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }
    private static void copyLimited(InputStream input, OutputStream output, long limit) throws IOException {
        byte[] buffer = new byte[8192];
        long size = 0;
        int read;
        while((read = input.read(buffer)) != -1) {
            size += read;
            if(size > limit) throw new IOException("Backup file exceeds size limit");
            output.write(buffer, 0, read);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int enter = getResources().getIdentifier("activity_close_enter", "anim", "android");
        int exit = getResources().getIdentifier("activity_close_exit", "anim", "android");
        overridePendingTransition(enter, exit);

        int requestCode = getIntent().getIntExtra("request_code", -1);
        Uri uri = getIntent().getParcelableExtra("uri");

        boolean isExport = requestCode == U.EXPORT;
        boolean isImport = requestCode == U.IMPORT;

        TextView textView = findViewById(R.id.progress_message);
        if(isExport) textView.setText(R.string.tb_backing_up_settings);
        if(isImport) textView.setText(R.string.tb_restoring_settings);

        if(savedInstanceState != null) return;

        new Thread(() -> {
            if(isExport) exportData(uri);
            if(isImport) importData(uri);

            finish();
            overridePendingTransition(enter, exit);
        }).start();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void exportData(Uri uri) {
        try(ZipOutputStream output = new ZipOutputStream(
                    new BufferedOutputStream(getContentResolver().openOutputStream(uri))
            )) {

            output.putNextEntry(new ZipEntry("backup.json"));
            JSONObject json = new JSONObject();

            BackupUtils.backup(this, new JSONBackupAgent(json));

            output.write(json.toString().getBytes());
            output.closeEntry();

            File imagesDir = new File(getFilesDir(), "tb_images");
            imagesDir.mkdirs();

            for(String filename : U.getImageFilenames()) {
                File customImage = new File(imagesDir, filename);
                if(customImage.exists()) {
                    output.putNextEntry(new ZipEntry("tb_images/" + filename));

                    try(BufferedInputStream input =
                                new BufferedInputStream(new FileInputStream(customImage))) {
                        copyLimited(input, output, MAX_IMAGE_BYTES);
                    }
                    output.closeEntry();
                }
            }

            setResult(R.string.tb_backup_successful);
        } catch (Throwable e) {
            setResult(R.string.tb_backup_failed);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void importData(Uri uri) {
        File importedFile = new File(getFilesDir(), "temp.zip");
        File statusFile = new File(getFilesDir(), "restore_in_progress");
        File successFile = new File(getFilesDir(), "restore_successful");
        boolean startedRestore = false;
        try {
            if(uri == null) throw new IOException("Missing backup URI");
            try(InputStream input = new BufferedInputStream(getContentResolver().openInputStream(uri));
                OutputStream output = new BufferedOutputStream(new FileOutputStream(importedFile))) {
                copyLimited(input, output, MAX_ARCHIVE_BYTES);
            }
            try(ZipFile archive = new ZipFile(importedFile)) {
                ZipEntry settings = archive.getEntry("backup.json");
                if(settings == null || settings.isDirectory())
                    throw new IOException("Missing backup.json");
                JSONObject json;
                try(InputStream input = new BufferedInputStream(archive.getInputStream(settings))) {
                    json = new JSONObject(new String(readLimited(input, MAX_JSON_BYTES),
                            StandardCharsets.UTF_8));
                }
                JSONBackupAgent agent = new JSONBackupAgent(json);
                BackupUtils.validateBackup(agent);

                // Validate all image entries before modifying saved settings.
                java.util.Map<String, byte[]> images = new java.util.HashMap<>();
                for(String filename : U.getImageFilenames()) {
                    ZipEntry entry = archive.getEntry("tb_images/" + filename);
                    if(entry != null) {
                        if(entry.isDirectory()) throw new IOException("Invalid image entry");
                        try(InputStream input = archive.getInputStream(entry)) {
                            images.put(filename, readLimited(input, MAX_IMAGE_BYTES));
                        }
                    }
                }

                if(statusFile.exists() || successFile.exists())
                    throw new IOException("Another restore is already pending");
                RestoreJournal.capture(this);
                if(!statusFile.createNewFile())
                    throw new IOException("Cannot record restore status");
                startedRestore = true;
                BackupUtils.restore(this, agent);

                File imagesDir = new File(getFilesDir(), "tb_images");
                if(!imagesDir.isDirectory() && !imagesDir.mkdirs())
                    throw new IOException("Cannot create image directory");
                for(String filename : U.getImageFilenames()) {
                    File image = new File(imagesDir, filename);
                    byte[] data = images.get(filename);
                    if(data == null) {
                        if(image.exists() && !image.delete())
                            throw new IOException("Cannot remove old image");
                    } else {
                        try(OutputStream output = new BufferedOutputStream(new FileOutputStream(image))) {
                            output.write(data);
                        }
                    }
                }
            }
            if(!statusFile.renameTo(successFile))
                throw new IOException("Cannot finalize backup restore");
        } catch(Exception e) {
            if(startedRestore && !successFile.exists()) {
                try {
                    RestoreJournal.rollback(this);
                } catch(IOException rollbackError) {
                    android.util.Log.e("Andesk", "Backup rollback will be retried on startup", rollbackError);
                }
            } else if(!startedRestore) {
                RestoreJournal.clear(this);
            }
            setResult(startedRestore ? R.string.tb_backup_failed : R.string.tb_backup_file_invalid);
        } finally {
            if(importedFile.exists()) importedFile.delete();
            if(startedRestore) U.restartApp(this, false);
        }
    }
}
