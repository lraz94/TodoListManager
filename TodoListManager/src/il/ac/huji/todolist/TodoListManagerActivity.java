package il.ac.huji.todolist;

import java.util.ArrayList;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
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
	private ArrayList <String> _items ;
	private ListView _list;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_todo_list_manager);
		_editNewItem = (EditText) findViewById(R.id.edtNewItem);
		_list = (ListView)findViewById(R.id.lstTodoItems);
		_adapter = new CustomListAdapter(this,R.layout.row);
		_list.setAdapter(_adapter);
		_items = new ArrayList<String>();
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
			_items.add(added);
			_adapter.add(added);
			break;
		}
		case R.id.menuItemDelete :{
			int positionDeleted = _list.getSelectedItemPosition();
			if(positionDeleted>0){
				String rem = _items.remove(positionDeleted);
				_adapter.remove(rem);
			}
			break;
		}
		}
		return true;
	}


	private class CustomListAdapter extends ArrayAdapter<String> {


		public CustomListAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
		}

		@Override
		public View getView(int position, View v, ViewGroup parent)    {
			LayoutInflater inflater = LayoutInflater.from(getContext());
			View view = inflater.inflate(R.layout.row, null);
			TextView task = (TextView) view.findViewById(R.id.textTask);
			task.setText(_items.get(position));
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