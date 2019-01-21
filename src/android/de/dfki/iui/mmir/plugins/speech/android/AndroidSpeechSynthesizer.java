package de.dfki.iui.mmir.plugins.speech.android;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;

@SuppressWarnings("deprecation")
public class AndroidSpeechSynthesizer extends CordovaPlugin {

	private static final String ACTION_SILENCE = "silence";

	private static final long DEFAULT_SILENCE_DURATION = 500L;

	private static final String ACTION_TTS = "speak";
	private static final String ACTION_STARTUP = "startup";
	private static final String ACTION_SHUTDOWN = "shutdown";
	private static final String ACTION_GET_LANGUAGE = "getLanguage";
	private static final String ACTION_IS_LANGUAGE_AVAILABLE = "isLanguageAvailable";
	private static final String ACTION_SET_LANGUAGE = "setLanguage";
	private static final String ACTION_GET_VOICE = "getVoice";
	private static final String ACTION_SET_VOICE = "setVoice";
	private static final String ACTION_CANCEL_TTS = "cancel";
	private static final String ACTION_GET_DEFAULT_LANGUAGE = "defaultLanguage";
	private static final String ACTION_GET_DEFAULT_VOICE = "defaultVoice";
	private static final String ACTION_GET_LANGUAGES = "languageList";
	private static final String ACTION_GET_VOICES = "voiceList";

	private static final int SDK_VERSION = Build.VERSION.SDK_INT;

	private static final String PLUGIN_NAME = "AndroidTTS";
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

	private VoiceFilterSelection lastVoiceSelection = null;

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

