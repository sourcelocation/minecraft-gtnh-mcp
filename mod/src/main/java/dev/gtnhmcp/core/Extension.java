package dev.gtnhmcp.core;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/** A plug-in discovered from {@code META-INF/services}; adding one never edits existing code. */
public interface Extension {

    /** False when the mod this extension reads from is not loaded. Must not touch that mod's classes. */
    default boolean available() {
        return true;
    }

    static <T extends Extension> List<T> load(Class<T> type) {
        List<T> found = new ArrayList<>();
        for (T extension : ServiceLoader.load(type, type.getClassLoader()))
            if (extension.available()) found.add(extension);
        return found;
    }
}
