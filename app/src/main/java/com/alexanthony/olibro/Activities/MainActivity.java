package com.alexanthony.olibro.Activities;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.alexanthony.olibro.Content.Book;
import com.alexanthony.olibro.Content.Chapter;
import com.alexanthony.olibro.Content.Track;
import com.alexanthony.olibro.DBHelpers.BookDBHelper;
import com.alexanthony.olibro.DBHelpers.ChapterDBHelper;
import com.alexanthony.olibro.PlayerService;
import com.alexanthony.olibro.PlayerService.MediaBinder;
import com.alexanthony.olibro.R;
import com.alexanthony.olibro.TrackAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

//import android.view.LayoutInflater;
//import android.widget.FrameLayout;

public class MainActivity extends BaseActivity {
    //track list variables
    private ArrayList<Track> trackList;
    private final String bookPath = "Audiobooks";

    private static final String TAG = "com.alexanthony.olibro.MainActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        ListView trackView = (ListView) setContentFrame(R.layout.track_list);
        //instantiate list
        trackList = new ArrayList<>();
        //get songs from device
		getTrackList();
		//sort alphabetically by title
		Collections.sort(trackList, new Comparator<Track>(){
			public int compare(Track a, Track b){
				return a.getTitle().compareTo(b.getTitle());
			}
		});
		//create and set adapter
		TrackAdapter trackAdt = new TrackAdapter(this, trackList);
        trackView.setAdapter(trackAdt);
        setControlListeners();
        populateBookDB();
    }

	//connect to the service
	private ServiceConnection playerConnection = new ServiceConnection(){

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			MediaBinder binder = (MediaBinder)service;
			//get service
			playerSrv = binder.getService();
			//pass list
			playerSrv.setList(trackList);
			trackBound = true;
            // TODO: If last playing track from previous run available, load it and setMediaControl()
            //setMediaControl();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			trackBound = false;
		}
	};

	//start and bind the service when the activity starts
	@Override
	protected void onStart() {
		super.onStart();
		if(playIntent==null){
			playIntent = new Intent(this, PlayerService.class);
			bindService(playIntent, playerConnection, Context.BIND_AUTO_CREATE);
			startService(playIntent);
		}
	}

	//user track select
	public void trackPicked(View view){
		playerSrv.setTrack(Integer.parseInt(view.getTag().toString()));
		playerSrv.playTrack();
        if (paused) {
            paused = false;
        }
        setMediaControl();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {        
	// Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
	}

	//method to retrieve track info from device
	public void getTrackList(){
		//query external audio
		ContentResolver mediaResolver = getContentResolver();
		Uri mediaUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.DATA + " like " + "'%" + bookPath + "/%'";
		Cursor mediaCursor = mediaResolver.query(mediaUri, null, selection, null, null);
		//iterate over results if valid
        if (mediaCursor == null) {
            // Handle error
            Log.e("MainActivity", "MainActivity.getTrackList - null cursor");
        }
        if (mediaCursor != null && mediaCursor.moveToFirst()) {
            //get columns
			int titleColumn = mediaCursor.getColumnIndex
					(android.provider.MediaStore.Audio.Media.TITLE);
			int idColumn = mediaCursor.getColumnIndex
					(android.provider.MediaStore.Audio.Media._ID);
			int authorColumn = mediaCursor.getColumnIndex
					(android.provider.MediaStore.Audio.Media.ARTIST);
			//add songs to list
			do {
				long thisId = mediaCursor.getLong(idColumn);
				String thisTitle = mediaCursor.getString(titleColumn);
				String thisAuthor = mediaCursor.getString(authorColumn);
				trackList.add(new Track(thisId, thisTitle, thisAuthor));
			} 
			while (mediaCursor.moveToNext());
		}
	}

	@Override
	protected void onDestroy() {
		stopService(playIntent);
		playerSrv=null;
		super.onDestroy();
	}
    
    //////////////////////// DB Stuff //////////////////////

    public void populateBookDB() {
        Log.i(TAG, "populateBookDB");
        String selection =MediaStore.Audio.Media.DATA +" like ?";
        String[] selectionArgs={"%"+File.pathSeparator+bookPath+File.pathSeparator+"%"};
        ContentResolver mediaResolver = getContentResolver();
        Cursor bookCursor = mediaResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                selection,
                selectionArgs,
                null);
        if (bookCursor.moveToFirst()) {
            Log.i(TAG, "populateBookDB bookCursor");
            int titleColumn = bookCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColumn = bookCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int authorColumn = bookCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int bookIDColumn = bookCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            int bookColumn = bookCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int durationColumn = bookCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            BookDBHelper bookDBHelper = new BookDBHelper(this);
            ChapterDBHelper chapterDBHelper = new ChapterDBHelper(this);
            do {
                Log.i(TAG, "populateBookDB loop");
                Chapter chapter = new Chapter();
                long bookID = bookCursor.getLong(bookIDColumn);
                long chapterID = bookCursor.getLong(idColumn);
                String filePath = bookCursor.getString(0);
                File file = new File(filePath);
                Log.i(TAG, "populateBookDB" + file.getName());
                Log.i(TAG, "populateBookDB" + file.getParent());
                
                if (!chapterDBHelper.getChapterInDB(chapterID)) {
                    chapter.setDuration(bookCursor.getLong(durationColumn));
                    chapter.setBookID(bookID);
                    chapter.setId(chapterID);
                    chapter.setFileName(file.getName());
                    chapter.setTitle(bookCursor.getString(titleColumn));
                    chapterDBHelper.insertChapter(chapter);
                }
                
                if (!bookDBHelper.getBookInDB(bookID)) {
                    Book book = new Book(bookID, bookCursor.getString(bookColumn), bookCursor.getString(authorColumn));
                    book.setPath(file.getParent());
                    bookDBHelper.insertBook(book);
                }
            } while (bookCursor.moveToNext());
        }
    }

}
