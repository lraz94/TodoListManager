package il.ac.huji.todolist;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/* 
 * App activity runs the list of tasks with titles, due dates and Flicker thumbnails.
 * At startup searches for #todoapp in Twiter and gets up to 100 new twits you have't selected,
 * and ask you the choose which to add to the list under the date of the insertion (no NLP).
 * Search String can be change and new search for new twits will take place. Twits are on track so you never get old twits
 * you refused to add the first time you saw them, even after reopening the app after you closed it.
 * Dates can always be updated. Tasks which their due date has passed are painted red.
 * The date only care for day, month, year and not specific time in the day so all tasks are considered as all-day event.
 * Changing Thumbnail is also opened in any time. Backed up by Parse and internal DB so the data is saved after closing.
 * Can perfrom dial up to tasks starts with "Call " which only opens dialer and put the rest of the "task" on the dialer screen as is.
 */ 
public class TodoListManagerActivity extends Activity {

	// Code for return from AddUpdateTodoItemActivity
	public static final int CODE_FOR_ADD_NEW = 100;
	// Code for return from Preferences
	public static final int CODE_FOR_PREFRENCES = 200;

	public static final String DEFAULT_TWIT = "#todoapp";

	// adapter for the list of items
	private CustomListAdapter _adapter;

	// TodoDAL object
	private TodoDAL _todoDal;

	// To verify if a new search is needed or we already search this word the last time.
	private String _lastTwitSearch;


	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy",Locale.US);
	// GUI constants
	private static final String STRING_FOR_NO_DATE = "No due date";
	private static final String CALL = "Call ";

