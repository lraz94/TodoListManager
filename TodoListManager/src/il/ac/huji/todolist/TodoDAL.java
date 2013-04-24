package il.ac.huji.todolist;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.json.JSONObject;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.ParseException;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;


/*
 * A class which holds all connections to SQL DB and Parse regarding adding new TodoItem,
 * Updating due date or thumbnail, or Deleting an exist TodoItem. 
 * Title and due date are saved both to DB and Parse. However thumbnails: Parse stores
 * the data in real ParseFile and require the given items to have a byte array for that, while
 * DB suits with only a path to the actual file on the device itself.
 * Nevertheless, only the updateThmbnale() method really uses the byte array or path, so it can be absent in other calls.
 * This is because the policy for new items that they are entered without a thumbnail (bytes nor path).
 * The requirement is on the caller responsibility and not checked.
 * Also, ALL ITEMS ARE FETCHED BY TITLE, which means updateDate() or delete can be done by title only (with given new
 * due date when needed) and null titles return the call w/o doing anything. Also it means that if user inserted 2 same titles,
 * both of them are entered, but will be removed or updated together.
 * The All() method will return all items stored but only with the title, due date and path String updated.
 * To acquire Bitmaps and byte arrays of thumbnails - one needed to load the data from the device by the path.
 * 
 * Please Note: Updating shall be done separately to due date and thumbnail (meaning the given item is changed only by one of them)
 * or else inconsistencies will rose at the gap. The user can use both methods in one shot and disregard this gap if wanted.
 * After the second update they are both consistent. Make sure not to call this class methods too close one to another since 
 * Parse work is done in background and need time for the connection and data uploading the take place - especially 
 * uploading thumbnails in updateThmbnale().
 */
public class TodoDAL {

	private Context _context;
	private SQLiteDatabase _db;

	// Constructor called once in an app start gets context with the parseApplication key and clientKey
	public TodoDAL(Context context) {
		_context = context;
		TodoListManagerDatabaseHelper helper = new TodoListManagerDatabaseHelper(_context);	
		// get SQL DB from helper class
		_db = helper.getWritableDatabase();
		// Initialize Parse and automatic user
		Parse.initialize(context, context.getResources().getString(R.string.parseApplication),
				context.getResources().getString(R.string.clientKey)); 
		ParseUser.enableAutomaticUser();	
	}

	// Insert by policy is done without thumbnail data (path/bitmap/byte array)
	public boolean insert(final TodoItem todoItem) {
		String itemTitle = todoItem.getTitle();
		if(itemTitle==null){
			return false;
		}
		// to db - call helper functions to generate the ContentValues object that takes title and due date.
		// thumbnail will be by policy null since it isn't suppose to be given.
		ContentValues task = getContentValuesByTodoItem(todoItem,itemTitle);
		long retDb = _db.insert("todo", null,task);
		boolean dbGood = retDb!=-1;

		// to parse
		ParseObject parseObj = new ParseObject("todo");
		parseObj.put("title",itemTitle);
		Date itemDueDate = todoItem.getDueDate();
		if(itemDueDate!=null){
			parseObj.put("due", itemDueDate.getTime());
		}
		else{
			parseObj.put("due", JSONObject.NULL);
		}
		parseObj.put("parseFile", JSONObject.NULL); // new items or always w/o thumbnale
		parseObj.saveInBackground();
		return dbGood; 
	}

	// Helper function to get the interesting attributes for the DB which are "title", "due" and "thumbpath"
	// Stores null if one is absent besides title which must be valid
	private ContentValues getContentValuesByTodoItem(TodoItem todoItem,String title){
		ContentValues toReturn = new ContentValues(); 
		toReturn.put("title",title);
		Date itemDueDate = todoItem.getDueDate();
		if(itemDueDate != null ){
			toReturn.put("due", itemDueDate.getTime());
		}
		else{
			toReturn.put("due", (Long) null);
		}
		String thumbPath = todoItem.getThumbnailPath();
		if(thumbPath != null){ // null in Inserted objects
			toReturn.put("thumbpath", thumbPath);
		}
		else{
			toReturn.put("thumbpath", (String) null);
		}
		return toReturn;
	}

	
	// Updates date only, if a thumbnale is changed also - don't forget to call both 
	// and meanwhile expect some inconsistencies with the Parse / DB.
	public boolean updateDate(final TodoItem todoItem) { 
		final String itemTitle = todoItem.getTitle();
		if(itemTitle==null){
			return false;
		}
		// to db
		ContentValues task = getContentValuesByTodoItem(todoItem,itemTitle);
		int count = _db.update("todo",task, "title = \""+itemTitle+"\"",null);
		boolean dbGood = count!=0;

		// to parse
		ParseQuery query = new ParseQuery("todo");
		query.whereEqualTo("title", itemTitle);
		query.findInBackground(new FindCallback() {
			public void done(List<ParseObject> matches,ParseException e) {
				if (e != null){
					e.printStackTrace();
				}
				else if(!matches.isEmpty()){
					for(ParseObject parseObj : matches){
						// old due is obsolete
						parseObj.remove("due");
						Date itemDueDate = todoItem.getDueDate();
						if(itemDueDate != null ){
							parseObj.put("due", itemDueDate.getTime());
						}
						else{
							parseObj.put("due", JSONObject.NULL);
						}
						// keep ParseFile as is
						parseObj.saveInBackground();
					}
				}
			}
		});
		return dbGood; 
	}

