package il.ac.huji.todolist;

import java.util.Calendar;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;


/*
 * Activity shows an EditText (TodoItem title) and a DatePicker (TodoItem due date),
 * which is called by outside activity in 2 occasions:
 * 1. User asked for a new Todo item.
 * 2. User asked to update a date of an existing Todo item.
 * 
 * In the first case - the title and due date are editable. In the second case -
 * only the change date possibility is allowed and the title GUI is disabled. 
 * This activity differs the two by having a String Extra called "text" representing the title
 * of the Todo item in the second case, which is absent in the first.
 * Calls back to the caller activity with a Long Extra "dueDate" representing the selected date
 * and "title" for the Todo item. Also, have a boolean Extra "update" which defined which of the 
 * two modes above was used.
 */
public class AddUpdateTodoItemActivity extends Activity {

	// constant maximum length of a String title the GUI can show,
	// which is truncated - in GUi only - to allow all buttons to be visible in the view.
	// See clipping bug inside code for the fix.
	public static final int MAXIMUM_TO_PRESENT = 60;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_new_todo_item);
		// Verify if its and update or add as described in class description above
		final Intent intent = getIntent();
		final String text = intent.getStringExtra("text");
		final EditText editTextNewItem = (EditText) findViewById(R.id.edtNewItem);
		if(text!=null){ // update
			if(text.length()<=MAXIMUM_TO_PRESENT){
				editTextNewItem.setText(text);
			}
			else{
				// #clipping bug note 1 : reduce the string in order not to get buttons out of frame
				editTextNewItem.setText(text.substring(0,MAXIMUM_TO_PRESENT)+"..."); 
			}
			editTextNewItem.setEnabled(false);
		}
		final DatePicker datePicker = (DatePicker) findViewById(R.id.datePicker);
		// User Click OK
		Button buttonOk = (Button) findViewById(R.id.btnOK);
		buttonOk.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(text==null && editTextNewItem.getText().toString().length()==0 ){
					return;
				}
				Intent result = new Intent();
				// Get fresh calendar with only the pattern dd/MM/yyyy sets.
				// This avoid tasks being 'obsolete' at the minute created since only this data matters.
				Calendar cal = Calendar.getInstance();
				int year = datePicker.getYear();
				int month = datePicker.getMonth();
				int day = datePicker.getDayOfMonth();
				cal.clear();
				cal.set(Calendar.YEAR,year);
				cal.set(Calendar.MONTH, month);
				cal.set(Calendar.DAY_OF_MONTH,day);
				// Extra for date
				result.putExtra(TodoListManagerActivity.DUE_DATE_EXTRA_STR,cal.getTime());
				if(text!=null){ // update
					// #clipping bug note 2: the title won't be taken from editTextNewItem but from text itself to preserve it
					result.putExtra(TodoListManagerActivity.TITLE_EXTRA_STR,text);
					result.putExtra(TodoListManagerActivity.IS_UPDATE_EXTRA_STR,true);
				}
				else{
					result.putExtra(TodoListManagerActivity.TITLE_EXTRA_STR,editTextNewItem.getText().toString());
					result.putExtra(TodoListManagerActivity.IS_UPDATE_EXTRA_STR,false);
				}
				setResult(RESULT_OK,result);
				finish();
			}
		});
		// Cancel option
		Button buttonCancel = (Button) findViewById(R.id.btnCancel);
		buttonCancel.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});
	}
}
