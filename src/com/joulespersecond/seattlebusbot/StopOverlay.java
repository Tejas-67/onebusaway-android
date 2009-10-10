package com.joulespersecond.seattlebusbot;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class StopOverlay extends ItemizedOverlay<OverlayItem> {
	//private static final String TAG = "StopOverlay";
	
	private ObaArray mStops;
	private Activity mActivity;
	
	private static final int getResourceIdForDirection(String direction) {
		if (direction.equals("N")) {
			return R.drawable.stop_n_state;
		} else if (direction.equals("NW")) {
			return R.drawable.stop_nw_state;	    			
		} else if (direction.equals("W")) {
			return R.drawable.stop_w_state;	    			
		} else if (direction.equals("SW")) {
			return R.drawable.stop_sw_state;	
		} else if (direction.equals("S")) {
			return R.drawable.stop_s_state;	
		} else if (direction.equals("SE")) {
			return R.drawable.stop_se_state;	
		} else if (direction.equals("E")) {
			return R.drawable.stop_e_state;	
		} else if (direction.equals("NE")) {
			return R.drawable.stop_ne_state; 		    	    		
		} else {
			Log.v("TransitBotStopOverlay", "Unknown direction: " + direction);
			return R.drawable.stop_n_state;
		}		
	}
	
	public class StopOverlayItem extends OverlayItem {
		private ObaStop mStop;

		public StopOverlayItem(ObaStop stop) {
			super(stop.getLocation(), stop.getName(), "");
			mStop = stop;
		}
		public ObaStop getStop() {
			return mStop;
		}
	}
	
	public StopOverlay(ObaArray stops, 
			Activity activity) {
		super(boundCenterBottom(activity.getResources().getDrawable(R.drawable.stop_n)));
		mStops = stops;
		mActivity = activity;
		populate();
	}
	@Override
	protected OverlayItem 
	createItem(int i) {
		final ObaStop stop = mStops.getStop(i);
		final OverlayItem item = new StopOverlayItem(stop);
		int res = getResourceIdForDirection(stop.getDirection());
		final Drawable marker = mActivity.getResources().getDrawable(res);
		item.setMarker(boundCenterBottom(marker));
		return item;
	}
	@Override
	public int size() {
		return mStops.length();
	}
	@Override
	public boolean onTrackballEvent(MotionEvent event, MapView view) {
		final float xDiff = event.getX();
		final float yDiff = event.getY();
		OverlayItem next = null;
		
		// Up
		if (yDiff <= -1) {
			next = findNext(getFocus(), true, true);
		}
		// Down
		else if (yDiff >= 1) {
			next = findNext(getFocus(), true, false);
		}
		// Right
		else if (xDiff >= 1) {
			next = findNext(getFocus(), false, true);
		}
		// Left
		else if (xDiff <= -1) {
			next = findNext(getFocus(), false, false);
		}
		if (next != null) {
			setFocus(next);
			view.postInvalidate();
		}
		return true;
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event, MapView view) {
		OverlayItem next = null;
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_UP:
			next = findNext(getFocus(), true, true);
			break;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			next = findNext(getFocus(), true, false);
			break;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			next = findNext(getFocus(), false, true);
			break;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			next = findNext(getFocus(), false, false);
			break;
		default:
			return false;
		}
		if (next != null) {
			setFocus(next);
			view.postInvalidate();
		}
		return true;
	}
	void setFocusById(String id) {
		final int size = size();
		for (int i=0; i < size; ++i) {
			StopOverlayItem item = (StopOverlayItem)getItem(i);
			if (id.equals(item.getStop().getId())) {
				setFocus(item);
				break;
			}
		}
	}
	
	/*
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event, MapView view) {
		// For now, eat all keys
		return true;
	}
	*/
	@Override
	protected boolean onTap(int index) {
		final OverlayItem item = getItem(index);
		if (item.equals(getFocus())) {
			ObaStop stop = mStops.getStop(index);
			Intent myIntent = new Intent(mActivity, StopInfoActivity.class);
			myIntent.putExtra(StopInfoActivity.STOP_ID, stop.getId());
			mActivity.startActivity(myIntent);			
		}
		else {
			setFocus(getItem(index));
		}
		return true;
	}
	
	// The find next routines find the closest item along the specified axis.

	OverlayItem findNext(OverlayItem initial, boolean lat, boolean positive) {
		final int size = size();
		final GeoPoint initialPoint = initial.getPoint();
		OverlayItem min = initial;
		int minDist = Integer.MAX_VALUE;
		
		for (int i=0; i < size; ++i) {
			OverlayItem item = getItem(i);
			GeoPoint point = item.getPoint();
			final int distX = point.getLongitudeE6() - initialPoint.getLongitudeE6();
			final int distY = point.getLatitudeE6()  - initialPoint.getLatitudeE6(); 
			
			// We have to eliminate anything that's going in the wrong direction,
			// or doesn't change in the correct axis (including the initial point)
			if (lat) {
				if (positive) {
					// Distance must be positive.
					if (distY <= 0) {
						continue;
					}
				}
				// Distance must to be negative.
				else if (distY >= 0) {
					continue;
				}
			}
			else {
				if (positive) {
					// Distance must be positive
					if (distX <= 0) {
						continue;
					}
				} 
				// Distance must be negative
				else if (distX >= 0) {
					continue;
				}
			}

			final int distSq = distX*distX + distY*distY;
		
			if (distSq < minDist) {
				min = item;
				minDist = distSq;
			}
		}		
		return min;
	}
}