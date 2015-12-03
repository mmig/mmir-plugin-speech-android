/**
 * The MIT License
 *
 *	Copyright (c) 2011-2013
 *	Colin Turner (github.com/koolspin)  
 *	Guillaume Charhon (github.com/poiuytrez)  
 *	
 *	Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *	
 *	The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *	
 *	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */
package de.dfki.iui.mmir.plugins.speech.android;

import java.util.Locale;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.json.JSONArray;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

/**
 * Style and such borrowed from the TTS and PhoneListener plugins
 */
public class AndroidSpeechRecognizer extends CordovaPlugin {
	
	
    public static final String ACTION_GET_LANGUAGES = "getSupportedLanguages";
	public static final String ACTION_RECOGNIZE = "recognize";
	public static final String ACTION_START_RECORDING = "startRecording";
	public static final String ACTION_STOP_RECORDING = "stopRecording";
	public static final String ACTION_CANCEL = "cancel";
//	public static final String ACTION_MIC_LEVEL = "getMicLevels";
	
	public static final String ACTION_MIC_LEVEL_LISTENER = "setMicLevelsListener";
	
	private static final String PLUGIN_NAME = AndroidSpeechRecognizer.class.getSimpleName();
	private static final int SDK_VERSION = Build.VERSION.SDK_INT;
//    private static int REQUEST_CODE = 1001;

//    private CallbackContext callbackContext;
    private LanguageDetailsReceiver languageDetailsChecker;
    
    private int recCounter = 0;
    private SpeechRecognizer speech;
    private Object speechLock = new Object();

    private ASRHandler currentRecognizer;
    
    /**
     * enable / disable sending RMS change events to the JavaScript plugin implementation.
     * 
     * This attribute will be set by the {@link AndroidSpeechRecognizer#ACTION_MIC_LEVEL_LISTENER ACTION_MIC_LEVEL_LISTENER}
     *  and all new {@link ASRHandler} will be initialized with the new value.
     * 
     * @see ASRHandler#setRmsChangedEnabled(boolean)
     * @see #setMicLevelsListener(boolean, CallbackContext)
     */
    private boolean enableMicLevelsListeners = false;
    
    CordovaInterface _cordova;
    
    @Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    	
    	this._cordova = cordova;
    	
    	this.mAudioManager = (AudioManager) this._cordova.getActivity().getSystemService(Context.AUDIO_SERVICE);
		
		super.initialize(cordova, webView);
	}

	@Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
		Boolean isValidAction = true;

//    	this.callbackContext= callbackContext;

		//FIXME DEBUG:
		try{
			LOG.i(PLUGIN_NAME + "_DEBUG", String.format("action '%s' with arguments: %s)", action, args.toString(2)));
		}catch(Exception e){}
		
		
		
		// Action selector
    	if (ACTION_RECOGNIZE.equals(action)) {
            // recognize speech
            startSpeechRecognitionActivity(args, callbackContext);     
        } else if (ACTION_GET_LANGUAGES.equals(action)) {
        	getSupportedLanguages(callbackContext);
        } else if (ACTION_START_RECORDING.equals(action)) {
        	startSpeechRecognitionActivity(args, callbackContext);
        } else if (ACTION_STOP_RECORDING.equals(action)) {
        	stopSpeechInput(callbackContext);
        } else if (ACTION_CANCEL.equals(action)) {
        	cancelSpeechInput(callbackContext);
//        } else if (ACTION_MIC_LEVEL.equals(action)) {
//        	returnMicLevels(callbackContext);
        } else if (ACTION_MIC_LEVEL_LISTENER.equals(action)) {
        	setMicLevelsListener(args, callbackContext);
        } else {
            // Invalid action
        	callbackContext.error("Unknown action: " + action);
        	isValidAction = false;
        }
    	
        return isValidAction;

    }

