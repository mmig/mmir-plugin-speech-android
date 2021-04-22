/**
 * The MIT License
 *
 *  Copyright (c) 2014-2016
 *  DFKI (github.com/mmig)
 *
 *
 *  based on work of:
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
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.RequiresApi;
import android.util.Log;

/**
 * Style and such borrowed from the TTS and PhoneListener plugins
 */
public class AndroidSpeechRecognizer extends CordovaPlugin implements AudioManager.OnAudioFocusChangeListener {

  public static final String ACTION_GET_LANGUAGES = "getSupportedLanguages";
  public static final String ACTION_RECOGNIZE = "recognize";
  public static final String ACTION_START_RECORDING = "startRecording";
  public static final String ACTION_STOP_RECORDING = "stopRecording";
  public static final String ACTION_CANCEL = "cancel";
//	public static final String ACTION_MIC_LEVEL = "getMicLevels";

  public static final String LANGUANGE_MODEL_SEARCH = "search";
  public static final String LANGUANGE_MODEL_DICTATION = "dictation";

  public static final String ACTION_MIC_LEVEL_LISTENER = "setMicLevelsListener";

  private static final String INIT_MESSAGE_CHANNEL = "msg_channel";

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

  /**
   * Back-channel to JavaScript-side
   */
  private CallbackContext messageChannel;

  CordovaInterface _cordova;

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {

    this._cordova = cordova;

    this.mAudioManager = (AudioManager) this._cordova.getActivity().getSystemService(Context.AUDIO_SERVICE);

    if(SDK_VERSION < Build.VERSION_CODES.M) {
      //for Android 5.1 or earlier (sdk 22): build-time permissions
      Utils.verifySpeechRecognitionPermissions(cordova.getActivity());
    }

    super.initialize(cordova, webView);
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {

    boolean isValidAction = true;

    //    	this.callbackContext= callbackContext;

    //		//FIXM DEBUG:
    //		try{
    //			LOG.d(PLUGIN_NAME + "_DEBUG", String.format("action '%s' with arguments: %s)", action, args.toString(2)));
    //		}catch(Exception e){}



    // Action selector
    if (ACTION_RECOGNIZE.equals(action)) {
      // recognize speech
      startSpeechRecognitionActivity(args, callbackContext, true);
    } else if (ACTION_GET_LANGUAGES.equals(action)) {
      getSupportedLanguages(callbackContext);
    } else if (ACTION_START_RECORDING.equals(action)) {
      startSpeechRecognitionActivity(args, callbackContext, false);
    } else if (ACTION_STOP_RECORDING.equals(action)) {
      stopSpeechInput(callbackContext);
    } else if (ACTION_CANCEL.equals(action)) {
      cancelSpeechInput(callbackContext);
      //        } else if (ACTION_MIC_LEVEL.equals(action)) {
      //        	returnMicLevels(callbackContext);
    } else if (ACTION_MIC_LEVEL_LISTENER.equals(action)) {
      setMicLevelsListener(args, callbackContext);
    } else if (INIT_MESSAGE_CHANNEL.equals(action)) {

      messageChannel = callbackContext;
      PluginResult result = new PluginResult(Status.OK, Utils.createMessage("action", "plugin", "status", "initialized plugin channel"));
      result.setKeepCallback(true);
      callbackContext.sendPluginResult(result);

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
  private void startSpeechRecognitionActivity(final JSONArray args, final CallbackContext callbackContext, final boolean isWithEndOfSpeechDetection) {

    //need to run recognition on UI thread (Android's SpeechRecognizer must run on main thread)
    cordova.getActivity().runOnUiThread(new Runnable() {

      @Override
      public void run() {
        _startSpeechRecognitionActivity(args, callbackContext, isWithEndOfSpeechDetection);
      }
    });

  }

  private void _startSpeechRecognitionActivity(JSONArray args, CallbackContext callbackContext, boolean isWithEndOfSpeechDetection) {

    if(SDK_VERSION >= Build.VERSION_CODES.M){
      //since Android 6 (sdk 23): runtime permissions!

      if(!Utils.verifySpeechRecognitionPermissions(cordova.getActivity())){
        int errorCode = SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS;
        JSONObject result = Utils.createMessage(
          Utils.FIELD_ERROR_CODE, errorCode,
          Utils.FIELD_MESSAGE, "onStart: "+Utils.getErrorMessage(errorCode)
        );
        callbackContext.error(result);
        return;
      }
    }

    int maxMatches = 0;
    String language = Locale.getDefault().toString();
    boolean isIntermediate = false;
    String languageModel = null;
    String prompt = "";//TODO remove? (not used when ASR is directly used as service here...)

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
        languageModel = args.getString(3);
      }
      if (args.length() > 4) {
        // Optional text prompt
        prompt = args.getString(4);
      } else {
        prompt = "Ein Test für einen Prompt";
      }

      //TODO if ... withoutEndOfSpeechDetection = ...
    }
    catch (Exception e) {
      Log.e(PLUGIN_NAME, String.format("startSpeechRecognitionActivity exception: %s", e.toString()));
    }

    String languageModelParam = RecognizerIntent.LANGUAGE_MODEL_FREE_FORM;
    if(languageModel != null && languageModel.length() > 0){
      if(LANGUANGE_MODEL_SEARCH.equals(languageModel)){
        languageModelParam = RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH;
      }
      else if("dictation".equals(languageModel)){
        languageModelParam = RecognizerIntent.LANGUAGE_MODEL_FREE_FORM;
      } else {
        LOG.w(PLUGIN_NAME, "invalid argument for language model (using default 'dictation' instead): '"+languageModel+"'");
      }
    }

    // Create the intent and set parameters
    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, languageModelParam);

    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);

