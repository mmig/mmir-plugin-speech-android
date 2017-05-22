package de.dfki.iui.mmir.plugins.speech.android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;

class ASRHandler implements RecognitionListener {

	private static final String JS_PLUGIN_ID = "dfki-mmir-plugin-speech-android.androidSpeechPlugin";

	private static final String ANDROID_SPEECH_EXTRA_UNSTABLE_TEXT = "android.speech.extra.UNSTABLE_TEXT";

	private static final String FIELD_RESULT_TYPE = "type";

	private static final String FIELD_RECOGNITION_RESULT_ALTERNATIVES = "alternatives";
	
	private static final String FIELD_RECOGNITION_RESULT_UNSTABLE = "unstable";

	private static final String FIELD_RECOGNITION_SCORE = "score";

	private static final String FIELD_RECOGNITION_RESULT = "result";

	private static final String FIELD_ERROR_CODE = "error_code";

	private static final String FIELD_MESSAGE = "msg";

	private static final String NAME = "ASRHandler";
	
	private static final int SDK_VERSION = Build.VERSION.SDK_INT;
	
	public static final int ERROR_SPEECH_NOT_STARTED_TIMEOUT = 101;
	
//	private final ToneGenerator tonePrompt;
	
	//NOTE requires <uses-permission android:name="android.permission.VIBRATE"/>
	private Vibrator hapticPrompt;
	
	private static enum ResultTypes {
        FINAL,					// last / final result
        INTERMEDIATE,			// stable intermediate result 
        INTERIM,				// temporal / interim result (may change)
        RECOGNITION_ERROR,
        RECORDING_BEGIN,
        RECORDING_DONE
    }
	
	private int id;
	private CallbackContext callbackContext;
	private CallbackContext doneCallbackContext;
	
	private Object callbackLock;
	private Object doneLock;
	
	private boolean isStopped;
	private boolean isFinal;

	//FIXME russa: for HACK for detecting multiple calls to onEndOfSpeech (happens on Android >= 5.0.1???)
	private boolean _isEnded;
	private AndroidSpeechRecognizer _owner;//FIXME debug/TEST: reference that allows canceling recognition manually in case onEndOfSpeech is called multiple times ...
	
	/**
	 * Count down timer for JellyBean work around:
	 *  recognizer stops *silently*, if no speech was detected within some seconds
	 */
    protected CountDownTimer noSpeechCountDown = new CountDownTimer(5000, 5000){

        @Override
        public void onTick(long millisUntilFinished){
            //do nothing
        }

        @Override
        public void onFinish(){
        	
            isNoSpeechTimeoutRunning = false;
            
            //just in case, DO cancel recognizer (in case it did not actually stop)
            _owner.cancelSpeechInput();
            
            //send custom error code, indicating the Jelly Bean problem as cause:
            onError(ERROR_SPEECH_NOT_STARTED_TIMEOUT);
        }
        
    };

    private boolean isNoSpeechTimeoutEnabled = false;//DEFAULT: disable no-speech-started workaround (does not work reliably yet!)
    private volatile boolean isNoSpeechTimeoutRunning;
		
	public ASRHandler(int id, boolean rmsChangedListeningEnabled, CallbackContext callbackContext, AndroidSpeechRecognizer recognizer){
		
		this.callbackLock = new Object();
		this.doneLock = new Object();
		
		this.id = id;
		this.rmsChangedEnabled = rmsChangedListeningEnabled;
		this.callbackContext = callbackContext;
		
//		this.rmsChanges = null;//new LinkedList<AudioLevelChange>();
		
		this.isStopped = false;
		this.isFinal = false;
		this._owner = recognizer;

//		ToneGenerator tg = null;
//		try {
//			tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
//		} catch(Exception ex){
//			printe("failed to create Tonegenerator for prompt");
//		}
//		this.tonePrompt = tg;
		
		print("created ASRHandler for request "+callbackContext.getCallbackId());
	}
	
	
	
