package de.dfki.iui.mmir.plugins.speech.android;

import java.util.HashMap;
import java.util.Locale;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.speech.tts.UtteranceProgressListener;

@SuppressWarnings("deprecation")
public class AndroidSpeechSynthesizer extends CordovaPlugin {

	private static final String ACTION_SILENCE = "silence";

	private static final int DEFAULT_SILENCE_DURATION = 500;

	private static final String ACTION_TTS = "speak";
	private static final String ACTION_STARTUP = "startup";
	private static final String ACTION_SHUTDOWN = "shutdown";
	private static final String ACTION_GET_LANGUAGE = "getLanguage";
	private static final String ACTION_IS_LANGUAGE_AVAILABLE = "isLanguageAvailable";
	private static final String ACTION_SET_LANGUAGE = "setLanguage";
	private static final String ACTION_CANCEL_TTS = "cancel";

	private static final int SDK_VERSION = Build.VERSION.SDK_INT;
	
	private static final String PLUGIN_NAME = "AndroidSpeechSynthesizer";
	private static final Locale DEFAULT_LANGUAGE = Locale.US;
	// private static final String LOG_TAG = "TTS";
	private static final int STOPPED = 0;
	private static final int INITIALIZING = 1;
	private static final int STARTED = 2;
	private TextToSpeech mTts = null;
	private int state = STOPPED;
	
	

	public static final String MSG_TYPE_FIELD = "type";
	public static final String MSG_DETAILS_FIELD = "message";
	public static final String MSG_ERROR_CODE_FIELD = "code";
	public static final String MSG_TTS_STARTED = "TTS_BEGIN";
	public static final String MSG_TTS_DONE = "TTS_DONE";
	public static final String MSG_TTS_ERROR = "TTS_ERROR";
	
	//"singleton" pattern: only 1 speech at a time (i.e.: no queuing)
	// -> this is the signifier for TTS-active state (set/reset in UtteranceComletedListener)
	private boolean isSpeaking = false;
	private boolean isCanceled = false;
	
	private int speechId = 0;

//	private String startupCallbackId = "";

	@SuppressLint("NewApi")
	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
		
		boolean isValidAction = true;
		PluginResult result = null;

		//TODO parameterize language/Locale setting
		