		try {

			if (ACTION_TTS.equals(action)) {

				boolean isLangOk = true;
				if(args.length() > 1){
					isLangOk = setLanguage(args.get(1));
				}

				//parse optional arguments:
				long pause = DEFAULT_SILENCE_DURATION;
				try {
					if (args.length() > 2) {
						// Optional pause duration
						long num = args.getLong(2);
						if(num >= 0l){
							//ignore negative values
							pause = num;
						}
					}
				}
				catch (Exception e) {
					Log.e(PLUGIN_NAME, String.format(ACTION_TTS + " exception: %s", e.toString()));
				}

				if(args.length() > 3){
					try {
						// Optional voice
						String voice = args.getString(3);
						boolean success = setVoice(voice);
						if(!isLangOk && success){
							isLangOk = true;
						}
					}
					catch (Exception e) {
						Log.e(PLUGIN_NAME, String.format(ACTION_TTS + " exception: %s", e.toString()));
					}
				}

				//prepare & read text:
				if (args.length() > 0 && isLangOk && isReady()) {

					Object ttsText = args.get(0);
					JSONArray sentences =  null;
					int len = -1;
					if(ttsText instanceof JSONArray){
						sentences = (JSONArray) ttsText;
						len = calcSpeechPartsCount(sentences.length());
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
						result = queueSentence((JSONArray)ttsText, pause);
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

			} else if (ACTION_CANCEL_TTS.equals(action)) {

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
					String lang;
					if(SDK_VERSION >= 21){
						lang = mTts.getVoice().getLocale().toString();
					} else {
						lang = mTts.getLanguage().toString();
					}
					result = new PluginResult(PluginResult.Status.OK, lang);
				}

			} else if (ACTION_GET_DEFAULT_LANGUAGE.equals(action)) {

				if (mTts != null) {
					if(SDK_VERSION < 18){
						result = new PluginResult(PluginResult.Status.ERROR, String.format("Cannot query default language: API is %d, but requires >= API 18.", SDK_VERSION));
					} else {
						String lang;
						if(SDK_VERSION >= 21){
							lang = mTts.getDefaultVoice().getLocale().toString();
						} else {
							lang = mTts.getDefaultLanguage().toString();
						}
						result = new PluginResult(PluginResult.Status.OK, lang);
					}
				}

			} else if (ACTION_GET_DEFAULT_VOICE.equals(action)) {

				if (mTts != null) {
					if(SDK_VERSION >= 21){
						result = new PluginResult(PluginResult.Status.OK, mTts.getDefaultVoice().getName());
					} else {
						result = new PluginResult(PluginResult.Status.ERROR, String.format("Cannot query default voice: API is %d, but requires >= API 21.", SDK_VERSION));
					}
				}

			} else if (ACTION_GET_LANGUAGES.equals(action)) {

				if (mTts != null) {
					if(SDK_VERSION >= 21){
						JSONArray list = new JSONArray();
						for(Locale l : mTts.getAvailableLanguages()){
							list.put(l.toString());
						}

						if (mTts != null) {
							result = new PluginResult(PluginResult.Status.OK, list);
						}
					} else {
						result = new PluginResult(PluginResult.Status.ERROR, String.format("Cannot query available languages: API is %d, but requires >= API 21.", SDK_VERSION));
					}
				}

			} else if (ACTION_GET_VOICES.equals(action)) {

				if (mTts != null) {
					if(SDK_VERSION >= 21){
						JSONArray list = new JSONArray();
						for(Voice v : mTts.getVoices()){
							list.put(v.getName());
						}

						if (mTts != null) {
							result = new PluginResult(PluginResult.Status.OK, list);
						}
					} else {
						result = new PluginResult(PluginResult.Status.ERROR, String.format("Cannot query available voices: API is %d, but requires >= API 21.", SDK_VERSION));
					}
				}

			} else if (ACTION_IS_LANGUAGE_AVAILABLE.equals(action)) {

				if (mTts != null) {
					Locale loc = getLocale(args.getString(0), true);
					int available = mTts.isLanguageAvailable(loc);
					result = new PluginResult(PluginResult.Status.OK, (available < 0) ? "false" : "true");
				}

			} else if (ACTION_SET_LANGUAGE.equals(action)) {

				if (mTts != null) {
					boolean success = setLanguage(args.getString(0));
					result = new PluginResult(PluginResult.Status.OK, success ? "true" : "false");
				}

			} else if (ACTION_GET_VOICE.equals(action)) {

				if (mTts != null) {
					if(SDK_VERSION >= 21){
						result = new PluginResult(PluginResult.Status.OK, mTts.getVoice().getName());
					} else {
						result = new PluginResult(PluginResult.Status.ERROR, String.format("Cannot query current voice: API is %d, but requires >= API 21.", SDK_VERSION));
					}
				}

			} else if (ACTION_SET_VOICE.equals(action)) {

				if (mTts != null) {
					if(SDK_VERSION >= 21){
						boolean success = setVoice(args.getString(0));
						result = new PluginResult(PluginResult.Status.OK, success ? "true" : "false");
					} else {
						result = new PluginResult(PluginResult.Status.ERROR, String.format("Cannot set voice to %s: API is %d, but requires >= API 21.", args.getString(0), SDK_VERSION));
					}
				}

			}
			else {
				result = new PluginResult(PluginResult.Status.ERROR, "Unknown action: " + action);
				isValidAction = false;
			}

		} catch (JSONException e) {

			e.printStackTrace();
			String msg = String.format("Error during '%s': could not process JSON arguments or response because of %s", action, e);
			result =  new PluginResult(PluginResult.Status.JSON_EXCEPTION, msg);

		}

		if(result != null)
			callbackContext.sendPluginResult(result);

		return isValidAction;
	}

