package com.github.onursert.bookpub;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    Context context;
    RecyclerView recyclerView;
    CustomAdapter customAdapter;
    RefreshEpub refreshEpub;
    List<List> bookList = new ArrayList<>();

    SharedPreferences sharedPreferences;

    private DrawerLayout drawer;

    public static int REQUEST_PERMISSIONS = 1;

    private static MainActivity instance;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        instance = this;

        //Navigation Drawer
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_library);
        navigationView.getMenu().getItem(1).setCheckable(false);
        navigationView.getMenu().getItem(2).setCheckable(false);
        navigationView.getMenu().getItem(3).setCheckable(false);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        //RecyclerView
        recyclerView = (RecyclerView) findViewById(R.id.custom_RecylerView);
        customAdapter = new CustomAdapter(context, bookList);
        recyclerView.setAdapter(customAdapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        registerForContextMenu(recyclerView);

        //Check Permission
        if (!storagePermission()) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
        } else {
            ListBooksNCheckPreferences();
        }
    }
    public static MainActivity getInstance() {
        return instance;
    }

    //List Books and Check Shared Preferences
    public void ListBooksNCheckPreferences() {
        refreshEpub = new RefreshEpub(context, customAdapter, bookList);
        try {
            refreshEpub.readFileFromInternalStorage();
            customAdapter.notifyDataSetChanged();
        } catch (IOException e) {
            e.printStackTrace();
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.getBoolean("firstrun", true)) {
            sharedPreferences.edit().putBoolean("keep_screen_on", true).commit();
            sharedPreferences.edit().putBoolean("auto_sync", true).commit();
            sharedPreferences.edit().putBoolean("rotation_lock", true).commit();
            sharedPreferences.edit().putBoolean("where_i_left", true).commit();
            sharedPreferences.edit().putBoolean("firstrun", false).commit();
        }
        checkAutoSync();
        checkSharedPreferences();
    }

    //Check Shared Preferences
    public void checkAutoSync() {
        if (sharedPreferences.getBoolean("auto_sync", false) == true) {
            refreshEpub.new refreshBooks().execute();
        } else {
            customAdapter.refreshingDone(bookList);
        }
    }
    public void checkSharedPreferences() {
        if (sharedPreferences.getBoolean("keep_screen_on", false) == true) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        if (sharedPreferences.getBoolean("rotation_lock", false) == true) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            }
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (storagePermission()) {
            checkSharedPreferences();
        }
    }

    //Custom Shared Preferences
    public static final String myPref = "preferenceName";
    public String getFromPreferences(String key) {
        SharedPreferences sharedPreferences = getSharedPreferences(myPref, 0);
        String str = sharedPreferences.getString(key, "null");
        return str;
    }
    public void setToPreferences(String key, String thePreference) {
        SharedPreferences.Editor editor = getSharedPreferences(myPref, 0).edit();
        editor.putString(key, thePreference);
        editor.commit();
    }

    //Navigation Drawer
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.nav_library:
                drawer.closeDrawer(GravityCompat.START);
                break;
            case R.id.nav_settings:
                Intent intentMainSettings = new Intent(context, MainSettings.class);
                context.startActivity(intentMainSettings);
                break;
            case R.id.nav_feedback:
                sendEmail();
                break;
            case R.id.nav_contact:
                Intent browserIntent2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/4f6e7572"));
                startActivity(browserIntent2);
                break;
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    private void sendEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO)
                .setData(new Uri.Builder().scheme("mailto").build())
                .putExtra(Intent.EXTRA_EMAIL, new String[]{"BookPub <somethingaboutapp@gmail.com>"})
                .putExtra(Intent.EXTRA_SUBJECT, "Feedback")
                .putExtra(Intent.EXTRA_TEXT, "");

        ComponentName emailApp = intent.resolveActivity(getPackageManager());
        ComponentName unsupportedAction = ComponentName.unflattenFromString("com.android.fallback/.Fallback");
        if (emailApp != null && !emailApp.equals(unsupportedAction))
            try {
                Intent chooser = Intent.createChooser(intent, "Send email with");
                startActivity(chooser);
                return;
            } catch (ActivityNotFoundException ignored) {
            }
        Toast.makeText(this, "Couldn't find an email app and account", Toast.LENGTH_LONG).show();
    }
    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    //Menu Search
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);
        whichSort(menu);
        whichShowHide(menu);

        MenuItem menuItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) menuItem.getActionView();

        searchView.setOnSearchClickListener(new SearchView.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (customAdapter.refreshingDoneBool) {
                    customAdapter.refreshingDone(bookList);
                }
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                customAdapter.getFilter().filter(newText);
                return false;
            }
        });
        return true;
    }
    //Menu Refresh, Sort, Show/Hide
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!storagePermission()) {
            Toast.makeText(getApplicationContext(), "Permission is not granted.", Toast.LENGTH_LONG).show();
            return false;
        }

        switch (item.getItemId()) {
            case R.id.refresh:
                refreshEpub.new refreshBooks().execute();
                return true;

            case R.id.sortTitle:
                try {
                    refreshEpub.sortTitle(bookList);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                customAdapter.notifyDataSetChanged();
                whichSort(mainMenu);
                return true;
            case R.id.sortAuthor:
                try {
                    refreshEpub.sortAuthor(bookList);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                customAdapter.notifyDataSetChanged();
                whichSort(mainMenu);
                return true;
            case R.id.sortImportTime:
                try {
                    refreshEpub.sortImportTime(bookList);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                customAdapter.notifyDataSetChanged();
                whichSort(mainMenu);
                return true;
            case R.id.sortOpenTime:
                try {
                    refreshEpub.sortOpenTime(bookList);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                customAdapter.notifyDataSetChanged();
                whichSort(mainMenu);
                return true;

            case R.id.showHideImportTime:
                if (getFromPreferences("showHideImportTime").equals("Invisible")) {
                    setToPreferences("showHideImportTime", "Visible");
                } else {
                    setToPreferences("showHideImportTime", "Invisible");
                }
                this.recreate();
                whichShowHide(mainMenu);
                return true;
            case R.id.showHideOpenTime:
                if (getFromPreferences("showHideOpenTime").equals("Invisible")) {
                    setToPreferences("showHideOpenTime", "Visible");
                } else {
                    setToPreferences("showHideOpenTime", "Invisible");
                }
                this.recreate();
                whichShowHide(mainMenu);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    Menu mainMenu;
    public void whichSort(Menu mainMenu) {
        this.mainMenu = mainMenu;
        if (getFromPreferences("sort").equals("sortAuthor")) {
            mainMenu.findItem(R.id.sortTitle).setTitle(Html.fromHtml("<font color='black'>Title</font>"));
            mainMenu.findItem(R.id.sortAuthor).setTitle(Html.fromHtml("<font color='#008577'>Author</font>"));
            mainMenu.findItem(R.id.sortImportTime).setTitle(Html.fromHtml("<font color='black'>Import Time</font>"));
            mainMenu.findItem(R.id.sortOpenTime).setTitle(Html.fromHtml("<font color='black'>Open Time</font>"));
        } else if (getFromPreferences("sort").equals("sortImportTime")) {
            mainMenu.findItem(R.id.sortTitle).setTitle(Html.fromHtml("<font color='black'>Title</font>"));
            mainMenu.findItem(R.id.sortAuthor).setTitle(Html.fromHtml("<font color='black'>Author</font>"));
            mainMenu.findItem(R.id.sortImportTime).setTitle(Html.fromHtml("<font color='#008577'>Import Time</font>"));
            mainMenu.findItem(R.id.sortOpenTime).setTitle(Html.fromHtml("<font color='black'>Open Time</font>"));
        } else if (getFromPreferences("sort").equals("sortOpenTime")) {
            mainMenu.findItem(R.id.sortTitle).setTitle(Html.fromHtml("<font color='black'>Title</font>"));
            mainMenu.findItem(R.id.sortAuthor).setTitle(Html.fromHtml("<font color='black'>Author</font>"));
            mainMenu.findItem(R.id.sortImportTime).setTitle(Html.fromHtml("<font color='black'>Import Time</font>"));
            mainMenu.findItem(R.id.sortOpenTime).setTitle(Html.fromHtml("<font color='#008577'>Open Time</font>"));
        } else {
            setToPreferences("sort", "sortTitle");
            mainMenu.findItem(R.id.sortTitle).setTitle(Html.fromHtml("<font color='#008577'>Title</font>"));
            mainMenu.findItem(R.id.sortAuthor).setTitle(Html.fromHtml("<font color='black'>Author</font>"));
            mainMenu.findItem(R.id.sortImportTime).setTitle(Html.fromHtml("<font color='black'>Import Time</font>"));
            mainMenu.findItem(R.id.sortOpenTime).setTitle(Html.fromHtml("<font color='black'>Open Time</font>"));
        }
    }
    public void whichShowHide(Menu mainMenu) {
        this.mainMenu = mainMenu;
        if (getFromPreferences("showHideImportTime").equals("Invisible")) {
            mainMenu.findItem(R.id.showHideImportTime).setTitle(Html.fromHtml("<font color='black'>Import Time</font>"));
        } else {
            setToPreferences("showHideImportTime", "Visible");
            mainMenu.findItem(R.id.showHideImportTime).setTitle(Html.fromHtml("<font color='#008577'>Import Time</font>"));
        }
        if (getFromPreferences("showHideOpenTime").equals("Invisible")) {
            mainMenu.findItem(R.id.showHideOpenTime).setTitle(Html.fromHtml("<font color='black'>Open Time</font>"));
        } else {
            setToPreferences("showHideOpenTime", "Visible");
            mainMenu.findItem(R.id.showHideOpenTime).setTitle(Html.fromHtml("<font color='#008577'>Open Time</font>"));
        }
    }

    //Context Menu
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.custom_RecylerView) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.edit_book_context_menu, menu);
        }
    }
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit:
                EditBookDialog editBook = new EditBookDialog(this, bookList.get(customAdapter.position).get(0).toString(), bookList.get(customAdapter.position).get(1).toString(), bookList.get(customAdapter.position).get(3).toString(), refreshEpub, customAdapter);
                editBook.show();
                return true;
            case R.id.delete:
                DeleteBookDialog deleteBook = new DeleteBookDialog(this, bookList.get(customAdapter.position).get(3).toString(), refreshEpub, customAdapter);
                deleteBook.show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    //Check Storage Permission
    public boolean storagePermission() {
        return (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ListBooksNCheckPreferences();
            } else {
                Toast.makeText(getApplicationContext(), "Permission is not granted.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}
