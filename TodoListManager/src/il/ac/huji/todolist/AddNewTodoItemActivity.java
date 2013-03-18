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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_new_todo_item);
		final EditText editTextNewItem = (EditText) findViewById(R.id.edtNewItem);
		final DatePicker datePicker = (DatePicker) findViewById(R.id.datePicker);
		Button buttonOk = (Button) findViewById(R.id.btnOK);
		buttonOk.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
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
				result.putExtra(TodoListManagerActivity.TITLE_EXTRA_STR,editTextNewItem.getText().toString());
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
