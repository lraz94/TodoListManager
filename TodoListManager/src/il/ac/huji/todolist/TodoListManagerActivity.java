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
import android.content.Context;
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
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class TodoListManagerActivity extends Activity {

	public static final int CODE_FOR_ADD_NEW = 100;
	public static final int CODE_FOR_PREFRENCES = 200;
	public static final String DEFAULT_TWIT = "#todoapp";
	private CustomListAdapter _adapter;
	private ListView _list;
	private TodoDAL _todoDal;
	private String _lastTwitSearch;


	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy",Locale.US);
	private static final String STRING_FOR_NO_DATE = "No due date";
	private static final String CALL = "Call ";

	public static final String DUE_DATE_EXTRA_STR = "dueDate";
	public static final String TITLE_EXTRA_STR = "title";
	public static final String IS_UPDATE_EXTRA_STR = "update";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_todo_list_manager);
		// init todoDal
		_todoDal = new TodoDAL(this);
		_list = (ListView)findViewById(R.id.lstTodoItems);
		List<TodoItem> stored = _todoDal.all();
		System.out.println("Stored tasks are: "+stored);
		for(TodoItem item : stored){
			loadBitmapFromFile(item);
		}
		_adapter = new CustomListAdapter(this,stored);
		_list.setAdapter(_adapter);
		registerForContextMenu(_list);	
		_lastTwitSearch = DEFAULT_TWIT;
		new TwitLoaderAsyncTask(this,DEFAULT_TWIT).execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.todo_list_manager, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);   
		switch (item.getItemId()){
		case R.id.menuItemAdd : {
			Intent intent = new Intent(this, AddNewTodoItemActivity.class);
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
		if (reqCode==CODE_FOR_ADD_NEW && resCode == RESULT_OK){
			if(data!=null){
				String title = data.getStringExtra(TITLE_EXTRA_STR);
				Date date = null;
				Object dateObj = data.getSerializableExtra(DUE_DATE_EXTRA_STR);
				if(dateObj!=null){
					date = (Date) dateObj;
				}
				boolean isUpdate = data.getBooleanExtra(IS_UPDATE_EXTRA_STR, false);
				if(isUpdate){
					// get existing object with same title, old date and might be thumbPath + bitmap
					TodoItem toFetch = _adapter.getItem(_adapter.getPosition(new TodoItem(title, null, null)));
					// update to new date
					TodoItem newTask = new TodoItem(title,date,toFetch.getThumbnailPath());
					newTask.setBitmap(toFetch.getBitmap());
					updateTodoDate(newTask);
				}
				else{
					TodoItem newTask = new TodoItem(title,date,null);
					insertTodo(newTask);
				}
			}
		}
		else if( reqCode!=CODE_FOR_ADD_NEW){ // preferences
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);   
			String twitSearch = prefs.getString(getString(R.string.twit_preference_key),DEFAULT_TWIT);
			if(twitSearch.length()>0 && !twitSearch.equals(_lastTwitSearch)){
				_lastTwitSearch = twitSearch;
				new TwitLoaderAsyncTask(this,twitSearch).execute();
			}
		}
	}

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

	public void deleteTodo(TodoItem todo) {
		_adapter.remove(todo);
		boolean fromDelete = _todoDal.delete(todo);
		System.out.println("Got from delete at TodoDAL: "+fromDelete);
	}

	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo info) {
		getMenuInflater().inflate(R.menu.ctxmenuextended, menu);
		int pos = ((AdapterContextMenuInfo) info).position;
		TodoItem taskPair = _adapter.getItem(pos);
		String taskStr = taskPair.getTitle();
		menu.setHeaderTitle(taskStr);
		if(taskStr==null || !taskStr.contains(CALL)){
			menu.removeItem(R.id.menuItemCall);
		}
		else{
			MenuItem callItem = menu.findItem(R.id.menuItemCall);
			callItem.setTitle(taskStr);
		}
		super.onCreateContextMenu(menu,v,info);
	}

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
				Intent dial = new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+gotFromTask));
				startActivity(dial);
			}
			break;
		}
		case R.id.menuItemUpdate:{
			Intent intent = new Intent(this, AddNewTodoItemActivity.class);
			intent.putExtra("text",taskPair.getTitle());
			String thumbPath = taskPair.getThumbnailPath();
			if(thumbPath!=null){
				intent.putExtra("thumbpath",thumbPath);
			}
			startActivityForResult(intent, CODE_FOR_ADD_NEW);
			break;
		}
		case R.id.menuItemThumbnail:{
			final Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.flickerdialog);
			dialog.setTitle("Add Thumbnail");
			final GridView gridview = (GridView) dialog.findViewById(R.id.flickerGrid);
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.empty);
			ArrayList<Bitmap> temp = new ArrayList<Bitmap>();
			for(int i = 0 ; i < FlickerAsyncTask.PHOTOS_PER_PAGE ; i++){
				temp.add(bitmap);
			}
			gridview.setAdapter(new TodoListManagerActivity.ImageAdapter(this, temp));
			final EditText editSearch = (EditText) dialog.findViewById(R.id.flickerSearchEdit);
			Button search = (Button) dialog.findViewById(R.id.searchFlickerButton);
			search.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String entered =  editSearch.getText().toString();
					if(entered.length()>0){
						new FlickerAsyncTask(TodoListManagerActivity.this,entered, gridview).execute();
					}
				}
			});
			Button flickerOK = (Button) dialog.findViewById(R.id.flickerOK);
			flickerOK.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Object selected = gridview.getTag(); // tagged the click
					if(selected!=null){
						try{
							Integer position = (Integer) selected;
							Bitmap bitmap = (Bitmap) gridview.getItemAtPosition(position);
							bitmapToAllOutputsUpdate(taskPair,bitmap);
							updateTodoThumbnale(taskPair);
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

	public void bitmapToAllOutputsUpdate(TodoItem todo, Bitmap bitmap) throws IOException {
		String pathForBitmap = todo.getTitle().replaceAll(" ","_").replaceAll(File.separator,"_"); // path is the title
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
		todo.setPathForThumbnale(pathForBitmap); // for db
		todo.setBitmap(bitmap); // to present now, save reloading
		todo.setBytesToThumbnale(bytes); // for Parse
	}

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
				Calendar toCompare = Calendar.getInstance();
				toCompare.clear();
				toCompare.set(Calendar.YEAR,now.get(Calendar.YEAR));
				toCompare.set(Calendar.MONTH,now.get(Calendar.MONTH));
				toCompare.set(Calendar.DAY_OF_MONTH,now.get(Calendar.DAY_OF_MONTH));
				Calendar other = Calendar.getInstance();
				other.setTime(dateGot);
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
