# mmir-plugin-speech-android
----

Cordova plugin (3.x) for the MMIR framework for accessing Android's system speech recognition and synthesis



The plugin provides access to Android's speech recognition service (i.e. it does not use / trigger
the default graphical interface when accessing using recognition via Intents).


This Cordova plugin is specifically targeted to be used with the [MMIR framework][1]:
On adding the plugin 2 MMIR "modules" (for recognition and synthesis) will be copied
into the platform's resource folders `<www assets>/mmirf/env/media/android*.js`

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
  D:\DevProjects\Eclipse_workplace\mmir-plugin-androidspeech

execute the following command in Cordova project's root directory: 
 cordova plugin add D:\DevProjects\Eclipse_workplace\mmir-plugin-androidspeech


## FILES

the MMIR modules the give access to the speech recognition / synthesis will be copied
    from the plugin directory 

 /www/androidAudioInput.js
 /www/androidTextToSpeech.js
 
into into the platform folders of the www-resource files to: 

 /www/mmirf/env/media/*

 
## MMIR CONFIGURATION

for configuring the MMIR app to use this plugin/module for its speech input/output do the following: 

edit the configuration file in 

 /www/config/configuration.json
 
modify of add (if it does not exist already) the configuration entries
for the MediaManager plugins, i.e. edit the JSON file to: 

{
 ...

    "mediaManager": {
    	"plugins": {
    		"browser": ["html5AudioOutput.js",
    		            "webkitAudioInput.js",
    		            "maryTextToSpeech.js"
    		],
    		"cordova": ["cordovaAudioOutput.js",
    		            "androidAudioInput.js",
    		            "androidTextToSpeech.js"
    		]
    	}
    }

 ...
}

change the "cordova" array entries to "androidAudioInput.js" and "androidTextToSpeech.js"
in order to use the 'native' Android ASR and TTS engine, when the application is run as Cordova app
on Android.



## DEVELOPMENT AND BUILD THE PLUGIN
------

NOTE:
"building" is not necessary for using the plugin, but it
may provide helpful feedback during plugin development.

This project requires Cordova 3.x for building the Java source.

You can checkout the CordovaLib project from a repository and then
reference the checked-out project from this project:

(1) checkout the CordovaLib project into the same Eclipse workspace: 
XXXX/CordovaLib TBA 

(2) (in Eclipse) open the project Properties for this project, goto "Java Build Path", open tab "Projects"
 and add the CordovaLib project (you may also need to clean / rebuild the project).


# API
----
t.b.d.


[1]: https://github.com/mmig/mmir
