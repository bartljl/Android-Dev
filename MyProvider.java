import java.io.IOException;
import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;

import java.io.StreamCorruptedException;

import java.net.ServerSocket;
import java.net.Socket;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.concurrent.Semaphore;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.telephony.TelephonyManager;



public class MyProvider extends ContentProvider 
{

  private DatabaseHelper mOpenHelper;
	private static final String DATABASE_NAME = "dht.db";
	private static final int DATABASE_VERSION = 2;
	private static final String AUTHORITY = "edu.buffalo.cse.cse486_586.simpledht.provider";
	private static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + "DHT/#");
	private static final String TABLE_NAME = "DHT";
	private static final Uri providerUri = Uri.parse("content://edu.buffalo.cse.cse486_586.simpledht.provider");
	
	private static final String KEY = "provider_key";
	private static final String VALUE = "provider_value";
	
	public static final int LOCAL_SERVERPORT = 10000;
	private static final String serverIpAddress = "10.0.2.2"; 
	
	private final Semaphore semaphore = new Semaphore(0);

	private static ServerSocket serversocket = null;
	private static String succ = null;
	private static String prev = null;
	
	//ClientThread clientthread;
	
	//int Port[] = {11108,11112,11116,11120,11124};
	String Device[] = {"5554","5556","5558","5560","5562"};
	
	//Cursor remote_cursor;
	MatrixCursor remote_cursor;
	
	String currentport = null;
	String node_id;
	String key_id;
	
	//LinkedList<String> nodelist = new LinkedList<String>();
	
	private String portStr;
	
	private static final UriMatcher sUriMatcher;
	static
    {
	  sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH); 
	  
      sUriMatcher.addURI(AUTHORITY, null, 1);
      ///sUriMatcher.addURI(AUTHORITY, "DHT/#", 2);
    }
	
	
	
    //Hash function
    public String genHash(String input) throws NoSuchAlgorithmException 
	{
		MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
		byte[] sha1Hash = sha1.digest(input.getBytes());
		Formatter formatter = new Formatter();
		for (byte b : sha1Hash) 
		{
			formatter.format("%02x", b);	
		}
		
		return formatter.toString();
	}
    
    
    
	//Database
	static class DatabaseHelper extends SQLiteOpenHelper {

	       DatabaseHelper(Context context) {

	           // calls the super constructor, requesting the default cursor factory.
	           super(context, DATABASE_NAME, null, DATABASE_VERSION);
	       }

		@Override
		public void onCreate(SQLiteDatabase db) {
			// TODO Auto-generated method stub
			db.execSQL("CREATE TABLE " + "DHT" + " (" + KEY + " CHAR(10)," + VALUE + " CHAR(10)" + ");" );
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			
		}
	}
	
	//Provider Create
	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		mOpenHelper = new DatabaseHelper(getContext());
		
		TelephonyManager tel = (TelephonyManager)this.getContext().getSystemService(Context.TELEPHONY_SERVICE);
		portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		
		//Node Identifier
		try {
			node_id = genHash(portStr);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		//start Server Thread
        Thread myserverThread = new Thread(new ServerThread());
        myserverThread.start();
        
		
		//New node join
        
		if(!portStr.equals("5554"))
		{
			Data newdata = new Data();
			newdata.setMsg("join");
			newdata.setNode(portStr);
			
			Thread myclientThread = new Thread(new Client(serverIpAddress,11108,newdata));
			myclientThread.start();
		}
		
		
		return true;
	}
	
	

	///Query
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
	    qb.setTables(TABLE_NAME);
	  
	    switch (sUriMatcher.match(uri)) 
	    {
	    case 1:
	    case 2:
	    	break;
	    	
	    default:
	    	throw new IllegalArgumentException("Unknown URI " + uri);
	    }
	    
	    //Opens the database object in "read" mode, since no writes need to be done.
	    SQLiteDatabase db = mOpenHelper.getReadableDatabase();
	    
	    Cursor c = qb.query(db,null,selection,null,null,null,null);
	    
	    //No result
	    if(c.getCount() == 0 && succ != null)
	    {
	    	Data querydata = new Data();
	    	querydata.setMsg("query");
	    	querydata.setKey(selection);
	    	querydata.setSrc_port(Integer.parseInt(portStr)*2);
	    	
	    	Thread myclientThread = new Thread(new Client(serverIpAddress,Integer.parseInt(succ)*2,querydata));
			myclientThread.start();
	    }
	    else if(c.getCount() == 0 && succ == null)
	    {
	    	Data querydata = new Data();
	    	querydata.setMsg("query");
	    	querydata.setKey(selection);
	    	querydata.setSrc_port(Integer.parseInt(portStr)*2);
	    	
	    	Thread myclientThread = new Thread(new Client(serverIpAddress,11124,querydata));
			myclientThread.start();
	    }
	    else
	    {
	    	c.setNotificationUri(getContext().getContentResolver(), uri);
	    	return c;
	    }
	    
	    
	    try {
				semaphore.acquire(1);
				c = remote_cursor;
				
			} 
	    catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    
	    return c;
	}
	
	/////Insert
	@Override
	public Uri insert(Uri uri, ContentValues initialvalues) {
		// TODO Auto-generated method stub
		
		// Validates the incoming URI. Only the full provider URI is allowed for inserts.
        if(sUriMatcher.match(uri) != 1 )//&& sUriMatcher.match(uri) != 2) 
        {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        
        //If the incoming values map is not null, uses it for the new values.
        if (initialvalues != null) {
            values = new ContentValues(initialvalues);

        } else {
            // Otherwise, create a new value map
            values = new ContentValues();
        }
        
        //Opens the database object in "write" mode
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        //String column_name = KEY + " " + VALUE
        String key = values.getAsString(KEY);
        String value = values.getAsString(VALUE);
        long rowId = 0;
        
        try {
        	    
			   if((prev != null && genHash(key).compareTo(node_id) < 0 && genHash(key).compareTo(genHash(prev))>0 ) ||
					 (prev == null && genHash(key).compareTo(node_id) < 0))
			   {
				   rowId = db.insert(TABLE_NAME, null, values);
				   if(rowId > 0)
			        {
			           //Creates a URI with the note ID pattern and the new row ID appended to it.
			           Uri noteUri = ContentUris.withAppendedId(CONTENT_URI, rowId);

			           // Notifies observers registered against this provider that the data changed.
			           getContext().getContentResolver().notifyChange(noteUri, null);
			           return noteUri;
			        }

			        // If the insert didn't succeed, then the rowID is <= 0. Throws an exception.
			        throw new SQLException("Failed to insert row into " + uri);
			   }
			   else if(succ == null && genHash(key).compareTo(node_id) > 0)
			   {
				   Data data = new Data();
				   data.setMsg("Finalinsert");
				   data.setKey(key);
				   data.setValue(value);
				   
				   Thread myclientThread = new Thread(new Client(serverIpAddress,11124,data));
				   myclientThread.start();
			   }
			   else if(genHash(key).compareTo(node_id) > 0 && succ != null)
			  { 
				   Data data = new Data();
				   data.setMsg("insert");
				   data.setKey(key);
				   data.setValue(value);
					
				   Thread myclientThread = new Thread(new Client(serverIpAddress,Integer.parseInt(succ)*2,data));
				   myclientThread.start();	
			  }
			   else if(genHash(key).compareTo(node_id) < 0 && prev != null)
			   {
				   Data data = new Data();
				   data.setMsg("insert");
				   data.setKey(key);
				   data.setValue(value);
					
				   Thread myclientThread = new Thread(new Client(serverIpAddress,Integer.parseInt(prev)*2,data));
				   myclientThread.start();	
			   }
			
			
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        
      
		return null;
	}
	
/////Not Used	
////////////////////////////////////////////////////////////////////////////////	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}
/////////////////////////////////////////////////////////////////////////////////////
//Not Used

	////Server
	class ServerThread implements Runnable
	{
		
		//private static String key_id;
		public void run() {
			// TODO Auto-generated method stub
			
			Socket s = null;
			try {
				
				serversocket = new ServerSocket(LOCAL_SERVERPORT);
				
			    }catch(IOException e){
			    	
			    	e.printStackTrace();
			    	
			    }
			
			while(true)
			{
				String message = null;
				String remote_node = null;
				
			    Data recv_data = new Data();
				
				try {
				s = serversocket.accept();
			    } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			    }
				
				try {
					ObjectInputStream input = new ObjectInputStream(s.getInputStream());
					recv_data = (Data)input.readObject();
					
					message = recv_data.getMsg();
					remote_node = recv_data.getNode();
					
				} catch (StreamCorruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				///Node join
				if(message.equals("join"))
				{
				   	try {
						if(node_id.compareTo(genHash(remote_node)) > 0 && prev == null)
						{
							prev = remote_node;
							Data data = new Data();
							data.setMsg("ReplyJoin");
							data.setSucc(portStr);
							
							Thread myclientThread = new Thread(new Client(serverIpAddress,Integer.parseInt(remote_node)*2,data));
							myclientThread.start();
						}
						
						else if(node_id.compareTo(genHash(remote_node)) < 0 && succ == null)
						{
							succ = remote_node;
							Data data = new Data();
							data.setMsg("ReplyJoin");
							data.setPrev(portStr);
							
							Thread myclientThread = new Thread(new Client(serverIpAddress,Integer.parseInt(remote_node)*2,data));
							myclientThread.start();
						}
						
						else if(prev != null && node_id.compareTo(genHash(remote_node)) > 0 && genHash(remote_node).compareTo(genHash(prev))>0)
						{
						    String temp = prev;
							prev = remote_node;
						    
							Data data1 = new Data();
							data1.setMsg("ReplyJoin");
							data1.setSucc(portStr);
						    data1.setPrev(temp);
						    
						    Thread myclientThread = new Thread(new Client(serverIpAddress,Integer.parseInt(remote_node)*2,data1));
							myclientThread.start();
							
							Data data2 = new Data();
							data2.setMsg("ReplyJoin");
							data2.setSucc(remote_node);
							myclientThread = new Thread(new Client(serverIpAddress,Integer.parseInt(temp)*2,data2));
							myclientThread.start();
							
						}
						else if(prev != null && node_id.compareTo(genHash(remote_node)) > 0 && genHash(remote_node).compareTo(genHash(prev)) < 0)
						{
							Data data = new Data();
							data.setMsg("join");
							data.setNode(remote_node);
							
							Thread myclientThread = new Thread(new Client(serverIpAddress,Integer.parseInt(prev)*2,data));
							myclientThread.start();
						}
						else if(succ != null && node_id.compareTo(genHash(remote_node)) < 0 && genHash(remote_node).compareTo(genHash(succ))<0)
						{
							 String temp = succ;
						     succ = remote_node;
							    
							 Data data1 = new Data();
							 data1.setMsg("ReplyJoin");
							 data1.setPrev(portStr);
							 data1.setSucc(temp);
							    
							 Thread myclientThread = new Thread(new Client(serverIpAddress,Integer.parseInt(remote_node)*2,data1));
							 myclientThread.start();
								
							 Data data2 = new Data();
							 data2.setMsg("ReplyJoin");
							 data2.setPrev(remote_node);
							 myclientThread = new Thread(new Client(serverIpAddress,Integer.parseInt(temp)*2,data2));
							 myclientThread.start();
						}
						else if(succ != null && node_id.compareTo(genHash(remote_node)) < 0 && genHash(remote_node).compareTo(genHash(succ))>0)
						{
							Data data = new Data();
							data.setMsg("join");
							data.setNode(remote_node);
							
							Thread myclientThread = new Thread(new Client(serverIpAddress,Integer.parseInt(succ)*2,data));
							myclientThread.start();
						}
						
						
					} catch (NoSuchAlgorithmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				
				if(message.equals("Finalinsert"))
				{
					
					SQLiteDatabase db = mOpenHelper.getWritableDatabase();
					ContentValues keyValueToInsert = new ContentValues();
					keyValueToInsert.put("provider_key",recv_data.getKey());
					keyValueToInsert.put("provider_value",recv_data.getValue());
					db.insert(TABLE_NAME, null, keyValueToInsert);
				
				}
				
				
				if(message.equals("ReplyJoin"))
				{
					prev = recv_data.getPrev();
					succ = recv_data.getSucc();
				}
				
				///Insert
				if(message.equals("insert"))
				{
					ContentValues keyValueToInsert = new ContentValues();
					keyValueToInsert.put("provider_key",recv_data.getKey());
					keyValueToInsert.put("provider_value",recv_data.getValue());
					insert(providerUri,keyValueToInsert);
				}
				
				//Query
				if(message.equals("query"))
				{
					SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
				    qb.setTables(TABLE_NAME);
				    SQLiteDatabase db = mOpenHelper.getReadableDatabase();
				    
				    //String clause = KEY + " = " + "'" + recv_data.getKey() + "'";
				    Cursor c = qb.query(db,null,recv_data.getKey(),null,null,null,null);
				    
				    if(c.getCount() == 0 && succ != null)
				    {
				    	Thread myclientThread = new Thread(new Client(serverIpAddress,Integer.parseInt(succ)*2,recv_data));
						myclientThread.start();
				    }
				    else if(c.getCount() == 0 && succ == null)
				    {
				    	Data querydata = new Data();
				    	querydata = recv_data;
				    	querydata.setMsg("query");
				    	//querydata.setKey(selection);
				    	//querydata.setSrc_port(Integer.parseInt(portStr)*2);
				    	
				    	Thread myclientThread = new Thread(new Client(serverIpAddress,11124,querydata));
						myclientThread.start();
				    }
				    else
				    {
				    	Data data = new Data();
				    	data.setMsg("ReplyQuery");
				    	c.moveToFirst();
				    	data.setKey(c.getString(0));
				    	data.setValue(c.getString(1));
				    	
				    	Thread myclientThread = new Thread(new Client(serverIpAddress,recv_data.getSrc_port(),data));
						myclientThread.start();
				    }
				}
				
				if(message.equals("ReplyQuery"))
				{
					String[] culumn_name = new String[] {KEY, VALUE};
					remote_cursor = new MatrixCursor(culumn_name);
					remote_cursor.addRow(new Object[]{recv_data.getKey(),recv_data.getValue()});
					semaphore.release(1);
				}
			
			}
				
		}
		
	}
	
	
	
}//End of content provider
