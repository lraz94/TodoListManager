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

/*
 * Loading Twits from Twiter given a search word with a processDialog in between.
 * 1. Look for the 100 first twits came out for the given search.
 * 2. Than, query for existing twits IDs in the SQL DB and each one there (by ID) - ignores it.
 * 3. All others - add them to the DB and represent on end in a list.
 * 4. On this list the user can select twits wanted to be added to his list via checkbox for each twit. 
 * 5. All selections are returned to get inserted at TodoListManagerActivity of the caller.
 */
public class TwitLoaderAsyncTask extends AsyncTask<Void,Integer, ArrayList<String>> {

	private static final int MAXIMUM_TWEATS = 100;
	// Holds for context and return for insertion
	private TodoListManagerActivity _ownerActivity;
	private ProgressDialog _progressDialog;
	// DB of twits
	private SQLiteDatabase _twitDB;
	private String _searchTwit;

	public TwitLoaderAsyncTask(TodoListManagerActivity owner, String searchTwit) {
		_ownerActivity = owner;
		_searchTwit = searchTwit;
		//  init dialog
		_progressDialog = new ProgressDialog(_ownerActivity);
		_progressDialog.setTitle("Twits loading");
		_progressDialog.setMessage("Getting twits for '"+_searchTwit+"'...");
		_progressDialog.setCancelable(false);
		// init DB
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
		// Get all stored twits IDs from local DB
		TreeSet<Long> alreadyFamiliarTwitsIDs = getAlreadyFamiliarTwitIDs();
		ArrayList<String> twitsTexts = new ArrayList<String>();
		// Connect Twitwer for respone via helper function
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
			for(int i = 0 ; i<arrSize ; i++){
				publishProgress(i+1);	
				JSONObject tweetObject = arr.getJSONObject(i);
				Long id = Long.parseLong(tweetObject.getString("id_str"));
				// See if ids exist - all of them we return false from add and therefor won't be added to user selection
				if(alreadyFamiliarTwitsIDs.add(id)){
					// insert id to db of IDs - new adding
					ContentValues twitID = new ContentValues(); 
					twitID.put("id",id);
					_twitDB.insert("twits", null,twitID);
					// add the twit itself to selection list
					String title = tweetObject.getString("text");
					if(title!=null){
						twitsTexts.add(title);
					}
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		_twitDB.close(); // don't need anymore (memleaks..)
		return twitsTexts;
	}	

	// Helper run quert to get all IDs of twits from the DB
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

	// Shows all results in a selection list with checkboxes
	@Override	
	protected void onPostExecute(ArrayList<String> result) {
		_progressDialog.dismiss();
		if(result==null){
			// errors on connection
			Toast.makeText(_ownerActivity, "Sorry, no match or might be errors on the net...",Toast.LENGTH_LONG).show();
			return;
		}
		if(! result.isEmpty()){
			// start dialog
			final Dialog dialog = new Dialog(_ownerActivity);
			dialog.setContentView(R.layout.selecttwits);
			dialog.setTitle("Add twits to your list");
			dialog.setCancelable(false);
			ListView listView = (ListView) dialog.findViewById(R.id.twitlist);
			// use TwitListAdapter
			final TwitListAdapter twitAdapter = new TwitListAdapter(result);
			listView.setAdapter(twitAdapter);
			// on click of the back to list the adapter itself store all checkboxes (i.e. items) selected  
			Button backToList = (Button) dialog.findViewById(R.id.backToListBtn);
			backToList.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					Calendar now = Calendar.getInstance();
					dialog.dismiss();
					TreeSet<Integer> positions = twitAdapter.getPositionChecked(); //get all checked items via indexes
					for(Integer pos : positions){
						// set current date in dd/MM/yyyy
						Calendar fixed = Calendar.getInstance();
						fixed.clear();
						fixed.set(Calendar.YEAR, now.get(Calendar.YEAR));
						fixed.set(Calendar.MONTH,now.get(Calendar.MONTH));
						fixed.set(Calendar.DAY_OF_MONTH,now.get(Calendar.DAY_OF_MONTH));
						// update the caller of the selection
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

	// helper for the connection with Twiter and getting String JSON result
	private String connectTwitGetRespone() {
		try {
			String forURl = _searchTwit;
			if(forURl.startsWith("#")){
				forURl = "%23"+forURl.substring(1);
			}
			String urlTxt = "http://search.twitter.com/search.json?q="+forURl+"&rpp=100&include_entities=true";
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


	// TwitListAdapter holds all selections with a TreeSet<Integer> which are the indexes - for better performance.
	// This is very important since scrolling makes the GUI show the checkbox as unchecked although its checked by the system concern and user.
	// Therefore this TreeSet holds the data and each time of getView() the specific position in analyzed if its checked by asigning 
	// A listener to the change and update the state in each time in both directions checked<->unchecked
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
			// must listen to the checkbox and update its state from checked to unchecked and vice versa in each call
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