	@Override
	protected void finalize() throws Throwable {
		if (isNoSpeechTimeoutRunning){
            noSpeechCountDown.cancel();
        }
		doEnableSound(false);
		super.finalize();
	}

	private void logs(String msg){
		printe("LOG_STORE_MSG "+msg);
	}

	private void printd(String msg){//DEBUG
		LOG.d(NAME + "_" + this.id, msg);
	}
	private void print(String msg){//DEBUG
		LOG.i(NAME + "_" + this.id, msg);
	}
	private void printw(String msg){//DEBUG
		LOG.w(NAME + "_" + this.id, msg);
	}
	private void printe(String msg){//DEBUG
		LOG.e(NAME + "_" + this.id, msg);
	}
	private void print(String msg, Throwable e){//DEBUG
		LOG.e(NAME, msg, e);
	}
	
	
//	/** Network operation timed out. */
//    public static final int ERROR_NETWORK_TIMEOUT = 1;
//    /** Other network related errors. */
//    public static final int ERROR_NETWORK = 2;
//    /** Audio recording error. */
//    public static final int ERROR_AUDIO = 3;
//    /** Server sends error status. */
//    public static final int ERROR_SERVER = 4;
//    /** Other client side errors. */
//    public static final int ERROR_CLIENT = 5;
//    /** No speech input */
//    public static final int ERROR_SPEECH_TIMEOUT = 6;
//    /** No recognition result matched. */
//    public static final int ERROR_NO_MATCH = 7;
//    /** RecognitionService busy. */
//    public static final int ERROR_RECOGNIZER_BUSY = 8;
//    /** Insufficient permissions */
//    public static final int ERROR_INSUFFICIENT_PERMISSIONS = 9;

	private String getMessage(int resultCode){
		String msg;
		if(resultCode == SpeechRecognizer.ERROR_NETWORK_TIMEOUT){
			msg = "Network Timeout Error";
		}else if(resultCode == SpeechRecognizer.ERROR_NETWORK){
			msg = "Network Error";
		}else if(resultCode == SpeechRecognizer.ERROR_AUDIO){
			msg = "Audio Error";
		}else if(resultCode == SpeechRecognizer.ERROR_SERVER){
			msg = "Server Error";
		}else if(resultCode == SpeechRecognizer.ERROR_CLIENT){
			msg = "Client Error";
		}else if(resultCode == SpeechRecognizer.ERROR_SPEECH_TIMEOUT){
			msg = "Speech Timeout Error";
		}else if(resultCode == SpeechRecognizer.ERROR_NO_MATCH){
			msg = "No Match Error";
		}else if(resultCode == SpeechRecognizer.ERROR_RECOGNIZER_BUSY){
			msg = "Recognizer Busy Error";
		}else if(resultCode == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS){
			msg = "Insufficient Permissions Error";
		}else if(resultCode == ERROR_SPEECH_NOT_STARTED_TIMEOUT){
			msg = "[Custom Error] Speech Not Started Timout Error";
		}else {
			msg = "Unknown Error: code "+resultCode;
		}
		return msg;
	}
	private String toString(byte[] buffer){
		if(buffer != null){
			return "byte[] (size " + buffer.length + ")";
		}
		return "null";
	}
	private String toString(Bundle params){
		if(params != null) try{
			return toString(params.keySet());
		} catch(Exception e){
			LOG.e(NAME, "error creating debug output for Bundle", e);
		}
		return "null";
	}
	private String toString(Collection<String> col){
		if(col == null){
			return "NULL";
		}
		String[] a = new String[col.size()];
		a = col.toArray(a);
		return Arrays.toString(a);
	}
	private String toString(float[] col){
		if(col == null){
			return "NULL";
		}
		return Arrays.toString(col);
	}
	
	private void doDisableSound() {
		this._owner.disableSoundFeedback();
	}

	private void doEnableSound() {
		doEnableSound(true);
	}
	
