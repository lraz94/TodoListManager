package il.ac.huji.todolist;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
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
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;

public class TodoListManagerActivity extends Activity {

	public static final int CODE_FOR_ADD_NEW = 100;
	private CustomListAdapter _adapter;
	private ListView _list;
	private TodoDAL _todoDal;
	
	
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy",Locale.US);
	private static final String STRING_FOR_NO_DATE = "No due date";
	private static final String CALL = "Call ";

	public static final String DUE_DATE_EXTRA_STR = "dueDate";
	public static final String TITLE_EXTRA_STR = "title";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_todo_list_manager);
		
		// init todoDal
		_todoDal = new TodoDAL(this);
				
		_list = (ListView)findViewById(R.id.lstTodoItems);
		List<ITodoItem> stored = _todoDal.all();
		System.out.println("Stored tasks are: "+stored);
		_adapter = new CustomListAdapter(this,stored);
		_list.setAdapter(_adapter);
		registerForContextMenu(_list);		
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
				TodoDAL.TaskDatePair newTask = new TodoDAL.TaskDatePair(title,date);
				_adapter.add(newTask);
				boolean fromInserst = _todoDal.insert(newTask);
				System.out.println("Got from insert: "+fromInserst);
			}
		}
	}


	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo info) {
		getMenuInflater().inflate(R.menu.ctxmenuextended, menu);
		int pos = ((AdapterContextMenuInfo) info).position;
		ITodoItem taskPair = _adapter.getItem(pos);
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
		final ITodoItem taskPair = _adapter.getItem(pos);
		switch (item.getItemId()){
		case R.id.menuItemDelete:{
			_adapter.remove(taskPair);
			boolean fromDelete = _todoDal.delete(taskPair);
			System.out.println("Got from delete: "+fromDelete);
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
		case R.id.updatedebug:{
			final Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.dialogforupdatedebug);
			final DatePicker datePicker = (DatePicker) dialog.findViewById(R.id.datePickerUpdateDebug);
			Button ok = (Button) dialog.findViewById(R.id.button1);
			ok.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					Calendar cal = Calendar.getInstance();
					int year = datePicker.getYear();
					int month = datePicker.getMonth();
					int day = datePicker.getDayOfMonth();
					cal.clear();
					cal.set(Calendar.YEAR,year);
					cal.set(Calendar.MONTH, month);
					cal.set(Calendar.DAY_OF_MONTH,day);
					ITodoItem update = new TodoDAL.TaskDatePair(taskPair.getTitle(),cal.getTime());
					_adapter.remove(taskPair);
					_adapter.add(update);
					boolean fromUpdate = _todoDal.update(update);
					System.out.println("Got from update: "+fromUpdate);
					dialog.dismiss();
				}
			});
			Button sendNull = (Button) dialog.findViewById(R.id.button2);
			sendNull.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					ITodoItem update = new TodoDAL.TaskDatePair(taskPair.getTitle(),null);
					_adapter.remove(taskPair);
					_adapter.add(update);
					boolean fromUpdate = _todoDal.update(update);
					System.out.println("Got from update: "+fromUpdate);
					dialog.dismiss();
				}
			});
			dialog.show();
			break;
		}
		}
		return true;
	}


	private class CustomListAdapter extends ArrayAdapter<ITodoItem> {
		public CustomListAdapter(TodoListManagerActivity activity, List<ITodoItem> tasks) {	
			super(activity, android.R.layout.simple_list_item_1, tasks);

		}

		@Override
		public View getView(int position, View v, ViewGroup parent)    {
			ITodoItem item = getItem(position);
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
			return view;
		}
	}
}