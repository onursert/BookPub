package com.github.onursert.bookpub;

import android.content.Context;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnzipEpub {

    Context context;
    List<String> pagesRef;
    List<String> pages;

    public UnzipEpub(Context context, List<String> pagesRef, List<String> pages) {
        this.context = context;
        this.pagesRef = pagesRef;
        this.pages = pages;
    }

    public void Unzip(String srcDir) {
        final String inputEpub = srcDir;

        File src = new File(srcDir);
        String fileName = src.getName().substring(0, src.getName().length() - 5);
        final String outputFolder = context.getCacheDir() + File.separator + fileName + File.separator;

        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(inputEpub))) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                File file = new File(outputFolder, zipEntry.getName());
                try {
                    EnsureZipPathSafety(file, outputFolder);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                if (zipEntry.isDirectory()) {
                    file.mkdirs();
                } else {
                    File parent = file.getParentFile();
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }
                    try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file))) {
                        byte[] buffer = new byte[1024];
                        int location;
                        while ((location = zipInputStream.read(buffer)) != -1) {
                            bufferedOutputStream.write(buffer, 0, location);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                zipEntry = zipInputStream.getNextEntry();
            }
            File fileDir = new File(outputFolder);
            SearchOpf(fileDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void EnsureZipPathSafety(final File outputFile, final String destDirectory) throws Exception {
        String destDirCanonicalPath = (new File(destDirectory)).getCanonicalPath();
        String outputFileCanonicalPath = outputFile.getCanonicalPath();
        if (!outputFileCanonicalPath.startsWith(destDirCanonicalPath)) {
            throw new Exception("Found Zip Path Traversal Vulnerability");
        }
    }
    public void SearchOpf(File dir) throws IOException {
        File listFile[] = dir.listFiles();
        if (listFile != null) {
            for (int i = 0; i < listFile.length; i++) {
                if (listFile[i].isDirectory()) {
                    SearchOpf(listFile[i]);
                } else {
                    if (listFile[i].getName().endsWith(".opf")) {
                        SearchPageRefs(listFile[i].getAbsolutePath());
                        SearchPages(listFile[i].getAbsolutePath());
                        return;
                    }
                }
            }
        }
    }
    public void SearchPageRefs(String opfPath) throws IOException {
        InputStream inputStream = new FileInputStream(opfPath);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.contains("itemref") && line.contains("idref")) {
                int idrefIndex = 0;
                while (idrefIndex != -1) {
                    idrefIndex = line.indexOf("idref", idrefIndex);
                    if (idrefIndex != -1) {
                        idrefIndex += "idref".length();
                        int firstIndex = line.indexOf("\"", idrefIndex);
                        int lastIndex = line.indexOf("\"", firstIndex + 1);

                        if (firstIndex != -1 && lastIndex != -1) {
                            pagesRef.add(line.substring(firstIndex + 1, lastIndex));
                        }
                    }
                }
            }
        }
        inputStream.close();
        bufferedReader.close();
    }
    public void SearchPages(String pagePath) throws IOException {
        for (int j = 0; j < pagesRef.size(); j++) {
            InputStream inputStream = new FileInputStream(pagePath);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(pagesRef.get(j)) && line.contains("href")) {

                    int veryLastItemIndex = 0;
                    String str = line.substring(0, line.indexOf(pagesRef.get(j)));
                    int lastItemIndex = 0;
                    while (lastItemIndex != -1) {
                        lastItemIndex = str.indexOf("item", lastItemIndex);
                        if (lastItemIndex != -1) {
                            lastItemIndex += "item".length();
                            veryLastItemIndex = lastItemIndex;
                        }
                    }

                    int hrefIndex = line.indexOf("href", veryLastItemIndex);
                    int firstIndex = line.indexOf("\"", hrefIndex);
                    int lastIndex = line.indexOf("\"", firstIndex + 1);
                    if (firstIndex != -1 && lastIndex != -1) {
                        File pagePath_File = new File(pagePath);
                        if (!pages.contains(pagePath_File.getParent() + File.separator + line.substring(firstIndex + 1, lastIndex))) {
                            pages.add(pagePath_File.getParent() + File.separator + line.substring(firstIndex + 1, lastIndex));
                            break;
                        }
                    }
                }
            }
            inputStream.close();
            bufferedReader.close();
        }
    }
}
