package com.github.onursert.bookpub;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SaveQuote {

    CustomWebView webView;
    List<List> quoteList;

    File parentFile;
    File file;
    FileOutputStream fileOutputStream;
    OutputStreamWriter writer;

    public SaveQuote(CustomWebView webView, List<List> quoteList) throws IOException {
        this.webView = webView;
        this.quoteList = quoteList;
    }

    public void getQuotes(String bookTitle) throws IOException {
        parentFile = new File(Environment.getExternalStorageDirectory(), "Book Quotes");
        file = new File(parentFile, bookTitle + " Quote List.txt");
        FileInputStream fileInputStream = new FileInputStream(file);
        InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        List<String> allLines = new ArrayList<>();
        String lines = "";
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (!line.contains("½½")) {
                lines += (line + System.getProperty("line.separator"));
            } else {
                if (lines != "") {
                    lines += line;
                    allLines.add(lines);
                    lines = "";
                } else {
                    allLines.add(line);
                }
            }
        }
        for (int i = 0; i < allLines.size(); i++) {
            String[] arrOfLine = allLines.get(i).split("½½");

            if (arrOfLine.length == 4) {
                List quoteInfo = new LinkedList();
                quoteInfo.add(arrOfLine[0]); //quote
                quoteInfo.add(arrOfLine[1]); //bookTitle
                quoteInfo.add(arrOfLine[2]); //pageNumber
                quoteInfo.add(arrOfLine[3]); //webViewScrollY

                quoteList.add(quoteInfo);
            }
        }
        fileInputStream.close();
        inputStreamReader.close();
        bufferedReader.close();
    }

    public void highlightQuote(int pageNumber) {
        webView.evaluateJavascript("function doSearch(text, backgroundColor) {\n" +
                "    if (window.find && window.getSelection) {\n" +
                "        var windowHeight = window.scrollY;\n" +
                "        document.designMode = 'on';\n" +
                "        var sel = window.getSelection();\n" +
                "        sel.collapse(document.body, 0);\n" +
                "        while (window.find(text)) {\n" +
                "            document.execCommand('HiliteColor', false, backgroundColor);\n" +
                "            sel.collapseToEnd();\n" +
                "        }\n" +
                "        document.designMode = 'off';\n" +
                "        window.scrollTo(0, windowHeight);\n" +
                "    }\n" +
                "}", null);

        for (int i = 0; i < quoteList.size(); i++) {
            if ((pageNumber == Integer.parseInt(quoteList.get(i).get(2).toString()))) {
                String editedQuote = quoteList.get(i).get(0).toString().replaceAll("'", "\\\\'");
                if (editedQuote.contains(System.getProperty("line.separator"))) {
                    String[] editedQuotes = editedQuote.split(System.getProperty("line.separator"));
                    for (int j = 0; j < editedQuotes.length; j++) {
                        webView.evaluateJavascript("doSearch('" + editedQuotes[j] + "', 'SkyBlue')", null);
                    }
                } else {
                    webView.evaluateJavascript("doSearch('" + editedQuote + "', 'SkyBlue')", null);
                }
            }
        }
    }

    public void removeQuote(String quote, String bookTitle, int turn, String themeBack) throws IOException {
        for (int i = turn; i < quoteList.size(); i++) {
            if (quote.contains(quoteList.get(i).get(0).toString())) {
                String removed = quoteList.get(i).get(0).toString();

                quoteList.remove(i);

                removed = removed.replaceAll("'", "\\\\'");
                if (removed.contains(System.getProperty("line.separator"))) {
                    String[] removedQuotes = removed.split(System.getProperty("line.separator"));
                    for (int j = 0; j < removedQuotes.length; j++) {
                        webView.evaluateJavascript("doSearch('" + removedQuotes[j] + "', '" + themeBack + "')", null);
                    }
                } else {
                    webView.evaluateJavascript("doSearch('" + removed + "', '" + themeBack + "')", null);
                }

                removeQuote(quote, bookTitle, turn, themeBack);

                quoteListChanged(quoteList, bookTitle);
            }
        }
    }

    public void addQuote(String quote, String bookTitle, int pageNumber, int webViewScrollY) throws IOException {
        if (!isExist(quote)) {
            isContaining(quote, 0);

            List quoteInfo = new LinkedList();
            quoteInfo.add(quote);
            quoteInfo.add(bookTitle);
            quoteInfo.add(pageNumber);
            quoteInfo.add(webViewScrollY);

            quoteList.add(quoteInfo);

            quote = quote.replaceAll("'", "\\\\'");
            if (quote.contains(System.getProperty("line.separator"))) {
                String[] quotes = quote.split(System.getProperty("line.separator"));
                for (int j = 0; j < quotes.length; j++) {
                    webView.evaluateJavascript("doSearch('" + quotes[j] + "', 'SkyBlue')", null);
                }
            } else {
                webView.evaluateJavascript("doSearch('" + quote + "', 'SkyBlue')", null);
            }

            quoteListChanged(quoteList, bookTitle);
        }
    }
    public boolean isExist(String quote) {
        for (int i = 0; i < quoteList.size(); i++) {
            if (quoteList.get(i).get(0).toString().contains(quote)) {
                return true;
            }
        }
        return false;
    }
    public void isContaining(String quote, int turn) {
        for (int i = turn; i < quoteList.size(); i++) {
            if (quote.contains(quoteList.get(i).get(0).toString())) {
                quoteList.remove(i);
                isContaining(quote, i);
            }
        }
    }

    public void quoteListChanged(List<List> quoteList, String bookTitle) throws IOException {
        parentFile = new File(Environment.getExternalStorageDirectory(), "Book Quotes");
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        file = new File(parentFile, bookTitle + " Quote List.txt");
        if (!file.exists()) {
            file.createNewFile();
        }
        fileOutputStream = new FileOutputStream(file, false);
        writer = new OutputStreamWriter(fileOutputStream);
        updateCache(quoteList);
        writer.close();
        if (fileOutputStream != null) {
            fileOutputStream.flush();
            fileOutputStream.close();
        }
    }
    public void updateCache(List<List> quoteList) throws IOException {
        for (int i = 0; i < quoteList.size(); i++) {
            writer.append(quoteList.get(i).get(0) + "½½" + quoteList.get(i).get(1) + "½½" + quoteList.get(i).get(2) + "½½" + quoteList.get(i).get(3) + "\r\n");
        }
    }
}
