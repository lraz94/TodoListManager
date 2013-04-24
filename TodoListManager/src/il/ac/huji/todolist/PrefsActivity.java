package il.ac.huji.todolist;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/*
 * Preferences activity which hold inside only one option:
 * Select search String or Hashtag for twiter twits search.
 * For hashtag - need to add # at the beginning of the string.
 * If this search isn't the same search last made - a new 
 * one will begin right after exiting the preferences via the
 * "go back" button on the device. Next time user opens the preferences
 * this search is remembered although the first search in the app is always for #todoapp,
 * so opening preferences and doing nothing (go back) will trigger a twit search
 * with this last saved search String.
 */
public class PrefsActivity extends PreferenceActivity{

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference);
	}
}