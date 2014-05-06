package com.example.intentcovertchannellistener.app;

import java.util.Date;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;

import com.example.intentutils.lib.*;

public class MessageReceiver extends Service
{
    protected static final String MESSAGE_STORE_KEY = "covert_message_store";
    protected static final String MESSAGE_SET_KEY = "covert_message_set";
	
    // TODO: Make this configurable
    private static final int BUILD_VERSION = 8;

    // Used to persist received messages so that they can be accessed later
    private SharedPreferences messageStore;

    // The schema to use for decoding received messages
    private LowerCaseAlphaEncoder schema;
    
    // Generates an alphabetical sequence of keys for storing messages
    private AlphabeticalKeySequence keySequence;
    
	// This is the object that receives interactions from clients.
    private final IBinder mBinder = new MessageBinder();
    
	@Override
    public void onCreate()
	{
		super.onCreate();
		messageStore = getSharedPreferences(MESSAGE_STORE_KEY, MODE_PRIVATE);
		schema = new LowerCaseAlphaEncoder();
		keySequence = new AlphabeticalKeySequence();
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
    	if(matchesFilter(intent))
    	{
    		long endTime = new Date().getTime();
    		
    		String intentAction = intent.getAction();
    		if(intentAction.equals(EncodingUtils.CALCULATE_THROUGHPUT_ACTION))
    		{
        		Intent responseIntent = new Intent();
        		responseIntent.setAction(EncodingUtils.SEND_TIME_ACTION);
        		responseIntent.putExtra(EncodingUtils.END_TIME_KEY, endTime);
        		sendBroadcast(responseIntent);
    		}
    		else if(intentAction.equals(EncodingUtils.CALCULATE_BIT_ERROR_RATE))
    		{
    			String receivedMessage = schema.decode(intent, BUILD_VERSION);
    			
        		Intent responseIntent = new Intent();
        		responseIntent.setAction(EncodingUtils.SEND_BIT_ERRORS_ACTION);
        		responseIntent.putExtra(EncodingUtils.END_TIME_KEY, endTime);
        		responseIntent.putExtra(EncodingUtils.RECEIVED_MESSAGE_KEY, receivedMessage);
        		
        		sendBroadcast(responseIntent);
    		}
    		else
    		{
    			// Default action
    			decodeAndStore(intent);
    		}
    	}

    	// TODO: Explore the possibility of stopping the service using 
    	// stopSelf() after the message Intent has been received and 
    	// persisted
        return START_NOT_STICKY;
    }

	@Override
    public void onDestroy()
    {
    	super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
    	if(matchesFilter(intent))
    	{
    		decodeAndStore(intent);
    	}
    	
        return mBinder;
    }
    
	private boolean matchesFilter(Intent intent)
    {
	    // TODO: Implement Intent filtering so that only Intents which are
		// covertly marked as containing a covert message (such as through
		// the ClipData channel) will attempted to be decoded
		
		// TODO: Represent the Intent filters as a custom class to allow the
		// signature/pattern/microprotocol being used and looked for to
		// vary independently
	    return (EncodingUtils.RECEIVER_COVERT_MESSAGE_ACTION.equals(intent.getAction()) ||
	    		EncodingUtils.CALCULATE_THROUGHPUT_ACTION.equals(intent.getAction()) ||
	    		EncodingUtils.CALCULATE_BIT_ERROR_RATE.equals(intent.getAction()));
    }
	
	private void decodeAndStore(Intent messageIntent)
	{
		String message = schema.decode(messageIntent, BUILD_VERSION);
    	SharedPreferences.Editor messageStoreEditor = messageStore.edit();
    	messageStoreEditor.putString(keySequence.next(), message);
    	messageStoreEditor.commit();
	}

	public class MessageBinder extends Binder
	{
		public MessageReceiver getService()
		{
			return MessageReceiver.this;
		}
	}
}
