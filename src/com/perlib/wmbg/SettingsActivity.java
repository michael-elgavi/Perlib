package com.perlib.wmbg;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.perlib.wmbg.R;
import com.perlib.wmbg.book.Library;
import com.perlib.wmbg.book.Settings;

public class SettingsActivity extends Activity {

	EditText customMessage;
	Button btnSave;
	Settings settings;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    setContentView(R.layout.activity_settings);
	    customMessage = (EditText) findViewById(R.id.customMessage);
	    btnSave = (Button) findViewById(R.id.btnSave);
	    
	    settings = Library.loadSettings(getApplicationContext());
	    customMessage.setText(settings.getEmailMessage());
	    
	    btnSave.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				settings.setEmailMessage(customMessage.getText().toString());
				Library.saveSettings(settings);
				Intent main = new Intent(getApplicationContext(), MainActivity.class);
				main.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(main);
			}
		});
	}
	
	
	
}