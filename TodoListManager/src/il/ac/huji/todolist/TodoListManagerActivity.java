package il.ac.huji.todolist;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class TodoListManagerActivity extends Activity {

	private CustomListAdapter _adapter;
	private EditText _editNewItem;
	private ListView _list;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_todo_list_manager);
		_editNewItem = (EditText) findViewById(R.id.edtNewItem);
		_list = (ListView)findViewById(R.id.lstTodoItems);
		_adapter = new CustomListAdapter(this,new ArrayList<String>());
		_list.setAdapter(_adapter);
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
			String added = _editNewItem.getText().toString();
			_adapter.add(added);
			break;
		}
		case R.id.menuItemDelete :{
			int positionDeleted = _list.getSelectedItemPosition();
			if(positionDeleted!=-1){
				_adapter.remove(_adapter.getItem(positionDeleted));
			}
			break;
		}
		}
		return true;
	}


	private class CustomListAdapter extends ArrayAdapter<String> {


		public CustomListAdapter(TodoListManagerActivity activity, List<String> courses) {	
			super(activity, android.R.layout.simple_list_item_1, courses);
			
		}

		@Override
		public View getView(int position, View v, ViewGroup parent)    {
			String strItem = getItem(position);
			LayoutInflater inflater = LayoutInflater.from(getContext());
			View view = inflater.inflate(R.layout.row, null);
			TextView task = (TextView) view.findViewById(R.id.textTask);
			task.setText(strItem);
			if(position%2==0){
				task.setTextColor(Color.RED);
			}	
			else{
				task.setTextColor(Color.BLUE);
			}
			return view;
		}

	}
}