package il.ac.huji.todolist;
import java.util.Date;

import android.graphics.Bitmap;

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

	// for DEBUG
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

