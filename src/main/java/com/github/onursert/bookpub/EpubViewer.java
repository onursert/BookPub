package com.github.onursert.bookpub;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class EpubViewer extends AppCompatActivity {

    Context context;
    CustomWebView webView;
    String path;

    SeekBar seekBar;
    boolean seeking = false;

    UnzipEpub unzipEpub;
    List<String> pagesRef = new ArrayList<>();
    List<String> pages = new ArrayList<>();
    int pageNumber = 0;

    RefreshEpub refreshEpub;
    SharedPreferences sharedPreferences;

    DrawerLayout drawer;
    NavigationView navigationViewContent;
    NavigationView navigationViewQuote;
    ImageButton shareQuotesButton;
    ImageButton deleteQuotesButton;

    SaveQuote saveQuote;
    List<List> quoteList = new ArrayList<>();
    String bookTitle;
    String gQuote = "";
    boolean searchViewLongClick = false;

    FindTitle findTitle = new FindTitle();
    
    int webViewScrollAmount = 0;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_epub_viewer);
        context = getApplicationContext();

        //Toolbar
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //WebView
        webView = (CustomWebView) findViewById(R.id.custom_WebView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDefaultTextEncodingName("utf-8");
        webView.setGestureDetector(new GestureDetector(new CustomeGestureDetector()));
        webView.setOnTouchListener(new View.OnTouchListener() {
            private static final int MAX_CLICK_DURATION = 60;
            private long startClickTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                SyncWebViewScrollSeekBar();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        startClickTime = Calendar.getInstance().getTimeInMillis();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                        if (clickDuration < MAX_CLICK_DURATION) {
                            RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.seekLayout);
                            if (relativeLayout.getVisibility() == View.VISIBLE) {
                                relativeLayout.setVisibility(View.GONE);
                            } else if (relativeLayout.getVisibility() == View.GONE) {
                                relativeLayout.setVisibility(View.VISIBLE);
                            }

                            if (toolbar.getVisibility() == View.VISIBLE) {
                                toolbar.setVisibility(View.GONE);
                                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                            } else if (toolbar.getVisibility() == View.GONE) {
                                toolbar.setVisibility(View.VISIBLE);
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                            }
                        }
                    }
                }
                return false;
            }
        });
        //Save Quotes
        try {
            saveQuote = new SaveQuote(webView, quoteList);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Seekbar
        final TextView textViewPercent = (TextView) findViewById(R.id.textViewPercent);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setMax(100);
        seekBar.setPadding(100, 0, 100, 0);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                this.progress = progress;
                textViewPercent.setText(progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                seeking = true;

                float whichPage = pages.size() * (float) progress / seekBar.getMax();

                float webViewHeight = (webView.getContentHeight() * webView.getScale()) - webView.getHeight();
                float franction = whichPage - ((int) whichPage);
                final int whichScroll = (int) (webViewHeight * franction);

                if (pages.size() >= 0 && pages.size() > whichPage) {
                    webView.loadUrl("file://" + pages.get((int) whichPage));
                    webView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            webView.scrollTo(0, whichScroll);
                            seeking = false;
                        }
                    }, 300);
                }
            }
        });

        refreshEpub = MainActivity.getInstance().refreshEpub;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        //Unzip and Show Epub
        path = getIntent().getStringExtra("path");
        try {
            Uri uri = this.getIntent().getData();
            if (uri != null) {
                path = Environment.getExternalStorageDirectory() + File.separator + uri.getLastPathSegment().split(":")[1];
                bookTitle = findTitle.FindTitle(path);
                getSupportActionBar().setTitle(bookTitle);
            } else {
                path = getIntent().getStringExtra("path");
                bookTitle = getIntent().getStringExtra("title");
                getSupportActionBar().setTitle(bookTitle);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        unzipEpub = new UnzipEpub(context, pagesRef, pages);
        unzipEpub.Unzip(path);
        if (pages.size() > 0) {
            if (sharedPreferences.getBoolean("where_i_left", false) == true) {
                if (getIntent().getStringExtra("currentPage") != null) {
                    pageNumber = Integer.parseInt(getIntent().getStringExtra("currentPage"));
                } else {
                    pageNumber = 0;
                }
                if (getIntent().getStringExtra("currentScroll") != null) {
                    webViewScrollAmount = Integer.parseInt(getIntent().getStringExtra("currentScroll"));
                }
            }
            webView.loadUrl("file://" + pages.get(pageNumber));
        }
        else {
            finish();
            Toast.makeText(context, "Unable to open", Toast.LENGTH_LONG).show();
        }

        //Save Quotes Get Quotes
        try {
            saveQuote.getQuotes(bookTitle);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Navigation Drawer
        drawer = findViewById(R.id.drawer_layout);
        navigationViewContent = findViewById(R.id.nav_view_content);
        for (int i = 0; i < pages.size(); i++) {
            String[] firstSplittedLink = pages.get(i).split("/");
            String[] secondSplittedLink = firstSplittedLink[firstSplittedLink.length - 1].split("\\.");
            navigationViewContent.getMenu().add(secondSplittedLink[0]);
            navigationViewContent.getMenu().getItem(i).setCheckable(true);

            navigationViewContent.getMenu().getItem(i).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    for (int i = 0; i < pages.size(); i++) {
                        String[] firstSplittedLink = pages.get(i).split("/");
                        String[] secondSplittedLink = firstSplittedLink[firstSplittedLink.length - 1].split("\\.");
                        if (secondSplittedLink[0].equals(item.toString())) {
                            webView.loadUrl("file://" + pages.get(i));
                            webViewScrollAmount = 0;
                            break;
                        }
                    }
                    drawer.closeDrawer(GravityCompat.START);
                    return false;
                }
            });
        }
        navigationViewQuote = findViewById(R.id.nav_view_quote);
        shareQuotesButton = navigationViewQuote.getHeaderView(0).findViewById(R.id.sharequotes);
        deleteQuotesButton = navigationViewQuote.getHeaderView(0).findViewById(R.id.deletequotes);
        shareQuotesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_SUBJECT, bookTitle + " Quote List");
                File parentFile = new File(Environment.getExternalStorageDirectory(), "Book Quotes");
                File file = new File(parentFile, bookTitle + " Quote List.txt");
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                intent.setType("text/plain");
                startActivity(Intent.createChooser(intent, bookTitle + " Quote List"));
            }
        });
        deleteQuotesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File parentFile = new File(Environment.getExternalStorageDirectory(), "Book Quotes");
                File file = new File(parentFile, bookTitle + " Quote List.txt");
                file.delete();
                quoteList.clear();
                reloadNavQuote();
                webView.reload();
            }
        });
        reloadNavQuote();

        checkSharedPreferences();
    }
    //After onCreate
    @Override
    protected void onStart() {
        super.onStart();
        webView.postDelayed(new Runnable() {
            @Override
            public void run() {
                webView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        webView.scrollTo(0, webViewScrollAmount);
                    }
                }, 300);
                webView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        webView.scrollTo(0, webViewScrollAmount);
                    }
                }, 500);
                webView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        webView.scrollTo(0, webViewScrollAmount);
                    }
                }, 750);
                webView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        webView.scrollTo(0, webViewScrollAmount);
                    }
                }, 1000);
                webView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        webView.scrollTo(0, webViewScrollAmount);
                    }
                }, 1500);
                webView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        webView.scrollTo(0, webViewScrollAmount);
                    }
                }, 2000);
                saveQuote.highlightQuote(pageNumber);
                SyncWebViewScrollSeekBar();
            }
        }, 500);
    }

    //Navigation Drawer
    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (drawer.isDrawerOpen(GravityCompat.END)) {
            drawer.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
            try {
                refreshEpub.addCurrentPageScroll(refreshEpub.bookList, path, pageNumber, webView.getScrollY());
                refreshEpub.addCurrentPageScroll(refreshEpub.customAdapter.searchedBookList, path, pageNumber, webView.getScrollY());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void reloadNavQuote() {
        navigationViewQuote.getMenu().clear();
        for (int i = 0; i < quoteList.size(); i++) {
            navigationViewQuote.getMenu().add(quoteList.get(i).get(0).toString());

            navigationViewQuote.getMenu().getItem(i).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    for (int j = 0; j < quoteList.size(); j++) {
                        if (quoteList.get(j).get(0).toString().equals(item.toString())) {
                            webView.loadUrl("file://" + pages.get(Integer.parseInt(quoteList.get(j).get(2).toString())));
                            webViewScrollAmount = Integer.parseInt(quoteList.get(j).get(3).toString());
                        }
                    }
                    drawer.closeDrawer(GravityCompat.END);
                    return false;
                }
            });
        }

        if (quoteList.isEmpty()) {
            shareQuotesButton.setVisibility(View.GONE);
            deleteQuotesButton.setVisibility(View.GONE);
        } else {
            shareQuotesButton.setVisibility(View.VISIBLE);
            deleteQuotesButton.setVisibility(View.VISIBLE);
        }
    }

    //Check Shared Preferences
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
        checkSharedPreferences();
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

    //Menu Back
    @Override
    public boolean onSupportNavigateUp() {
        try {
            refreshEpub.addCurrentPageScroll(refreshEpub.bookList, path, pageNumber, webView.getScrollY());
            refreshEpub.addCurrentPageScroll(refreshEpub.customAdapter.searchedBookList, path, pageNumber, webView.getScrollY());
        } catch (IOException e) {
            e.printStackTrace();
        }
        finish();
        return true;
    }
    //Menu Search, Back
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.epub_viewer_menu, menu);
        whichFontFamily(menu);
        whichFontSize(menu);
        whichFontStyle(menu);
        whichFontWeight(menu);
        whichTextAlign(menu);
        whichLineHeight(menu);
        whichMargin(menu);
        whichTheme(menu);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                webView.loadUrl("javascript:(function() { " + "var text=''; setInterval(function(){ if (window.getSelection().toString() && text!==window.getSelection().toString()){ text=window.getSelection().toString(); console.log(text); }}, 20);" + "})()");
                webView.setWebChromeClient(new WebChromeClient() {
                    public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                        gQuote = message;
                    }
                });
                
                @Override
                public void onProgressChanged(WebView view, int progress) {
                    if (view.getProgress() == 100) {
                        webView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                webView.scrollTo(0, webViewScrollAmount);
                                saveQuote.highlightQuote(pageNumber);
                            }
                        }, 500);
                    }
                }

                InjectCss(view, "::selection { background: #ffb7b7; }");
                InjectCss(view, "* { padding: 0px !important; letter-spacing: normal !important; max-width: none !important; }");
                InjectCss(view, "* { font-family: " + getFromPreferences("font-family") + " !important; }");
                InjectCss(view, "* { font-size: " + getFromPreferences("font-size") + " !important; }");
                InjectCss(view, "* { font-style: " + getFromPreferences("font-style") + " !important; }");
                InjectCss(view, "* { font-weight: " + getFromPreferences("font-weight") + " !important; }");
                InjectCss(view, "* { text-align: " + getFromPreferences("text-align") + " !important; }");
                InjectCss(view, "body { background: " + getFromPreferences("themeback") + " !important; }");
                InjectCss(view, "* { color: " + getFromPreferences("themefront") + " !important; }");
                InjectCss(view, "* { line-height: " + getFromPreferences("line-height") + " !important; }");
                InjectCss(view, "body { margin: " + getFromPreferences("margin") + " !important; }");
                InjectCss(view, "img { display: block !important; width: 100% !important; height: auto !important; }");

                try {
                    url = URLDecoder.decode(url, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                for (int i = 0; i < pages.size(); i++) {
                    if (url.contains(pages.get(i))) {
                        pageNumber = pages.indexOf(pages.get(i));
                        if (pageNumber > -1) {
                            navigationViewContent.getMenu().getItem(pageNumber).setChecked(true);
                            TextView textViewPage = (TextView) findViewById(R.id.textViewPage);
                            textViewPage.setText("Page: " + navigationViewContent.getCheckedItem().toString());
                            if (!seeking) {
                                webView.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        SyncWebViewScrollSeekBar();
                                    }
                                }, 500);
                            }
                        }
                        break;
                    }
                }
            }
        });
        webView.postDelayed(new Runnable() {
            @Override
            public void run() {
                webView.reload();
            }
        }, 150);

        MenuItem menuItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setMaxWidth(500);
        menu.findItem(R.id.find_next).setVisible(false);
        searchView.setOnSearchClickListener(new SearchView.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainMenu.findItem(R.id.find_next).setVisible(true);
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                mainMenu.findItem(R.id.find_next).setVisible(false);
                return false;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                webView.findAllAsync(newText);
                return false;
            }
        });
        searchView.findViewById(R.id.search_src_text).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                searchViewLongClick = true;
                return false;
            }
        });

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        return true;
    }
    //Menu Font Family, Font Size, Font Style, Font Weight, Text-Align, Line Height, Margin, Theme
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.find_next:
                webView.findNext(true);
                return true;

            case R.id.sans_serif:
                setToPreferences("font-family", "sans-serif");
                whichFontFamily(mainMenu);
                return true;
            case R.id.monospace:
                setToPreferences("font-family", "monospace");
                whichFontFamily(mainMenu);
                return true;
            case R.id.serif:
                setToPreferences("font-family", "serif");
                whichFontFamily(mainMenu);
                return true;
            case R.id.cursive:
                setToPreferences("font-family", "cursive");
                whichFontFamily(mainMenu);
                return true;
            case R.id.default_family:
                setToPreferences("font-family", "default");
                whichFontFamily(mainMenu);
                return true;

            case R.id.ninety:
                setToPreferences("font-size", "90%");
                whichFontSize(mainMenu);
                return true;
            case R.id.ninety_five:
                setToPreferences("font-size", "95%");
                whichFontSize(mainMenu);
                return true;
            case R.id.hundred:
                setToPreferences("font-size", "100%");
                whichFontSize(mainMenu);
                return true;
            case R.id.hundred_five:
                setToPreferences("font-size", "105%");
                whichFontSize(mainMenu);
                return true;
            case R.id.hundred_ten:
                setToPreferences("font-size", "110%");
                whichFontSize(mainMenu);
                return true;

            case R.id.normal_style:
                setToPreferences("font-style", "normal");
                whichFontStyle(mainMenu);
                return true;
            case R.id.italic:
                setToPreferences("font-style", "italic");
                whichFontStyle(mainMenu);
                return true;
            case R.id.default_style:
                setToPreferences("font-style", "default");
                whichFontStyle(mainMenu);
                return true;

            case R.id.normal_weight:
                setToPreferences("font-weight", "normal");
                whichFontWeight(mainMenu);
                return true;
            case R.id.bold:
                setToPreferences("font-weight", "bold");
                whichFontWeight(mainMenu);
                return true;
            case R.id.default_weight:
                setToPreferences("font-weight", "default");
                whichFontWeight(mainMenu);
                return true;

            case R.id.left:
                setToPreferences("text-align", "left");
                whichTextAlign(mainMenu);
                return true;
            case R.id.right:
                setToPreferences("text-align", "right");
                whichTextAlign(mainMenu);
                return true;
            case R.id.center:
                setToPreferences("text-align", "center");
                whichTextAlign(mainMenu);
                return true;
            case R.id.justify:
                setToPreferences("text-align", "justify");
                whichTextAlign(mainMenu);
                return true;
            case R.id.default_align:
                setToPreferences("text-align", "default");
                whichTextAlign(mainMenu);
                return true;

            case R.id.onetwo:
                setToPreferences("line-height", "1.2");
                whichLineHeight(mainMenu);
                return true;
            case R.id.onefour:
                setToPreferences("line-height", "1.4");
                whichLineHeight(mainMenu);
                return true;
            case R.id.onesix:
                setToPreferences("line-height", "1.6");
                whichLineHeight(mainMenu);
                return true;
            case R.id.oneeight:
                setToPreferences("line-height", "1.8");
                whichLineHeight(mainMenu);
                return true;
            case R.id.two:
                setToPreferences("line-height", "2");
                whichLineHeight(mainMenu);
                return true;

            case R.id.zeropercent:
                setToPreferences("margin", "0%");
                whichMargin(mainMenu);
                return true;
            case R.id.onepercent:
                setToPreferences("margin", "1%");
                whichMargin(mainMenu);
                return true;
            case R.id.twopercent:
                setToPreferences("margin", "2%");
                whichMargin(mainMenu);
                return true;
            case R.id.threepercent:
                setToPreferences("margin", "3%");
                whichMargin(mainMenu);
                return true;
            case R.id.fourpercent:
                setToPreferences("margin", "4%");
                whichMargin(mainMenu);
                return true;
            case R.id.fivepercent:
                setToPreferences("margin", "5%");
                whichMargin(mainMenu);
                return true;

            case R.id.ghostwhite:
                setToPreferences("themeback", "GhostWhite");
                setToPreferences("themefront", "DarkSlateGray");
                whichTheme(mainMenu);
                return true;
            case R.id.darkslategray:
                setToPreferences("themeback", "DarkSlateGray");
                setToPreferences("themefront", "GhostWhite");
                whichTheme(mainMenu);
                return true;
            case R.id.bisque:
                setToPreferences("themeback", "Bisque");
                setToPreferences("themefront", "DimGrey");
                whichTheme(mainMenu);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    Menu mainMenu;
    public void whichFontFamily(Menu mainMenu) {
        this.mainMenu = mainMenu;
        if (getFromPreferences("font-family").equals("sans-serif")) {
            mainMenu.findItem(R.id.sans_serif).setTitle(Html.fromHtml("<font face='sans-serif' color='#008577'>Sans Serif</font>"));
            mainMenu.findItem(R.id.serif).setTitle(Html.fromHtml("<font face='serif' color='black'>Serif</font>"));
            mainMenu.findItem(R.id.monospace).setTitle(Html.fromHtml("<font face='monospace' color='black'>Monospace</font>"));
            mainMenu.findItem(R.id.cursive).setTitle(Html.fromHtml("<font face='cursive' color='black'>Cursive</font>"));
            mainMenu.findItem(R.id.default_family).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else if (getFromPreferences("font-family").equals("serif")) {
            mainMenu.findItem(R.id.sans_serif).setTitle(Html.fromHtml("<font face='sans-serif' color='black'>Sans Serif</font>"));
            mainMenu.findItem(R.id.serif).setTitle(Html.fromHtml("<font face='serif' color='#008577'>Serif</font>"));
            mainMenu.findItem(R.id.monospace).setTitle(Html.fromHtml("<font face='monospace' color='black'>Monospace</font>"));
            mainMenu.findItem(R.id.cursive).setTitle(Html.fromHtml("<font face='cursive' color='black'>Cursive</font>"));
            mainMenu.findItem(R.id.default_family).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else if (getFromPreferences("font-family").equals("monospace")) {
            mainMenu.findItem(R.id.sans_serif).setTitle(Html.fromHtml("<font face='sans-serif' color='black'>Sans Serif</font>"));
            mainMenu.findItem(R.id.serif).setTitle(Html.fromHtml("<font face='serif' color='black'>Serif</font>"));
            mainMenu.findItem(R.id.monospace).setTitle(Html.fromHtml("<font face='monospace' color='#008577'>Monospace</font>"));
            mainMenu.findItem(R.id.cursive).setTitle(Html.fromHtml("<font face='cursive' color='black'>Cursive</font>"));
            mainMenu.findItem(R.id.default_family).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else if (getFromPreferences("font-family").equals("cursive")) {
            mainMenu.findItem(R.id.sans_serif).setTitle(Html.fromHtml("<font face='sans-serif' color='black'>Sans Serif</font>"));
            mainMenu.findItem(R.id.serif).setTitle(Html.fromHtml("<font face='serif' color='black'>Serif</font>"));
            mainMenu.findItem(R.id.monospace).setTitle(Html.fromHtml("<font face='monospace' color='black'>Monospace</font>"));
            mainMenu.findItem(R.id.cursive).setTitle(Html.fromHtml("<font face='cursive' color='#008577'>Cursive</font>"));
            mainMenu.findItem(R.id.default_family).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else {
            setToPreferences("font-family", "default");
            mainMenu.findItem(R.id.sans_serif).setTitle(Html.fromHtml("<font face='sans-serif' face='sans-serif' color='black'>Sans Serif</font>"));
            mainMenu.findItem(R.id.serif).setTitle(Html.fromHtml("<font face='serif' color='black'>Serif</font>"));
            mainMenu.findItem(R.id.monospace).setTitle(Html.fromHtml("<font face='monospace' color='black'>Monospace</font>"));
            mainMenu.findItem(R.id.cursive).setTitle(Html.fromHtml("<font face='cursive' color='black'>Cursive</font>"));
            mainMenu.findItem(R.id.default_family).setTitle(Html.fromHtml("<font color='#008577'>Default</font>"));
        }
        webView.reload();
    }
    public void whichFontSize(Menu mainMenu) {
        this.mainMenu = mainMenu;
        if (getFromPreferences("font-size").equals("90%")) {
            mainMenu.findItem(R.id.ninety).setTitle(Html.fromHtml("<font color='#008577'>90%</font>"));
            mainMenu.findItem(R.id.ninety_five).setTitle(Html.fromHtml("<font color='black'>95%</font>"));
            mainMenu.findItem(R.id.hundred).setTitle(Html.fromHtml("<font color='black'>100%</font>"));
            mainMenu.findItem(R.id.hundred_five).setTitle(Html.fromHtml("<font color='black'>105%</font>"));
            mainMenu.findItem(R.id.hundred_ten).setTitle(Html.fromHtml("<font color='black'>110%</font>"));
        } else if (getFromPreferences("font-size").equals("95%")) {
            mainMenu.findItem(R.id.ninety).setTitle(Html.fromHtml("<font color='black'>90%</font>"));
            mainMenu.findItem(R.id.ninety_five).setTitle(Html.fromHtml("<font color='#008577'>95%</font>"));
            mainMenu.findItem(R.id.hundred).setTitle(Html.fromHtml("<font color='black'>100%</font>"));
            mainMenu.findItem(R.id.hundred_five).setTitle(Html.fromHtml("<font color='black'>105%</font>"));
            mainMenu.findItem(R.id.hundred_ten).setTitle(Html.fromHtml("<font color='black'>110%</font>"));
        } else if (getFromPreferences("font-size").equals("105%")) {
            mainMenu.findItem(R.id.ninety).setTitle(Html.fromHtml("<font color='black'>90%</font>"));
            mainMenu.findItem(R.id.ninety_five).setTitle(Html.fromHtml("<font color='black'>95%</font>"));
            mainMenu.findItem(R.id.hundred).setTitle(Html.fromHtml("<font color='black'>100%</font>"));
            mainMenu.findItem(R.id.hundred_five).setTitle(Html.fromHtml("<font color='#008577'>105%</font>"));
            mainMenu.findItem(R.id.hundred_ten).setTitle(Html.fromHtml("<font color='black'>110%</font>"));
        } else if (getFromPreferences("font-size").equals("110%")) {
            mainMenu.findItem(R.id.ninety).setTitle(Html.fromHtml("<font color='black'>90%</font>"));
            mainMenu.findItem(R.id.ninety_five).setTitle(Html.fromHtml("<font color='black'>95%</font>"));
            mainMenu.findItem(R.id.hundred).setTitle(Html.fromHtml("<font color='black'>100%</font>"));
            mainMenu.findItem(R.id.hundred_five).setTitle(Html.fromHtml("<font color='black'>105%</font>"));
            mainMenu.findItem(R.id.hundred_ten).setTitle(Html.fromHtml("<font color='#008577'>110%</font>"));
        } else {
            setToPreferences("font-size", "100%");
            mainMenu.findItem(R.id.ninety).setTitle(Html.fromHtml("<font color='black'>90%</font>"));
            mainMenu.findItem(R.id.ninety_five).setTitle(Html.fromHtml("<font color='black'>95%</font>"));
            mainMenu.findItem(R.id.hundred).setTitle(Html.fromHtml("<font color='#008577'>100%</font>"));
            mainMenu.findItem(R.id.hundred_five).setTitle(Html.fromHtml("<font color='black'>105%</font>"));
            mainMenu.findItem(R.id.hundred_ten).setTitle(Html.fromHtml("<font color='black'>110%</font>"));
        }
        webView.reload();
    }
    public void whichFontStyle(Menu mainMenu) {
        this.mainMenu = mainMenu;
        if (getFromPreferences("font-style").equals("normal")) {
            mainMenu.findItem(R.id.normal_style).setTitle(Html.fromHtml("<font color='#008577'>Normal</font>"));
            mainMenu.findItem(R.id.italic).setTitle(Html.fromHtml("<font color='black'>Italic</font>"));
            mainMenu.findItem(R.id.default_style).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else if (getFromPreferences("font-style").equals("italic")) {
            mainMenu.findItem(R.id.normal_style).setTitle(Html.fromHtml("<font color='black'>Normal</font>"));
            mainMenu.findItem(R.id.italic).setTitle(Html.fromHtml("<font color='#008577'>Italic</font>"));
            mainMenu.findItem(R.id.default_style).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else {
            setToPreferences("font-style", "default");
            mainMenu.findItem(R.id.normal_style).setTitle(Html.fromHtml("<font color='black'>Normal</font>"));
            mainMenu.findItem(R.id.italic).setTitle(Html.fromHtml("<font color='black'>Italic</font>"));
            mainMenu.findItem(R.id.default_style).setTitle(Html.fromHtml("<font color='#008577'>Default</font>"));
        }
        webView.reload();
    }
    public void whichFontWeight(Menu mainMenu) {
        this.mainMenu = mainMenu;
        if (getFromPreferences("font-weight").equals("normal")) {
            mainMenu.findItem(R.id.normal_weight).setTitle(Html.fromHtml("<font color='#008577'>Normal</font>"));
            mainMenu.findItem(R.id.bold).setTitle(Html.fromHtml("<font color='black'>Bold</font>"));
            mainMenu.findItem(R.id.default_weight).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else if (getFromPreferences("font-weight").equals("bold")) {
            mainMenu.findItem(R.id.normal_weight).setTitle(Html.fromHtml("<font color='black'>Normal</font>"));
            mainMenu.findItem(R.id.bold).setTitle(Html.fromHtml("<font color='#008577'>Bold</font>"));
            mainMenu.findItem(R.id.default_weight).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else {
            setToPreferences("font-weight", "default");
            mainMenu.findItem(R.id.normal_weight).setTitle(Html.fromHtml("<font color='black'>Normal</font>"));
            mainMenu.findItem(R.id.bold).setTitle(Html.fromHtml("<font color='black'>Bold</font>"));
            mainMenu.findItem(R.id.default_weight).setTitle(Html.fromHtml("<font color='#008577'>Default</font>"));
        }
        webView.reload();
    }
    public void whichTextAlign(Menu mainMenu) {
        this.mainMenu = mainMenu;
        if (getFromPreferences("text-align").equals("left")) {
            mainMenu.findItem(R.id.left).setTitle(Html.fromHtml("<font color='#008577'>Left</font>"));
            mainMenu.findItem(R.id.right).setTitle(Html.fromHtml("<font color='black'>Right</font>"));
            mainMenu.findItem(R.id.center).setTitle(Html.fromHtml("<font color='black'>Center</font>"));
            mainMenu.findItem(R.id.justify).setTitle(Html.fromHtml("<font color='black'>Justify</font>"));
            mainMenu.findItem(R.id.default_align).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else if (getFromPreferences("text-align").equals("right")) {
            mainMenu.findItem(R.id.left).setTitle(Html.fromHtml("<font color='black'>Left</font>"));
            mainMenu.findItem(R.id.right).setTitle(Html.fromHtml("<font color='#008577'>Right</font>"));
            mainMenu.findItem(R.id.center).setTitle(Html.fromHtml("<font color='black'>Center</font>"));
            mainMenu.findItem(R.id.justify).setTitle(Html.fromHtml("<font color='black'>Justify</font>"));
            mainMenu.findItem(R.id.default_align).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else if (getFromPreferences("text-align").equals("center")) {
            mainMenu.findItem(R.id.left).setTitle(Html.fromHtml("<font color='black'>Left</font>"));
            mainMenu.findItem(R.id.right).setTitle(Html.fromHtml("<font color='black'>Right</font>"));
            mainMenu.findItem(R.id.center).setTitle(Html.fromHtml("<font color='#008577'>Center</font>"));
            mainMenu.findItem(R.id.justify).setTitle(Html.fromHtml("<font color='black'>Justify</font>"));
            mainMenu.findItem(R.id.default_align).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else if (getFromPreferences("text-align").equals("justify")) {
            mainMenu.findItem(R.id.left).setTitle(Html.fromHtml("<font color='black'>Left</font>"));
            mainMenu.findItem(R.id.right).setTitle(Html.fromHtml("<font color='black'>Right</font>"));
            mainMenu.findItem(R.id.center).setTitle(Html.fromHtml("<font color='black'>Center</font>"));
            mainMenu.findItem(R.id.justify).setTitle(Html.fromHtml("<font color='#008577'>Justify</font>"));
            mainMenu.findItem(R.id.default_align).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else {
            setToPreferences("text-align", "default");
            mainMenu.findItem(R.id.left).setTitle(Html.fromHtml("<font color='black'>Left</font>"));
            mainMenu.findItem(R.id.right).setTitle(Html.fromHtml("<font color='black'>Right</font>"));
            mainMenu.findItem(R.id.center).setTitle(Html.fromHtml("<font color='black'>Center</font>"));
            mainMenu.findItem(R.id.justify).setTitle(Html.fromHtml("<font color='black'>Justify</font>"));
            mainMenu.findItem(R.id.default_align).setTitle(Html.fromHtml("<font color='#008577'>Default</font>"));
        }
        webView.reload();
    }
    public void whichLineHeight(Menu mainMenu) {
        this.mainMenu = mainMenu;
        if (getFromPreferences("line-height").equals("1.2")) {
            mainMenu.findItem(R.id.onetwo).setTitle(Html.fromHtml("<font color='#008577'>1.2</font>"));
            mainMenu.findItem(R.id.onefour).setTitle(Html.fromHtml("<font color='black'>1.4</font>"));
            mainMenu.findItem(R.id.onesix).setTitle(Html.fromHtml("<font color='black'>1.6</font>"));
            mainMenu.findItem(R.id.oneeight).setTitle(Html.fromHtml("<font color='black'>1.8</font>"));
            mainMenu.findItem(R.id.two).setTitle(Html.fromHtml("<font color='black'>2</font>"));
        } else if (getFromPreferences("line-height").equals("1.4")) {
            mainMenu.findItem(R.id.onetwo).setTitle(Html.fromHtml("<font color='black'>1.2</font>"));
            mainMenu.findItem(R.id.onefour).setTitle(Html.fromHtml("<font color='#008577'>1.4</font>"));
            mainMenu.findItem(R.id.onesix).setTitle(Html.fromHtml("<font color='black'>1.6</font>"));
            mainMenu.findItem(R.id.oneeight).setTitle(Html.fromHtml("<font color='black'>1.8</font>"));
            mainMenu.findItem(R.id.two).setTitle(Html.fromHtml("<font color='black'>2</font>"));
        } else if (getFromPreferences("line-height").equals("1.8")) {
            mainMenu.findItem(R.id.onetwo).setTitle(Html.fromHtml("<font color='black'>1.2</font>"));
            mainMenu.findItem(R.id.onefour).setTitle(Html.fromHtml("<font color='black'>1.4</font>"));
            mainMenu.findItem(R.id.onesix).setTitle(Html.fromHtml("<font color='black'>1.6</font>"));
            mainMenu.findItem(R.id.oneeight).setTitle(Html.fromHtml("<font color='#008577'>1.8</font>"));
            mainMenu.findItem(R.id.two).setTitle(Html.fromHtml("<font color='black'>2</font>"));
        } else if (getFromPreferences("line-height").equals("2")) {
            mainMenu.findItem(R.id.onetwo).setTitle(Html.fromHtml("<font color='black'>1.2</font>"));
            mainMenu.findItem(R.id.onefour).setTitle(Html.fromHtml("<font color='black'>1.4</font>"));
            mainMenu.findItem(R.id.onesix).setTitle(Html.fromHtml("<font color='black'>1.6</font>"));
            mainMenu.findItem(R.id.oneeight).setTitle(Html.fromHtml("<font color='black'>1.8</font>"));
            mainMenu.findItem(R.id.two).setTitle(Html.fromHtml("<font color='#008577'>2</font>"));
        } else {
            setToPreferences("line-height", "1.6");
            mainMenu.findItem(R.id.onetwo).setTitle(Html.fromHtml("<font color='black'>1.2</font>"));
            mainMenu.findItem(R.id.onefour).setTitle(Html.fromHtml("<font color='black'>1.4</font>"));
            mainMenu.findItem(R.id.onesix).setTitle(Html.fromHtml("<font color='#008577'>1.6</font>"));
            mainMenu.findItem(R.id.oneeight).setTitle(Html.fromHtml("<font color='black'>1.8</font>"));
            mainMenu.findItem(R.id.two).setTitle(Html.fromHtml("<font color='black'>2</font>"));
        }
        webView.reload();
    }
    public void whichMargin(Menu mainMenu) {
        this.mainMenu = mainMenu;
        if (getFromPreferences("margin").equals("0%")) {
            mainMenu.findItem(R.id.zeropercent).setTitle(Html.fromHtml("<font color='#008577'>0%</font>"));
            mainMenu.findItem(R.id.onepercent).setTitle(Html.fromHtml("<font color='black'>1%</font>"));
            mainMenu.findItem(R.id.twopercent).setTitle(Html.fromHtml("<font color='black'>2%</font>"));
            mainMenu.findItem(R.id.threepercent).setTitle(Html.fromHtml("<font color='black'>3%</font>"));
            mainMenu.findItem(R.id.fourpercent).setTitle(Html.fromHtml("<font color='black'>4%</font>"));
            mainMenu.findItem(R.id.fivepercent).setTitle(Html.fromHtml("<font color='black'>5%</font>"));
        } else if (getFromPreferences("margin").equals("1%")) {
            mainMenu.findItem(R.id.zeropercent).setTitle(Html.fromHtml("<font color='black'>0%</font>"));
            mainMenu.findItem(R.id.onepercent).setTitle(Html.fromHtml("<font color='#008577'>1%</font>"));
            mainMenu.findItem(R.id.twopercent).setTitle(Html.fromHtml("<font color='black'>2%</font>"));
            mainMenu.findItem(R.id.threepercent).setTitle(Html.fromHtml("<font color='black'>3%</font>"));
            mainMenu.findItem(R.id.fourpercent).setTitle(Html.fromHtml("<font color='black'>4%</font>"));
            mainMenu.findItem(R.id.fivepercent).setTitle(Html.fromHtml("<font color='black'>5%</font>"));
        } else if (getFromPreferences("margin").equals("2%")) {
            mainMenu.findItem(R.id.zeropercent).setTitle(Html.fromHtml("<font color='black'>0%</font>"));
            mainMenu.findItem(R.id.onepercent).setTitle(Html.fromHtml("<font color='black'>1%</font>"));
            mainMenu.findItem(R.id.twopercent).setTitle(Html.fromHtml("<font color='#008577'>2%</font>"));
            mainMenu.findItem(R.id.threepercent).setTitle(Html.fromHtml("<font color='black'>3%</font>"));
            mainMenu.findItem(R.id.fourpercent).setTitle(Html.fromHtml("<font color='black'>4%</font>"));
            mainMenu.findItem(R.id.fivepercent).setTitle(Html.fromHtml("<font color='black'>5%</font>"));
        } else if (getFromPreferences("margin").equals("3%")) {
            mainMenu.findItem(R.id.zeropercent).setTitle(Html.fromHtml("<font color='black'>0%</font>"));
            mainMenu.findItem(R.id.onepercent).setTitle(Html.fromHtml("<font color='black'>1%</font>"));
            mainMenu.findItem(R.id.twopercent).setTitle(Html.fromHtml("<font color='black'>2%</font>"));
            mainMenu.findItem(R.id.threepercent).setTitle(Html.fromHtml("<font color='#008577'>3%</font>"));
            mainMenu.findItem(R.id.fourpercent).setTitle(Html.fromHtml("<font color='black'>4%</font>"));
            mainMenu.findItem(R.id.fivepercent).setTitle(Html.fromHtml("<font color='black'>5%</font>"));
        } else if (getFromPreferences("margin").equals("4%")) {
            mainMenu.findItem(R.id.zeropercent).setTitle(Html.fromHtml("<font color='black'>0%</font>"));
            mainMenu.findItem(R.id.onepercent).setTitle(Html.fromHtml("<font color='black'>1%</font>"));
            mainMenu.findItem(R.id.twopercent).setTitle(Html.fromHtml("<font color='black'>2%</font>"));
            mainMenu.findItem(R.id.threepercent).setTitle(Html.fromHtml("<font color='black'>3%</font>"));
            mainMenu.findItem(R.id.fourpercent).setTitle(Html.fromHtml("<font color='#008577'>4%</font>"));
            mainMenu.findItem(R.id.fivepercent).setTitle(Html.fromHtml("<font color='black'>5%</font>"));
        } else {
            setToPreferences("margin", "5%");
            mainMenu.findItem(R.id.zeropercent).setTitle(Html.fromHtml("<font color='black'>0%</font>"));
            mainMenu.findItem(R.id.onepercent).setTitle(Html.fromHtml("<font color='black'>1%</font>"));
            mainMenu.findItem(R.id.twopercent).setTitle(Html.fromHtml("<font color='black'>2%</font>"));
            mainMenu.findItem(R.id.threepercent).setTitle(Html.fromHtml("<font color='black'>3%</font>"));
            mainMenu.findItem(R.id.fourpercent).setTitle(Html.fromHtml("<font color='black'>4%</font>"));
            mainMenu.findItem(R.id.fivepercent).setTitle(Html.fromHtml("<font color='#008577'>5%</font>"));
        }
        webView.reload();
    }
    public void whichTheme(Menu mainMenu) {
        this.mainMenu = mainMenu;
        if (getFromPreferences("themeback").equals("GhostWhite")) {
            mainMenu.findItem(R.id.ghostwhite).setTitle(Html.fromHtml("<font color='#008577'>Ghost White</font>"));
            mainMenu.findItem(R.id.darkslategray).setTitle(Html.fromHtml("<font color='black'>Dark Slate Gray</font>"));
            mainMenu.findItem(R.id.bisque).setTitle(Html.fromHtml("<font color='black'>Bisque</font>"));
        } else if (getFromPreferences("themeback").equals("DarkSlateGray")) {
            mainMenu.findItem(R.id.ghostwhite).setTitle(Html.fromHtml("<font color='black'>Ghost White</font>"));
            mainMenu.findItem(R.id.darkslategray).setTitle(Html.fromHtml("<font color='#008577'>Dark Slate Gray</font>"));
            mainMenu.findItem(R.id.bisque).setTitle(Html.fromHtml("<font color='black'>Bisque</font>"));
        } else {
            setToPreferences("themeback", "Bisque");
            setToPreferences("themefront", "DimGrey");
            mainMenu.findItem(R.id.ghostwhite).setTitle(Html.fromHtml("<font color='black'>Ghost White</font>"));
            mainMenu.findItem(R.id.darkslategray).setTitle(Html.fromHtml("<font color='black'>Dark Slate Gray</font>"));
            mainMenu.findItem(R.id.bisque).setTitle(Html.fromHtml("<font color='#008577'>Bisque</font>"));
        }
        webView.reload();
    }

    //WebvView LongClick Menu
    @Override
    public void onActionModeStarted(ActionMode mode) {
        if (!searchViewLongClick) {
            mode.getMenu().add(0, 15264685, 0, "Highlight");
            mode.getMenu().add(0, 45657841, 0, "Default");
            mode.getMenu().findItem(15264685).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    try {
                        if (!gQuote.equals("") || gQuote.equals(null)) {
                            saveQuote.addQuote(gQuote, bookTitle, pageNumber, webView.getScrollY());
                            reloadNavQuote();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return false;
                }
            });
            mode.getMenu().findItem(45657841).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    try {
                        if (!gQuote.equals("") || gQuote.equals(null)) {
                            saveQuote.removeQuote(gQuote, bookTitle, 0, getFromPreferences("themeback"));
                            reloadNavQuote();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return false;
                }
            });
        }
        super.onActionModeStarted(mode);
    }
    @Override
    public void onActionModeFinished(ActionMode mode) {
        searchViewLongClick = false;
        super.onActionModeFinished(mode);
    }
    //WebView Gesture
    private class CustomeGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) return false;
            if (e1.getPointerCount() > 1 || e2.getPointerCount() > 1) return false;
            else {
                try { // right to left swipe .. go to next page
                    if (e1.getX() - e2.getX() > 150 && Math.abs(velocityX) > 1000) {

                        return true;
                    } //left to right swipe .. go to prev page
                    else if (e2.getX() - e1.getX() > 150 && Math.abs(velocityX) > 1000) {

                        return true;
                    } //bottom to top, go to next document
                    else if (e1.getY() - e2.getY() > 150 && Math.abs(velocityY) > 1000 && webView.getScrollY() >= Math.round(webView.getContentHeight() * webView.getScale()) - webView.getHeight() - 10) {
                        if (pageNumber < pages.size() - 1) {
                            pageNumber++;
                            webView.loadUrl("file://" + pages.get(pageNumber));
                            seekBar.setProgress(seekBar.getMax() * pageNumber / pages.size());
                            webViewScrollAmount = 0;
                        }
                        return true;
                    } //top to bottom, go to prev document
                    else if (e2.getY() - e1.getY() > 150 && Math.abs(velocityY) > 1000 && webView.getScrollY() <= 10) {
                        if (pageNumber > 0) {
                            pageNumber--;
                            webView.loadUrl("file://" + pages.get(pageNumber));
                            seekBar.setProgress(seekBar.getMax() * pageNumber / pages.size());
                            webViewScrollAmount = (int) (webView.getContentHeight() * webView.getScale()) - webView.getHeight();
                        }
                        return true;
                    }
                } catch (Exception e) {
                }
                return false;
            }
        }
    }
    //Inject CSS to WebView
    private final static String CREATE_CUSTOM_SHEET =
            "if (typeof(document.head) != 'undefined' && typeof(customSheet) == 'undefined') {"
                    + "var customSheet = (function() {"
                    + "var style = document.createElement(\"style\");"
                    + "style.appendChild(document.createTextNode(\"\"));"
                    + "document.head.appendChild(style);"
                    + "return style.sheet;"
                    + "})();"
                    + "}";
    private void InjectCss(WebView webView, String... cssRules) {
        StringBuilder jsUrl = new StringBuilder("javascript:");
        jsUrl.append(CREATE_CUSTOM_SHEET).append("if (typeof(customSheet) != 'undefined') {");
        int cnt = 0;
        for (String cssRule : cssRules) {
            jsUrl.append("customSheet.insertRule('").append(cssRule).append("', ").append(cnt++).append(");");
        }
        jsUrl.append("}");
        webView.loadUrl(jsUrl.toString());
    }
    //Sync WebView Scroll and Seek Bar Progress
    public void SyncWebViewScrollSeekBar() {
        int real = seekBar.getMax() * pageNumber / pages.size();

        float webViewHeight = (webView.getContentHeight() * webView.getScale()) - webView.getHeight();
        float partPerPage = seekBar.getMax() / pages.size();
        float fraction = ((float) webView.getScrollY()) / webViewHeight * partPerPage;

        seekBar.setProgress(real + ((int) fraction));
    }
}
