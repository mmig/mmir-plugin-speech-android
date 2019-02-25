# dfki-mmir-plugin-speech-android
----

Cordova plugin for the MMIR framework for accessing Android's system speech recognition and synthesis

The plugin provides access to Android's speech recognition service (i.e. it does not use / trigger
the default graphical interface when accessing using recognition via Intents).

NOTE: This plugin version uses `gradle` configuration(s). For a version without `gradle`,
      see the `gradleless` branch of the repository.


This Cordova plugin is specifically targeted to be used with the [MMIR framework][1]: 
On adding the plugin, 2 MMIR "modules" (for recognition and synthesis) will be copied
into the platform's resource folders `<www assets>/mmirf/env/media/android*.js`

For details on using (speech) plugins in the MMIR framework, also see the corresponding
section in the [wiki][2].

# USAGE
------


## INSTALLATION

### From GIT repository

execute the following command in Cordova project's root directory: 

    cordova plugin add https://github.com/mmig/mmir-plugin-speech-android.git


### From local copy of the repository

(1) check out the repository into a local directory (or download its ZIP file and decompress it)

(2) add the plugin to the Cordova project:

use command: 

    cordova plugin add <file path to plugin directory>

If plugin source code (from this repository) is located in directory: 

    D:\DevProjects\Eclipse_workplace\mmir-plugin-speech-android

execute the following command in Cordova project's root directory: 

    cordova plugin add D:\DevProjects\Eclipse_workplace\mmir-plugin-speech-android


## FILES

the MMIR modules the give access to the speech recognition / synthesis will be copied
from the plugin directory 

    /www/asrAndroid.js
    /www/ttsAndroid.js
 
into into the platform folders of the www-resource files to: 

    /www/mmirf/env/media/*

 
## MMIR CONFIGURATION

for configuring the MMIR app to use this plugin/module for its speech input/output do the following: 

edit the configuration file in 

    /www/config/configuration.json
 
modify or add (if it does not exist already) the configuration entries
for the MediaManager plugins, i.e. edit the JSON file to: 
```javascript
{
 ...

    "mediaManager": {
    	"plugins": {
    		"browser": [
    			...
    		],
    		"cordova": ["cordovaAudioOutput",
    		            "asrAndroid",
    		            "ttsAndroid"
    		]
    	}
    }

 ...
}
```
i.e. change (or add) the `"cordova"` array entries to `"asrAndroid.js"` and `"ttsAndroid.js"`
in order to use the 'native' Android ASR and TTS engine, when the application is run as Cordova app
on Android.


# API
----


### Speech Input

A general description for the MMIR speech input API can be found in the [wiki][3].

```javascript
mmir.media.recognize([options: Options, statusCallback: Function, failureCallback: Function])
mmir.media.startRecord([options: Options, statusCallback: Function, failureCallback: Function])
```

supported Options by this plugin:  
 * `success: OPTIONAL Function`, the status-callback (see arg statusCallback in [wiki][3])
 * `error: OPTIONAL Function`, the error callback (see arg failureCallback in [wiki][3])
 * `language: OPTIONAL String`, the language for recognition (if omitted, the current language setting is used)
 * `intermediate: OTPIONAL Boolean`, set true for receiving intermediate results (NOTE not all ASR engines may support intermediate results)
 * `results: OTPIONAL Number`, set how many recognition alternatives should be returned at most (NOTE not all ASR engines may support this option)
 * `mode: OTPIONAL "search" | "dictation"`, set how many recognition alternatives should be returned at most (NOTE not all ASR engines may support this option)
 * `eosPause: OTPIONAL "short" | "long"`, length of pause after speech for end-of-speech detection (NOTE not all ASR engines may support this option)
 * `disableImprovedFeedback: OTPIONAL Boolean`, disable improved feedback when using intermediate results (NOTE not all ASR engines may support this option)

### Speech Output

A general description for the MMIR speech output API can be found in the [wiki][4].

```javascript
mmir.media.tts(options: Options | string | Array<string>[, onPlayedCallback: Function, failureCallback: Function, onReadyCallback: Function])
```

supported Options by this plugin:
 * `text: String | String[]`, text that should be read aloud
 * `pauseDuration: OPTIONAL Number`, the length of the pauses between sentences (i.e. for String Arrays) in milliseconds
 * `language: OPTIONAL String`, the language for synthesis (if omitted, the current language setting is used)
 * `voice: OPTIONAL String`, the voice (language specific) for synthesis; NOTE that the specific available voices depend on the TTS engine
 * `success: OPTIONAL Function`, the on-playing-completed callback (see arg onPlayedCallback in [wiki][4])
 * `error: OPTIONAL Function`, the error callback (see arg failureCallback in [wiki][4])
 * `ready: OPTIONAL Function`, the audio-ready callback (see arg onReadyCallback in [wiki][4])


_Note: the function `textToSpeech()` is a deprecated alias for `tts()`_




# DEVELOPMENT AND BUILDING THE PLUGIN
------

NOTE:
"building" is not necessary for using the plugin, but it
may provide helpful feedback during plugin development.

This project requires Cordova 5.x for building the Java source.

You can checkout the CordovaLib project from a repository and then
reference the checked-out project from this project:

(1) checkout the Cordova5Lib project into the same Eclipse workspace: 

    t.b.a.: XXXX/Cordova5Lib 

(2) (in Eclipse) open the project Properties for this project, goto "Java Build Path", open tab "Projects"
 and add the CordovaLib project (you may also need to clean / rebuild the project).



[1]: https://github.com/mmig/mmir
[2]: https://github.com/mmig/mmir/wiki/3.9.2-Speech-Processing-in-MMIR
[3]: https://github.com/mmig/mmir/wiki/3.9.2-Speech-Processing-in-MMIR#speech-input-api
[4]: https://github.com/mmig/mmir/wiki/3.9.2-Speech-Processing-in-MMIR#speech-output-api
