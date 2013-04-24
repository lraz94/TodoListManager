package il.ac.huji.todolist;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.GridView;

/*
 * An asynchronic task for getting Flicker pics for a requested search String.
 * Gets a Context holding Flicker api key, a String of search and a GridView. 
 * Have a constant number of pics to show - PHOTOS_PER_PAGE.
 * Gathering data is done in three phases:
 * 1. Search by requesting Flicker for locations in farms of the wanted pics (which rise by the search word).
 * 2. Download these pics from those farms as streams.
 * 3. Decode the data to Bitmap objects.
 * After fetching the data it shows each bitmap on an ImageView upon the grid which can be clicked.
 * The background of the selected pic is changed and its position in the GridView
 * is saved as a Tag in order the retrieve this data when the procedure ends.
 * Throughout the procedure of data gathering form Flicker - a progress dialog is shown. 
 * 
 */
public class FlickerAsyncTask  extends AsyncTask<Void,Integer, ArrayList<Bitmap>>{

	// Maximum photos retreived from search
	public static final int PHOTOS_PER_PAGE = 9;

	private Context _ownerActivity;
	private ProgressDialog _progressDialog;
	private String _search;
	private String _apiKey;
	private GridView _grid;
	// helper to know which pic was selected before this one in order the change the background back to white
	private View _lastViewSelected;

	public FlickerAsyncTask(Context context, String search,GridView grid) {
		_ownerActivity = context;
		_search = search;
		_grid = grid;
		_lastViewSelected = null;
		_apiKey = context.getString(R.string.flicker_api_key);
		// set the progress dialog
		_progressDialog = new ProgressDialog(_ownerActivity);
		_progressDialog.setTitle("Flicker photos");
		_progressDialog.setMessage("Getting flicker photos for '"+_search+"'...");
		_progressDialog.setCancelable(false);	
	}	

	@Override	
	protected void onPreExecute() {	
		// Show the progress dialog
		_progressDialog.show();
		super.onPreExecute();
	}


	@Override
	protected ArrayList<Bitmap> doInBackground(Void... params) {
		try{
			ArrayList<Bitmap> result = new ArrayList<Bitmap>();
			// request to Flicker
			URL url = new URL("http://api.flickr.com/services/rest/?method=flickr.photos.search&text=" +
					_search + "&api_key=" + _apiKey + "&per_page="+ PHOTOS_PER_PAGE + "&format=json");
			// Json answer of locations of results in Flicker farms
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			InputStream in = new BufferedInputStream(conn.getInputStream());
			InputStreamReader flickerInput = new InputStreamReader(in);
			BufferedReader flickerReader = new BufferedReader(flickerInput);
			StringBuilder response = new StringBuilder();
			String lineIn;
			while ((lineIn = flickerReader.readLine()) != null) {
				response.append(lineIn);
			}
			flickerReader.close();
			// not yet a valid Json response since Flicker gives a headline which 
			// isn't valid and truncated.
			int start = response.toString().indexOf("(") + 1;
			int end = response.toString().length() - 1;
			String jSONString = response.toString().substring( start, end);
			// After cutting off, its ok
			JSONObject jSONObject = new JSONObject(jSONString); 
			JSONObject jSONObjectInner = jSONObject.getJSONObject("photos");
			JSONArray photoArray = jSONObjectInner.getJSONArray("photo"); 
			for(int i = 0 ; i < photoArray.length() ; i++){
				publishProgress(i+1);	
				JSONObject photo = photoArray.getJSONObject(i);
				// Ask for helper function the construct a url string from the JSONObject and desired size.
				// This url is for the farm the specific file is stored int
				String constructed = constructFlickrImgUrl(photo, size._t);
				// Request the file from the farm
				URL perPhoto = new URL(constructed);
				HttpURLConnection connPerPhoto  = (HttpURLConnection) perPhoto.openConnection();
				InputStream inPerPhoto = new BufferedInputStream(connPerPhoto.getInputStream());
				// Decode to bitmap and add
				result.add(BitmapFactory.decodeStream(inPerPhoto));
				inPerPhoto.close();
			}
			return result;
		}catch(JSONException e){
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}

	// Different sizes for images as described in: flickr.com/services/api/misc.urls.html
	// _t is used here and proper to thumbnail
	enum size {
		_s , _t ,_m
	};

	// helper method, to construct the url from the json object.
	// You can define the size of the image that you want, with the size parameter. 
	private String constructFlickrImgUrl(JSONObject input, @SuppressWarnings("rawtypes") Enum size) throws JSONException {
		String FARMID = input.getString("farm");
		String SERVERID = input.getString("server");
		String SECRET = input.getString("secret");
		String ID = input.getString("id");
		StringBuilder sb = new StringBuilder();
		sb.append("http://farm");
		sb.append(FARMID);
		sb.append(".static.flickr.com/");
		sb.append(SERVERID);
		sb.append("/");
		sb.append(ID);
		sb.append("_");
		sb.append(SECRET);
		sb.append(size.toString());                    
		sb.append(".jpg");
		return sb.toString();
	}


	@Override	
	protected void onProgressUpdate(Integer... values) {	
		_progressDialog.setMessage("Processing photo #"+values[0]);
		super.onProgressUpdate(values);
	}

	@Override
	protected void onPostExecute(final ArrayList<Bitmap> result) {
		_progressDialog.dismiss();
		if(result!=null){
			if( ! result.isEmpty()){
				_grid.setAdapter(new ImageAdapter(_ownerActivity, result));
				_grid.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView<?> arg0, View view,	int position, long arg3) {
						// tag the position of the click for future inquiring
						_grid.setTag(position);
						// Draw the former in white
						if(_lastViewSelected!=null){
							_lastViewSelected.setBackgroundColor(Color.WHITE);
						}
						// Draw new one in cyan
						_lastViewSelected = view;
						view.setBackgroundColor(Color.CYAN);
					}
				});
			}
			else{
				Toast.makeText(_ownerActivity,"Flicker didn't have any photos for this search try again" , Toast.LENGTH_LONG).show();
			}
		}
		else{
			Toast.makeText(_ownerActivity,"Errors in connecting to Flicker, try again" , Toast.LENGTH_LONG).show();
		}
		super.onPostExecute(result);
	}
	
	
	// Image adapter used for the GridView and ImageViews inside it
	// Gets Context and array of Bitmap object to show	
	public static class ImageAdapter extends BaseAdapter {
		private Context _context;
		private ArrayList<Bitmap> _arr;

		public ImageAdapter(Context c,ArrayList<Bitmap> arr) {  
			_context = c; 
			_arr = arr;
		}

		public int getCount() { 
			return _arr.size();  
		}    

		public Object getItem(int position) {   
			return _arr.get(position);
		}	

		public long getItemId(int position) {     
			return position;
		}  

		// create a new ImageView for each item referenced by the Adapter
		public View getView(int position, View convertView, ViewGroup parent) {   
			ImageView imageView;     
			if (convertView == null) {  // if it's not recycled, initialize some attributes    
				imageView = new ImageView(_context);    
				imageView.setLayoutParams(new GridView.LayoutParams(90, 90));
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);    
				imageView.setPadding(3, 3, 3, 3);   
			} 
			else {
				imageView = (ImageView) convertView;    
			} 
			imageView.setImageBitmap(_arr.get(position));
			return imageView; 
		}
	}

}