//    private void returnMicLevels(CallbackContext callbackContext) {
//		JSONArray list;
//    	if(currentRecognizer != null){
//			list = AudioLevelChange.toJSON(currentRecognizer.getAudioLevels());
//		}
//    	else {
//    		list = new JSONArray();
//    	}
//    	
//    	callbackContext.success(list);
//	}

	// Get the list of supported languages
    private void getSupportedLanguages(CallbackContext callbackContext) {
    	
    	if (languageDetailsChecker == null ){
    		languageDetailsChecker = new LanguageDetailsReceiver(callbackContext);
    	}
    	else {
    		languageDetailsChecker.setCallbackContext(callbackContext);
    	}
    	
    	// Create and launch get languages intent
    	Intent detailsIntent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
    	cordova.getActivity().sendOrderedBroadcast(detailsIntent, null, languageDetailsChecker, null, Activity.RESULT_OK, null, null);
	}

	/**
     * Fire an intent to start the speech recognition activity.
     *
     * @param args Argument array with the following string args: [req code][number of matches][prompt string]
     */
    private void startSpeechRecognitionActivity(final JSONArray args, final CallbackContext callbackContext) {
    	
    	//need to run recognition on UI thread (Android's SpeechRecognizer must run on main thread)
    	cordova.getActivity().runOnUiThread(new Runnable() {
    			
			@Override
			public void run() {
				_startSpeechRecognitionActivity(args, callbackContext);
			}
		});
    	
    }
    
    private void _startSpeechRecognitionActivity(JSONArray args, CallbackContext callbackContext) {
        int maxMatches = 0;
        String prompt = "";//TODO remove? (not used when ASR is directly used as service here...)
        String language = Locale.getDefault().toString();
        boolean isIntermediate = false;

        try {
        	if (args.length() > 0) {
            	// Optional language specified
            	language = args.getString(0);
            }
            if (args.length() > 1) {
            	isIntermediate = args.getBoolean(1);
            }
            if (args.length() > 2) {
            	// Maximum number of matches, 0 means that the recognizer "decides"
                String temp = args.getString(2);
                maxMatches = Integer.parseInt(temp);
            }
            if (args.length() > 3) {
            	// Optional text prompt
                prompt = args.getString(3);
            }

            //TODO if ... withoutEndOfSpeechDetection = ...
        }
        catch (Exception e) {
            Log.e(PLUGIN_NAME, String.format("startSpeechRecognitionActivity exception: %s", e.toString()));
        }

        // Create the intent and set parameters
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
        
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000);
         

        if (maxMatches > 0)
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxMatches);
        
        if (!prompt.equals(""))
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt);
        
//        //FIXME TEST try to simulate start/stop-recording behavior (without end-of-speech detection) 
//        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, new Long(10000));
////      intent.putExtra(RecognizerIntent. EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS , 6 * 1000);
        
        
        if(isIntermediate)
        	intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        
