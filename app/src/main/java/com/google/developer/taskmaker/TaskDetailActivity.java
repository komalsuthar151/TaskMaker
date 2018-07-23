package com.google.developer.taskmaker;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.developer.taskmaker.data.DatabaseContract;
import com.google.developer.taskmaker.reminders.AlarmScheduler;
import com.google.developer.taskmaker.views.DatePickerFragment;

import java.util.Calendar;

public class TaskDetailActivity extends AppCompatActivity implements
        DatePickerDialog.OnDateSetListener, LoaderManager.LoaderCallbacks<Cursor> {

    private TextView mTaskDescription;
    private TextView mDueDate;
    String taskDecription;
    int mpriority;
    long dueDate = Long.MAX_VALUE;
    ImageView mPriority;
    private static final int TASK_DETAIL_LOADER = 0;
    private Uri taskUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.task_details);
        //Task must be passed to this activity as a valid provider Uri
        //TODO: Display attributes of the provided task in the UI
        taskUri = getIntent().getData();
        mTaskDescription = (TextView)findViewById(R.id.text_description);
        mDueDate =(TextView)findViewById(R.id.due_date);
        mPriority = (ImageView)findViewById(R.id.priority);

        if(taskUri!=null){
            getLoaderManager().initLoader(TASK_DETAIL_LOADER,null, this);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_task_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_reminder:
                DatePickerFragment dialogFragment = new DatePickerFragment();
                dialogFragment.show(getSupportFragmentManager(),"datePicker()");
                return true;
            case R.id.action_delete:
                deleteTask();
                return true;
             default:
                 return super.onOptionsItemSelected(item);
        }
    }

    private void deleteTask(){
        if(taskUri!=null){
            int rowDeleted = getContentResolver().delete(taskUri, null, null);
            if(rowDeleted ==0)
            {
                Toast.makeText(this, "Deleting Task Failed",Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, "Task Deleted Successfully!",Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        //TODO: Handle date selection from a DatePickerFragment
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);

        setDateSelection(c.getTimeInMillis());
    }

    public void setDateSelection(long selectedTimestamp){
        dueDate = selectedTimestamp;
        long time = System.currentTimeMillis();
        if(dueDate < time){
            Toast.makeText(this, "Task Alarm cannot be set to past date", Toast.LENGTH_SHORT)
                            .show();
        return;
        }else{
            new AlarmScheduler().scheduleAlarm(getApplicationContext(), dueDate, taskUri);
            Toast.makeText(this, "Task alarm set", Toast.LENGTH_SHORT).show();
        }
    }

    public long getDateSelection(){
        return dueDate;
    }
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        String[] mProjection =
                {
                        DatabaseContract.TaskColumns._ID,
                        DatabaseContract.TaskColumns.DESCRIPTION,
                        DatabaseContract.TaskColumns.IS_COMPLETE,
                        DatabaseContract.TaskColumns.IS_PRIORITY,
                        DatabaseContract.TaskColumns.DUE_DATE
                };

        String mSelectionClause = null;
        String[] mSelectionArgs = null;
        String mSortOrder = null;
        return new CursorLoader(this, taskUri, mProjection, mSelectionClause, mSelectionArgs, mSortOrder);

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if(cursor == null || cursor.getCount() < 1){
            return;
        }
        if(cursor.moveToFirst()){
            int taskdescription = cursor.getColumnIndex(DatabaseContract.TaskColumns.DESCRIPTION);
            int duedate = cursor.getColumnIndex(DatabaseContract.TaskColumns.DUE_DATE);
            int priority = cursor.getColumnIndex(DatabaseContract.TaskColumns.IS_PRIORITY);

            taskDecription = cursor.getString(taskdescription);
            dueDate = cursor.getLong(duedate);
            mpriority = cursor.getInt(priority);

            mTaskDescription.setText(taskDecription);
            if(mpriority ==0){
            mPriority.setImageResource(R.drawable.ic_not_priority);
            }else{
                mPriority.setImageResource(R.drawable.ic_priority);
            }

            if(getDateSelection()== Long.MAX_VALUE){
                mDueDate.setText("");
            }else{
                CharSequence format = DateUtils.getRelativeTimeSpanString(this,dueDate);
                mDueDate.setText("Due Date " + format);
            }
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
