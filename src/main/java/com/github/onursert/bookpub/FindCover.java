package com.github.onursert.bookpub;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FindCover {

    public Bitmap FindCoverRef(String srcDir) throws IOException {
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
                    if (line.contains("itemref") && line.contains("idref")) {
                        int idrefIndex = line.indexOf("idref");
                        int firstIndex = line.indexOf("\"", idrefIndex);
                        int lastIndex = line.indexOf("\"", firstIndex + 1);
                        if (firstIndex != -1 && lastIndex != -1 && firstIndex + 1 != lastIndex) {
                            return FindCoverHtml(srcDir, line.substring(firstIndex + 1, lastIndex));
                        } else {
                            return FindCoverItself(srcDir);
                        }
                    }
                }
                inputStream.close();
                bufferedReader.close();
                return FindCoverItself(srcDir);
            }
        }
        zipFile.close();
        return FindCoverItself(srcDir);
    }

    public Bitmap FindCoverHtml(String srcDir, String refName) throws IOException {
        refName = URLDecoder.decode(refName, "UTF-8");
        ZipFile zipFile = new ZipFile(srcDir);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            if (zipEntry.toString().endsWith(".opf")) {
                InputStream inputStream = zipFile.getInputStream(zipEntry);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.contains(refName) && line.contains("href")) {
                        int veryLastItemIndex = 0;
                        String str = line.substring(0, line.indexOf(refName));
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
                        if (firstIndex != -1 && lastIndex != -1 && firstIndex + 1 != lastIndex) {
                            return FindCoverImage(srcDir, line.substring(firstIndex + 1, lastIndex));
                        } else {
                            return FindCoverItself(srcDir);
                        }
                    }
                }
                inputStream.close();
                bufferedReader.close();
                return FindCoverItself(srcDir);
            }
        }
        zipFile.close();
        return FindCoverItself(srcDir);
    }

    public Bitmap FindCoverImage(String srcDir, String htmlName) throws IOException {
        htmlName = URLDecoder.decode(htmlName, "UTF-8");
        ZipFile zipFile = new ZipFile(srcDir);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            if (zipEntry.toString().contains(htmlName)) {
                InputStream inputStream = zipFile.getInputStream(zipEntry);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    CharSequence[] charSequence = {"<image", "<img"};
                    for (int i = 0; i < charSequence.length; i++) {
                        if (line.contains(charSequence[i])) {
                            int imgIndex = line.indexOf(charSequence[i].toString());
                            if (line.contains("href")) {
                                int hrefIndex = line.indexOf("href", imgIndex);
                                int firstIndex = line.indexOf("\"", hrefIndex);
                                int lastIndex = line.indexOf("\"", firstIndex + 1);
                                if (firstIndex != -1 && lastIndex != -1 && firstIndex + 1 != lastIndex) {
                                    return FindCoverLast(srcDir, line.substring(firstIndex + 1, lastIndex));
                                } else {
                                    return FindCoverItself(srcDir);
                                }
                            } else if (line.contains("src")) {
                                int srcIndex = line.indexOf("src", imgIndex);
                                int firstIndex = line.indexOf("\"", srcIndex);
                                int lastIndex = line.indexOf("\"", firstIndex + 1);

                                if (firstIndex != -1 && lastIndex != -1 && firstIndex + 1 != lastIndex) {
                                    return FindCoverLast(srcDir, line.substring(firstIndex + 1, lastIndex));
                                } else {
                                    return FindCoverItself(srcDir);
                                }
                            } else {
                                return FindCoverItself(srcDir);
                            }
                        }
                    }
                }
                inputStream.close();
                bufferedReader.close();
                return FindCoverItself(srcDir);
            }
        }
        zipFile.close();
        return FindCoverItself(srcDir);
    }

    public Bitmap FindCoverLast(String srcDir, String imageName) throws IOException {
        imageName = URLDecoder.decode(imageName, "UTF-8");
        ZipFile zipFile = new ZipFile(srcDir);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        Bitmap photo = null;
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            if (imageName.contains("/")) {
                String[] arrOfStr = imageName.split("/");
                imageName = arrOfStr[arrOfStr.length - 1];
            }

            /*
            if(imageName.contains("..")){
                int dotdotIndex = imageName.indexOf("..");
                int firstIndex = imageName.indexOf("/", dotdotIndex);
                imageName = imageName.substring(firstIndex);
            }
            */

            if (zipEntry.toString().contains(imageName)) {
                photo = BitmapFactory.decodeStream(zipFile.getInputStream(zipEntry));
                return photo;
            }
        }
        FindCoverItself(srcDir);
        zipFile.close();
        return null;
    }

    public Bitmap FindCoverItself(String srcDir) throws IOException {
        ZipFile zipFile = new ZipFile(srcDir);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        Bitmap photo = null;
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            CharSequence[] charSequence = {"cover.jpg", "cover.JPG", "cover.jpeg", "cover.JPEG", "cover.png", "cover.PNG"};
            for (int i = 0; i < charSequence.length; i++) {
                if (zipEntry.toString().contains(charSequence[i])) {
                    photo = BitmapFactory.decodeStream(zipFile.getInputStream(zipEntry));
                    return photo;
                }
            }
        }
        FindCoverItself2(srcDir);
        zipFile.close();
        return null;
    }

    public Bitmap FindCoverItself2(String srcDir) throws IOException {
        ZipFile zipFile = new ZipFile(srcDir);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        Bitmap photo = null;
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            CharSequence[] charSequence = {"jpg", "JPG", "jpeg", "JPEG", "png", "PNG"};
            for (int i = 0; i < charSequence.length; i++) {
                if (zipEntry.toString().contains(charSequence[i])) {
                    photo = BitmapFactory.decodeStream(zipFile.getInputStream(zipEntry));
                    return photo;
                }
            }
        }
        zipFile.close();
        return null;
    }
}
