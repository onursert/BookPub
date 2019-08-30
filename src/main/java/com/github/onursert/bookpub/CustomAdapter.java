package com.github.onursert.bookpub;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.MyViewHolder> implements Filterable {
    Context context;
    private List<List> bookList;
    RefreshEpub refreshEpub;

    LayoutInflater inflater;
    int position;

    public List<List> searchedBookList;

    public CustomAdapter(Context context, List<List> bookList) {
        inflater = LayoutInflater.from(context);
        this.context = context;
        this.bookList = bookList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.custom_row, parent, false);
        MyViewHolder holder = new MyViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, int i) {
        List bookInfo = bookList.get(i);
        try {
            myViewHolder.setData(bookInfo);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }

    boolean refreshingDoneBool = false;
    public void refreshingDone(List<List> bookList) {
        searchedBookList = new ArrayList<>(bookList);
        refreshingDoneBool = true;
    }
    @Override
    public Filter getFilter() {
        return filter;
    }
    private Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<List> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(searchedBookList);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (List infos : searchedBookList) {
                    if (infos.get(0).toString().toLowerCase().contains(filterPattern) || infos.get(1).toString().toLowerCase().contains(filterPattern)) {
                        filteredList.add(infos);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (refreshingDoneBool) {
                bookList.clear();
                bookList.addAll((List) results.values);
                notifyDataSetChanged();
            }
        }
    };

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView title, author, importTime, openTime;
        ImageView image;

        public MyViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.bookTitle);
            author = (TextView) view.findViewById(R.id.bookAuthor);
            image = (ImageView) view.findViewById(R.id.bookCover);
            importTime = (TextView) view.findViewById(R.id.bookImportTime);
            openTime = (TextView) view.findViewById(R.id.bookOpenTime);
            refreshEpub = MainActivity.getInstance().refreshEpub;

            updateViews();

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    position = getLayoutPosition();
                    return false;
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intentEpubViewer = new Intent(context, EpubViewer.class);
                    intentEpubViewer.putExtra("title", bookList.get(getLayoutPosition()).get(0).toString());
                    intentEpubViewer.putExtra("path", bookList.get(getLayoutPosition()).get(3).toString());
                    intentEpubViewer.putExtra("currentPage", bookList.get(getLayoutPosition()).get(6).toString());
                    intentEpubViewer.putExtra("currentScroll", bookList.get(getLayoutPosition()).get(7).toString());
                    context.startActivity(intentEpubViewer);

                    String openTime = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
                    try {
                        String clickedBookPath = bookList.get(getLayoutPosition()).get(3).toString();
                        refreshEpub.addOpenTime(bookList, clickedBookPath, openTime);
                        if (refreshingDoneBool) {
                            refreshEpub.addOpenTime(searchedBookList, clickedBookPath, openTime);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        public void setData(List bookInfo) throws ParseException {
            title.setText(bookInfo.get(0).toString());
            author.setText(bookInfo.get(1).toString());
            Picasso.get().load("file://" + bookInfo.get(2).toString()).error(R.drawable.ic_book_black_24dp).resize(240, 320).into(image);
            importTime.setText("Import: " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new SimpleDateFormat("yyyyMMdd_HHmmss").parse(bookInfo.get(4).toString())));
            openTime.setText("Open: " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new SimpleDateFormat("yyyyMMdd_HHmmss").parse(bookInfo.get(5).toString())));
        }

        public void updateViews() {
            if (refreshEpub.getFromPreferences("showHideImportTime").equals("Invisible")) {
                importTime.setVisibility(View.INVISIBLE);
            } else if (refreshEpub.getFromPreferences("showHideImportTime").equals("Visible")) {
                importTime.setVisibility(View.VISIBLE);
            } else {
                importTime.setVisibility(View.VISIBLE);
            }
            if (refreshEpub.getFromPreferences("showHideOpenTime").equals("Invisible")) {
                openTime.setVisibility(View.INVISIBLE);
            } else if (refreshEpub.getFromPreferences("showHideOpenTime").equals("Visible")) {
                openTime.setVisibility(View.VISIBLE);
            } else {
                openTime.setVisibility(View.VISIBLE);
            }
        }
    }
}
