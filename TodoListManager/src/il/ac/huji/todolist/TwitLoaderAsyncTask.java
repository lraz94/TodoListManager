package il.ac.huji.todolist;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TreeSet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class TwitLoaderAsyncTask extends AsyncTask<Void,Integer, ArrayList<String>> {

	private static final int MAXIMUM_TWEATS = 100;
	private TodoListManagerActivity _ownerActivity;
	private ProgressDialog _progressDialog;
	private SQLiteDatabase _twitDB;
	private String _searchTwit;

	public TwitLoaderAsyncTask(TodoListManagerActivity owner, String searchTwit) {
		_ownerActivity = owner;
		_searchTwit = searchTwit;
		_progressDialog = new ProgressDialog(_ownerActivity);
		_progressDialog.setTitle("Twits loading");
		_progressDialog.setMessage("Getting twits for '"+_searchTwit+"'...");
		_progressDialog.setCancelable(false);	
		TwitsIdDatabaseHelper helper = new TwitsIdDatabaseHelper(_ownerActivity);	
		_twitDB = helper.getWritableDatabase();
		
	}	

	@Override	
	protected void onPreExecute() {	
		_progressDialog.show();
		super.onPreExecute();
	}

	@Override	
	protected ArrayList<String> doInBackground(Void... args){
		TreeSet<Long> alreadyFamiliarTwitsIDs = getAlreadyFamiliarTwitIDs();
		ArrayList<String> twitsTexts = new ArrayList<String>();
		String response = connectTwitGetRespone();
		if(response==null){
			return null;
		}
		try {
			JSONObject json = new JSONObject(response);
			JSONArray arr = json.getJSONArray("results");
			int arrSize = arr.length();
			if(arrSize > MAXIMUM_TWEATS){ // not needed only for double check on rpp=100 
				arrSize = MAXIMUM_TWEATS;
			}
			int valid = 0; // TODO delete valid debug
			for(int i = 0 ; i<arrSize ; i++){
				publishProgress(i+1);	
				JSONObject tweetObject = arr.getJSONObject(i);
				Long id = Long.parseLong(tweetObject.getString("id_str"));
				if(alreadyFamiliarTwitsIDs.add(id)){
					// insert to db of IDs
					ContentValues twitID = new ContentValues(); 
					twitID.put("id",id);
					_twitDB.insert("twits", null,twitID);
					String title = tweetObject.getString("text");
					if(title!=null){
						twitsTexts.add(title);
						valid++;
					}
				}
			}
			System.out.println("Valid twits:"+valid+" | Array Size: "+arrSize);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return twitsTexts;
	}	

	private TreeSet<Long> getAlreadyFamiliarTwitIDs() {
		TreeSet<Long> ids = new TreeSet<Long>();
		Cursor cursor = _twitDB.query("twits", new String[] {"id"},null, null, null, null, null);
		if (cursor.moveToFirst()) {
			do {
				Long id = cursor.getLong(0);
				ids.add(id);
			} while (cursor.moveToNext());
		}
		return ids;
	}

	@Override	
	protected void onProgressUpdate(Integer... values) {	
		_progressDialog.setMessage("Processing twit numeber "+values[0]);
		super.onProgressUpdate(values);
	}

	@Override	
	protected void onPostExecute(ArrayList<String> result) {
		_progressDialog.dismiss();
		if(result==null){
			// errors on connection
			Toast.makeText(_ownerActivity, "Sorry, errors on connecting to Twiter",Toast.LENGTH_LONG).show();
			return;
		}
		if(! result.isEmpty()){
			final Dialog dialog = new Dialog(_ownerActivity);
			dialog.setContentView(R.layout.selecttwits);
			dialog.setTitle("Add twits to your list");
			dialog.setCancelable(false);
			ListView listView = (ListView) dialog.findViewById(R.id.twitlist);
			final TwitListAdapter twitAdapter = new TwitListAdapter(result);
			listView.setAdapter(twitAdapter);
			Button backToList = (Button) dialog.findViewById(R.id.backToListBtn);
			backToList.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					Calendar now = Calendar.getInstance();
					dialog.dismiss();
					TreeSet<Integer> positions = twitAdapter.getPositionChecked();
					for(Integer pos : positions){
						Calendar fixed = Calendar.getInstance();
						fixed.clear();
						fixed.set(Calendar.YEAR, now.get(Calendar.YEAR));
						fixed.set(Calendar.MONTH,now.get(Calendar.MONTH));
						fixed.set(Calendar.DAY_OF_MONTH,now.get(Calendar.DAY_OF_MONTH));
						TodoItem task = new TodoItem(twitAdapter.getItem(pos),fixed.getTime(),null);
						_ownerActivity.insertTodo(task);
					}
				}
			});
			dialog.show();
		}
		else{
			Toast.makeText(_ownerActivity, "No new twits to add for '"+_searchTwit+"'", Toast.LENGTH_SHORT).show();
		}
		super.onPostExecute(result);
	}

	private String connectTwitGetRespone() {
		try {
			String forURl = _searchTwit;
			if(forURl.startsWith("#")){
				forURl = "%23"+forURl.substring(1);
			}
			String urlTxt = "http://search.twitter.com/search.json?q="+forURl+"&rpp=100&include_entities=true";
			System.out.println("url for: "+urlTxt); // TODO remove syso
			URL url = new URL(urlTxt);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			InputStream in = new BufferedInputStream(conn.getInputStream());
			InputStreamReader tweetInput = new InputStreamReader(in);
			BufferedReader tweetReader = new BufferedReader(tweetInput);
			StringBuilder resonse = new StringBuilder();
			String lineIn;
			while ((lineIn = tweetReader.readLine()) != null) {
				resonse.append(lineIn);
			}
			tweetReader.close();
			return resonse.toString(); 
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}


	private class TwitListAdapter extends ArrayAdapter<String> {
		TreeSet<Integer> _positionsChecked;
		
		public TwitListAdapter(List<String> tasks) {	
			super(_ownerActivity, android.R.layout.simple_list_item_1, tasks);
			_positionsChecked = new TreeSet<Integer>();
		}	
		
		public TreeSet<Integer> getPositionChecked(){
			return _positionsChecked;
		}

		@Override
		public View getView(final int position, View v, ViewGroup parent)    {
			final String text = getItem(position);
			LayoutInflater inflater = LayoutInflater.from(_ownerActivity);
			final View view = inflater.inflate(R.layout.twitrow, null);
			TextView title = (TextView) view.findViewById(R.id.twitText);
			title.setText(text);
			final CheckBox checkbox = (CheckBox) view.findViewById(R.id.twitCheckBox);
			if(_positionsChecked.contains(position)){
				checkbox.setChecked(true); // for checkboxes selection erase while scroll bug
			}
			checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if(isChecked){
						_positionsChecked.add(position);
					}
					else{
						_positionsChecked.remove(position);
					}
				}
			});
			return view;
		}
	}
}
