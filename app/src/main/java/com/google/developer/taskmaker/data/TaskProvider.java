package com.google.developer.taskmaker.data;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

public class TaskProvider extends ContentProvider {
    private static final String TAG = TaskProvider.class.getSimpleName();

    private static final int CLEANUP_JOB_ID = 43;

    private static final int TASKS = 100;
    private static final int TASKS_WITH_ID = 101;

    private TaskDbHelper mDbHelper;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        // content://com.google.developer.taskmaker/tasks
        sUriMatcher.addURI(DatabaseContract.CONTENT_AUTHORITY,
                DatabaseContract.TABLE_TASKS,
                TASKS);

        // content://com.google.developer.taskmaker/tasks/id
        sUriMatcher.addURI(DatabaseContract.CONTENT_AUTHORITY,
                DatabaseContract.TABLE_TASKS + "/#",
                TASKS_WITH_ID);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new TaskDbHelper(getContext());
        manageCleanupJob();
        return true;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null; /* Not used */
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor = null;

        int match =  sUriMatcher.match(uri);
        switch (match){

            case TASKS:
                cursor = database.query(DatabaseContract.TABLE_TASKS, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;

            case TASKS_WITH_ID:
                selection = DatabaseContract.TaskColumns._ID + "=?";
                selectionArgs = new  String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(DatabaseContract.TABLE_TASKS, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI" + uri);

        }
        //TODO: Implement task query
        //TODO: Expected "query all" Uri: content://com.google.developer.taskmaker/tasks
        //TODO: Expected "query one" Uri: content://com.google.developer.taskmaker/tasks/{id}
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
        //        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {

        final int match = sUriMatcher.match(uri);
        switch (match){
            case TASKS:
                return insertTask(uri, values);
            default:
                throw new IllegalArgumentException("Insertion not supported for" + uri);
        }

        //TODO: Implement new task insert
        //TODO: Expected Uri: content://com.google.developer.taskmaker/tasks
    }

    private Uri insertTask(Uri uri, ContentValues values){

        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        long id = database.insert(DatabaseContract.TABLE_TASKS, null, values);
        if(id == -1)
        {
            Log.e(TAG, "Failed to insert row for "+uri);
            return null;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match){
            case TASKS_WITH_ID:
                selection = DatabaseContract.TaskColumns._ID + "=?";
                selectionArgs = new String[]{ String.valueOf(ContentUris.parseId(uri))};
                return updateTask(uri, values, selection, selectionArgs);
            default:
                    throw new IllegalArgumentException("Update is not supported for" + uri);
        }
        //TODO: Implement existing task update
        //TODO: Expected Uri: content://com.google.developer.taskmaker/tasks/{id}
    }
    private int updateTask(Uri uri, ContentValues values, String selection, String[] selectionArgs){
        if(values.size()==0){
            return 0;
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        int rowUpdated = database.update(DatabaseContract.TABLE_TASKS, values, selection, selectionArgs);

        if(rowUpdated != 0)
        {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowUpdated;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        switch (sUriMatcher.match(uri)) {
            case TASKS:
                //Rows aren't counted with null selection
                selection = (selection == null) ? "1" : selection;
                break;
            case TASKS_WITH_ID:
                long id = ContentUris.parseId(uri);
                selection = String.format("%s = ?", DatabaseContract.TaskColumns._ID);
                selectionArgs = new String[]{String.valueOf(id)};
                break;
            default:
                throw new IllegalArgumentException("Illegal delete URI");
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int count = db.delete(DatabaseContract.TABLE_TASKS, selection, selectionArgs);

        if (count > 0) {
            //Notify observers of the change
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return count;
    }

    /* Initiate a periodic job to clear out completed items */
    private void manageCleanupJob() {
        Log.d(TAG, "Scheduling cleanup job");
        JobScheduler jobScheduler = (JobScheduler) getContext()
                .getSystemService(Context.JOB_SCHEDULER_SERVICE);

        //Run the job approximately every hour
        long jobInterval = 3600000L;

        ComponentName jobService = new ComponentName(getContext(), CleanupJobService.class);
        JobInfo task = new JobInfo.Builder(CLEANUP_JOB_ID, jobService)
                //.setPeriodic(jobInterval)
                .setMinimumLatency(jobInterval).setOverrideDeadline(jobInterval)
                .setPersisted(true)
                .build();

        if (jobScheduler.schedule(task) != JobScheduler.RESULT_SUCCESS) {
            Log.w(TAG, "Unable to schedule cleanup job");
        }
    }
}
