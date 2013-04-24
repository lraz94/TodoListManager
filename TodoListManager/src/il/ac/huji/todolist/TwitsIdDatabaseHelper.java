package il.ac.huji.todolist;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

// DB helper for twits. interest only in the id of a twit. DB called "twits_id", table called "twits" with
// one column "id" which is a number represents a unique twit. 
// Does nothing on upgrade
public class TwitsIdDatabaseHelper extends SQLiteOpenHelper {

	public TwitsIdDatabaseHelper(Context context) {
		super(context, "twits_id",null,1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("create table twits ( id integer );");
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		// nothing to do
	}
}
