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

public class AddNewTodoItemActivity extends Activity {

	public static final int MAXIMUM_TO_PRESENT = 60;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_new_todo_item);
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
		Button buttonOk = (Button) findViewById(R.id.btnOK);
		buttonOk.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(text==null && editTextNewItem.getText().toString().length()==0 ){
					return;
				}
				Intent result = new Intent();
				Calendar cal = Calendar.getInstance();
				int year = datePicker.getYear();
				int month = datePicker.getMonth();
				int day = datePicker.getDayOfMonth();
				cal.clear();
				cal.set(Calendar.YEAR,year);
				cal.set(Calendar.MONTH, month);
				cal.set(Calendar.DAY_OF_MONTH,day);
				result.putExtra(TodoListManagerActivity.DUE_DATE_EXTRA_STR,cal.getTime());
				if(text!=null){
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
		Button buttonCancel = (Button) findViewById(R.id.btnCancel);
		buttonCancel.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});
	}


}