	private void doEnableSound(boolean isDelayed) {
		//since the method is also called from finalize(), we need to check if the owner still exists, befor calling one of its methods:
		if(this._owner != null){
			if(isDelayed)
				this._owner.enableSoundFeedbackDelayed();
			else
				this._owner.enableSoundFeedback();
		}
	}


	private boolean isNoSpeechTimeout(){
		return isNoSpeechTimeoutEnabled
				//only for JellyBean (4.1.x)
				// see http://stackoverflow.com/questions/17592333/google-voice-recognizer-doesnt-starts-on-android-4-x?noredirect=1#comment27730909_17592333
				//version 4.2.x seems to have fixed this
				// see http://www.ubergizmo.com/2013/09/htc-one-to-receive-android-4-3-jelly-bean-update-this-september/
				&& SDK_VERSION == Build.VERSION_CODES.JELLY_BEAN;
	}

	private void doStartTimeout() {
		if(isNoSpeechTimeout()){
			isNoSpeechTimeoutRunning = true;
			noSpeechCountDown.start();
		}
	}

	private void doCancelTimeout() {
		if (isNoSpeechTimeoutRunning){
	        isNoSpeechTimeoutRunning = false;
	        noSpeechCountDown.cancel();
	    }
	}
	

	@Override
	public void onReadyForSpeech(Bundle params) {

		print("onReadyForSpeech with params: "+ toString(params));
		
		doDisableSound();
		doStartTimeout();
		
		//TODO add tone-prompt?
//		tonePrompt.startTone(ToneGenerator.TONE_PROP_ACK);//R.raw.beep
		
		JSONObject result = new JSONObject();
        try {
//            result.put(FIELD_RECOGNITION_RESULT, "");
//            result.put(FIELD_RECOGNITION_SCORE, -1);
            result.put(FIELD_RESULT_TYPE, ResultTypes.RECORDING_BEGIN.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        PluginResult recBeginResult = new PluginResult(Status.OK, result);
        recBeginResult.setKeepCallback(true);

        synchronized (this.callbackLock) {
	        this.callbackContext.sendPluginResult(recBeginResult);
		}
	}



	@Override
	public void onBeginningOfSpeech() {
		
		print("onBeginningOfSpeech");
		
		//... speech input will be processed, so there is no need for count down anymore:
        doCancelTimeout();
	}


	////////////////////////////// START: MicLevels Listener Implementation //////////////////////////////////////

	private boolean rmsChangedEnabled = true;

	private float _lastRms = 0f;
	private long _lastChangeNotification = -1l;
	private LinkedList<Float> rmsValues = new LinkedList<Float>();
	
	private static final float CHANGE_THRESHOLD = 0.1f;
	private static final long MIN_CHANGE_INTERVAL = 100l;
	
	private static final float RMS_CHANGE_NORMALIZATION_FACTOR = 4.5f;
	
	@Override
	public void onRmsChanged(float rmsdB) {
		
		if(!rmsChangedEnabled){
			return;
		}
		
//		printd("onRmsChanged: "+rmsdB);
		
//		if(this.rmsChanges != null){
//			synchronized (this.rmsChanges) {
//				this.rmsChanges.add(new AudioLevelChange(System.currentTimeMillis(), rmsdB));	
//			}
//		}
		
		//TODO implement second mode: instead of storing the audio level, 
		//                            use an additional callbackContext (i.e. a registered "listener")
		//							  for signaling the changed value.
		//							  (note: needs to "closed" when recognition stopped)
		
		rmsValues.add(rmsdB);
		
		//use "temporal smoothing" for sending RMS changes
		long currentTime = System.currentTimeMillis();
		if(_lastChangeNotification == -1 || currentTime - _lastChangeNotification >= MIN_CHANGE_INTERVAL){
			
			float min = Float.MAX_VALUE;
			float max = Float.MIN_VALUE;
			for(Float v : rmsValues){
				if(v < min){
					min = v;
				}
				if(v > max){
					max = v;
				}
			}
			
			//select the value with the largest change (compared to last-sent-value)
			float maxDiff, value;
			float diffMin = Math.abs(_lastRms - min);
			float diffMax = Math.abs(_lastRms - max);
			if(diffMin > diffMax){
				maxDiff = diffMin;
				value = min;
			}
			else {
				maxDiff = diffMax;
				value = max;
			}
			
			//only send RMS change message, if difference is larger than the threshold
			if(maxDiff > CHANGE_THRESHOLD){
				
//				LOG.d(NAME + "_" + id, "RMD Db changed (diff "+maxDiff+") "+_lastRms +" -> " +value);
				
				//reset / update values for "next round"
				_lastRms = value;
				_lastChangeNotification = currentTime;
				rmsValues.clear();
				
				value *= RMS_CHANGE_NORMALIZATION_FACTOR;
				
				this._owner.sendMicLevels(value);
			}	
		}
	}
	public boolean isRmsChangedEnabled() {
		return rmsChangedEnabled;
	}
	/**
	 * Enable / disable sending RMS changes to the JavaScript plugin implementation
	 * (which will then notify any registered listeners for this event).
	 * 
	 * Changing the enabled-state will reset the {@link #rmsValues} list
	 * (i.e. temporarily stored RMS values for calculating the RMS value that will be sent).
	 * 
	 * @param rmsChangedEnabled
	 */
	public void setRmsChangedEnabled(boolean rmsChangedEnabled) {
		if(this.rmsChangedEnabled != rmsChangedEnabled){
			this.rmsValues.clear();
		}
		this.rmsChangedEnabled = rmsChangedEnabled;
	}
	
	////////////////////////////// END: MicLevels Listener Implementation //////////////////////////////////////
	

	@Override
	public void onBufferReceived(byte[] buffer) {

		printd("onBufferReceived: size "+ toString(buffer));
	}

	private int _onEndOfSpeechCounter = 0;
	
	@Override
	public void onEndOfSpeech() {

		printd("onEndOfSpeech");
		
		//FIXME russa: seems to get called multiple times on some devices/Android-versions!!! (-> Nexus 5, API 21, v5.0.1)
		if(this._isEnded){
			printe("onEndOfSpeech: called multiple times!");
			//TODO ... in case it is called multiple times: the recognizer seems to keep running ... stop it manually here?
			
//			//HACK stop recognizer manually:
//			if(this._owner != null){
//				this._owner.cancelSpeechInput();
//				this._owner = null;
//			} else {
//				printe("onEndOfSpeech called multiple times (no _owner set anymore)!");
//			}
			
			return;
		}
		
		//FIXME DEBUG TEST
		++_onEndOfSpeechCounter;
		if(this._isEnded){
			logs("onEndOfSpeech called "+_onEndOfSpeechCounter+" times." + (isStopped? " stopped" : "") + (isFinal? " final" : ""));
		}
		
		
		if(this.hapticPrompt != null){
			this.hapticPrompt.vibrate(100);
		}
		
		if(this.isStopped){
			this.isFinal = true;
		}
		
		JSONObject result = new JSONObject();
        try {
//            result.put(FIELD_RECOGNITION_RESULT, "");
//            result.put(FIELD_RECOGNITION_SCORE, -1);
            result.put(FIELD_RESULT_TYPE, ResultTypes.RECORDING_DONE.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        PluginResult recDoneResult = new PluginResult(Status.OK, result);
        recDoneResult.setKeepCallback(true);

        synchronized (this.callbackLock) {
        	this._isEnded = true;
	        this.callbackContext.sendPluginResult(recDoneResult);
		}
	}

	@Override
	public void onError(int error) {

		String msg = "onError: "+getMessage(error);
		printe(msg);
		
		doCancelTimeout();
		doEnableSound();
		
//		PluginResult result = new PluginResult(PluginResult.Status.ERROR, msg);
//		//TODO which errors are FATAL, which ones are runtime-errors/"warnings"?
//		// -> seems that codes < 0 && > 5 cause recognizer to stop...
//		result.setKeepCallback(true);
//		this.callbackContext.sendPluginResult(result);
		
		/*keep callback open? -> */
		boolean isKeepOpen =  error == SpeechRecognizer.ERROR_CLIENT? true : false;
		sendError(error, msg, isKeepOpen);
//		if(!isKeepOpen){
//			this.callbackContext = null;
//		}
	}



	//FIXME DEBUG TEST
	private int _onResultsCounter;
	
	@SuppressLint("InlinedApi")
	@Override
	public void onResults(Bundle results) {
		
		doEnableSound();
		
		//FIXME DEBUG TEST
		++_onResultsCounter;
		if(_onResultsCounter > 1){
			logs("onResults called "+_onResultsCounter+" times." + (isStopped? " stopped" : "") + (isFinal? " final" : ""));
		}
		if(this._isEnded){
			logs("onResults called after endOfSpeech.");
		}
		
		ArrayList<String> asr = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
		
		float[] scores;
		if(SDK_VERSION >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
			//CONFIDENCE_SCORES requires API level 14
			scores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
		}
		else {
			scores = new float[asr.size()];
		}
		
		print("onResults:\n\tres: "+toString(asr)+"\n\tscr: "+toString(scores));
		
		synchronized (this.callbackLock) {
			sendSpeechResults(asr, toList(scores), this.callbackContext);
			// "in-valdiate" callback context (since it is closed now!):
			this.callbackContext = null;
		}
		
		if(this.isFinal){
			
			synchronized (this.doneLock) {
				sendSpeechResults(new ArrayList<String>(), new ArrayList<Float>(), doneCallbackContext);	
				// "in-valdiate" DONE callback (since it is closed now!):
				this.doneCallbackContext = null;
			}
		}
	}

	@Override
	public void onPartialResults(Bundle partialResults) {
		
		ArrayList<String> pasr = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

		//HACK: we also want the unstable results (using undocumented field UNSTABLE_TEXT)
		ArrayList<String> unstablePasr = partialResults.getStringArrayList(ANDROID_SPEECH_EXTRA_UNSTABLE_TEXT);

		
		//DISABLED russa on older Android version this may cause errors (e.g. on Android API 16 (Samsung S2, v4.2.1))
//		//DEBUG:
//		String msg = String.format("onPartialResults: %s %s\t(data fields: %s)",
//				toString(pasr),
//				(unstablePasr.size() > 0? ", UNSTABLE " + toString(unstablePasr):""),
//				toString(partialResults.keySet())
//		);
//		if(unstablePasr.size() > 0)
//			printd(msg);
//		else
//			print(msg);
//		//DEBUG end
		
		
		if(pasr != null || (unstablePasr != null && unstablePasr.size() > 0)){
			
			if(pasr == null){
				pasr = new ArrayList<String>();
			}
			
			synchronized (this.callbackLock) {
				sendSpeechResults(pasr, unstablePasr, new ArrayList<Float>(0), this.callbackContext, true);	
			}
		}
	}

	@Override
	public void onEvent(int eventType, Bundle params) {
		// TODO Auto-generated method stub
		printe("onEvent: type "+eventType+", with params: "+ toString(params));
	}
	
//	public List<AudioLevelChange> getAudioLevels(){
//		if(this.rmsChanges != null){
//			
//			List<AudioLevelChange> result;
//			synchronized (this.rmsChanges) {
//				result = new LinkedList<AudioLevelChange>(this.rmsChanges);
//				this.rmsChanges.clear();
//			}
//			return result;
//		}
//		else {
//			
//			//"start" gathering of audio levels
//			this.rmsChanges =  new LinkedList<AudioLevelChange>();
//			
//			return new LinkedList<AudioLevelChange>();
//		}
//	}
	
	public void setDoneCallbackContext(CallbackContext doneCallbackContext) {
		this.doneCallbackContext = doneCallbackContext;
	}
	
	public void stopRecording(CallbackContext doneCallbackContext) {
		
		//FIXME DEBUG TEST
		if(this.isStopped){
			logs("stopRecording called multiple times.");
		}
		
		synchronized (this.callbackLock) {
			if(callbackContext == null){
				//this recognition handler is already closed!
				LOG.i(NAME, "stopRecording: cannot stop, since SPEECH INPUT is not active (any more).");
				
				//"simulate" empty ASR result for DONE callback:
				// (-> STOP does not cause an error, if called on an inactive ASR handler ... TODO should this be an error?)
				doneCallbackContext.success(new JSONObject());
			}
		}
		this.doneCallbackContext = doneCallbackContext;
		this.isStopped = true;
		
		LOG.i(NAME, "stopRecording: prepared "+NAME+" for final sending final result.");
	}


	private void sendSpeechResults(
    		ArrayList<String> matches,
    		ArrayList<Float> scores,
    		CallbackContext callbackContext
    ) {
		sendSpeechResults(matches, null, scores, callbackContext, false);
	};
	
    private void sendSpeechResults(
    		ArrayList<String> matches,
    		ArrayList<String> unstableMatches,
    		ArrayList<Float> scores,
    		CallbackContext callbackContext,
    		boolean isInterimResult
    ) {

        JSONObject result = new JSONObject();
        try {
        	
        	setResultMatches(result, matches, unstableMatches, scores);
        	setResultType(result, isInterimResult);
			
		} catch (JSONException e) {

//			e.printStackTrace();
			print(e.getLocalizedMessage(), e);
			
			//this should actually never happen (-> error / change in JSONObject implementation?)
			// ... -> just send "simple" result-object (i.e. omit scores...):
			callbackContext.success(new JSONArray(matches));
			return;////////////////////// EARLY EXIT /////////////////
		}
        
        PluginResult resultMsg = new PluginResult(PluginResult.Status.OK, result);
        if(isInterimResult){
        	resultMsg.setKeepCallback(true);
        }
        callbackContext.sendPluginResult(resultMsg);
    }
    
    private void sendError(
    		int errorCode,
    		String message,
    		boolean isInterimResult
    ) {

        JSONObject result = new JSONObject();
        try {
        	
        	result.put(FIELD_ERROR_CODE, errorCode);
        	result.put(FIELD_MESSAGE, message);
        	setResultType(result, isInterimResult);
			
		} catch (JSONException e) {

//			e.printStackTrace();
			print(e.getLocalizedMessage(), e);
			
			//this should actually never happen (-> error / change in JSONObject implementation?)
			// ... -> just send "simple" result-object (i.e. omit scores...):
			synchronized (this.callbackLock) {
				if(callbackContext != null)
					callbackContext.error(errorCode);
				else
					LOG.e(NAME, String.format("sendError: invalid callback context (already closed) -- could not send error: %s (code %d)", message, errorCode));
			}
			return;////////////////////// EARLY EXIT /////////////////
		}
        
        PluginResult resultMsg = new PluginResult(PluginResult.Status.ERROR, result);
        if(isInterimResult){
        	resultMsg.setKeepCallback(true);
        }
        
        synchronized (this.callbackLock) {
        	if(callbackContext != null)
        		callbackContext.sendPluginResult(resultMsg);
        	else
				LOG.e(NAME, String.format("sendError: invalid callback context (already closed) -- could not send error: %s (code %d)", message, errorCode));
		}
        
        
        
        //if the final callback is already set, we also need to "inform" it about the error
        if(!isInterimResult && this.isStopped && this.doneCallbackContext != null){
			
        	LOG.e(NAME, String.format("sendError: sending error message to DONE-CALLBACK: %s (code %d)", message, errorCode));
        	
        	synchronized (this.doneLock) {
	        	
        		JSONObject doneResult = new JSONObject();
	            try {
	            	
	            	doneResult.put(FIELD_ERROR_CODE, errorCode);
	            	doneResult.put(FIELD_MESSAGE, message);
	            	setResultType(doneResult, isInterimResult);
	    			
	    		} catch (JSONException e) {
	
	//    			e.printStackTrace();
	    			print(e.getLocalizedMessage(), e);
	    			
	    			//this should actually never happen (-> error / change in JSONObject implementation?)
	    			// ... -> just send "simple" result-object (i.e. omit scores...):
	    				
					if(doneCallbackContext != null)
						doneCallbackContext.error(errorCode);
					else
						LOG.e(NAME, String.format("sendError: invalid DONE callback context (already closed) -- could not send error: %s (code %d)", message, errorCode));
					
					// "in-valdiate" DONE callback (since it will be closed now!):
					this.doneCallbackContext = null;
					
	    			return;////////////////////// EARLY EXIT /////////////////
	    		}
	            
	            PluginResult doneResultMsg = new PluginResult(PluginResult.Status.ERROR, doneResult);
            
				
	        	if(doneCallbackContext != null)
	        		doneCallbackContext.sendPluginResult(doneResultMsg);
	        	else
					LOG.e(NAME, String.format("sendError: invalid DONE callback context (already closed) -- could not send error: %s (code %d)", message, errorCode));
	        	
				// "in-valdiate" DONE callback (since it is closed now!):
				this.doneCallbackContext = null;
				
			}//END synchronized(doneLock)
        	
		}//END if(isStopped)
    }
    
    private void setResultType(JSONObject result, boolean isInterim) throws JSONException{
    	
    	//TODO implement real decision for FINAL, INTERMEDIATE, INTERIM
    	String type = isInterim ? ResultTypes.INTERIM.toString() : 
    			this.isFinal? ResultTypes.FINAL.toString() : ResultTypes.INTERMEDIATE.toString();
    			
    	result.put(FIELD_RESULT_TYPE, type);
    	
    }
    
    private void setResultMatches(JSONObject result, ArrayList<String> matches, ArrayList<String> unstableMatches, ArrayList<Float> scores) throws JSONException{
    	
    	int len = matches.size();
    	int len2 = scores.size();
    	int len3 = unstableMatches != null? unstableMatches.size() : 0;
    	
    	if(len > 0){
    		
    		//add main / first result at "root" level of the return-object:
            result.put(FIELD_RECOGNITION_RESULT, matches.get(0));
            
            if(len2 > 0){
            	result.put(FIELD_RECOGNITION_SCORE, scores.get(0));
            }
            
            //add alternative results
            if(len > 1){
            	
	            JSONArray alternatives = new JSONArray();
	            
	            for(int i = 1; i < len; ++i){
	            	
	            	JSONObject altResult = new JSONObject();
	            	altResult.put(FIELD_RECOGNITION_RESULT, matches.get(i));
	            	
	                if(len2 > i){
	                	altResult.put(FIELD_RECOGNITION_SCORE, scores.get(i));
	                } else {
	                	printw("no score for result at index "+i);
	                }
	                
	                //add unstable results, if available & not empty (e.g. omit 0-length Strings)
	                if(len3 > i && unstableMatches.get(i) != null && unstableMatches.get(i).length() > 0){
	                	altResult.put(FIELD_RECOGNITION_RESULT_UNSTABLE, unstableMatches.get(i));
	                }
	                
	                alternatives.put(altResult);
	            }
	            
	            result.put(FIELD_RECOGNITION_RESULT_ALTERNATIVES, alternatives);
            
            }//END: add alternatives
    	}
    	
    	//add unstable for main/root, if available
    	if(len3 > 0 && unstableMatches.get(0) != null && unstableMatches.get(0).length() > 0){
    		
    		result.put(FIELD_RECOGNITION_RESULT_UNSTABLE, unstableMatches.get(0));
    	}
    }
    
	private ArrayList<Float> toList(float[] a){
    	ArrayList<Float> l = new ArrayList<Float>(a.length);
    	for(float f: a){
    		l.add(f);
    	}
    	return l;
    }


	public Vibrator getHapticPrompt() {
		return hapticPrompt;
	}


	public void setHapticPrompt(Vibrator hapticPrompt) {
		this.hapticPrompt = hapticPrompt;
	}
	
}