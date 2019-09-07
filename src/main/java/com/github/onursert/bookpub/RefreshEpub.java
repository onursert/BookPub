package com.github.onursert.bookpub;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class RefreshEpub {

    Context context;
    CustomAdapter customAdapter;
    List<List> bookList;

    public RefreshEpub(Context context, CustomAdapter customAdapter, List<List> bookList) {
        this.context = context;
        this.customAdapter = customAdapter;
        this.bookList = bookList;
    }

    //Custom Shared Preferences
    public static final String myPref = "preferenceName";
    public String getFromPreferences(String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(myPref, 0);
        String str = sharedPreferences.getString(key, "null");
        return str;
    }
    public void setToPreferences(String key, String thePreference) {
        SharedPreferences.Editor editor = context.getSharedPreferences(myPref, 0).edit();
        editor.putString(key, thePreference);
        editor.commit();
    }

    //Read File From Internal Storage
    public void readFileFromInternalStorage() throws IOException {
        FileInputStream fileInputStream = context.openFileInput("bookList.txt");
        InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            String[] arrOfLine = line.split("½½");
            List bookInfo = new LinkedList();
            bookInfo.add(arrOfLine[0]); //bookTitle
            bookInfo.add(arrOfLine[1]); //bookAuthor
            bookInfo.add(arrOfLine[2]); //bookCover
            bookInfo.add(arrOfLine[3]); //bookPath
            bookInfo.add(arrOfLine[4]); //importTime
            bookInfo.add(arrOfLine[5]); //openTime
            bookInfo.add(arrOfLine[6]); //currentPage
            bookInfo.add(arrOfLine[7]); //currentScroll

            bookList.add(bookInfo);
        }
        fileInputStream.close();
        inputStreamReader.close();
        bufferedReader.close();
    }

    /*Search Begin*/
    FindTitle findTitle = new FindTitle();
    FindAuthor findAuthor = new FindAuthor();
    FindCover findCover = new FindCover();

    File file;
    File fileImages;

    FileOutputStream fileOutputStream;
    FileOutputStream fileOutputStreamImages;
    OutputStreamWriter writer;

    Bitmap bitmap;

    public void SearchEpub() throws IOException {
        file = new File(context.getFilesDir(), "bookList.txt");
        fileImages = new File(context.getFilesDir() + File.separator + "bookImages");

        if (!file.exists()) {
            file.createNewFile();
        }
        fileImages.mkdirs();

        fileOutputStream = new FileOutputStream(file, false);
        writer = new OutputStreamWriter(fileOutputStream);

        bitmap = null;

        isRemoved(0);
        FindEpub(Environment.getExternalStorageDirectory());
        sortByPreferences(bookList);

        writer.close();
        if (fileOutputStream != null) {
            fileOutputStream.flush();
            fileOutputStream.close();
        }

        if (fileOutputStreamImages != null) {
            fileOutputStreamImages.flush();
            fileOutputStreamImages.close();
        }
    }
    public void isRemoved(int turn) {
        for (int i = turn; i < bookList.size(); i++) {
            File file = new File((String) bookList.get(i).get(3));
            if (!file.exists()) {
                bookList.remove(i);
                removedCount++;
                isRemoved(i);
            }
        }
    }
    public void FindEpub(File dir) throws IOException {
        File listFile[] = dir.listFiles();
        if (listFile != null) {
            for (int i = 0; i < listFile.length; i++) {
                if (listFile[i].isDirectory()) {
                    FindEpub(listFile[i]);
                } else {
                    if (listFile[i].getName().endsWith(".epub") || listFile[i].getName().endsWith(".EPUB")) {
                        if (!isExist(listFile[i].getAbsolutePath())) {
                            String imageName = findTitle.FindTitle(listFile[i].getAbsolutePath()) + ".jpeg";
                            File imageItem = new File(fileImages, imageName);
                            if (!imageItem.exists()) {
                                bitmap = (Bitmap) findCover.FindCoverRef(listFile[i].getAbsolutePath());
                                if (bitmap != null) {
                                    fileOutputStreamImages = new FileOutputStream(imageItem);
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStreamImages);
                                } else {
                                    File fileNull = new File("null");
                                    imageItem = fileNull;
                                }
                            }

                            String importTime = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

                            List bookInfo = new LinkedList();
                            bookInfo.add(findTitle.FindTitle(listFile[i].getAbsolutePath())); //bookTitle
                            bookInfo.add(findAuthor.FindAuthor(listFile[i].getAbsolutePath())); //bookAuthor
                            bookInfo.add(imageItem); //bookCover
                            bookInfo.add(listFile[i].getAbsolutePath()); //bookPath
                            bookInfo.add(importTime); //importTime
                            bookInfo.add("19200423_000000"); //openTime
                            bookInfo.add(0); //currentPage
                            bookInfo.add(0); //currentScroll

                            if (fileOutputStreamImages != null) {
                                fileOutputStreamImages.flush();
                                fileOutputStreamImages.close();
                            }

                            addedCount++;
                            bookList.add(bookInfo);
                        }
                    }
                }
            }
        }
    }
    public boolean isExist(String path) {
        for (int i = 0; i < bookList.size(); i++) {
            if (bookList.get(i).get(3).equals(path)) {
                return true;
            }
        }
        return false;
    }
    public void sortByPreferences(List<List> bookList) throws IOException {
        if (getFromPreferences("sort").equals("sortTitle")) {
            sortTitle(bookList);
        } else if (getFromPreferences("sort").equals("sortAuthor")) {
            sortAuthor(bookList);
        } else if (getFromPreferences("sort").equals("sortImportTime")) {
            sortImportTime(bookList);
        } else if (getFromPreferences("sort").equals("sortOpenTime")) {
            sortOpenTime(bookList);
        } else {
            sortTitle(bookList);
        }
    }
    /*Search End*/

    //Menu Refresh, Sort, Show/Hide
    int addedCount;
    int removedCount;
    final Handler handler = new Handler();
    class refreshBooks extends AsyncTask<Void, Void, Void> {
        Toast toast = Toast.makeText(context, "Searching...", Toast.LENGTH_LONG);

        @Override
        protected void onPreExecute() {
            addedCount = 0;
            removedCount = 0;
            customAdapter.refreshingDoneBool = false;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    toast.show();
                    customAdapter.notifyDataSetChanged();
                    handler.postDelayed(this, 1000);
                }
            }, 1000);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                SearchEpub();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            handler.removeCallbacksAndMessages(null);
            toast.cancel();
            customAdapter.notifyDataSetChanged();
            customAdapter.refreshingDone(bookList);
            Toast.makeText(context, addedCount + " book(s) added and " + removedCount + " book(s) removed", Toast.LENGTH_SHORT).show();
        }
    }
    public void sortTitle(List<List> bookList) throws IOException {
        setToPreferences("sort", "sortTitle");
        Collections.sort(bookList, new Comparator<List>() {
            @Override
            public int compare(List o1, List o2) {
                try {
                    return o1.get(0).toString().compareTo(o2.get(0).toString());
                } catch (NullPointerException e) {
                    return 0;
                }
            }
        });
        bookListChanged(bookList);
    }
    public void sortAuthor(List<List> bookList) throws IOException {
        setToPreferences("sort", "sortAuthor");
        Collections.sort(bookList, new Comparator<List>() {
            @Override
            public int compare(List o1, List o2) {
                try {
                    return o1.get(1).toString().compareTo(o2.get(1).toString());
                } catch (NullPointerException e) {
                    return 0;
                }
            }
        });
        bookListChanged(bookList);
    }
    public void sortImportTime(List<List> bookList) throws IOException {
        setToPreferences("sort", "sortImportTime");
        Collections.sort(bookList, new Comparator<List>() {
            @Override
            public int compare(List o1, List o2) {
                try {
                    if (o2.get(4).toString().compareTo(o1.get(4).toString()) == 0) {
                        return o1.get(0).toString().compareTo(o2.get(0).toString());
                    } else {
                        return o2.get(4).toString().compareTo(o1.get(4).toString());
                    }
                } catch (NullPointerException e) {
                    return 0;
                }
            }
        });
        bookListChanged(bookList);
    }
    public void sortOpenTime(List<List> bookList) throws IOException {
        setToPreferences("sort", "sortOpenTime");
        Collections.sort(bookList, new Comparator<List>() {
            @Override
            public int compare(List o1, List o2) {
                try {
                    return o2.get(5).toString().compareTo(o1.get(5).toString());
                } catch (NullPointerException e) {
                    return 0;
                }
            }
        });
        bookListChanged(bookList);
    }

    //Write BookList To Cache
    public void bookListChanged(List<List> bookList) throws IOException {
        file = new File(context.getFilesDir(), "bookList.txt");
        if (!file.exists()) {
            file.createNewFile();
        }
        fileOutputStream = new FileOutputStream(file, false);
        writer = new OutputStreamWriter(fileOutputStream);
        updateCache(bookList);
        writer.close();
        if (fileOutputStream != null) {
            fileOutputStream.flush();
            fileOutputStream.close();
        }
    }
    public void updateCache(List<List> bookList) throws IOException {
        for (int i = 0; i < bookList.size(); i++) {
            writer.append(bookList.get(i).get(0) + "½½" + bookList.get(i).get(1) + "½½" + bookList.get(i).get(2) + "½½" + bookList.get(i).get(3) + "½½" + bookList.get(i).get(4) + "½½" + bookList.get(i).get(5) + "½½" + bookList.get(i).get(6) + "½½" + bookList.get(i).get(7) + "\r\n");
        }
    }

    //Functions Which Come From Another Class
    public void addOpenTime(List<List> bookList, String path, String openTime) throws IOException {
        for (int i = 0; i < bookList.size(); i++) {
            if (bookList.get(i).get(3).equals(path)) {
                List bookInfo = new LinkedList();
                bookInfo.add(bookList.get(i).get(0)); //bookTitle
                bookInfo.add(bookList.get(i).get(1)); //bookAuthor
                bookInfo.add(bookList.get(i).get(2)); //bookCover
                bookInfo.add(bookList.get(i).get(3)); //bookPath
                bookInfo.add(bookList.get(i).get(4)); //importTime
                bookInfo.add(openTime); //openTime
                bookInfo.add(bookList.get(i).get(6)); //currentPage
                bookInfo.add(bookList.get(i).get(7)); //currentScroll
                bookList.set(i, bookInfo);
                break;
            }
        }
        sortByPreferences(bookList);
        customAdapter.notifyDataSetChanged();
    }
    public void editBook(List<List> bookList, String title, String author, String path) throws IOException {
        for (int i = 0; i < bookList.size(); i++) {
            if (bookList.get(i).get(3).equals(path)) {
                File parentFile = new File(Environment.getExternalStorageDirectory(), "Book Quotes");
                File file = new File(parentFile, bookList.get(i).get(0) + " Quote List.txt");
                File parentFile2 = new File(Environment.getExternalStorageDirectory(), "Book Quotes");
                File file2 = new File(parentFile2, title + " Quote List.txt");
                file.renameTo(file2);
                
                List bookInfo = new LinkedList();
                bookInfo.add(title); //bookTitle
                bookInfo.add(author); //bookAuthor
                bookInfo.add(bookList.get(i).get(2)); //bookCover
                bookInfo.add(bookList.get(i).get(3)); //bookPath
                bookInfo.add(bookList.get(i).get(4)); //importTime
                bookInfo.add(bookList.get(i).get(5)); //openTime
                bookInfo.add(bookList.get(i).get(6)); //currentPage
                bookInfo.add(bookList.get(i).get(7)); //currentScroll
                bookList.set(i, bookInfo);
                break;
            }
        }
        sortByPreferences(bookList);
        customAdapter.notifyDataSetChanged();
    }
    public void deleteBook(List<List> bookList, String path, Boolean deleteDevice) throws IOException {
        for (int i = 0; i < bookList.size(); i++) {
            if (bookList.get(i).get(3).equals(path)) {
                bookList.remove(i);
                if (deleteDevice) {
                    File file = new File(path);
                    file.delete();
                }
                break;
            }
        }
        sortByPreferences(bookList);
        customAdapter.notifyDataSetChanged();
    }
    public void addCurrentPageScroll(List<List> bookList, String path, Integer currentPage, Integer currentScroll) throws IOException {
        for (int i = 0; i < bookList.size(); i++) {
            if (bookList.get(i).get(3).equals(path)) {
                List bookInfo = new LinkedList();
                bookInfo.add(bookList.get(i).get(0)); //bookTitle
                bookInfo.add(bookList.get(i).get(1)); //bookAuthor
                bookInfo.add(bookList.get(i).get(2)); //bookCover
                bookInfo.add(bookList.get(i).get(3)); //bookPath
                bookInfo.add(bookList.get(i).get(4)); //importTime
                bookInfo.add(bookList.get(i).get(5)); //openTime
                bookInfo.add(currentPage); //currentPage
                bookInfo.add(currentScroll); //currentScroll
                bookList.set(i, bookInfo);
                break;
            }
        }
        sortByPreferences(bookList);
        customAdapter.notifyDataSetChanged();
    }
}