    if(!isWithEndOfSpeechDetection){

      // try to simulate start/stop-recording behavior (without end-of-speech detection)

      //NOTE these setting do not seem to have any effect for default Google Recognizer API level > 16

      intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, new Long(10000));
      intent.putExtra(RecognizerIntent. EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS , new Long(6 * 1000));
    }

    if (maxMatches > 0)
      intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxMatches);

    if (!prompt.equals(""))
      intent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt);


    if(isIntermediate)
      intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

    //NOTE the extra package seems to be required for older Android versions, but not since API level 17(?)
    if(SDK_VERSION <= Build.VERSION_CODES.JELLY_BEAN)
      intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, cordova.getActivity().getPackageName());

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

  //send mic-levels value to JavaScript side
  void sendMicLevels(float levels){

    PluginResult micLevels = new PluginResult(Status.OK, Utils.createMessage("action", "miclevels", "value", levels));
    micLevels.setKeepCallback(true);

    messageChannel.sendPluginResult(micLevels);
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

  @RequiresApi(26)
  private AudioFocusRequest mAudioFocusReq;

  private boolean isDisableSoundPrompt(){
    //TODO impl. "smarter" detection? (russa: which version added the sounds?)
    // return SDK_VERSION >= Build.VERSION_CODES.JELLY_BEAN;
    return false;//FIXME does not work as intended (at least not for sdk >= 27), disable for now
  }

  int soloCounter = 0;
  void disableSoundFeedback() {

    if(!mIsStreamSolo && isDisableSoundPrompt()){

      if(delayedEnableSoundHandler != null){
        delayedEnableSoundHandler.cancel();
        delayedEnableSoundHandler = null;
      }

      if(SDK_VERSION >= Build.VERSION_CODES.O) {
        disableSoundSdk26();;
      } else if(SDK_VERSION >= Build.VERSION_CODES.M) {
        disableSoundSdk23();
      } else {
        disableSoundSdk22();
      }

      if(Log.isLoggable(PLUGIN_NAME, Log.VERBOSE)) Log.v(PLUGIN_NAME, "DISABLE SOUND -> solo-counter: "+(++soloCounter));
    }
  }

  void enableSoundFeedback() {

    if (mIsStreamSolo){

      if(SDK_VERSION >= Build.VERSION_CODES.O) {
        enableSoundSdk26();
      } else if(SDK_VERSION >= Build.VERSION_CODES.M) {
        enableSoundSdk23();
      } else {
        enableSoundSdk22();
      }

      if(Log.isLoggable(PLUGIN_NAME, Log.VERBOSE)) Log.v(PLUGIN_NAME, "ENABLE SOUND -> solo-counter: "+(--soloCounter));
    }
  }

  protected void disableSoundSdk22() {
    mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, true);
    mIsStreamSolo = true;
  }

  @RequiresApi(23)
  protected void disableSoundSdk23() {
    mAudioManager.requestAudioFocus(this, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);
  }

  @RequiresApi(26)
  protected void disableSoundSdk26() {
    if(mAudioFocusReq == null) {
      mAudioFocusReq = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
              .setAudioAttributes(new AudioAttributes.Builder()
                      .setLegacyStreamType(AudioManager.STREAM_VOICE_CALL)
                      .build())
              .setOnAudioFocusChangeListener(this)
              .build();
    }
    mAudioManager.abandonAudioFocusRequest(mAudioFocusReq);
  }

  protected void enableSoundSdk22() {
    mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, false);
    mIsStreamSolo = false;
  }

  @RequiresApi(23)
  protected void enableSoundSdk23() {
    mAudioManager.abandonAudioFocus(this);
  }

  @RequiresApi(26)
  protected void enableSoundSdk26() {
    if(mAudioFocusReq != null) {
      mAudioManager.abandonAudioFocusRequest(mAudioFocusReq);
    }
  }

  public void onAudioFocusChange(int focusChange) {
    if(focusChange ==  AudioManager.AUDIOFOCUS_GAIN){
      mIsStreamSolo = true;
    } else { //if(focusChange ==  AudioManager.AUDIOFOCUS_LOSS || focusChange ==  AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || focusChange ==  AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
      mIsStreamSolo = false;
    }
  }

  //	private Object delayedEnableSoundLock = new Object(); FIXME use lock/synchronized when accessing delayedEnableSoundHandler?
  private DelayedEnableSound delayedEnableSoundHandler = null;
  private int reenableSoundDelay = 500;//ms <- delay for re-enabling sound after recognition has finished (the delay needs to be long enough to suppress the last (un-wanted) sound-feedback of the recognition)
  void enableSoundFeedbackDelayed() {

    // TODO implement without running on UI thread & scheduling another
    // "thread" (would need Looper impl. when running delayed?)
    if(delayedEnableSoundHandler != null){
      delayedEnableSoundHandler.cancel();
    }
    delayedEnableSoundHandler = new DelayedEnableSound();
    cordova.getActivity().runOnUiThread(delayedEnableSoundHandler);
  }


  static Handler delayedSoundHandler = null;
  private class DelayedEnableSound implements Runnable {

    private Object delayedSoundLock = new Object();
    private boolean isCanceled = false;

    @Override
    public void run() {

      synchronized(delayedSoundLock){

        if(isCanceled){
          return; ////////////////// EARLY EXIT /////////////////////
        }

        if(delayedSoundHandler == null){
          delayedSoundHandler = new Handler();
        }
      }

      boolean isScheduled = delayedSoundHandler.postDelayed(new Runnable() {
        @Override
        public void run() {

          synchronized(delayedSoundLock){

            if(isCanceled){
              return; ////////////////// EARLY EXIT /////////////////////
            }

            isCanceled = true;

            if (AndroidSpeechRecognizer.this != null) {
              AndroidSpeechRecognizer.this.enableSoundFeedback();
            }
          }

        }

      }, reenableSoundDelay);

      if (!isScheduled) {

        synchronized(delayedSoundLock){

          if(isCanceled){
            return; ////////////////// EARLY EXIT /////////////////////
          }

          isCanceled = true;

          if (AndroidSpeechRecognizer.this != null) {
            AndroidSpeechRecognizer.this.enableSoundFeedback();
          }
        }

      }

    }

    public void cancel(){

      synchronized(delayedSoundLock){

        if(isCanceled){
          return; ////////////////// EARLY EXIT /////////////////////
        }

        isCanceled = true;

        if(delayedSoundHandler != null){

          delayedSoundHandler.removeCallbacks(this);
        }
      }
    }

  }

}