		try {
			
			if (ACTION_TTS.equals(action)) {
				
				boolean isLangOk = true;
				if(args.length() > 1){
					isLangOk = setLanguage(args.get(1));
				}
				
				if (args.length() > 0 && isLangOk && isReady()) {
					
					Object ttsText = args.get(0);
					JSONArray sentences =  null;
					int len = -1;
					if(ttsText instanceof JSONArray){
						sentences = (JSONArray) ttsText;
						len = sentences.length();
					}
					
					//TODO for now, always stop current TTS (if there is one active), before starting a new one
					//		--> "singleton" TTS
					//TODO keep using instance of SpeechCompletedListener + map [utteranceId]->[callbackContext], then 
					//			(1) notify accordingly in SpeechCompletedListener.onUtteranceCompleted(utteranceId)...
					//			(2) remove entry in map, when speech has completed
					//		-> impl. similar/extended UtteranceProgressListener for API level >= 15
					
					
					if(isSpeaking){
						isCanceled = true;
						mTts.stop();
					}

					//TODO re-use SpeechCompletedListener instance?
					if (SDK_VERSION >= 15) {
						mTts.setOnUtteranceProgressListener(new SpeechCompletedListener(callbackContext, len));
						//NOTE isSpeaking = true is set in SpeechCompletedListener.onStart
					}
					else {
						//for API level < 15 use OnUtteranceCompletedListener
						mTts.setOnUtteranceCompletedListener(new SpeechCompletedListener(callbackContext, len));
						isSpeaking = true;//"singleton" signifier for TTS-active; reset in onUtteranceComleted of listener						
					}

					isCanceled = false;
					
					if(sentences != null){
						result = queueSentence((JSONArray)ttsText);
					} else if (ttsText != null) {
						result = queueText(ttsText.toString());
					} else {
						result = new PluginResult(PluginResult.Status.ERROR, "invalid argument: cannot invoke TTS for NULL argument.");
					}
					
					
				} else {
					
					JSONObject error = new JSONObject();
					if(args.length() < 1){
						error.put(MSG_DETAILS_FIELD, "argument(s) missing: specified no text for TTS.");
					} else if(isLangOk){
						//TTS service is not ready yet
						error.put(MSG_DETAILS_FIELD, "TTS service is still initialzing.");
						error.put(MSG_ERROR_CODE_FIELD, AndroidSpeechSynthesizer.INITIALIZING);
					} else {
						//something went wrong while setting the language:
						if(args.length() > 1)
							error.put(MSG_DETAILS_FIELD, "TTS service cannot synthesise language "+args.getString(1)+".");
						else
							error.put(MSG_DETAILS_FIELD, "TTS service requested for UNKNOWN language.");
					}
					result =  new PluginResult(PluginResult.Status.ERROR, error);
				}
				
			} else if (ACTION_SILENCE.equals(action)) {
				
				if (isReady()) {
					isCanceled = false;
					long duration = args.length() > 0? args.getLong(0) : DEFAULT_SILENCE_DURATION;
					mTts.playSilence(duration, TextToSpeech.QUEUE_ADD, null);
					result =  new PluginResult(PluginResult.Status.OK);
				} else {
					JSONObject error = new JSONObject();
					error.put(MSG_DETAILS_FIELD, "TTS service is still initializing.");
					error.put(MSG_ERROR_CODE_FIELD, AndroidSpeechSynthesizer.INITIALIZING);
					result = new PluginResult(PluginResult.Status.ERROR, error);
				}
				
			} else if (ACTION_STARTUP.equals(action)) {
				
				if (mTts == null) {
//					this.startupCallbackId = callbackId;
					state = AndroidSpeechSynthesizer.INITIALIZING;
					
					mTts = new TextToSpeech(this.cordova.getActivity(), new TTSInitListener(callbackContext));
					mTts.setLanguage(DEFAULT_LANGUAGE);

					LOG.i(PLUGIN_NAME,"TTS is initializing...");
				}
				result =  new PluginResult(PluginResult.Status.OK, AndroidSpeechSynthesizer.INITIALIZING);
				result.setKeepCallback(true);
				
			} else if (ACTION_SHUTDOWN.equals(action)) {
				
				if (mTts != null) {
					mTts.shutdown();
				}
				result = new PluginResult(PluginResult.Status.OK);
				
			} else if (ACTION_GET_LANGUAGE.equals(action)) {
				
				if (mTts != null) {
					result = new PluginResult(PluginResult.Status.OK, mTts.getLanguage().toString());
				}
				
			} else if (ACTION_IS_LANGUAGE_AVAILABLE.equals(action)) {
				
				if (mTts != null) {
					Locale loc = new Locale(args.getString(0));
					int available = mTts.isLanguageAvailable(loc);
					result = new PluginResult(PluginResult.Status.OK, (available < 0) ? "false" : "true");
				}
				
			} else if (ACTION_SET_LANGUAGE.equals(action)) {
				
				if (mTts != null) {
					boolean success = setLanguage(args.getString(0));
					result = new PluginResult(PluginResult.Status.OK, success ? "false" : "true");
				}
				
			}
			else if (ACTION_CANCEL_TTS.equals(action)) {
				
				if (mTts != null && isReady()) {
					isCanceled = true;
					int cancelResult = mTts.stop();
					result =  new PluginResult(PluginResult.Status.OK, cancelResult == TextToSpeech.SUCCESS? "stopped" : "failed");//TODO in case of ERROR return status.error
				} else if (mTts != null && !isReady()) {
					result =  new PluginResult(PluginResult.Status.OK, "Cancel: cannot cancel TTS (engine not initialized yet).");
				} else {
					JSONObject error = new JSONObject();
					//TODO create appropriate message ...
					error.put(MSG_DETAILS_FIELD, "TTS service is still initialzing.");
					error.put(MSG_ERROR_CODE_FIELD, AndroidSpeechSynthesizer.INITIALIZING);
					result = new PluginResult(PluginResult.Status.ERROR, error);
				}
			}
			else {
				isValidAction = false;
			}
			
		} catch (JSONException e) {
			
			e.printStackTrace();
			String msg = String.format("Error during '%s': could not process JSON arguments or response because of %s", action, e);
			result =  new PluginResult(PluginResult.Status.JSON_EXCEPTION, msg);
			
		}
		
		callbackContext.sendPluginResult(result);
		
