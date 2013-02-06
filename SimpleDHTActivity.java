import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class SimpleDHTActivity extends Activity {
    /** Called when the activity is first created. */
  
	private static TextView queryHistory;
	private static Button testbutton,dumpbutton;
	private static final int MSG_ID = 0x1337;
	private static String disMsg = null;
	
	private static final Uri providerUri = Uri.parse("content://edu.buffalo.cse.cse486_586.simpledht.provider");
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        queryHistory = (TextView) findViewById(R.id.textView1);
        testbutton = (Button)findViewById(R.id.testbutton);
        dumpbutton = (Button)findViewById(R.id.dumpbutton);
        
        testbutton.setOnClickListener(test);
        dumpbutton.setOnClickListener(dump);  
    }
    
    Handler myUpdateHandler = new Handler()
    {
    	
    	public void handleMessage(Message Msg)
    	{
    		switch(Msg.what)
    		{
    		case MSG_ID:
    			
    			 String s = disMsg + "\n";
    			 queryHistory.append(s);
              
    			 //tv.setText(mClientMsg);
                 break;
                 
    		default:
    			 break;
    	
    		}
    		
    		super.handleMessage(Msg);
    	}
    };
    
    private OnClickListener test = new OnClickListener() 
    {
    	public void onClick(View v)
		{
    		Thread TempThread = new Thread(new TestThread());
    		TempThread.start();

		}
		
    };  
    
    private OnClickListener dump = new OnClickListener() 
    {
    	public void onClick(View v)
		{
    		Thread TempThread = new Thread(new DumpThread());
    		TempThread.start();

		}
		
    };  
    
    class TestThread implements Runnable
    {

		public void run() {
		  // TODO Auto-generated method stub
			
		  try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  int i,j;
		  for(i = 0; i<=9 ; i++)
		  {
			
			  ContentValues keyValueToInsert = new ContentValues();
		      
		      keyValueToInsert.put("provider_key", Integer.toString(i));
		      String value = "Test" + Integer.toString(i);
		      keyValueToInsert.put("provider_value",value);
		      getContentResolver().insert(providerUri,keyValueToInsert);
		      
		      try {
				Thread.sleep(1000);
			    } catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			    }
		      
		  }
		  
		  String s = null;
		  Cursor resultCursor;
		  
		  for(j = 0; j<= 9 ; j++)
		  {
			  Message message = new Message();
			  message.what = MSG_ID;
			  
			  String clause = "provider_key = " + "'" + Integer.toString(j) + "'"; 
			  resultCursor = getContentResolver().query(providerUri,null,clause,null,null);
			  
			  resultCursor.moveToFirst();
			  s = "<" + resultCursor.getString(0) + "," + resultCursor.getString(1) + ">";
			  disMsg = s;
			  myUpdateHandler.sendMessage(message);
			  
			  try {
				Thread.sleep(1000);
			      } catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			     }
		
		  }
		  
		 
    	
      }
		
    }//End of Test
    
    //Show ALL LOCAl stored <key,value>
    class DumpThread implements Runnable
    {

    	String s = null;
		public void run() {
			// TODO Auto-generated method stub
		
			  Message message = new Message();
			  message.what = MSG_ID; 
			  
			  Cursor cursor;
			  cursor = getContentResolver().query(providerUri,null,null,null,null);
			  
			  if(cursor != null)
			  {
				  cursor.moveToFirst();
				  s = "<" + cursor.getString(0) + "," + cursor.getString(1) + ">" + "\n";
				   
				  while(!cursor.isLast())
				  {
					   cursor.moveToNext();
					   s = s + "<" + cursor.getString(0) + "," + cursor.getString(1) + ">" + "\n";  
				  }
				 
			  }
			  
			  disMsg = s;
			  myUpdateHandler.sendMessage(message);
			
		}
    }//End of Dump
    
  }//Main Activity End
