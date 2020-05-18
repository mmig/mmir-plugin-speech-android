package de.dfki.iui.mmir.plugins.speech.android;

import org.apache.cordova.LOG;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

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
  public static void verifySpeechRecognitionPermissions(Activity activity) {

    boolean missingPermission = isSpeechRecognitionPermissionMissing(activity);

    if (missingPermission) {
      // We don't have permission so prompt the user
      requestSpeechRecognitionPermissions(activity);
    }
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
