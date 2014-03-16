package com.perlib.wmbg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.perlib.wmbg.book.Book;
import com.perlib.wmbg.book.BookJsonAdapter;
import com.perlib.wmbg.book.Library;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainMenu extends Activity implements OnDownloadComplete {

	Button btnSearchBook;
	Button btnScanBook;
	Button btnManualAddBook;
	
	IntentIntegrator scanIntegrator = new IntentIntegrator(this);
	
	List<Book> items = new ArrayList<Book>();
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    setContentView(R.layout.activity_menu);
	    
	    btnSearchBook = (Button) findViewById(R.id.btnSeachBooks);
	    btnScanBook = (Button) findViewById(R.id.btnScanBook);
	    btnManualAddBook = (Button) findViewById(R.id.btnManualAddBook);
	    
	    loadData();
	    
	    
	    btnSearchBook.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				Intent searchBook = new Intent(getApplicationContext(), MainActivity.class);
				searchBook.putParcelableArrayListExtra("items", (ArrayList<? extends Parcelable>) items);
				startActivity(searchBook);
			}
		});
	    
	    btnScanBook.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
			    scanIntegrator.initiateScan();
				
			}
		});
	    
	    btnManualAddBook.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				Intent addBook = new Intent(getApplicationContext(), AddBook.class);
				addBook.putParcelableArrayListExtra("items", (ArrayList<? extends Parcelable>) items);
				addBook.putExtra("mode", AddBook.MODE_MANUAL);
				startActivity(addBook);
			}
		});
	}
	
	private void loadData()
	{
		String fs = System.getProperty("file.separator");
		File sd = Environment.getExternalStorageDirectory();
		File listfile = new File(sd+fs+"booklist.txt");
		
		if(listfile.exists())
		{
			try {
				BufferedReader br = new BufferedReader(new FileReader(listfile));
				
				String line;
				List<Book> list = new ArrayList<Book>();
				Gson gson = new Gson();
				Book[] bookArray = new Book[]{};
				while((line = br.readLine()) != null)
				{
					bookArray = gson.fromJson(line, Book[].class);
				}
				list = new ArrayList<Book>(Arrays.asList(bookArray));
				items = list;
				br.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if (scanResult != null) {
			String contents = scanResult.getContents();
			if(contents != null)
			{
				Library.handleISBN(contents, getApplicationContext(), this);
			}
		}
	}
	
	@Override
	public void OnTaskFinished(String result) {
		
		Gson gson = new Gson();
		BookJsonAdapter adapter = gson.fromJson(result , BookJsonAdapter.class);
		if(adapter == null)
		{
			Toast.makeText(getApplicationContext(), getString(R.string.InvalidISBN) , Toast.LENGTH_SHORT).show();
			return;
		}
		Book resultBook = adapter.convertToBook();
		if(resultBook == null)
		{
			Toast.makeText(getApplicationContext(), getString(R.string.InvalidISBN) , Toast.LENGTH_SHORT).show();
			return;
		}
		
		Intent scanBook = new Intent(getApplicationContext(), ScanBook.class);
		scanBook.putParcelableArrayListExtra("items", (ArrayList<? extends Parcelable>) items);
		scanBook.putExtra("result", resultBook);
		startActivity(scanBook);
	}

}
