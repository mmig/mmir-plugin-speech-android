package de.dfki.iui.mmir.plugins.speech.android;

import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

public class AndroidSpeechCapabilityChecker extends CordovaPlugin {

  private static final String NAME = "AndroidSpeech.CapabilityCheck";

  private static final int SDK_VERSION = Build.VERSION.SDK_INT;

  private static final int RESULT_CODE_CHECK_TTS_AVAILABILITY = 234235;
  private static final int RESULT_CODE_INSTALL_LANGUAGES = 234236;
  private static final int RESULT_CODE_INSTALL_ASR_APP = 234237;
  private static final int RESULT_CODE_INSTALL_TTS_APP = 234238;

  /** for Intent for opening applications page in Google Play: Voice Search package */
  private static final String GOOGLE_VOICE_SEARCH_ID = "com.google.android.googlequicksearchbox";//[DISABLED may not be available for all languages/countries!] "com.google.android.voicesearch"
  /** for Intent for opening applications page in Google Play: TTS / Speech Output */
  private static final String GOOGLE_TTS_ID = "com.google.android.tts";//"com.svox.classic"//"com.ivona.tts"

  private static final String FIELD_SPEECH_RECOGNITION_NAME = "asrAvailable";
  private static final String FIELD_SPEECH_SYNTHESIS_NAME = "ttsAvailable";
  private static final String FIELD_SPEECH_SYNTHESIS_LANGUAGE_NAME = "ttsLanguageAvailable";
  private static final String FIELD_SPEECH_RECOGNITION_PERMISSION_NAME = "asrPermission";
  private static final String FIELD_SPEECH_SYNTHESIS_PERMISSION_NAME = "ttsPermission";
  private static final String FIELD_DESCRIPTION_NAME = "description";

  private static final String ACTION_CHECK_ASR = "asr_check";
  private static final String ACTION_CHECK_TTS = "tts_check";
  private static final String ACTION_CHECK_TTS_LANGUAGE = "language_tts_check";
  private static final String ACTION_PERMISSIONS_ASR = "asr_permissions_check";
  private static final String ACTION_PERMISSIONS_TTS = "tts_permissions_check";
  private static final String ACTION_ASK_PERMISSIONS_ASR = "asr_ask_permissions";
  private static final String ACTION_ASK_PERMISSIONS_TTS = "tts_ask_permissions";

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {

    boolean isValidAction = true;
    PluginResult result = null;

    try {

      if (ACTION_CHECK_ASR.equals(action)) {

        //TODO support arguments for setting custom dialog labels!

        isValidAction = true;
        verifySpeechRecognitionAvailable(callbackContext);

      } else if (ACTION_CHECK_TTS.equals(action)) {

        //TODO support arguments for setting custom dialog labels!

        isValidAction = true;
        verifySpeechSyntesisAvailable(callbackContext, false);

      } else if (ACTION_CHECK_TTS_LANGUAGE.equals(action)) {

        //TODO support arguments for setting custom dialog labels!
        //TODO support argument for checking specific languages

        isValidAction = true;
        checkSpeechSynthesizerLanguages(callbackContext);

      } else if (ACTION_PERMISSIONS_ASR.equals(action)) {

        isValidAction = true;
        boolean res = !Utils.isSpeechRecognitionPermissionMissing(cordova.getActivity());
        result =  new PluginResult(PluginResult.Status.OK, Utils.createMessage(FIELD_SPEECH_RECOGNITION_PERMISSION_NAME, res));

      } else if (ACTION_PERMISSIONS_TTS.equals(action)) {

        isValidAction = true;
        boolean res = !Utils.isSpeechSynthesisPermissionMissing(cordova.getActivity());
        result =  new PluginResult(PluginResult.Status.OK, Utils.createMessage(FIELD_SPEECH_SYNTHESIS_PERMISSION_NAME, res));


      } else if (ACTION_ASK_PERMISSIONS_ASR.equals(action)) {

        isValidAction = true;

        AndroidSpeechPermissionCheck plugin = new AndroidSpeechPermissionCheck(callbackContext);
        plugin.privateInitialize("AndroidSpeechPermissionCheckASR_temp", cordova, webView, preferences);
        plugin.requestSpeechRecognitionPermissions();

      } else if (ACTION_ASK_PERMISSIONS_TTS.equals(action)) {

        isValidAction = true;

        AndroidSpeechPermissionCheck plugin = new AndroidSpeechPermissionCheck(callbackContext);
        plugin.privateInitialize("AndroidSpeechPermissionCheckTTS_temp", cordova, webView, preferences);
        plugin.requestSpeechSynthesisPermissions();
      }

    } catch (Exception e) {

      String msg = String.format("Error during '%s': %s", action, e);
      LOG.e(NAME, msg, e);
      result =  new PluginResult(PluginResult.Status.JSON_EXCEPTION, msg);

    }

    if(result != null)
      callbackContext.sendPluginResult(result);

    return isValidAction;
  }

