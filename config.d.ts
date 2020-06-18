
import { MediaManagerPluginEntry, MediaPluginEnvType, SpeechConfigPluginEntry } from 'mmir-lib';

/**
 * (optional) entry "asrAnroid" and "ttsAndroid" in main configuration.json
 * for settings of asrAndroid and ttsAndroid module.
 *
 * Some of these settings can also be specified by using the options argument
 * in the ASR and TTS functions of {@link PluginMediaManager}, e.g.
 * {@link PluginMediaManager#recognize} or {@link PluginMediaManager#startRecord}
 * (if specified via the options, values will override configuration settings).
 */
export interface PluginConfig {
  asrAndroid?: ASRPluginConfigEntry;
  ttsAndroid?: TTSPluginConfigEntry | PluginSpeechConfigEntry;
}


export interface ASRPluginConfigEntry extends MediaManagerPluginEntry {

 /**
  * the module/plugin name for the MediaManager plugins configuration
  * @default "mmir-plugin-speech-android"
  */
  mod: 'mmir-plugin-speech-android';
 /**
  * the plugin type
  * @default "asr"
  */
  type: 'asr';
  /**
  * the environment(s) in which this plugin can/should be enabled
   * @default "android"
   */
  env: Array< 'android' | 'cordova' | MediaPluginEnvType | string > | 'android' | 'cordova' | MediaPluginEnvType | string;

  //TODO?
  // /** OPTIONAL number of n-best results that should (max.) be returned: integer, DEFAULT 1 */
  // results?: number;

  //TODO?
  // /** OPTIONAL  set recognition mode */
  // mode?: 'search' | 'dictation';

  //TODO support credentials via JS?
}

export interface TTSPluginConfigEntry extends MediaManagerPluginEntry {

 /**
  * the module/plugin name for the MediaManager plugins configuration
  * @default "mmir-plugin-speech-android/ttsAndroid"
  */
  mod: 'mmir-plugin-speech-android/ttsAndroid';
 /**
  * the plugin type
  * @default "tts"
  */
  type: 'tts';
 /**
  * the environment(s) in which this plugin can/should be enabled
  * @default "android"
  */
  env: Array< 'android' | 'cordova' | MediaPluginEnvType | string > | 'android' | 'cordova' | MediaPluginEnvType | string;

  //TODO support credentials via JS?
}

/**
 * Speech config entry for the plugin: per language (code) configuration e.g. for
 * adjusting the language-code or setting a specific voice for the language
 */
export interface PluginSpeechConfigEntry extends SpeechConfigPluginEntry {
  /** OPTIONAL
   * the language/country for TTS
   * @type string
   */
  language?: string;
  /** OPTIONAL
   * a specific voice for TTS
   * @type string
   */
  voice?: 'female' | 'male' | string;
}
