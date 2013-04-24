package il.ac.huji.todolist;
import java.util.Date;

import android.graphics.Bitmap;

/*
 * Todo item object has
 * 1. Title - must. can't be null or empty.
 * 2. Due date - optional.
 * 3. Thumbnail path - optional.
 * 4. Bitmap - optional , added in separate method. For the convince of the user.
 * 5. Array of bytes of thumbnail - optional , added in separate method.
 * The policy is the 2 TodoItems are equal iff their title is the same.
 * All methods are ordinary get/set.
 */
public class TodoItem{

	private String _task;
	private Date _date;
	private String _thumbnailPath;
	private Bitmap _bitmap;
	private byte[] _byteOfThumbnale;

	public TodoItem(String task,Date date,String thumbnailPath){
		_task = task;
		_date = date;
		_thumbnailPath = thumbnailPath;
	}

	public String getTitle() {
		return _task;
	}

	public Date getDueDate() {
		return _date;
	}

	// toString() for DEBUG isn't used on app but good practice to have in any class
	public String toString(){
		String dateFormated = null;
		if(_date!=null){
			dateFormated = Long.toString(_date.getTime());
		}
		return "Title: "+_task+" | Due date: "+dateFormated+" | Thumbnail path: "+_thumbnailPath;
	}

	public boolean equals(Object o){
		return ((TodoItem) o).getTitle().equals(this.getTitle());
	}


	public String getThumbnailPath() {
		return _thumbnailPath;
	}

	public void setPathForThumbnale(String thumbnailPath){
		_thumbnailPath = thumbnailPath;
	}

	public void setBitmap(Bitmap bitmap){
		_bitmap = bitmap;
	}

	public Bitmap getBitmap(){
		return _bitmap;
	}

	public void setBytesToThumbnale(byte[] bytes){
		_byteOfThumbnale = bytes;
	}

	public byte[] getBytesOfThumbnale() {
		return _byteOfThumbnale;
	}
}

