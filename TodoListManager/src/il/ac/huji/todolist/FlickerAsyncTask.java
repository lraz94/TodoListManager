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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;
import android.widget.GridView;

public class FlickerAsyncTask  extends AsyncTask<Void,Integer, ArrayList<Bitmap>>{

	public static final int PHOTOS_PER_PAGE = 9;
	private TodoListManagerActivity _ownerActivity;
	private ProgressDialog _progressDialog;
	private String _search;
	private String _apiKey;
	private GridView _grid;
	protected View _lastViewSelected;

	public FlickerAsyncTask(TodoListManagerActivity owner,String search,GridView grid) {
		_ownerActivity = owner;
		_search = search;
		_grid = grid;
		_lastViewSelected = null;
		_apiKey = owner.getString(R.string.flicker_api_key);
		_progressDialog = new ProgressDialog(_ownerActivity);
		_progressDialog.setTitle("Flicker photos");
		_progressDialog.setMessage("Getting flicker photos for '"+_search+"'...");
		_progressDialog.setCancelable(false);	
	}	

	@Override	
	protected void onPreExecute() {	
		_progressDialog.show();
		super.onPreExecute();
	}


	@Override
	protected ArrayList<Bitmap> doInBackground(Void... params) {
		try{
			ArrayList<Bitmap> result = new ArrayList<Bitmap>();
			URL url = new URL("http://api.flickr.com/services/rest/?method=flickr.photos.search&text=" +
					_search + "&api_key=" + _apiKey + "&per_page="+ PHOTOS_PER_PAGE + "&format=json");
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
			// not yet a valid Json response
			int start = response.toString().indexOf("(") + 1;
			int end = response.toString().length() - 1;
			String jSONString = response.toString().substring( start, end);
			//after cutting off the junk, its ok
			JSONObject jSONObject = new JSONObject(jSONString); 
			JSONObject jSONObjectInner = jSONObject.getJSONObject("photos");
			JSONArray photoArray = jSONObjectInner.getJSONArray("photo"); 
			for(int i = 0 ; i < photoArray.length() ; i++){
				publishProgress(i+1);	
				JSONObject photo = photoArray.getJSONObject(i);
				String constructed = constructFlickrImgUrl(photo, size._t);
				URL perPhoto = new URL(constructed);
				HttpURLConnection connPerPhoto  = (HttpURLConnection) perPhoto.openConnection();
				InputStream inPerPhoto = new BufferedInputStream(connPerPhoto.getInputStream());
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

	// source: flickr.com/services/api/misc.urls.html
	enum size {
		_s , _t ,_m
	};

	// helper method, to construct the url from the json object. You can define the size of the image that you want, with the size parameter. 
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
				_grid.setAdapter(new TodoListManagerActivity.ImageAdapter(_ownerActivity, result));
				_grid.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView<?> arg0, View view,	int position, long arg3) {
						// tag the position of the click
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
}
