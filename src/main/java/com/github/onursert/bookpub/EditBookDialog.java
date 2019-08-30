package com.github.onursert.bookpub;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;

public class EditBookDialog extends Dialog implements android.view.View.OnClickListener {

    public Activity activity;
    private String bookTitle;
    private String bookAuthor;
    private String bookPath;
    RefreshEpub refreshEpub;
    CustomAdapter customAdapter;

    public Button update;
    public Button cancel;
    public EditText title;
    public EditText author;

    public EditBookDialog(Activity activity, String bookTitle, String bookAuthor, String bookPath, RefreshEpub refreshEpub, CustomAdapter customAdapter) {
        super(activity);
        this.activity = activity;
        this.bookTitle = bookTitle;
        this.bookAuthor = bookAuthor;
        this.bookPath = bookPath;
        this.refreshEpub = refreshEpub;
        this.customAdapter = customAdapter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.edit_book_dialog);

        title = (EditText) findViewById(R.id.editBookTitle);
        title.setText(bookTitle, TextView.BufferType.EDITABLE);
        author = (EditText) findViewById(R.id.editBookAuthor);
        author.setText(bookAuthor, TextView.BufferType.EDITABLE);

        update = (Button) findViewById(R.id.updateButton);
        update.setOnClickListener(this);
        cancel = (Button) findViewById(R.id.cancelEditButton);
        cancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.updateButton:
                try {
                    refreshEpub.editBook(refreshEpub.bookList, title.getText().toString(), author.getText().toString(), bookPath);
                    refreshEpub.editBook(customAdapter.searchedBookList, title.getText().toString(), author.getText().toString(), bookPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.cancelEditButton:
                dismiss();
                break;

            default:
                break;
        }
        dismiss();
    }
}
