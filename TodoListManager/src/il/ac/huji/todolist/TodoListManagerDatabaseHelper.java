package il.ac.huji.todolist;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

// DB helper for todo items. DB called "todo_db", table called "todo" with:
// "_id" autoincremented , "title" and "thumbpath" as text and due as integer (long) 
// Does nothing on upgrade
public class TodoListManagerDatabaseHelper extends SQLiteOpenHelper {

	public TodoListManagerDatabaseHelper(Context context) {
		super(context, "todo_db",null,1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("create table todo ( _id integer primary key autoincrement,"	
				+  " title text, due integer , thumbpath text );");
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		// nothing to do
	}
}
