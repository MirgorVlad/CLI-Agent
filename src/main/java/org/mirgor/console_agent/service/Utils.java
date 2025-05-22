package org.mirgor.console_agent.service;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    private static final EncodingRegistry REGISTRY = Encodings.newDefaultEncodingRegistry();
    private static final Encoding ENCODING = REGISTRY.getEncoding(EncodingType.CL100K_BASE);

    public static int countTokens(String text) {
        return ENCODING.encode(text).size();
    }

    public static List<File> findFilesByName(Path dir, String fileName) {
        List<File> result = new ArrayList<>();
        if (fileName == null || fileName.isEmpty()) return result;
        File[] files = dir.toFile().listFiles();
        if (files == null) return result;
        for (File file : files) {
            if (file.isDirectory()) {
                result.addAll(findFilesByName(file.toPath(), fileName));
            } else if (file.getName().equalsIgnoreCase(fileName)) {
                result.add(file);
            }
        }
        return result;
    }

    private static String getFilenameWithoutExtension(File file) {
        String name = file.getName();
        int index = name.lastIndexOf('.');
        return (index == -1) ? name : name.substring(0, index);
    }

    public static String getFileContents(InputStream in) throws IOException {
        return new String(in.readAllBytes());
    }
}