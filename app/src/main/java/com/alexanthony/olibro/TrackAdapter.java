package com.alexanthony.olibro;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TrackAdapter extends BaseAdapter {
	
	//track list and layout
	private ArrayList<Track> tracks;
	private LayoutInflater trackInf;
	
	//constructor
	public TrackAdapter(Context c, ArrayList<Track> theTracks){
		tracks = theTracks;
		trackInf=LayoutInflater.from(c);
	}

	@Override
	public int getCount() {
		return tracks.size();
	}

	@Override
	public Object getItem(int arg0) {
		return null;
	}

	@Override
	public long getItemId(int arg0) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		//map to track layout
		LinearLayout trackLay = (LinearLayout)trackInf.inflate
				(R.layout.track, parent, false);
		//get title and author views
		TextView songView = (TextView)trackLay.findViewById(R.id.track_title);
		TextView authorView = (TextView)trackLay.findViewById(R.id.track_author);
		//get track using position
		Track currTrack = tracks.get(position);
		//get title and author strings
		songView.setText(currTrack.getTitle());
		authorView.setText(currTrack.getAuthor());
		//set position as tag
		trackLay.setTag(position);
		return trackLay;
	}

}