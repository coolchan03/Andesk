/* Copyright 2026 Andesk contributors
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

package com.farmerbb.taskbar.util;

import android.content.Context;
import android.util.AtomicFile;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Crash-safe persistence for the small serialized lists kept in app storage (pinned apps,
 * blocked apps, window sizes). Writes go through {@link AtomicFile}, so a crash or a full disk
 * in the middle of a save leaves the previous contents intact instead of a truncated file, and
 * every stream is closed on all paths.
 */
final class SerializedStore {
    private static final String TAG = "Andesk";

    private SerializedStore() {}

    /** @return the stored object, or null when the file is missing or unreadable. */
    static <T extends Serializable> T load(Context context, String name, Class<T> type) {
        AtomicFile file = new AtomicFile(context.getFileStreamPath(name));
        try(FileInputStream input = file.openRead();
                ObjectInputStream objects = new ObjectInputStream(input)) {
            Object value = objects.readObject();
            return type.isInstance(value) ? type.cast(value) : null;
        } catch(IOException | ClassNotFoundException | RuntimeException e) {
            // A missing file is the normal first-run case; anything else deserves a log line.
            if(file.getBaseFile().exists())
                Log.w(TAG, "Unable to read " + name + "; starting empty", e);
            return null;
        }
    }

    /** @return true if the value was fully written. */
    static boolean save(Context context, String name, Serializable value) {
        AtomicFile file = new AtomicFile(context.getFileStreamPath(name));
        FileOutputStream output = null;
        try {
            output = file.startWrite();
            ObjectOutputStream objects = new ObjectOutputStream(output);
            objects.writeObject(value);
            objects.flush();
            file.finishWrite(output);
            return true;
        } catch(IOException | RuntimeException e) {
            Log.w(TAG, "Unable to save " + name, e);
            if(output != null) file.failWrite(output);
            return false;
        }
    }
}
