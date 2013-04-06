package il.ac.huji.todolist;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.json.JSONObject;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.Parse;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


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

	public boolean insert(ITodoItem todoItem) {
		final String itemTitle = todoItem.getTitle();
		if(itemTitle==null){
			return false;
		}
		final Date itemDueDate = todoItem.getDueDate();

		// to db
		ContentValues task = new ContentValues(); 
		task.put("title",itemTitle);
		if(itemDueDate != null ){
			task.put("due", itemDueDate.getTime());
		}
		else{
			task.put("due", (Long) null);
		}
		long retDb = _db.insert("todo", null,task);
		boolean dbGood = retDb!=-1;

		// to parse
		ParseObject privObj = new ParseObject("todo");
		privObj.put("title",itemTitle);
		if(itemDueDate!=null){
			privObj.put("due", itemDueDate.getTime());
		}
		else{
			privObj.put("due", JSONObject.NULL);
		}
		privObj.saveInBackground();
		System.out.println("Parser: inserted parseObj: title="+privObj.getString("title")+" | due="+privObj.getLong("due"));
		return dbGood; 
	}

	public boolean update(ITodoItem todoItem) { 
		final String itemTitle = todoItem.getTitle();
		if(itemTitle==null){
			return false;
		}
		final Date itemDueDate = todoItem.getDueDate();

		// to db
		ContentValues task = new ContentValues(); 
		task.put("title",itemTitle);
		if(itemDueDate!=null){
			task.put("due", itemDueDate.getTime());
		}
		else{
			task.put("due",(Long) null);
		}
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
						if(itemDueDate!=null){
							parseObj.put("due", itemDueDate.getTime());
						}
						else{
							parseObj.put("due", JSONObject.NULL);

						}
						parseObj.saveInBackground();
						System.out.println("Parser: updated parseObj to: title="+parseObj.getString("title")+" | due="+parseObj.getLong("due"));
					}
				}
				else{
					System.out.println("Parser: requested to update non existing item title: "+itemTitle);
				}
			}
		});
		return dbGood; 
	}

	public boolean delete(ITodoItem todoItem) {
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
						System.out.println("Parser: deleted parseObj of: title="+parseObj.getString("title")+" | due="+parseObj.getLong("due"));
					}
				}
				else{
					System.out.println("Parser: requested to delete non existing item title: "+itemTitle);
				}
			}
		});
		return dbGood;
	}

	public List<ITodoItem> all() {
		ArrayList<ITodoItem> ret = new ArrayList<ITodoItem>();
		Cursor cursor = _db.query("todo", new String[] { "title", "due" },null, null, null, null, null);
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
					ret.add(new TaskDatePair(title,due));
				} while (cursor.moveToNext());
			}
		}finally{
			cursor.close();
		}

		// DEBUG parse
		ParseQuery query = new ParseQuery("todo");
		query.findInBackground(new FindCallback() {
			public void done(List<ParseObject> matches,ParseException e) {
				if (e != null){
					e.printStackTrace();
				}
				else{
					if(matches.isEmpty()){
						System.out.println("Parser: no parsed items stored");
					}
					else{
						for(ParseObject parseObj : matches){
							System.out.println("Parser: got parsed: title="+parseObj.getString("title")+" | due="+parseObj.getLong("due"));
						}
					}
				}
			}
		});

		return ret;
	}

	public static class TaskDatePair implements ITodoItem{

		private String _task;
		private Date _date;

		public TaskDatePair(String task,Date date){
			_task = task;
			_date = date;
		}

		@Override
		public String getTitle() {
			return _task;
		}

		@Override
		public Date getDueDate() {
			return _date;
		}

		// for DEBUG
		public String toString(){
			String dateFormated = null;
			if(_date!=null){
				dateFormated = Long.toString(_date.getTime());
			}
			return "Title: "+_task+" | Due date: "+dateFormated;
		}
	}
}