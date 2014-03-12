package com.perlib.wmbg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.nhaarman.listviewanimations.itemmanipulation.OnDismissCallback;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.SwipeDismissAdapter;
import com.perlib.wmbg.R;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.gson.Gson;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.perlib.wmbg.book.Book;
import com.perlib.wmbg.book.BookJsonAdapter;
import com.perlib.wmbg.book.Library;
import com.perlib.wmbg.book.Settings;
import com.perlib.wmbg.fortsevendeg.swipelistview.SwipeListView;

public class MainActivity extends Activity implements OnDownloadComplete{

	//Book[] items = new Book[]{};
	List<Book> items = new ArrayList<Book>(); 
	Settings settings;
	DownloadInfo downloader;
	private OnDownloadComplete downloadListener;
	SwipeListView bookList;
	SimpleAdapter adapter;
	//SwipeDismissAdapter swipeAdapter;
	List<Map<String,String>> displayList = new ArrayList<Map<String,String>>();;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		bookList = (SwipeListView)findViewById(R.id.bookList);
		downloadListener = this;
		
		settings = Library.loadSettings(getApplicationContext());
		
		if(getIntent().getExtras() != null)
		{
			Bundle b = getIntent().getExtras();
			items = b.getParcelableArrayList("items");
		}
		adapter = new SimpleAdapter(getApplicationContext(), displayList, R.layout.simple_list_item_3, new String[] {"name", "Author", "lendedto","date"}, new int[] {R.id.text1,R.id.text2,R.id.text3,R.id.text4});
		/*swipeAdapter = new SwipeDismissAdapter(adapter ,new OnDismissCallback() {
			
			@Override
			public void onDismiss(AbsListView listView, int[] reverseSortedPositions) {
			    for (int position : reverseSortedPositions) {
			    	if(settings.getSwipeMode() == Settings.MODE_DELETE_ITEM)
			    	{
			    		deleteItem(position);
			    	}
			    	else if(settings.getSwipeMode() == Settings.MODE_RETURN_ITEM)
			    	{
			    		returnItem(position);
			    	}
			    }
				
			}
		});*/
		refreshList();
		bookList.setOffsetLeft(50);
		bookList.setOffsetRight(50);
		
