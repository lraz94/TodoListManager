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


public class TodoDAL {

	private Context _context;
	private SQLiteDatabase _db;

	public TodoDAL(Context context) {
		_context = context;
		TodoListManagerDatabaseHelper helper = new TodoListManagerDatabaseHelper(_context);	
		_db = helper.getWritableDatabase();
		Parse.initialize(context, context.getResources().getString(R.string.parseApplication),
				context.getResources().getString(R.string.clientKey)); 
		ParseUser.enableAutomaticUser();	
	}

	public SQLiteDatabase getDBforExit(){
		return _db;
	}

	public boolean insert(final TodoItem todoItem) {
		String itemTitle = todoItem.getTitle();
		if(itemTitle==null){
			return false;
		}
		// to db
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
		System.out.println("Parser: inserted parseObj: title="+parseObj.getString("title")+" | due="+parseObj.getLong("due"));
		return dbGood; 
	}

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
						System.out.println("Parser: updated date for parseObj to: title="+parseObj.getString("title")+" | due="+
								parseObj.getLong("due")+" | parseFile="+parseObj.getParseFile("parseFile"));
					}
				}
				else{
					System.out.println("Parser: requested to update date for non existing item title: "+itemTitle);
				}
			}
		});
		return dbGood; 
	}

	public boolean updateThmbnale(final TodoItem todoItem) { 
		final String itemTitle = todoItem.getTitle();
		if(itemTitle==null){
			return false;
		}
		// to db
		ContentValues task = getContentValuesByTodoItem(todoItem,itemTitle);
		int count = _db.update("todo",task, "title = \""+itemTitle+"\"",null);
		boolean dbGood = count!=0;

		// to parse
		new UpdateParseAsyncTask(itemTitle,todoItem).execute();
		return dbGood; 
	}

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
						parseObj.remove("parseFile");
						ParseFile parse = new ParseFile(_todo.getBytesOfThumbnale());
						parse.save();
						parseObj.put("parseFile", parse);
						parseObj.save();
						// keep date as is
						System.out.println("Parser: updated parseFile parseObj to: title="+parseObj.getString("title")+" | due="+
								parseObj.getLong("due")+" | parseFile="+parseObj.getParseFile("parseFile"));
					}
				}
				else{
					System.out.println("Parser: requested to update file of non existing item title: "+_itemTitle);
				}
			} catch (ParseException e) {}
			return null;
		}


	}

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
						System.out.println("Parser: deleted parseObj of: title="+parseObj.getString("title")+" | due="+parseObj.getLong("due")
								+" | parseFile="+parseObj.getParseFile("parseFile"));
					}
				}
				else{
					System.out.println("Parser: requested to delete non existing item title: "+itemTitle);
				}
			}
		});
		return dbGood;
	}

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
}