	private PluginResult queueText(String text) {
		//create utterance ID

		PluginResult result = null;
		int idNumber = getNextIdNumber();
		String utteranceId = getId(idNumber);
		Bundle params = null;
		//	 	//TODO? supported bundle params:
		//	 	if(SDK_VERSION >= 21){
		//	 		params = createParamsBundle(volume, streamId, pan);
		//	 	}
		int ttsResult;
		Exception error = null;
		try {
			ttsResult = doQueue(text, TextToSpeech.QUEUE_FLUSH, null, params, utteranceId);//TODO: should this be QUEUE_ADD? configurable/parameterized?
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

	private PluginResult queueSentence(JSONArray sentences, long silenceDuration) {

		int ttsResult = TextToSpeech.ERROR;
		Exception error = null;
		PluginResult result = null;
		int i=0;
		int size = sentences.length();
		int ttsParts = calcSpeechPartsCount(size);

		int utteranceId = getNextIdNumber();
		for(; i < size; ++i){

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
				String constUtteranceId = getNextId(utteranceId, i+1, ttsParts);

				Bundle paramsBundle = null;
				//			 	//TODO? supported bundle params:
				//			 	if(SDK_VERSION >= 21){
				//			 		paramsBundle = createParamsBundle(volume, streamId, pan);
				//			 	}

				//for first entry: FLUSH queue (-> see queueText(..))
				int queueMode = i==0? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD;//TODO: should this always be QUEUE_ADD? configurable/parameterized?

				try {

					if(text == null || text.length() < 1){

						ttsResult = doPlaySilence(silenceDuration, queueMode, null, constUtteranceId);
					} else {

						ttsResult = doQueue(text, queueMode, null, paramsBundle, constUtteranceId);
					}

					if(ttsResult == TextToSpeech.ERROR){

						break;

					} else if(ttsResult == TextToSpeech.SUCCESS && i < size - 1){
						//-> insert pause between "sentences"

						String pauseUtteranceId = getNextId(utteranceId, i+1, ttsParts);
						ttsResult = doPlaySilence(silenceDuration, TextToSpeech.QUEUE_ADD, null, pauseUtteranceId);

						//if pause did cause error: log the error
						if(ttsResult == TextToSpeech.ERROR){

							Log.e(PLUGIN_NAME, "could not insert pause of "+silenceDuration+" ms for sentence at index "+i);

							//reset error status
							//   for evaluation of PluginResult
							//   -> only consider "real text errors" for that
							ttsResult = TextToSpeech.SUCCESS;
						}
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

		if(ttsResult == TextToSpeech.SUCCESS){

			result =  doCreateSpeakSuccessResult(utteranceId);
			result.setKeepCallback(true);

		} else if(ttsResult == TextToSpeech.ERROR){
			String msg = "Could not add entry "+i+" to TTS queue";
			if(error != null){
				msg += " Error: " + error.toString();
			}
			LOG.e(PLUGIN_NAME, msg, error);
			result = new PluginResult(PluginResult.Status.ERROR, msg);
		}
		return result;
	}

	/**
	 * HELPER calculate the count of speech-parts for a list (i.e. including silences etc.)
	 * @param listSize
	 * 			the list/array size of speech parts
	 * @return
	 */
	private int calcSpeechPartsCount(int listSize) {
		return 2 * listSize - 1;//after each speech-part, a silence will be added (except for the last one)
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

	private int doQueue(String text, int queueType, HashMap<String, String> params, Bundle paramsBundle, String utteranceId) {
		if(SDK_VERSION < 21){
			if(params == null && utteranceId != null){
				params = createParamsMap(utteranceId);
			}
			return doQueue_old(text, queueType, params, utteranceId);
		} else {
			return doQueue_api21(text, queueType, paramsBundle, utteranceId);
		}
	}

	//@TargetApi(20)//for API level <= 20
	private int doQueue_old(String text, int queueType, HashMap<String, String> params, String utteranceId) {
		return mTts.speak(text, queueType, params);
	}

	@TargetApi(21)
	private int doQueue_api21(String text, int queueType, Bundle params, String utteranceId) {
		return mTts.speak(text, queueType, params, utteranceId);
	}


	private int doPlaySilence(long duration, int queueMode, HashMap<String, String> params, String utteranceId){
		if(SDK_VERSION < 21){
			if(params == null && utteranceId != null){
				params = createParamsMap(utteranceId);
			}
			return doPlaySilence_old(duration, queueMode, params);
		} else {
			return doPlaySilence_api21(duration, queueMode, utteranceId);
		}
	}

	//@TargetApi(20)//for API level <= 20
	private int doPlaySilence_old(long duration, int queueMode, HashMap<String, String> params){
		return  mTts.playSilence(duration, queueMode, params);
	}

	@TargetApi(21)
	private int doPlaySilence_api21(long duration, int queueMode, String utteranceId){
		return  mTts.playSilentUtterance(duration, queueMode, utteranceId);
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

	private HashMap<String, String> createParamsMap(String utteranceId){
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
		return params;
	}

	private Bundle createParamsBundle(float relativeVolume){
		return null;
		//	 	//TODO? supported bundle params: KEY_PARAM_STREAM, KEY_PARAM_VOLUME, KEY_PARAM_PAN
		//	 	Bundle paramsBundle = new Bundle();
		//	 	paramsBundle.putFloat(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME, relativeVolume);
		//	 	paramsBundle.putInt(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_STREAM, audioStream);
		//	 	paramsBundle.putFloat(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_PAN, pan);
		//	 	return paramsBundle;
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

		Locale loc = getLocale(lang, false);

		if(loc == null)
			return true;/////////////// EARLY EXIT ////////////////////////

		int available = mTts.setLanguage(loc);

		if(LOG.isLoggable(LOG.DEBUG)){
			LOG.d(PLUGIN_NAME, String.format("set language to %s: %s", loc, getLangMessage(available)));
		}

		return (available >= TextToSpeech.LANG_AVAILABLE);
	}

	/**
	 * get the Language (Locale) according to <code>lang</code>.
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
	 * @param isGetDefault
	 *      if <code>true</code>, always returns a locale
	 *        even if <code>lang</code> is invalid
	 *        (then returns the currently set Locale)
	 * @return
	 * 		  if <code>isGetDefault</code> is <code>true</code>, always returns
	 * 	      a Locale object, otherwise returns <code>null</code> if
	 * 		    <code>lang</code> is <code>null</code> or is empty.
	 */
	@TargetApi(21)
	private Locale getLocale(Object lang, boolean isGetDefault){

		String languageCode;

		if(lang == JSONObject.NULL)
			return !isGetDefault? null : SDK_VERSION >= 21? mTts.getVoice().getLocale() : mTts.getLanguage();/////////////// EARLY EXIT ////////////////////////

			if(lang instanceof String)
				languageCode = (String) lang;
			else
				languageCode = String.valueOf(lang);

			if(languageCode == null || languageCode.length() < 1){
				return !isGetDefault? null : SDK_VERSION >= 21? mTts.getVoice().getLocale() : mTts.getLanguage();/////////////// EARLY EXIT ////////////////////////
			}

			String[] parts = languageCode.split("-|_");
			int len = parts.length;
			Locale loc;
			if(len == 3){
				loc = new Locale(parts[0], parts[1], parts[2]);
			} else if(len == 2){
				loc = new Locale(parts[0], parts[1]);
			} else {
				if(len != 1 && LOG.isLoggable(LOG.WARN)){
					LOG.w(PLUGIN_NAME, String.format("unknown format for language code: %s"), languageCode);
				}
				loc = new Locale(languageCode);
			}

			return loc;
	}

	/**
	 * Set the Language (Locale) according to <code>lang</code>.
	 *
	 * @param voice
	 * 			the voice name, or a feature value (e.g. gender "male" or "female").
	 * 			If <code>null</code>, {@link JSONObject#NULL}, or an
	 * 				empty String, the language will not be changed (and
	 * 				<code>true</code> will be returned).
	 * @return
	 * 		<code>true</code> if <code>voice</code> is <code>null</code>
	 * 			or is empty, or if the voice could successfully be set to
	 * 			<code>voice</code>.
	 * 		<code>false</code>, if the voice could not be set to
	 * 			<code>voice</code>.
	 */
	@TargetApi(21)
	private boolean setVoice(Object voice) {

		String voiceName;

		if(voice == JSONObject.NULL)
			return true;/////////////// EARLY EXIT ////////////////////////

		if(voice instanceof String)
			voiceName = (String) voice;
		else
			voiceName = String.valueOf(voice);

		if(voiceName == null || voiceName.length() < 1){
			// no (valid) parameter: succeeded by not setting voice
			return true;/////////////// EARLY EXIT ////////////////////////
		}

		Voice v = getVoice(voiceName);
		if(v == null){
			v = selectBestVoice(voiceName);
		}

		if(v == null){
			// could not set set voice according to parameter -> failed
			this.lastVoiceSelection = null;
			return false;/////////////// EARLY EXIT ////////////////////////
		}

		int available = mTts.setVoice(v);
		LOG.d(PLUGIN_NAME, String.format("set voice to %s: %s", voiceName, getLangMessage(available)));
		return (available == TextToSpeech.SUCCESS);
	}

	@TargetApi(21)
	private Voice getVoice(String voiceName){

		if(SDK_VERSION >= 21) {

			final Set<Voice> voices = mTts.getVoices();
			final int size = voices.size();

			if(this.lastVoiceSelection != null && 
					this.lastVoiceSelection.selectionSize == size &&
					this.lastVoiceSelection.locale == null &&
					this.lastVoiceSelection.filter.equals(voiceName)
					){
				final Voice v = this.lastVoiceSelection.voice;
				if(LOG.isLoggable(LOG.DEBUG)) LOG.d(PLUGIN_NAME, String.format(String.format("getVoice(\"%s\"): using voice from last selecting ->  %s (%s): quality %d | latency %d | features: %s", voiceName, v.getName(), v.getLocale().toString(), v.getQuality(), v.getLatency(), java.util.Arrays.toString(v.getFeatures().toArray()))));
				return v;
			}

			for (Voice v : voices) {
				if (voiceName.equals(v.getName())) {
					this.lastVoiceSelection = new VoiceFilterSelection(null, voiceName, v, size);
					return v;
				}
			}
		}
		return null;
	}

	/**
	 * Try to find the "best" voice using the
	 * voices' properties a sorting criteria:
	 * 	* installed
	 *  * network connection required
	 *  * country ID
	 *  * quality
	 *  * latency
	 *
	 * @param filter
	 * 			either a part of the voice name/ID (ignoring case), or a voice feature
	 * @return a Voice or null, if no voice matches filter
	 */
	@TargetApi(21)
	private Voice selectBestVoice(String filter){

		if(SDK_VERSION < 21){
			LOG.w(PLUGIN_NAME, String.format("setting voice not supported: require API >= 21, but level is %s", SDK_VERSION));
			return null;//////////////////////////// EARLY EXIT ////////////////////////////////
		}

		final Locale loc = mTts.getVoice().getLocale();
		final Set<Voice> voices = mTts.getVoices();
		final int size = voices.size();

		if(this.lastVoiceSelection != null && 
				this.lastVoiceSelection.locale != null &&
				this.lastVoiceSelection.selectionSize == size &&
				this.lastVoiceSelection.filter.equals(filter) &&
				this.lastVoiceSelection.locale.equals(loc)
				){
			final Voice v = this.lastVoiceSelection.voice;
			if(LOG.isLoggable(LOG.DEBUG)) LOG.d(PLUGIN_NAME, String.format(String.format("selectBestVoice(\"%s\"): using voice from last selecting ->  %s (%s): quality %d | latency %d | features: %s", filter, v.getName(), v.getLocale().toString(), v.getQuality(), v.getLatency(), java.util.Arrays.toString(v.getFeatures().toArray()))));
			return v;
		}

		final Voice defVoice = mTts.getDefaultVoice();
		final String notInstalledFeature = TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED;

		TreeSet<Voice> list = new TreeSet<Voice>(new Comparator<Voice>(){

			@Override
			public int compare(Voice v1, Voice v2) {

				boolean v1i = !v1.getFeatures().contains(notInstalledFeature);
				boolean v2i = !v2.getFeatures().contains(notInstalledFeature);

				if(v1i == v2i){

					if(v1.isNetworkConnectionRequired() == v1.isNetworkConnectionRequired()){

						if(v1.getLocale().getISO3Language().equals(v2.getLocale().getISO3Language()) && v1.getLocale().getISO3Country().equals(v2.getLocale().getISO3Country())){

							String isoCode = loc.getISO3Country();
							int fact = isoCode == null || isoCode.length() == 0 || v1.getLocale().getISO3Country().equals(isoCode)? 1 : 2;
							int qual = v1.getQuality() - v2.getQuality();
							if (qual == 0) {
								if (v1.getLatency() == v2.getLatency()) {
									if (defVoice != null) {
										if (defVoice.equals(v1)) {
											return fact * 1;
										} else if (defVoice.equals(v2)) {
											return fact * -1;
										} else {
											return fact * v1.getName().compareToIgnoreCase(v2.getName());
										}
									} else {
										return fact * v1.getName().compareToIgnoreCase(v2.getName());
									}
								} else {
									return fact * v1.getLatency() < v2.getLatency() ? 1 : -1;
								}
							} else {
								return fact * qual;
							}

						} else {
							if(v1.getLocale().getISO3Language().equals(v2.getLocale().getISO3Language())){

								if(v1.getLocale().getISO3Country().equals(loc.getISO3Country())){
									return 1;
								} else if(v2.getLocale().getISO3Country().equals(loc.getISO3Country())) {
									return -1;
								} else {
									return v1.getName().compareToIgnoreCase(v2.getName());
								}

							} else if(v1.getLocale().getISO3Language().equals(loc.getISO3Language())){
								return v1.getLocale().getISO3Country().equals(loc.getISO3Country())? 1 : 2;
							} else if(v2.getLocale().getISO3Language().equals(loc.getISO3Language())){
								return v2.getLocale().getISO3Country().equals(loc.getISO3Country())? -1 : -2;
							} else {
								return v1.getName().compareToIgnoreCase(v2.getName());
							}
						}

					} else {
						return v1.isNetworkConnectionRequired()? 1 : -1;
					}
				} else {
					return v1i? 1 : -1;
				}
			}

		});

		String lang = loc.getISO3Language();
		boolean isFilter = filter != null && filter.length() > 0;
		Pattern p = !isFilter? Pattern.compile(".") : Pattern.compile("\\b" + filter.toLowerCase() + "(?:\\b|_)");
		Pattern pFeat = !isFilter? null : Pattern.compile("=\\b" + filter.toLowerCase() + "(?:\\b)");
		LOG.d(PLUGIN_NAME, String.format("FILTER voice-list: \"%s\" -> %s", filter, p));
		for(Voice v : voices){

			if(LOG.isLoggable(LOG.DEBUG)) LOG.d(PLUGIN_NAME, String.format("UNFILTERD -> %s (%s): quality %d | latency %d | features: %s", v.getName(), v.getLocale().toString(), v.getQuality(), v.getLatency(), java.util.Arrays.toString(v.getFeatures().toArray())));

			if(lang.equals(v.getLocale().getISO3Language())){
				//-> voice must be for selected language

				Matcher m = p.matcher(v.getName().toLowerCase());
				if((m.find() || v.getFeatures().contains(filter))) {

					//-> if name matches filter/query
					list.add(v);

				} else {

					for(String feature : v.getFeatures()){
						Matcher mFeat = pFeat.matcher(feature.toLowerCase());
						if(mFeat.find()){
							//-> if feature-value matches filter/query
							list.add(v);
						}
					}
				}
			}
		}

		if(list.size() == 0){
			return null;
		}


		if(LOG.isLoggable(LOG.DEBUG)){
			LOG.d(PLUGIN_NAME, String.format("FILTERED&SORTED for Locale %s (%s) %s", loc.getISO3Language(), loc.getISO3Country(), loc.getVariant()));
			for(Voice v : list){
				LOG.d(PLUGIN_NAME, String.format("FILTERED&SORTED -> %s (%s): quality %d | latency %d | features: %s", v.getName(), v.getLocale().toString(), v.getQuality(), v.getLatency(), java.util.Arrays.toString(v.getFeatures().toArray())));
			}
		}

		final Voice selVoice = list.last();
		this.lastVoiceSelection = new VoiceFilterSelection(loc, filter, selVoice, size);
		return selVoice;
	}

	/**
	 * Is the TTS service ready to play yet?
	 *
	 * @return
	 */
	private boolean isReady() {
		return (state == AndroidSpeechSynthesizer.STARTED) ? true : false;
	}

	/**
	 * helper class for cache the last (successful) voice selection
	 * 
	 * -> avoid iterating over voices-list and/or trying to "selecting best matching" voice
	 *    every time when the the same voice is / keeps being selected
	 *
	 */
	private class VoiceFilterSelection {
		private Locale locale;
		private String filter;
		private Voice voice;
		private int selectionSize;
		/**
		 * 
		 * @param locale 
		 * 				the Locale: NULL, if voice is an exact match for filter-field against the voice's name (from available voices listing),
		 * 				otherwise if "select best" heuristic was used, the Locale that was set when selecting the voice
		 * @param filter 
		 * 				the filter/name query for setting the voice; must NOT be NULL
		 * @param voice 
		 * 				the corresponding Voice that was determined based on the filter-String (and possibly on the Locale)
		 * @param selectionSize 
		 * 				the size of the list of available voices when the voice was selected 
		 * 				(-> used as primitive check, whether the amount of available voices have changed)
		 */
		public VoiceFilterSelection(Locale locale, String filter, Voice voice, int selectionSize) {
			super();
			this.locale = locale;
			this.filter = filter;
			this.voice = voice;
			this.selectionSize = selectionSize;
		}
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

		if(returnCode == TextToSpeech.SUCCESS)				//Denotes a successful operation.
			return "successful operation";
		if(returnCode == TextToSpeech.ERROR)				//Denotes a generic operation failure.
			return "generic operation failure";

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
