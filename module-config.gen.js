
/*********************************************************************
 * This file is automatically generated by mmir-plugins-export tools *
 *        Do not modify: ANY CHANGES WILL GET DISCARDED              *
 *********************************************************************/

module.exports = {
  pluginName: ["ttsAndroid","asrAndroid"],
  plugins: {
    ttsAndroid: {
      pluginName: "ttsAndroid",
      config: [
        /**
         * the environment(s) in which this plugin can/should be enabled
         * @default "android"
         */
        "env",
        /**
         * the plugin type
         * @default "tts"
         */
        "type",
        /**
         * the module/plugin name for the MediaManager plugins configuration
         * @default "mmir-plugin-speech-android/ttsAndroid"
         */
        "mod"
      ],
      defaultValues: {
        env: "android",
        type: "tts",
        mod: "mmir-plugin-speech-android/ttsAndroid"
      },
      speechConfig: [
        /** OPTIONAL
         * a specific voice for TTS
         * @type string
         */
        "voice",
        /** OPTIONAL
         * the language/country for TTS
         * @type string
         */
        "language"
      ]
    },
    asrAndroid: {
      pluginName: "asrAndroid",
      config: [
        /**
         * the environment(s) in which this plugin can/should be enabled
         * @default "android"
         */
        "env",
        /**
         * the plugin type
         * @default "asr"
         */
        "type",
        /**
         * the module/plugin name for the MediaManager plugins configuration
         * @default "mmir-plugin-speech-android"
         */
        "mod"
      ],
      defaultValues: {
        env: "android",
        type: "asr",
        mod: "mmir-plugin-speech-android"
      }
    }
  }
};
