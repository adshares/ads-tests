/*
 * Copyright (C) 2018 Adshares sp. z. o.o.
 *
 * This file is part of ADS Tests
 *
 * ADS Tests is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ADS Tests is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ADS Tests. If not, see <https://www.gnu.org/licenses/>.
 */

package net.adshares.ads.qa.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

public class Utils {

    /**
     * Deletes directory with its content. Does not follow symbolic links.
     *
     * @param path directory to delete
     */
    public static void deleteDirectory(String path) throws IOException {
        Path dir = Paths.get(path);

        if (Files.exists(dir)) {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc == null) {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    } else {
                        // directory iteration failed
                        throw exc;
                    }
                }
            });
        }
    }

    /**
     * Formats time stamp as date String.
     *
     * @param timeStamp seconds from January 1st 1970
     * @return time stamp in "yyyy-MM-dd HH:mm:ss" format
     */
    public static String formatSecondsAsDate(long timeStamp) {
        Date date = Date.from(Instant.ofEpochSecond(timeStamp));
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    /**
     * Converts String to JsonObject.
     *
     * @param jsonString json formatted String
     * @return JsonObject
     */
    public static JsonObject convertStringToJsonObject(String jsonString) {
        JsonParser parser = new JsonParser();
        return parser.parse(jsonString).getAsJsonObject();
    }

    /**
     * Adds indentation to json String.
     *
     * @param jsonString json String
     * @return formatted json String
     */
    public static String jsonPrettyPrint(String jsonString) {
        JsonObject json = convertStringToJsonObject(jsonString);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(json);
    }

    /**
     * Return file content as String.
     *
     * @param fileName file name
     * @return file content or empty String, if error occurred
     */
    public static String readFileAsString(String fileName) {
        byte[] encoded;
        String fileContent;
        try {
            encoded = Files.readAllBytes(Paths.get(fileName));
            fileContent = new String(encoded, StandardCharsets.UTF_8);
        } catch (IOException e) {
            fileContent = "";
        }
        return fileContent;
    }
}