	// Updates thumbnail only, if a date is changed also - don't forget to call both 
	// and meanwhile expect some inconsistencies with the Parse / DB
	// As mentioned above - this class needs special care not to call 
	// repeatedly in short periods of time since it's uploading big data to Parse cloud.
	public boolean updateThmbnale(final TodoItem todoItem) { 
		final String itemTitle = todoItem.getTitle();
		if(itemTitle==null){
			return false;
		}
		// to db
		ContentValues task = getContentValuesByTodoItem(todoItem,itemTitle);
		int count = _db.update("todo",task, "title = \""+itemTitle+"\"",null);
		boolean dbGood = count!=0;

		// to parse by separate asyncTask in order to save the file properly
		new UpdateParseAsyncTask(itemTitle,todoItem).execute();
		return dbGood; 
	}
	
	// AsyncTask for the update of thumbnail which calls the cloud on its own background thead
	// without invoking another one for file saving and than entire parse object saving.
	private static class UpdateParseAsyncTask  extends AsyncTask<Void,Void, Void>{
		private String _itemTitle;
		private TodoItem _todo;

		public UpdateParseAsyncTask(String title,TodoItem todo){
			_itemTitle = title;
			_todo = todo;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			try {
				ParseQuery query = new ParseQuery("todo");
				query.whereEqualTo("title", _itemTitle);
				List<ParseObject> list = query.find();
				if(!list.isEmpty()){
					for(final ParseObject parseObj : list){
						// obsolete file
						parseObj.remove("parseFile");
						ParseFile parse = new ParseFile(_todo.getBytesOfThumbnale());
						// Save new file
						parse.save();
						parseObj.put("parseFile", parse);
						// Save parse object
						parseObj.save();
					}
				}
			} catch (ParseException e) {}
			return null;
		}


	}

	// deleted item by title
	public boolean delete(TodoItem todoItem) {
		final String itemTitle = todoItem.getTitle();
		if(itemTitle==null){
			return false;
		}

		// to db
		int count = _db.delete("todo", "title = \""+itemTitle+"\"",null);
		boolean dbGood = count!=0;

		// to parse
		ParseQuery query = new ParseQuery("todo");
		query.whereEqualTo("title", itemTitle);
		query.findInBackground(new FindCallback() {
			public void done(List<ParseObject> matches,ParseException e) {
				if (e != null){
					e.printStackTrace();
				}
				else if(!matches.isEmpty()){
					for(ParseObject parseObj : matches){
						parseObj.deleteInBackground();
					}
				}
			}
		});
		return dbGood;
	}

	// Gives all Todo with only title due date and thumbnail path if exist
	public List<TodoItem> all() {
		ArrayList<TodoItem> ret = new ArrayList<TodoItem>();
		Cursor cursor = _db.query("todo", new String[] { "title", "due","thumbpath" },null, null, null, null, null);
		try{
			if (cursor.moveToFirst()) {
				do {
					String title = cursor.getString(0);
					Date due = null;
					try{
						Long dueLong = cursor.getLong(1);
						if(dueLong!=null && dueLong > 0){
							due= new Date(dueLong);
						}
					}catch(Exception e){}
					String thumbPath = cursor.getString(2);
					ret.add(new TodoItem(title,due,thumbPath));
				} while (cursor.moveToNext());
			}
		}finally{
			cursor.close();
		}
		return ret;
	}
	
	public void closeDB(){
		_db.close();
	}
}