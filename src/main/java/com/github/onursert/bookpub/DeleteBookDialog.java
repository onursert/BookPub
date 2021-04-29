package com.github.onursert.bookpub;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;

import java.io.IOException;

public class DeleteBookDialog extends Dialog implements android.view.View.OnClickListener {

    public Activity activity;
    private String bookName;
    private String bookPath;
    RefreshEpub refreshEpub;
    CustomAdapter customAdapter;

    public TextView name;
    public CheckBox deleteDevice;
    public Button delete;
    public Button cancel;

    public DeleteBookDialog(Activity activity, String bookName, String bookPath, RefreshEpub refreshEpub, CustomAdapter customAdapter) {
        super(activity);
        this.activity = activity;
        this.bookPath = bookPath;
        this.bookName = bookName;
        this.refreshEpub = refreshEpub;
        this.customAdapter = customAdapter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.delete_book_dialog);
        
        name = (TextView) findViewById(R.id.bookNameTextView);
        name.setText("Do you want to delete " + bookName);

        deleteDevice = (CheckBox) findViewById(R.id.deleteCheckBox);

        delete = (Button) findViewById(R.id.deleteButton);
        delete.setOnClickListener(this);
        cancel = (Button) findViewById(R.id.cancelDeleteButton);
        cancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.deleteButton:
                try {
                    refreshEpub.deleteBook(refreshEpub.bookList, bookPath, deleteDevice.isChecked());
                    refreshEpub.deleteBook(customAdapter.searchedBookList, bookPath, deleteDevice.isChecked());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.cancelDeleteButton:
                dismiss();
                break;

            default:
                break;
        }
        dismiss();
    }
}
