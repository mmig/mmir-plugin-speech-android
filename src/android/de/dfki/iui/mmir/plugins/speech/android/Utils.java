package de.dfki.iui.mmir.plugins.speech.android;

import org.apache.cordova.LOG;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.speech.SpeechRecognizer;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class Utils {

  private static final String NAME = "AndroidSpeechPlugin::Util";

  // Speech Recognition Permissions
  public static final int REQUEST_SPEECH_RECOGNITION = 1363699479;
  public static String[] PERMISSIONS_SPEECH_RECOGNITION = {
      Manifest.permission.RECORD_AUDIO
  };

  //Speech Synthesis Permissions
  public static final int REQUEST_SPEECH_SYNTHESIS = 1363699480;
  public static String[] PERMISSIONS_SPEECH_SYNTHESIS = {};//NOTE no special permissions required for built-in speech synthesis

  public static final int ERROR_SPEECH_NOT_STARTED_TIMEOUT = 101;

  public static final String FIELD_RESULT_TYPE = "type";
  public static final String FIELD_RECOGNITION_RESULT_ALTERNATIVES = "alternatives";
  public static final String FIELD_RECOGNITION_RESULT_UNSTABLE = "unstable";
  public static final String FIELD_RECOGNITION_SCORE = "score";
  public static final String FIELD_RECOGNITION_RESULT = "result";
  public static final String FIELD_ERROR_CODE = "error_code";
  public static final String FIELD_MESSAGE = "msg";

  /**
   * Checks if the activity has permission(s) for speech recognition
   *
   * @param activity
   */
  static boolean isPermissionMissing(@NonNull Activity activity, @NonNull String[] permissions) {

    boolean missingPermission = false;
    // Check if we have permissions
    for(String p : permissions){
      int permission = ActivityCompat.checkSelfPermission(activity, p);
      if(permission != PackageManager.PERMISSION_GRANTED){
        missingPermission = true;
        break;
      }
    }

    return missingPermission;
  }

  /**
   * prompt user to allow for (required)  permission(s)
   *
   * @param activity
   */
  static void requestPermissions(@NonNull Activity activity, @NonNull String[] permissions, int requestCode) {
    ActivityCompat.requestPermissions(activity, permissions, requestCode);
  }

  /**
   * Checks if the activity has permission(s) for speech recognition
   *
   * @param activity
   */
  public static boolean isSpeechRecognitionPermissionMissing(@NonNull Activity activity) {

    return isPermissionMissing(activity, PERMISSIONS_SPEECH_RECOGNITION);
  }


  /**
   * prompt user to allow for speech recognition (i.e. enabled its required  permission(s))
   *
   * @param activity
   */
  public static void requestSpeechRecognitionPermissions(Activity activity) {
    requestPermissions(
      activity,
      PERMISSIONS_SPEECH_RECOGNITION,
      REQUEST_SPEECH_RECOGNITION
    );
  }

  /**
   * Checks if the activity has permission(s) for speech synthesis
   *
   * @param activity
   */
  public static boolean isSpeechSynthesisPermissionMissing(@NonNull Activity activity) {

    return isPermissionMissing(activity, PERMISSIONS_SPEECH_SYNTHESIS);
  }


  /**
   * prompt user to allow for speech synthesis (i.e. enabled its required  permission(s))
   *
   * @param activity
   */
  public static void requestSpeechSynthesisPermissions(Activity activity) {
    requestPermissions(
      activity,
      PERMISSIONS_SPEECH_SYNTHESIS,
      REQUEST_SPEECH_SYNTHESIS
    );
  }

  /**
   * Checks if the activity has permission(s) for speech recognition
   *
   * If the activity does not has permission(s) then the user will be prompted to grant permission(s)
   *
   * @param activity
   */
  public static boolean verifySpeechRecognitionPermissions(Activity activity) {

    boolean missingPermission = isSpeechRecognitionPermissionMissing(activity);

    if (missingPermission) {
      // We don't have permission so prompt the user
      requestSpeechRecognitionPermissions(activity);
    }
    return !missingPermission;
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

  public static String getErrorMessage(int errorCode){
    String msg;
    if(errorCode == SpeechRecognizer.ERROR_NETWORK_TIMEOUT){
      msg = "Network Timeout Error";
    }else if(errorCode == SpeechRecognizer.ERROR_NETWORK){
      msg = "Network Error";
    }else if(errorCode == SpeechRecognizer.ERROR_AUDIO){
      msg = "Audio Error";
    }else if(errorCode == SpeechRecognizer.ERROR_SERVER){
      msg = "Server Error";
    }else if(errorCode == SpeechRecognizer.ERROR_CLIENT){
      msg = "Client Error";
    }else if(errorCode == SpeechRecognizer.ERROR_SPEECH_TIMEOUT){
      msg = "Speech Timeout Error";
    }else if(errorCode == SpeechRecognizer.ERROR_NO_MATCH){
      msg = "No Match Error";
    }else if(errorCode == SpeechRecognizer.ERROR_RECOGNIZER_BUSY){
      msg = "Recognizer Busy Error";
    }else if(errorCode == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS){
      msg = "Insufficient Permissions Error";
    }else if(errorCode == ERROR_SPEECH_NOT_STARTED_TIMEOUT){
      msg = "[Custom Error] Speech Not Started Timout Error";
    }else {
      msg = "Unknown Error: code "+errorCode;
    }
    return msg;
  }

  public static JSONObject createMessage(Object ...args){

    JSONObject msg = new JSONObject();

    addToMessage(msg, args);

    return msg;
  }

  public static void addToMessage(JSONObject msg, Object ...args){

    int size = args.length;
    if(size % 2 != 0){
      LOG.e(NAME, "Invalid argument length (must be even number): "+size);
    }

    Object temp;
    String name;

    for(int i=0; i < size; i+=2){

      temp = args[i];
      if(!(temp instanceof String)){
        LOG.e(NAME, "Invalid argument type at "+i+" lenght (must be a String): "+temp);
        name = String.valueOf(temp);
      } else {
        name = (String) temp;
      }

      if(i+1 < size){
        temp = args[i+1];
      } else {
        temp = null;
      }

      try {

        msg.putOpt(name, temp);

      } catch (JSONException e) {
        LOG.e(NAME, "Failed to add value "+temp+" to message object", e);
      }
    }
  }
}