  public void verifySpeechRecognitionAvailable(CallbackContext callbackContext){
    //verify that Speech Recognizer is available:
    // if not, ask permission to install it
    checkSpeechRecognizer(cordova, webView, preferences, callbackContext);
  }

  public void verifySpeechSyntesisAvailable(CallbackContext callbackContext, boolean checkLanguageAvailability){

    boolean isTts = isSpeechSynthesisActivityPresent(cordova.getActivity());
    LOG.i(NAME, "Speech TTS available:  "+isTts);

    if(!isTts){
      
      installGoogleSynthesis(cordova, webView, preferences, callbackContext);
      
    } else {
      
      if(checkLanguageAvailability){
        
        checkSpeechSynthesizerLanguages(callbackContext);
        
      } else {
        
        sendResult(callbackContext, FIELD_SPEECH_SYNTHESIS_NAME, true, "Speech Synthesis service is available.");
      }
    }
  }


  //////////////////////////////////
  // based on http://stackoverflow.com/a/10912048/4278324

  void checkSpeechSynthesizerLanguages(CallbackContext callbackContext) {
    Intent checkIntent = new Intent();
    checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);

    AndroidSpeechCheck plugin = new AndroidSpeechCheck(callbackContext);
    plugin.privateInitialize("AndroidSpeechCheckTTSLanguage_temp", cordova, webView, preferences);
    cordova.startActivityForResult(plugin, checkIntent, RESULT_CODE_CHECK_TTS_AVAILABILITY);
  }

  /**
   * Checks availability of speech synthesis Activity
   *
   * @param callerActivity – Activity that called the checking
   * @return true – if Activity there available, false – if Activity is absent
   */
  static boolean isSpeechSynthesisActivityPresent(Activity callerActivity) {
    try {
      // getting an instance of package manager
      PackageManager pm = callerActivity.getPackageManager();
      // a list of activities, which can process speech recognition Intent
      List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA), 0);

      if (activities.size() != 0) {    // if list not empty
        return true;                // then we can recognize the speech
      }
    } catch (Exception e) {

    }

    return false; // we have no activities to recognize the speech
  }

  /**
   * Asking the permission for installing Google Voice Synthesis.
   * If permission granted – sent user to Google Play
   */
  private static void installGoogleSynthesis(final CordovaInterface cordova, final CordovaWebView appView, final CordovaPreferences preferences, final CallbackContext callbackContext) {

    // creating a dialog asking user if he want
    // to install the Voice Search
    Dialog dialog = new AlertDialog.Builder(cordova.getActivity())
            //TODO externalize String messages
            .setMessage("For reading it's necessary to install \"Google TTS\"")    // dialog message
            .setTitle("Install TTS from Google Play?")                             // dialog header

            // CONFIRM button
            .setPositiveButton("Install", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                Exception ex = startStoreActivity(GOOGLE_TTS_ID, cordova, appView, preferences, callbackContext);
                if(ex != null){
                  final String msg = "Failed to start activity for installing Speech Synthesis App "+GOOGLE_TTS_ID+": "+ex;
                  LOG.e(NAME, msg, ex);
                  sendResult(callbackContext, FIELD_SPEECH_SYNTHESIS_NAME, false, msg);
                }
              }})
            // CANCEL button
            .setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                final String msg = "User declined to install Speech Synthesis App "+GOOGLE_TTS_ID;
                LOG.i(NAME, msg);
                sendResult(callbackContext, FIELD_SPEECH_SYNTHESIS_NAME, false, msg);
              }})
            .create();

    dialog.show();    // showing dialog
  }


  /**
   * Asking the permission for installing additional/missing language for Speech Synthesis.
   * If permission granted – sent user to Google Play
   */
  private static void installGoogleSynthesisLanguages(final CordovaInterface cordova, final CordovaWebView webView, final CordovaPreferences preferences, final CallbackContext callbackContext) {

    Intent installIntent = new Intent();
    installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);

    AndroidSpeechCheck plugin = new AndroidSpeechCheck(callbackContext);
    plugin.privateInitialize("AndroidSpeechCheckTTSLanguageInstall_temp", cordova, webView, preferences);
    cordova.startActivityForResult(plugin, installIntent, RESULT_CODE_INSTALL_LANGUAGES);
  }



  //////////////////////////////////////////////////////////////////////
  //
  // check / install voice recognition
  //
  // based on https://software.intel.com/en-us/articles/developing-android-applications-with-voice-recognition-features


  /**
   * Running the recognition process. Checks availability of recognition Activity,
   * If Activity is absent, send user to Google Play to install Google Voice Search.
   * If Activity is available, send Intent for running.
   *
   */
  static private void checkSpeechRecognizer(CordovaInterface cordova, CordovaWebView appView, CordovaPreferences preferences, CallbackContext callbackContext) {

    // check if there is recognition Activity
    if (isSpeechRecognitionActivityPresented(cordova.getActivity()) != true) {
      //TODO externalize String messages
      // if no, then showing notification to install Voice Search
      Toast.makeText(cordova.getActivity(), "In order to activate speech recognition you must install \"Google Voice Search\"", Toast.LENGTH_LONG).show();
      // start installing process
      installGoogleVoiceSearch(cordova, appView, preferences, callbackContext);
    } else {
      sendResult(callbackContext, FIELD_SPEECH_RECOGNITION_NAME, true, "Speech Recognizer is available.");
    }
  }

  /**
   * Checks availability of speech recognizing Activity
   *
   * @param callerActivity – Activity that called the checking
   * @return true – if Activity there available, false – if Activity is absent
   */
  private static boolean isSpeechRecognitionActivityPresented(Activity callerActivity) {

    //      boolean b = SpeechRecognizer.isRecognitionAvailable(cordova.getActivity());

    try {
      // getting an instance of package manager
      PackageManager pm = callerActivity.getPackageManager();
      // a list of activities, which can process speech recognition Intent
      List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);

      if (activities.size() != 0) {    // if list not empty
        return true;                   // then we can recognize the speech
      }
    } catch (Exception e) {

    }

    return false; // we have no activities to recognize the speech
  }

  private static void sendResult(CallbackContext callbackContext, String field, boolean isEnabled, String debugMsg, Object ...fields){
    if(callbackContext != null){
      JSONObject result = Utils.createMessage(field, isEnabled, FIELD_DESCRIPTION_NAME, debugMsg);
      if(fields != null && fields.length >= 0){
        Utils.addToMessage(result, fields); 
      }
      callbackContext.success(result);
    } else {
      LOG.e(NAME, String.format("%s: (availability) result is %s", debugMsg, isEnabled));
    }
  }

  @SuppressWarnings("deprecation")
  private static Exception startStoreActivity(final String appPackageId, final CordovaInterface cordova, final CordovaWebView webView, final CordovaPreferences preferences, final CallbackContext callbackContext){

    try {
      // creating an Intent for opening applications page in Google Play for installing app
      Intent installIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+appPackageId));

      // setting flags to avoid going in application history (Activity call stack)
      final int flags;
      if(SDK_VERSION >= 21){
        flags = Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
      } else {
        flags = Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET;
      }
      installIntent.setFlags(flags);


      AndroidSpeechCheck plugin = new AndroidSpeechCheck(callbackContext);
      plugin.privateInitialize("AndroidSpeechInstallApp_temp", cordova, webView, preferences);
      cordova.startActivityForResult(plugin, installIntent, RESULT_CODE_CHECK_TTS_AVAILABILITY);

      return null;
    } catch(Exception e){
      return e;
    }
  }

  /**
   * Asking the permission for installing Google Voice Search.
   * If permission granted – sent user to Google Play
   */
  private static void installGoogleVoiceSearch(final CordovaInterface cordova, final CordovaWebView appView, final CordovaPreferences preferences, final CallbackContext callbackContext) {

    // creating a dialog asking user if he want
    // to install the Voice Search
    Dialog dialog = new AlertDialog.Builder(cordova.getActivity())
            //TODO externalize String messages
            .setMessage("For recognition it's necessary to install \"Google Voice Search\"")    // dialog message
            .setTitle("Install Voice Search from Google Play?")                                 // dialog header
            .setPositiveButton("Install", new DialogInterface.OnClickListener() {
              // CONFIRM button
              @Override
              public void onClick(DialogInterface dialog, int which) {

                Exception ex = startStoreActivity(GOOGLE_VOICE_SEARCH_ID, cordova, appView, preferences, callbackContext);
                if(ex != null){
                  final String msg = "Failed to start activity for installing Speech Recognition App "+GOOGLE_VOICE_SEARCH_ID+": "+ex;
                  LOG.e(NAME, msg, ex);
                  sendResult(callbackContext, FIELD_SPEECH_RECOGNITION_NAME, false, msg);
                }
              }})
            // CANCEL button
            .setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                final String msg = "User declined to install Speech Recognition App "+GOOGLE_VOICE_SEARCH_ID;
                LOG.i(NAME, msg);
                sendResult(callbackContext, FIELD_SPEECH_RECOGNITION_NAME, false, msg);
              }})
            .create();

    dialog.show();
  }

  private static class AndroidSpeechCheck extends CordovaPlugin {

    public CallbackContext callbackContext;
    public AndroidSpeechCheck(CallbackContext callbackContext){
      this.callbackContext = callbackContext;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

      if (requestCode == RESULT_CODE_CHECK_TTS_AVAILABILITY) {

        if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
          Bundle extras = data.getExtras();
          Object e1 = extras.get(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES);
          Object e2 = extras.get(TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES);

          LOG.d(NAME, String.format("Extras AVAILABLE: '%s' | UNAVAILABLE: '%s'", e1,e2));

          //FIXME should check, if specific/required language(s) are available

          boolean ttsServiceAvailable = isSpeechSynthesisActivityPresent(cordova.getActivity());
          sendResult(callbackContext, FIELD_SPEECH_SYNTHESIS_LANGUAGE_NAME, true, "Language(s) for Speech Synthesis are available.", FIELD_SPEECH_SYNTHESIS_NAME, ttsServiceAvailable);

        } else if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL) {

          // missing data, install it

          installGoogleSynthesisLanguages(cordova, webView, preferences, callbackContext);
        }

      } else if (requestCode == RESULT_CODE_INSTALL_LANGUAGES) {

        //FIXME need to check if installation was actually done or canceled
        //              boolean isInstalled = resultCode == ???;
        boolean isInstalled = true;

        boolean ttsServiceAvailable = isSpeechSynthesisActivityPresent(cordova.getActivity());

        if (isInstalled) {
          sendResult(callbackContext, FIELD_SPEECH_SYNTHESIS_LANGUAGE_NAME, true, "Installed language(s) for Speech Synthesis.", FIELD_SPEECH_SYNTHESIS_NAME, ttsServiceAvailable);
        } else {
          sendResult(callbackContext, FIELD_SPEECH_SYNTHESIS_LANGUAGE_NAME, false, "Canceled installation for Speech Synthesis language(s).", FIELD_SPEECH_SYNTHESIS_NAME, ttsServiceAvailable);
        }

      } else if (requestCode == RESULT_CODE_INSTALL_TTS_APP) {

        //FIXME need to check if installation was actually done or canceled
        //              boolean isInstalled = resultCode == ???;
        boolean isInstalled = true;

        if (isInstalled) {
          sendResult(callbackContext, FIELD_SPEECH_SYNTHESIS_NAME, true, "Installed Speech Synthesis App.");
        } else {
          sendResult(callbackContext, FIELD_SPEECH_SYNTHESIS_NAME, false, "Canceled installation for Speech Synthesis App.");
        }

      } else if (requestCode == RESULT_CODE_INSTALL_ASR_APP) {

        //FIXME need to check if installation was actually done or canceled
        //              boolean isInstalled = resultCode == ???;
        boolean isInstalled = true;

        if (isInstalled) {
          sendResult(callbackContext, FIELD_SPEECH_RECOGNITION_NAME, true, "Installed Speech Recognition App.");
        } else {
          sendResult(callbackContext, FIELD_SPEECH_RECOGNITION_NAME, false, "Canceled installation for Speech Recognition App.");
        }

      } else {

        LOG.w(NAME,  String.format("Unknown activity result code %d (req. code %d), result data: %s", resultCode, requestCode, data));
      }

      super.onActivityResult(requestCode, resultCode, data);
    }

  }

  private static class AndroidSpeechPermissionCheck extends CordovaPlugin {

    public CallbackContext callbackContext;
    public AndroidSpeechPermissionCheck(CallbackContext callbackContext){
      this.callbackContext = callbackContext;
    }


    public void requestSpeechRecognitionPermissions(){
      cordova.requestPermissions(this, Utils.REQUEST_SPEECH_RECOGNITION, Utils.PERMISSIONS_SPEECH_RECOGNITION);
    }

    public void requestSpeechSynthesisPermissions(){
      cordova.requestPermissions(this, Utils.REQUEST_SPEECH_SYNTHESIS, Utils.PERMISSIONS_SPEECH_SYNTHESIS);
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {

      if (requestCode == Utils.REQUEST_SPEECH_RECOGNITION) {

        boolean isAllowed = isPermissionsGranted(permissions, grantResults);

        if (isAllowed) {
          sendResult(callbackContext, FIELD_SPEECH_RECOGNITION_PERMISSION_NAME, true, "Granted permission(s) for Speech Recognition.");
        } else {
          sendResult(callbackContext, FIELD_SPEECH_RECOGNITION_PERMISSION_NAME, false, "Did not grant permission(s) for Speech Recognition.");
        }

      } else if (requestCode == Utils.REQUEST_SPEECH_SYNTHESIS) {

        boolean isAllowed = isPermissionsGranted(permissions, grantResults);

        if (isAllowed) {
          sendResult(callbackContext, FIELD_SPEECH_SYNTHESIS_PERMISSION_NAME, true, "Granted permission(s) for Speech Synthesis.");
        } else {
          sendResult(callbackContext, FIELD_SPEECH_SYNTHESIS_PERMISSION_NAME, false, "Did not grant permission(s) for Speech Synthesis.");
        }

      } else {

        LOG.w(NAME,  String.format("Unknown permission request code %d, result data: permissions %s, results %s", requestCode, permissions, grantResults));
      }

      super.onRequestPermissionResult(requestCode, permissions, grantResults);
    }

    private boolean isPermissionsGranted(String[] permissions, int[] grantResults){
      int len = grantResults.length;
      if(len > 0 && permissions.length <= len){
        for(int i=0; i < len; ++i){
          if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
            return false;
          }
        }
        return true;
      }
      return false;
    }


  }

}