		return isValidAction;
	}

	private PluginResult queueText(String text) {
		//create utterance ID
		
		PluginResult result = null;
		int idNumber = getNextIdNumber();
		String utteranceId = getId(idNumber);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
		int ttsResult;
		Exception error = null;
		try {
			ttsResult = doQueue(text, TextToSpeech.QUEUE_FLUSH, params);//TODO: should this be QUEUE_ADD? configurable/parameterized?
			if(ttsResult == TextToSpeech.SUCCESS){
				result =  doCreateSpeakSuccessResult(idNumber);
				result.setKeepCallback(true);
			}
		} catch(Exception e){
			error = e;
			ttsResult = TextToSpeech.ERROR;
		}
		
		if(ttsResult == TextToSpeech.ERROR){
			result = new PluginResult(PluginResult.Status.ERROR, error.toString());//TODO include error's stacktrace?
		}
		return result;
	}
	
	private PluginResult queueSentence(JSONArray sentences) {
		
		int ttsResult = TextToSpeech.ERROR;
		Exception error = null;
		PluginResult result = null;
		int i=0;
		
		int utteranceId = getNextIdNumber();
		for(int size = sentences.length(); i < size; ++i){
			
			Object obj = null;
			try {
				obj = sentences.get(i);
			} catch (JSONException e1) {
				error = e1;
				ttsResult = TextToSpeech.ERROR;
				break;
			}
			
			if(obj != null){
				
				String text = obj.toString();
				
				//create utterance ID
				String constUtteranceId = getNextId(utteranceId, i+1, size);
				HashMap<String, String> params = new HashMap<String, String>();
				params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, constUtteranceId);
				
				//for first entry: FLUSH queue (-> see queueText(..))
				int queueMode = i==0? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD;//TODO: should this always be QUEUE_ADD? configurable/parameterized?
				
				try {
					
					if(text == null || text.length() < 1){
						ttsResult = mTts.playSilence(DEFAULT_SILENCE_DURATION, queueMode, params);
					} else {
						ttsResult = doQueue(text, queueMode, params);
					}
					if(ttsResult == TextToSpeech.SUCCESS){
						result =  doCreateSpeakSuccessResult(utteranceId);
						result.setKeepCallback(true);
					}
				} catch(Exception e){
					error = e;
					ttsResult = TextToSpeech.ERROR;
					break;
				}
				
				if(ttsResult == TextToSpeech.ERROR){
					break;
				}
			}
		}
		
		if(ttsResult == TextToSpeech.ERROR){
			String msg = "Could not add entry "+i+" to TTS queue";
			if(error != null){
				msg += " Error: " + error.toString();//TODO include error's stacktrace?
			}
			result = new PluginResult(PluginResult.Status.ERROR, msg);
		}
		return result;
	}
	
	private PluginResult doCreateSpeakSuccessResult(int id){
		
		if (SDK_VERSION >= 15) {
			return new PluginResult(PluginResult.Status.NO_RESULT);
		} else {
			//signal "started" for onStart callback in API level < 15 
			//	-> i.e. no support for onStart in SpeechListener, "pretend" that is starts immediately
			String utteranceId = getId(id);
			return createOnStartResult(utteranceId);
		}
		
	}

	private int doQueue(String text, int queueType, HashMap<String, String> params) {
		return mTts.speak(text, queueType, params);
	}

	private String getNextId() {
		return getId(getNextIdNumber());
	}
	
	private String getId(int id) {
		return "android-tts-"+id;
	}
	
	private int getNextIdNumber() {
		return ++speechId;
	}
	
	/**
	 * create an utterance ID where the utterance is split-up into multiple 
	 * "sub-utterances" (e.g. a paragraph that is split-up into its senctences).
	 * 
	 * @param id
	 * 			the "main" ID of the utterance (i.e. create once per utterance via {@link #getNextIdNumber()}) 
	 * @param no
	 * 			number of the "sub-utterance" (starting with 1)
	 * @param size
	 * 			amount of "sub-utterances"
	 * @return
	 */
	private String getNextId(int id, int no, int size) {
		return "android-tts-"+ id + "-"+no+"-of-"+size;
	}

	/**
	 * Set the Language (Locale) according to <code>lang</code>.
	 * 
	 * @param lang
	 * 			the language code:
	 * 				if not a String, the value will be converted
	 * 				to a String.
	 * 			If <code>null</code>, {@link JSONObject#NULL}, or an
	 * 				empty String, the language will not be changed (and
	 * 				<code>true</code> will be returned).
	 * 			Otherwise the Locale object for <code>lang</code>
	 * 				will be used to set the language of the synthesizer.
	 * @return
	 * 		<code>true</code> if <code>lang</code> is <code>null</code>
	 * 			or is empty, or if the language could successfully set to
	 * 			the Locale of <code>lang</code>.
	 * 		<code>false</code>, if the language could not be set to
	 * 			<code>lang</code>.
	 */
	private boolean setLanguage(Object lang) {
		
		String languageCode;
		
		if(lang == JSONObject.NULL)
			return true;/////////////// EARLY EXIT ////////////////////////
		
		if(lang instanceof String)
			languageCode = (String) lang;
		else
			languageCode = String.valueOf(lang);
		
		if(languageCode == null || languageCode.length() < 1){
			return true;/////////////// EARLY EXIT ////////////////////////
		}
		
		Locale loc = new Locale(languageCode);
		int available = mTts.setLanguage(loc);
		LOG.d(PLUGIN_NAME, String.format("set language to %s: %s", languageCode, getLangMessage(available)));
		return (available >= TextToSpeech.LANG_AVAILABLE);
	}

	/**
	 * Is the TTS service ready to play yet?
	 * 
	 * @return
	 */
	private boolean isReady() {
		return (state == AndroidSpeechSynthesizer.STARTED) ? true : false;
	}

	
	private class TTSInitListener  implements OnInitListener {
		
		private CallbackContext callbackContext;
		public TTSInitListener(CallbackContext callbackContext){
			this.callbackContext = callbackContext;
		}
		/**
		 * Called when the TTS service is initialized.
		 * 
		 * @param status
		 */
		@Override
		public void onInit(int status) {
			if (status == TextToSpeech.SUCCESS) {
				state = AndroidSpeechSynthesizer.STARTED;
				//TODO implement / activate this for CALLBACK onStart (when supported by javascript interface)
				PluginResult result = new PluginResult(PluginResult.Status.OK, AndroidSpeechSynthesizer.STARTED);
				result.setKeepCallback(false);
//				this.success(result, this.startupCallbackId);
				this.callbackContext.sendPluginResult(result);
			} else if (status == TextToSpeech.ERROR) {
				state = AndroidSpeechSynthesizer.STOPPED;
				PluginResult result = new PluginResult(PluginResult.Status.ERROR, AndroidSpeechSynthesizer.STOPPED);
				result.setKeepCallback(false);
//				this.error(result, this.startupCallbackId);
				this.callbackContext.sendPluginResult(result);
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	private class SpeechCompletedListener extends UtteranceProgressListener implements OnUtteranceCompletedListener {
		
		private boolean isError;
		private int utterancesCount;
		private int doneCount;
		private int startedCount;
		private int activeCount;
		private CallbackContext callbackContext;
//		public SpeechCompletedListener(CallbackContext callbackContext){
		
//			this(callbackContext, -1);
//		}
		
		public SpeechCompletedListener(CallbackContext callbackContext, int utteranceParts){
			this.callbackContext = callbackContext;
			this.utterancesCount = utteranceParts;
			this.isError = false;
			this.doneCount = 0;
		}
		
		//API level >= 15
		@Override
		public void onStart(String utteranceId) {
			// TODO Auto-generated method stub
			//TODO implement / activate this for CALLBACK onStart (when supported by javascript interface)
			isSpeaking = true;
			
			++this.activeCount;
			++this.startedCount;
			
			//send callback if either
			// (1) there is only one utterance to read, or
			// (2) this is the first time an utterance was started
			if(this.utterancesCount == -1 || this.startedCount == 1){
				
				PluginResult result = createOnStartResult(utteranceId);
				this.callbackContext.sendPluginResult(result);
				
			} else {
				
				LOG.i(PLUGIN_NAME, String.format("started utterance '%s' (%d of %d)", utteranceId, this.startedCount, this.utterancesCount));
				
				//TODO send messages for prepare/ready mechanism?
			}
		}
		

		//API level < 15
		@Override
		public void onUtteranceCompleted(String utteranceId) {
			this._onCompleted(utteranceId);
		}

		//API level >= 15
		@Override
		public void onDone(String utteranceId) {
			this._onCompleted(utteranceId);
		}
		
		private void _onCompleted(String utteranceId){
			
			--this.activeCount;
			++this.doneCount;
			
			
			if(this.utterancesCount == -1 || this.doneCount == this.utterancesCount || isCanceled){

				isSpeaking = false;

				String msg = "Speech (id "+utteranceId+") finished.";
				
				LOG.d(PLUGIN_NAME, msg);
				
				JSONObject doneResult = createResultObj(MSG_TTS_DONE, msg);
				PluginResult result = null;
				if(doneResult != null){
					result = new PluginResult(PluginResult.Status.OK, doneResult);
				} else {
					result = new PluginResult(PluginResult.Status.OK, msg);
				}
				
				
				result.setKeepCallback(false);
				this.callbackContext.sendPluginResult(result);
				
			} else {
				
				LOG.i(PLUGIN_NAME, String.format("finished utterance '%s' (%d of %d)", utteranceId, this.startedCount, this.utterancesCount));
				
			}
		}

		//API level < 21
		@SuppressLint("Override")
		public void onError(String utteranceId) {
			this._onError(utteranceId);
		}
		
		//API level >= 21
		@SuppressLint("Override")
		@TargetApi(21)
		public void onError(String utteranceId, int errorCode) {//API level 21
			this._onError(utteranceId, errorCode);
		}
		
		private void _onError(String utteranceId){ this._onError(utteranceId, 0); }
		private void _onError(String utteranceId, int errorCode){
			isError = true;
			isSpeaking = false;
			
			String msg = "Error during speech (id "+utteranceId+").";
			if(errorCode < 0)
				msg += " Cause ("+errorCode+"): " + getErrorMessage(errorCode);
			
			JSONObject errorResult = createResultObj(MSG_TTS_ERROR, msg);
			PluginResult result = null;
			if(errorResult != null){
				result = new PluginResult(PluginResult.Status.ERROR, errorResult);
			} else {
				result = new PluginResult(PluginResult.Status.ERROR, msg);
			}
			
			result.setKeepCallback(false);
			this.callbackContext.sendPluginResult(result);
		}
		
	}
	
	private static PluginResult createOnStartResult(String utteranceId){
		
		String msg = "Speech (id "+utteranceId+") started.";
		
		JSONObject beginResult = createResultObj(MSG_TTS_STARTED, msg);
		PluginResult result = null;
		if(beginResult != null){
			result = new PluginResult(PluginResult.Status.OK, beginResult);
		} else {
			result = new PluginResult(PluginResult.Status.OK, msg);
		}
		result.setKeepCallback(true);
		
		return result;
	}
	
	private static JSONObject createResultObj(String msgType, String msgDetails) {
		try {
			
			JSONObject msg = new JSONObject();
			msg.putOpt(MSG_TYPE_FIELD, msgType);
			msg.putOpt(MSG_DETAILS_FIELD, msgDetails);
			return msg;
			
		} catch (JSONException e) {
			//this should never happen, but just in case: print error message
			LOG.e(PLUGIN_NAME, "could not create '"+msgType+"' reply for message '"+msgDetails+"'", e);
		}
		return null;
	}
	
	private static String getErrorMessage(int errorCode){
		if(errorCode == TextToSpeech.ERROR)						 //Denotes a generic operation failure.
			return "generic operation failure";
		if(errorCode == -8)//TextToSpeech.ERROR_INVALID_REQUEST) //Denotes a failure caused by an invalid request.
			return "invalid request";
		if(errorCode == -6)//TextToSpeech.ERROR_NETWORK) 		 //Denotes a failure caused by a network connectivity problems.
			return "network connectivity problems";
		if(errorCode == -7)//TextToSpeech.ERROR_NETWORK_TIMEOUT) //Denotes a failure caused by network timeout.
			return "network timeout";
		if(errorCode == -9)//TextToSpeech.ERROR_NOT_INSTALLED_YET) //Denotes a failure caused by an unfinished download of the voice data.
			return "unfinished download of the voice data";
		if(errorCode == -5)//TextToSpeech.ERROR_OUTPUT) 		 //Denotes a failure related to the output (audio device or a file).
			return "output problem (audio device or a file)";
		if(errorCode == -4)//TextToSpeech.ERROR_SERVICE) 		 //Denotes a failure of a TTS service.
			return "TTS service";
		if(errorCode == -3)//TextToSpeech.ERROR_SYNTHESIS) 		 //Denotes a failure of a TTS engine to synthesize the given input. )
			return "TTS engine to synthesize the given input";
		
		return "unknow error code: "+errorCode;
	}
	
	private static String getLangMessage(int returnCode){
		
		if(returnCode == TextToSpeech.LANG_MISSING_DATA)	//Denotes the language data is missing.
			return "language data is missing";
		if(returnCode == TextToSpeech.LANG_NOT_SUPPORTED)	//Denotes the language is not supported.
			return "language is not supported";

		if(returnCode == TextToSpeech. LANG_COUNTRY_VAR_AVAILABLE ) //Denotes the language is available exactly as specified by the locale.
			return "language is available exactly as specified by the locale";
		if(returnCode == TextToSpeech.LANG_COUNTRY_AVAILABLE )		//Denotes the language is available for the language and country specified by the locale, but not the variant. 
			return "language is available for the language and country specified by the locale, but not the variant";
		if(returnCode == TextToSpeech.LANG_AVAILABLE)				//Denotes the language is available for the language by the locale, but not the country and variant. 
			return "language is available for the language by the locale, but not the country and variant";
				
		return "unknow return code: "+returnCode;
	}
	/**
	 * Clean up the TTS resources
	 */
	@Override
	public void onDestroy() {
		if (mTts != null) {
			mTts.shutdown();
		}
	}
}