//        //TODO remove?
//        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, cordova.getActivity().getPackageName());
        
        synchronized (speechLock){
        	
	        if(speech != null){
	        	speech.destroy();
	        }
		    speech = SpeechRecognizer.createSpeechRecognizer(cordova.getActivity());
		    
		    disableSoundFeedback();
		    
	        ++recCounter;
	        currentRecognizer = new ASRHandler(recCounter, enableMicLevelsListeners, callbackContext, this);
	        currentRecognizer.setHapticPrompt( (Vibrator) this.cordova.getActivity().getSystemService(Context.VIBRATOR_SERVICE));
	        speech.setRecognitionListener(currentRecognizer);
	        speech.startListening(intent);
        
        }
    }
    
    private void stopSpeechInput(final CallbackContext callbackContext){
    	
    	
    	if(this.speech != null && this.currentRecognizer != null){
    		
    		//TODO synchronize access on currentRecognizer?
    		cordova.getActivity().runOnUiThread(new Runnable() {
    			
    			@Override
    			public void run() {
    				
    				synchronized (speechLock) {
						
	    				if(currentRecognizer != null){
	    					currentRecognizer.stopRecording(callbackContext);
	    				}
	    	    		
				    	if(speech != null){
				    		speech.stopListening();
//				    		speech = null;
				    	}
				    	
				    	if(AndroidSpeechRecognizer.this != null){
				    		AndroidSpeechRecognizer.this.enableSoundFeedback();
				    	}
    				}
    			}
    		});
    		
    	}
    	else {
    		callbackContext.error("recording was not started yet");
    	}
    }
    
    private void setMicLevelsListener(final JSONArray args, final CallbackContext callbackContext) {
    	try {
    		boolean enabled;
        	if (args.length() > 0) {
            	//extract enabled/disabled setting from args
        		enabled = args.getBoolean(0);
            }
        	else {
        		callbackContext.error("setMicLevelsListener: missing argument BOOLEAN.");
        		return; /////////////////// EARLY EXIT //////////////////////////
        	}
        	
        	setMicLevelsListener(enabled, callbackContext);
        }
        catch (Exception e) {
        	String msg = String.format("setMicLevelsListener exception: %s", e.toString());
            Log.e(PLUGIN_NAME, msg);
    		callbackContext.error(msg);
        }	
    }
    
    private void setMicLevelsListener(final boolean enabled, final CallbackContext callbackContext){
    	
    	enableMicLevelsListeners = enabled;
    	
    	if(this.speech != null && this.currentRecognizer != null){
    		
    		//TODO synchronize access on currentRecognizer?
    		cordova.getActivity().runOnUiThread(new Runnable() {//TODO test if this can run a background-thread via cordova.getThreadPool()
    			
    			@Override
    			public void run() {
    				
    				synchronized (speechLock) {
        				if(speech != null && currentRecognizer != null){
        					currentRecognizer.setRmsChangedEnabled(enabled);
        				}
					}
    				
    				if(AndroidSpeechRecognizer.this != null){
    					AndroidSpeechRecognizer.this.enableSoundFeedback();
    				}
    				
    				callbackContext.success();
    			}
    		});
    		
    	}
    	else {
    		callbackContext.success("recognition is currently not running");
    	}
    }
    
    private void cancelSpeechInput(final CallbackContext callbackContext){
    	
    	if(speech != null){
    		
    		//need to run stop-recognition on UI thread (Android's SpeechRecognizer must run on main thread)
    		cordova.getActivity().runOnUiThread(new Runnable() {
    			
    			@Override
    			public void run() {
    	    		
    				try{
    					
    					synchronized (speechLock) {
    				    	if(speech != null){
    				    		speech.destroy();
    				    		speech = null;
    				    	}
						}
    					
    					if(AndroidSpeechRecognizer.this != null){
    						AndroidSpeechRecognizer.this.enableSoundFeedback();
    					}
    					
	    	    		callbackContext.success();
    				}
    				catch(Exception e){
    					
    					LOG.e(PLUGIN_NAME, "cancelRecoginition: an error occured "+e, e);
    					
    					callbackContext.error(e.toString());
    				}
    			}
    		});
    		
    	} else {

        	callbackContext.success();	
    	}
    	
    }
    
    //FIXME TEST private/package-level method that allows canceling recognition
    void cancelSpeechInput(){
    	synchronized (speechLock) {
	    	if(this.speech != null){
	    		speech.destroy();//FIXME russa: speech.stopListening() and speech.cancel() do not seem to do the trick -> onRmsChanged is still called!
	    		speech = null;
	    	}
    	}
    	enableSoundFeedback();
    }
    

//    /**
//     * Handle the results from the recognition activity.
//     */
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (resultCode == Activity.RESULT_OK) {
//            // Fill the list view with the strings the recognizer thought it could have heard
//            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
//            
//
//            float[] scores = data.getFloatArrayExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES);
//            
//            returnSpeechResults(matches, toList(scores) );
//        }
//        else {
//            // Failure - Let the caller know
//            this.callbackContext.error(Integer.toString(resultCode));
//        }
//
//        super.onActivityResult(requestCode, resultCode, data);
//    }
    
    @Override
	public void onPause(boolean multitasking) {
    	enableSoundFeedback();
    	synchronized (speechLock) {
			if(speech != null){
				speech.destroy();
				speech = null;
			}
    	}
	}

	@Override
	public void onDestroy() {
		enableSoundFeedback();
		synchronized (speechLock) {
			if(speech != null){
				speech.destroy();
				speech = null;
			}
		}
	}
	

    
    protected AudioManager mAudioManager; 
    protected volatile boolean mIsCountDownOn;
    private boolean mIsStreamSolo;
    
    private boolean isDisableSoundPrompt(){
    	//TODO impl. "smarter" detection? (russa: which version added the sounds?)
    	return SDK_VERSION >= Build.VERSION_CODES.JELLY_BEAN;
    }
    
	void disableSoundFeedback() {
		if(!mIsStreamSolo && isDisableSoundPrompt()){
		    mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, true);
		    mIsStreamSolo = true;
		}
	}

	void enableSoundFeedback() {
		if (mIsStreamSolo){
	         mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, false);
	         mIsStreamSolo = false;
	    }
	}
    
}