		bookList.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, final View view,
					final int position, long id) {
				
				AlertDialog.Builder options_builder = new AlertDialog.Builder(MainActivity.this);
				options_builder.setTitle(getString(R.string.options)).setItems(getResources().getStringArray(R.array.options_array), new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						switch(which){
						case 0:
							deleteItem(position);
							break;
						case 1:
							String uriText = null;
							uriText =
							"mailto:"+items.get(position).getEmail() + 
							"?subject=" + Uri.encode(getString(R.string.emailSubject), "UTF-8") + 
							"&body=" + Uri.encode(settings.getEmailMessage().replaceAll("@book@", items.get(position).getName()));
							Uri uri = Uri.parse(uriText);
					        Intent myActivity2 = new Intent(Intent.ACTION_SENDTO);                              
					        myActivity2.setData(uri);
					        startActivity(Intent.createChooser(myActivity2, getString(R.string.sendEmail)));
							break;
						case 2:
							returnItem(position);
						default:
							break;
						}
					}
				});
				
				AlertDialog dialog = options_builder.create();
				dialog.show();
				
				

				return true;
			}
		});
		
		bookList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				goto_editbook(position);
				
			}

		});
		
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
			case R.id.addbook:
				goto_newbook();
				return true;
			case R.id.settings:
				goto_settings();
				return true;
			case R.id.delete:
				deleteByScan();
				return true;
			default:
				return false;
		
		}
	}
	

	private void deleteByScan() {
		IntentIntegrator scanIntegrator = new IntentIntegrator(this);
		scanIntegrator.initiateScan();
		
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		
		IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if (scanningResult != null) {
			String contents = scanningResult.getContents();
			if(contents != null)
			{
				if(contents.length() == 0){Toast.makeText(getApplicationContext(), getString(R.string.InvalidISBN) , Toast.LENGTH_SHORT).show();return;}
				if(!Library.isConnectedToInternet(getApplicationContext())){Toast.makeText(getApplicationContext(), getString(R.string.noConnection) , Toast.LENGTH_SHORT).show();return;}
				downloader = new DownloadInfo(downloadListener);
				downloader.execute(contents);
			}
		}
		else{
		    Toast toast = Toast.makeText(getApplicationContext(), 
		        "No scan data received!", Toast.LENGTH_SHORT);
		    toast.show();
		}
	}

	private void goto_settings() {
		Intent settings = new Intent(getApplicationContext(), SettingsActivity.class);
		startActivity(settings);
		
	}

	private void goto_newbook() {
		
		Intent addbook = new Intent(getApplicationContext(), AddBook.class);
		Bundle b = new Bundle();
		b.putParcelableArrayList("items", (ArrayList<? extends Parcelable>) items);
		addbook.putExtras(b);
		startActivity(addbook);
	}
	
	private void goto_editbook(int position) {
		
		Intent addbook = new Intent(getApplicationContext(), EditBook.class);
		Bundle b = new Bundle();
		b.putParcelableArrayList("items", (ArrayList<? extends Parcelable>) items);
		b.putInt("position", position);
		addbook.putExtras(b);
		startActivity(addbook);
	}
	
	private void loadData()
	{
		//String eol = System.getProperty("line.separator");
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	
	private void refreshList()
	{
		loadData();
		displayList.clear();
		for(Iterator<Book> i = items.iterator(); i.hasNext(); ) {
		    Book item = i.next();
		    Map<String,String> stringMap = new HashMap<String,String>();
		    stringMap.put("name", item.getName());
		    String lendedtotext = "";
		    String dateText = "";
		    if(item.getLendedTo().length() == 0)
		    {
		    	lendedtotext = getString(R.string.none);
		    }
		    else
		    {
		    	lendedtotext = item.getLendedTo();
		    }
		    if(!(item.getDateLended() == -1 && item.getDueDate() == -1))
		    {
			    GregorianCalendar gcDateLended = new GregorianCalendar();
			    gcDateLended.setTimeInMillis(item.getDateLended()*1000);
			    GregorianCalendar gcDueDate = new GregorianCalendar();
			    gcDueDate.setTimeInMillis(item.getDueDate()*1000);
			    SimpleDateFormat format = new SimpleDateFormat("d/M/y", Locale.US);
			    dateText = format.format(gcDateLended.getTime()) + " - " + format.format(gcDueDate.getTime());
		    }
		    stringMap.put("lendedto", getString(R.string.lendedToDisplay) + lendedtotext);
		    stringMap.put("Author", getString(R.string.by) + item.getAuthor());
		    stringMap.put("date", dateText);
		    displayList.add(stringMap);
		}
		//itemsArray = displayList.toArray(itemsArray);
		//SimpleAdapter adapter = new SimpleAdapter(getApplicationContext(), displayList, R.layout.simple_list_item_3, new String[] {"name", "Author", "lendedto","date"}, new int[] {R.id.text1,R.id.text2,R.id.text3,R.id.text4});
		//ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, itemsArray);
		//if(settings.getSwipeMode() == Settings.MODE_NOTHING)
		//{
			bookList.setAdapter(adapter);
		//}
		//else
		//{
		//	bookList.setAdapter(swipeAdapter);
		//	swipeAdapter.setAbsListView(bookList);
		//}
		adapter.notifyDataSetChanged();
	}

	private void deleteItem(final int position)
	{
		if(settings.isConfirmDelete())
		{
			AlertDialog.Builder delete_builder = new AlertDialog.Builder(MainActivity.this);
			delete_builder.setPositiveButton(getString(R.string.deleteYes) , new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
					items.remove(position);
					displayList.remove(position);
					adapter.notifyDataSetChanged();
					Library.saveInfo(items);
				}
			});
			delete_builder.setNegativeButton(getString(R.string.deleteCancel), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
					
				}
			});
			delete_builder.setMessage(getString(R.string.deleteConfirm) + ' ' + '"' + items.get(position).getName() + '"' + "?").setTitle(getString(R.string.deleteConfirmTitle));
			AlertDialog dialog = delete_builder.create();
			dialog.show();
		}
		else
		{
			items.remove(position);
			displayList.remove(position);
			adapter.notifyDataSetChanged();
			Library.saveInfo(items);
		}
	}
	
	private void returnItem(final int position)
	{
		Book item = items.get(position);
		item.setLendedTo("");
		item.setEmail("");
		item.setDateLended(-1);
		item.setDueDate(-1);
		items.set(position, item);
		Library.saveInfo(items);
		refreshList();
	}
	
	private void returnOrDeleteItem(final int position)
	{
		AlertDialog.Builder delete_or_return_builder = new AlertDialog.Builder(MainActivity.this);
		delete_or_return_builder.setPositiveButton(getString(R.string.chooseReturn) , new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				returnItem(position);
			}
		});
		delete_or_return_builder.setNegativeButton(getString(R.string.chooseDelete), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				deleteItem(position);
			}
		});
		delete_or_return_builder.setMessage(getString(R.string.returnOrDelete));
		AlertDialog dialog = delete_or_return_builder.create();
		dialog.show();
	}

	@Override
	public void OnTaskFinished() {
		
		Gson gson = new Gson();
		BookJsonAdapter adapter = gson.fromJson(downloader.getJsonResult(), BookJsonAdapter.class);
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
		final List<Integer> matcheIds = new ArrayList<Integer>();
		List<Book> matches = new ArrayList<Book>();
		List<String> matchDisplay = new ArrayList<String>();
		int it = 0;
		for(Iterator<Book> i = items.iterator(); i.hasNext(); )
		{
			Book item = i.next();
			if(item.getName().equals(resultBook.getName()))
			{
				matcheIds.add(it);
				matches.add(item);
				matchDisplay.add("Lended To: " + item.getLendedTo());
			}
			it++;
		}
		if(matches.size() == 1)
		{
			returnOrDeleteItem(matcheIds.get(0));
		}
		else if(matches.size() == 0)
		{
			return;
		}
		else
		{
			String[] displayArray = new String[]{};
			displayArray = matchDisplay.toArray(displayArray);
			AlertDialog.Builder options_builder = new AlertDialog.Builder(MainActivity.this);
			options_builder.setTitle(getString(R.string.duplicateBooks)).setItems(displayArray, new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
					returnOrDeleteItem(matcheIds.get(which));
				}
			});
			
			AlertDialog dialog = options_builder.create();
			dialog.show();
		}
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		refreshList();
	}
	
	@Override
	public void onStart()
	{
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}
	
	@Override
	public void onStop()
	{
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}
	
}
