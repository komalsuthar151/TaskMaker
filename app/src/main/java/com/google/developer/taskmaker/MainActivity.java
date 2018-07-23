package com.google.developer.taskmaker;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.developer.taskmaker.data.DatabaseContract;
import com.google.developer.taskmaker.data.TaskAdapter;

import static com.google.developer.taskmaker.data.DatabaseContract.DATE_SORT;
import static com.google.developer.taskmaker.data.DatabaseContract.DEFAULT_SORT;

public class MainActivity extends AppCompatActivity implements
        TaskAdapter.OnItemClickListener,
        View.OnClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    private TaskAdapter mAdapter;
    private static final int TASK_LOADER = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.fab);
        fab.setOnClickListener(this);

        mAdapter = new TaskAdapter(null,this);
        mAdapter.setOnItemClickListener(this);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        getSupportLoaderManager().initLoader(TASK_LOADER, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* Click events in Floating Action Button */
    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, AddTaskActivity.class);
        startActivity(intent);
    }

    /* Click events in RecyclerView items */
    @Override
    public void onItemClick(View v, int position) {
        //TODO: Handle list item click event
        Intent intent = new Intent(MainActivity.this, TaskDetailActivity.class);
        Uri currentTaskUri = ContentUris.withAppendedId(DatabaseContract.CONTENT_URI, mAdapter.getItemId(position));
        intent.setData(currentTaskUri);
        startActivity(intent);
    }

    /* Click events on RecyclerView item checkboxes */
    @Override
    public void onItemToggled(boolean active, int position) {
        //TODO: Handle task item checkbox event
        Uri currentTaskUri = ContentUris.withAppendedId(DatabaseContract.CONTENT_URI, mAdapter.getItemId(position));

        ContentValues values = new ContentValues();
        values.put(DatabaseContract.TaskColumns.IS_COMPLETE, 1);

        int rowAffected = getContentResolver().update(currentTaskUri, values, null, null);

        if(rowAffected == 0)
        {
            Toast.makeText(this, "Task not Completed", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "Task Completed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String orderByConstant ="";

        String orderBy = sharedPreferences.getString(
           getString(R.string.pref_sortBy_default),
                getString(R.string.pref_sortBy_due)
        );

        if(orderBy.equals(this.getString(R.string.pref_sortBy_default))){
            orderByConstant = DEFAULT_SORT;
        }else if(orderBy.equals(this.getString(R.string.pref_sortBy_due))){
            orderByConstant= DATE_SORT;
        }else{
            Toast.makeText(this, "No option Available", Toast.LENGTH_SHORT).show();
        }

        String [] projection ={
                DatabaseContract.TaskColumns._ID,
                DatabaseContract.TaskColumns.DESCRIPTION,
                DatabaseContract.TaskColumns.IS_COMPLETE,
                DatabaseContract.TaskColumns.IS_PRIORITY,
                DatabaseContract.TaskColumns.DUE_DATE,
        };

        return new CursorLoader(this,
                DatabaseContract .CONTENT_URI,
                projection,
                null,
                null,
                orderByConstant);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        mAdapter.swapCursor(null);
    }

}