	// constant Extras in AddUpdateTodoItemActivity
	public static final String DUE_DATE_EXTRA_STR = "dueDate";
	public static final String TITLE_EXTRA_STR = "title";
	public static final String IS_UPDATE_EXTRA_STR = "update";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_todo_list_manager);
		// init todoDal
		_todoDal = new TodoDAL(this);
		// init list
		ListView list = (ListView)findViewById(R.id.lstTodoItems);
		// get all todoItems from the todoDal with thubnails as paths
		List<TodoItem> stored = _todoDal.all();
		// Load a bitmap to each item if path exists - does not update byte array (can only erase it and update from here).
		for(TodoItem item : stored){
			loadBitmapFromFile(item);
		}
		// setting adapter
		_adapter = new CustomListAdapter(this,stored);
		list.setAdapter(_adapter);
		// context menu registering
		registerForContextMenu(list);	
		// twits search
		_lastTwitSearch = DEFAULT_TWIT;
		new TwitLoaderAsyncTask(this,DEFAULT_TWIT).execute();
	}

	// Closing DB for memory leaks avoidance
	@Override
	protected void onDestroy() {
		if(_todoDal!=null){
			_todoDal.closeDB();
		}
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.todo_list_manager, menu);
		return true;
	}

	// menu have add a new item ability and show preferences
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);   
		switch (item.getItemId()){
		case R.id.menuItemAdd : {
			Intent intent = new Intent(this, AddUpdateTodoItemActivity.class);
			startActivityForResult(intent, CODE_FOR_ADD_NEW);
			break;
		}
		case R.id.menuItemPreferences:{
			Intent intent = new Intent(this, PrefsActivity.class); 
			startActivityForResult(intent, CODE_FOR_PREFRENCES);
			break;
		}
		}
		return true;
	}


	protected void onActivityResult(int reqCode, int resCode, Intent data) {
		// return from AddUpdateTodoItemActivity
		if (reqCode==CODE_FOR_ADD_NEW && resCode == RESULT_OK){
			if(data!=null){
				String title = data.getStringExtra(TITLE_EXTRA_STR);
				Date date = null;
				Object dateObj = data.getSerializableExtra(DUE_DATE_EXTRA_STR);
				if(dateObj!=null){
					date = (Date) dateObj;
				}
				boolean isUpdate = data.getBooleanExtra(IS_UPDATE_EXTRA_STR, false);
				// determine update or add
				if(isUpdate){
					// get existing object with same title, old date and might be thumbPath / bitmap
					TodoItem toFetch = _adapter.getItem(_adapter.getPosition(new TodoItem(title, null, null)));
					// update to new date
					TodoItem newTask = new TodoItem(title,date,toFetch.getThumbnailPath());
					// set all other method as a shallow as an alias (shallow copy)
					newTask.setBitmap(toFetch.getBitmap());
					// use helper function to update todoDAL + adapter
					updateTodoDate(newTask);
				}
				else{
					TodoItem newTask = new TodoItem(title,date,null);
					// use helper function to insert on todoDAL + adapter
					insertTodo(newTask);
				}
			}
		}
		else if( reqCode!=CODE_FOR_ADD_NEW){ // preferences
			// Get the twit search word
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);   
			String twitSearch = prefs.getString(getString(R.string.twit_preference_key),DEFAULT_TWIT);
			// Verify if a new and if so do a twit search
			if(twitSearch.length()>0 && !twitSearch.equals(_lastTwitSearch)){
				_lastTwitSearch = twitSearch;
				new TwitLoaderAsyncTask(this,twitSearch).execute();
			}
		}
	}

	// helper functions to maintain changes to adapter+todoDAL 
	// all since "requery" for Corser if depracted.
	public void insertTodo(TodoItem todo) {
		_adapter.add(todo);
		boolean fromInserst = _todoDal.insert(todo);
		System.out.println("Got from insert at TodoDAL: "+fromInserst);
	}

	public void updateTodoDate(TodoItem todo){
		int position = _adapter.getPosition(todo);
		_adapter.remove(todo);
		_adapter.insert(todo,position);
		boolean fromUpdate = _todoDal.updateDate(todo);
		System.out.println("Got from update date at TodoDAL: "+fromUpdate);
	}

	public void updateTodoThumbnale(TodoItem todo){
		int position = _adapter.getPosition(todo);
		_adapter.remove(todo);
		_adapter.insert(todo,position);
		boolean fromUpdate = _todoDal.updateThmbnale(todo);
		System.out.println("Got from update thumbnale at TodoDAL: "+fromUpdate);
	}

	public void deleteTodo(TodoItem todo) {
		_adapter.remove(todo);
		boolean fromDelete = _todoDal.delete(todo);
		System.out.println("Got from delete at TodoDAL: "+fromDelete);
	}

	// load bitmap from path on the device
	private void loadBitmapFromFile(TodoItem todo) {
		String path = todo.getThumbnailPath();
		if(path!=null){
			try {
				todo.setBitmap(BitmapFactory.decodeStream(openFileInput(path)));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	// Called on long click on an item
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo info) {
		getMenuInflater().inflate(R.menu.ctxmenuextended, menu);
		int pos = ((AdapterContextMenuInfo) info).position;
		TodoItem taskPair = _adapter.getItem(pos);
		String taskStr = taskPair.getTitle();
		menu.setHeaderTitle(taskStr);
		// See if the remove call option or not based on the title
		if(taskStr==null || !taskStr.contains(CALL)){
			menu.removeItem(R.id.menuItemCall);
		}
		else{
			MenuItem callItem = menu.findItem(R.id.menuItemCall);
			callItem.setTitle(taskStr);
		}
		super.onCreateContextMenu(menu,v,info);
	}

	// Select context item: Delete, Call, Update Due Date or Add Thumbnail
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		int pos = info.position;
		final TodoItem taskPair = _adapter.getItem(pos);
		switch (item.getItemId()){
		case R.id.menuItemDelete:{
			deleteTodo(taskPair);
			break;
		}
		case R.id.menuItemCall:{
			String gotFromTask = taskPair.getTitle();
			if(gotFromTask!=null && gotFromTask.startsWith(CALL)){
				gotFromTask = gotFromTask.substring(CALL.length());
				// launch intent for dial
				Intent dial = new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+gotFromTask));
				startActivity(dial);
			}
			break;
		}
		case R.id.menuItemUpdate:{
			Intent intent = new Intent(this, AddUpdateTodoItemActivity.class);
			intent.putExtra("text",taskPair.getTitle());
			String thumbPath = taskPair.getThumbnailPath();
			if(thumbPath!=null){
				intent.putExtra("thumbpath",thumbPath);
			}
			// launch intent to update
			startActivityForResult(intent, CODE_FOR_ADD_NEW);
			break;
		}
		case R.id.menuItemThumbnail:{
			// Show dialog which will triger Flicker search after inserting non empry search String+hitting Ok
			final Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.flickerdialog);
			dialog.setTitle("Add Thumbnail");
			// Get GridView to work on
			final GridView gridview = (GridView) dialog.findViewById(R.id.flickerGrid);
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.empty);
			// Get temporary pic with "no picture" text to show until proper ones will arrive.
			ArrayList<Bitmap> temp = new ArrayList<Bitmap>();
			for(int i = 0 ; i < FlickerAsyncTask.PHOTOS_PER_PAGE ; i++){
				temp.add(bitmap);
			}
			// setting the temporary adapter
			gridview.setAdapter(new FlickerAsyncTask.ImageAdapter(this, temp));
			final EditText editSearch = (EditText) dialog.findViewById(R.id.flickerSearchEdit);
			Button search = (Button) dialog.findViewById(R.id.searchFlickerButton);
			search.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String entered =  editSearch.getText().toString();
					if(entered.length()>0){
						// good search word - launch
						new FlickerAsyncTask(TodoListManagerActivity.this,entered, gridview).execute();
					}
				}
			});
			Button flickerOK = (Button) dialog.findViewById(R.id.flickerOK);
			flickerOK.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Object selected = gridview.getTag(); // tagged the click on the FlickerAsyncTask
					if(selected!=null){
						try{
							Integer position = (Integer) selected; // find position
							Bitmap bitmap = (Bitmap) gridview.getItemAtPosition(position); // get bitmap
							// publish to every stream: save locally and update the item itself with all data:
							// The path to the file saved, byte array of thumbnail and Bitmap got before
							bitmapToAllOutputsUpdate(taskPair,bitmap); 
							updateTodoThumbnale(taskPair); // save on adapater + todoDAL
							dialog.dismiss();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					else{
						Toast.makeText(TodoListManagerActivity.this,"Click a thumbnale and press OK", Toast.LENGTH_SHORT).show();
					}
				}
			});
			Button flickerCancel = (Button) dialog.findViewById(R.id.flickerCancel);
			flickerCancel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
				}
			});
			dialog.show();
			break;
		}
		}
		return true;
	}

	// Saves the Bitmap locally with the item title as file name with spearators or blanks replaced with '_'
	// Also update the item with all conected to thumbnail (path,byte array and bitmap) 
	private void bitmapToAllOutputsUpdate(TodoItem todo, Bitmap bitmap) throws IOException {
		String pathForBitmap = todo.getTitle().replaceAll(" ","_").replaceAll(File.separator,"_"); // path is the title, separators unallowed
		// Prepare byte stream for Parse
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
		byteStream.flush();
		byte[] bytes = byteStream.toByteArray();
		// Save to local file
		FileOutputStream fileOut = openFileOutput(pathForBitmap, Activity.MODE_PRIVATE); 
		byteStream.writeTo(fileOut);
		fileOut.flush();
		byteStream.close();
		fileOut.close();
		// Update the item
		todo.setPathForThumbnale(pathForBitmap); // for db
		todo.setBitmap(bitmap); // to present now, save reloading
		todo.setBytesToThumbnale(bytes); // for Parse
	}

	// Custom list adapter to hold the items
	private class CustomListAdapter extends ArrayAdapter<TodoItem> {
		public CustomListAdapter(TodoListManagerActivity activity, List<TodoItem> tasks) {	
			super(activity, android.R.layout.simple_list_item_1, tasks);
		}

		@Override
		public View getView(int position, View v, ViewGroup parent)    {
			TodoItem item = getItem(position);
			LayoutInflater inflater = LayoutInflater.from(getContext());
			View view = inflater.inflate(R.layout.row, null);
			TextView title = (TextView) view.findViewById(R.id.txtTodoTitle);
			title.setText(item.getTitle());
			TextView dateTxt = (TextView) view.findViewById(R.id.txtTodoDueDate);
			Date dateGot = item.getDueDate();
			if(dateGot!=null){
				dateTxt.setText(DATE_FORMAT.format(dateGot));
				// this date
				Calendar now = Calendar.getInstance();
				// only interest in dd/MM/yyyy and clear other Calendar fields
				Calendar toCompare = Calendar.getInstance();
				toCompare.clear();
				toCompare.set(Calendar.YEAR,now.get(Calendar.YEAR));
				toCompare.set(Calendar.MONTH,now.get(Calendar.MONTH));
				toCompare.set(Calendar.DAY_OF_MONTH,now.get(Calendar.DAY_OF_MONTH));
				// the item date
				Calendar other = Calendar.getInstance();
				other.setTime(dateGot);
				// only interest in dd/MM/yyyy and clear other Calendar fields
				Calendar gotDateToComapre = Calendar.getInstance();
				gotDateToComapre.clear();
				gotDateToComapre.set(Calendar.YEAR,other.get(Calendar.YEAR));
				gotDateToComapre.set(Calendar.MONTH,other.get(Calendar.MONTH));
				gotDateToComapre.set(Calendar.DAY_OF_MONTH,other.get(Calendar.DAY_OF_MONTH));
				if(gotDateToComapre.before(toCompare)){
					title.setTextColor(Color.RED);
					dateTxt.setTextColor(Color.RED);
				}
				else{
					title.setTextColor(Color.BLACK);
					dateTxt.setTextColor(Color.BLACK);					
				}
			}
			else{
				dateTxt.setText(STRING_FOR_NO_DATE);
				title.setTextColor(Color.BLACK);
				dateTxt.setTextColor(Color.BLACK);
			}
			Bitmap bitmap = item.getBitmap();
			if(bitmap!=null){
				ImageView imageview = (ImageView) view.findViewById(R.id.flickerImageView);
				imageview.setImageBitmap(bitmap);
			}
			return view;
		}
	}
}
