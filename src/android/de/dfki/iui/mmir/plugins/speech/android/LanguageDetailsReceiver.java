package de.dfki.iui.mmir.plugins.speech.android;

import java.util.ArrayList;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;

public class LanguageDetailsReceiver extends BroadcastReceiver {
	
	private List<String> languages;
	private String errorMessage;
    private ArrayList<CallbackContext> callbacks;

    public LanguageDetailsReceiver(CallbackContext callbackContext) {
    	super();
    	this.callbacks = new ArrayList<CallbackContext>();
    	this.callbacks.add(callbackContext);
    }
    
	@Override
	public synchronized void onReceive(Context context, Intent intent) {
		
		ArrayList<CallbackContext> list;
		synchronized (this.callbacks) {
			
			list = new ArrayList<CallbackContext>(this.callbacks);
			this.callbacks.clear();
		}


		Bundle results = getResultExtras(true);
        
        if (results.containsKey(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)){
        	
        	languages = results.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);
            
        } else {
        	
        	this.errorMessage = "Could not retrieve the list of supported languages";
        	
        }
		
		for(CallbackContext callbackContext : list)
			sendReply(callbackContext);
	}
	
	private void sendReply(CallbackContext callbackContext){
		PluginResult result;
		if(this.hasError())
			result = new PluginResult(PluginResult.Status.ERROR, this.errorMessage);
		else
			result = new PluginResult(PluginResult.Status.OK, new JSONArray(this.languages));
		
		callbackContext.sendPluginResult(result);
	}

	public void setCallbackContext(CallbackContext callbackContext) {
		synchronized (this.callbacks) {
//			if(this.errorMessage == null && this.supportedLanguages == null)
				this.callbacks.add(callbackContext);
//			else
//				sendReply(callbackContext);
		}
	}
	
	public boolean hasError(){
		return errorMessage != null;
	}
	
}
