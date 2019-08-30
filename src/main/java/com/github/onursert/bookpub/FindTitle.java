package com.github.onursert.bookpub;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FindTitle {

    public String FindTitle(String srcDir) throws IOException {
        srcDir = URLDecoder.decode(srcDir, "UTF-8");
        ZipFile zipFile = new ZipFile(srcDir);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            if (zipEntry.toString().endsWith(".opf")) {
                InputStream inputStream = zipFile.getInputStream(zipEntry);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.contains("title") && !line.contains("calibre:title_sort")) {
                        int titleIndex = line.indexOf("title");
                        int firstIndex = line.indexOf(">", titleIndex);
                        int lastIndex = line.indexOf("<", firstIndex);
                        if (firstIndex != -1 && lastIndex != -1) {
                            return line.substring(firstIndex + 1, lastIndex);
                        } else {
                            File file = new File(srcDir);
                            return file.getName().substring(0, file.getName().length() - 5);
                        }
                    }
                }
                inputStream.close();
                bufferedReader.close();
                File file = new File(srcDir);
                return file.getName().substring(0, file.getName().length() - 5);
            }
        }
        zipFile.close();
        File file = new File(srcDir);
        return file.getName().substring(0, file.getName().length() - 5);
    }
}
