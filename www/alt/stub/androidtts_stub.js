﻿/*
 * 	Copyright (C) 2012-2015 DFKI GmbH
 * 	Deutsches Forschungszentrum fuer Kuenstliche Intelligenz
 * 	German Research Center for Artificial Intelligence
 * 	http://www.dfki.de
 * 
 * 	Permission is hereby granted, free of charge, to any person obtaining a 
 * 	copy of this software and associated documentation files (the 
 * 	"Software"), to deal in the Software without restriction, including 
 * 	without limitation the rights to use, copy, modify, merge, publish, 
 * 	distribute, sublicense, and/or sell copies of the Software, and to 
 * 	permit persons to whom the Software is furnished to do so, subject to 
 * 	the following conditions:
 * 
 * 	The above copyright notice and this permission notice shall be included 
 * 	in all copies or substantial portions of the Software.
 * 
 * 	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 * 	OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * 	MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * 	IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 * 	CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
 * 	TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
 * 	SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

var exec = require('cordova/exec');

/**
 */
function AndroidSpeechSynthesisPlugin() {
}

AndroidSpeechSynthesisPlugin.STOPPED = 0;
AndroidSpeechSynthesisPlugin.INITIALIZING = 1;
AndroidSpeechSynthesisPlugin.STARTED = 2;

/**
 * Play the passed in text as synthesized speech
 * @function speak 
 * @param {DOMString} text
 * @param {DOMString} language
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidSpeechSynthesisPlugin.prototype.tts = function(text, language, successCallback, errorCallback) {
	
//     return exec(successCallback, errorCallback, "AndroidSpeechSynthesisPlugin", "speak", [text, language]);
	if(errorCallback){
		errorCallback('Error for tts(): not implemented');
	}
};

/**
 * @deprecated use #tts function instead (NOTE the different order of the arguments!)
 */
AndroidSpeechSynthesisPlugin.prototype.speak = function(text, successCallback, errorCallback, language) {
//    return this.tts(text, language, successCallback, errorCallback);
	if(errorCallback){
		errorCallback('Error for speak(): not implemented');
	}
};

/** 
 * Play silence for the number of ms passed in as duration
 * 
 * @function silence 
 * @param {long} duration
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidSpeechSynthesisPlugin.prototype.silence = function(duration, successCallback, errorCallback) {
//     return exec(successCallback, errorCallback, "AndroidSpeechSynthesisPlugin", "silence", [duration]);
	if(errorCallback){
		errorCallback('Error for silence(): not implemented');
	}
};

/**
 * Starts up the AndroidSpeechSynthesisPlugin Service
 * 
 * @function startup 
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidSpeechSynthesisPlugin.prototype.startup = function(successCallback, errorCallback) {
	
//     return exec(successCallback, errorCallback, "AndroidSpeechSynthesisPlugin", "startup", []);
	if(errorCallback){
		errorCallback('Error for startup(): not implemented');
	}
};

/**
 * Shuts down the AndroidSpeechSynthesisPlugin Service if you no longer need it.
 * 
 * @function shutdown 
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidSpeechSynthesisPlugin.prototype.shutdown = function(successCallback, errorCallback) {
//     return exec(successCallback, errorCallback, "AndroidSpeechSynthesisPlugin", "shutdown", []);
	if(errorCallback){
		errorCallback('Error for shutdown(): not implemented');
	}
};

/**
 * Finds out if the language is currently supported by the AndroidSpeechSynthesisPlugin service.
 * 
 * @function isLanguageAvailable 
 * @param {DOMSting} lang
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidSpeechSynthesisPlugin.prototype.isLanguageAvailable = function(lang, successCallback, errorCallback) {
//     return exec(successCallback, errorCallback, "AndroidSpeechSynthesisPlugin", "isLanguageAvailable", [lang]);
	if(errorCallback){
		errorCallback('Error for isLanguageAvailable(): not implemented');
	}
};

/**
 * Finds out the current language of the AndroidSpeechSynthesisPlugin service.
 * 
 * @function successCallback 
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidSpeechSynthesisPlugin.prototype.getLanguage = function(successCallback, errorCallback) {
//     return exec(successCallback, errorCallback, "AndroidSpeechSynthesisPlugin", "getLanguage", []);
     if(errorCallback){
 		errorCallback('Error for getLanguage(): not implemented');
 	}
};

/**
 * Finds out the current voice of the AndroidSpeechSynthesisPlugin service.
 * 
 * @function getVoice 
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidSpeechSynthesisPlugin.prototype.getVoice = function(successCallback, errorCallback) {
//    return exec(successCallback, errorCallback, "AndroidSpeechSynthesisPlugin", "getVoice", []);
	if(errorCallback){
 		errorCallback('Error for getVoice(): not implemented');
 	}
};

/**
 * Sets the language of the AndroidSpeechSynthesisPlugin service.
 * 
 * @function setLanguage 
 * @param {String} lang
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidSpeechSynthesisPlugin.prototype.setLanguage = function(lang, successCallback, errorCallback) {
//    return exec(successCallback, errorCallback, "AndroidSpeechSynthesisPlugin", "setLanguage", [lang]);
	if(errorCallback){
 		errorCallback('Error for setLanguage(): not implemented');
 	}
};

/**
 * Sets the voice of the AndroidSpeechSynthesisPlugin service.
 * 
 * @function setVoice 
 * @param {String} voice
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidSpeechSynthesisPlugin.prototype.setVoice = function(voice, successCallback, errorCallback) {
//    return exec(successCallback, errorCallback, "AndroidSpeechSynthesisPlugin", "setVoice", [voice]);
	if(errorCallback){
 		errorCallback('Error for setVoice(): not implemented');
 	}
};


/**
 * Get all available languages of the AndroidSpeechSynthesisPlugin service:
 * <code>successCallback(languageList: Array<string>)</code>
 *
 * @function getLanguages
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidSpeechSynthesisPlugin.prototype.getLanguages = function(successCallback, errorCallback) {
//    return exec(successCallback, errorCallback, "AndroidSpeechSynthesisPlugin", "languageList", []);
	if(errorCallback){
 		errorCallback('Error for getLanguages(): not implemented');
 	}
};

/**
 * Get all available voices of the AndroidSpeechSynthesisPlugin service:
 * <code>successCallback(voiceList: Array<string>)</code>
 *
 * @function getVoices
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidSpeechSynthesisPlugin.prototype.getVoices = function(successCallback, errorCallback) {
//    return exec(successCallback, errorCallback, "AndroidSpeechSynthesisPlugin", "voiceList", []);
	if(errorCallback){
 		errorCallback('Error for getVoices(): not implemented');
 	}
};

/**
 * Get the default language of the AndroidSpeechSynthesisPlugin service:
 * <code>successCallback(language: string)</code>
 *
 * @function getDefaultLanguage
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidSpeechSynthesisPlugin.prototype.getDefaultLanguage = function(successCallback, errorCallback) {
//    return exec(successCallback, errorCallback, "AndroidSpeechSynthesisPlugin", "defaultLanguage", []);
	if(errorCallback){
 		errorCallback('Error for getDefaultLanguage(): not implemented');
 	}
};

/**
 * Get the default voice of the AndroidSpeechSynthesisPlugin service:
 * <code>successCallback(voice: string)</code>
 *
 * @function getDefaultVoice
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidSpeechSynthesisPlugin.prototype.getDefaultVoice = function(successCallback, errorCallback) {
//    return exec(successCallback, errorCallback, "AndroidSpeechSynthesisPlugin", "defaultVoice", []);
	if(errorCallback){
 		errorCallback('Error for getDefaultVoice(): not implemented');
 	}
};


/**
 * Cancel AndroidSpeechSynthesisPlugin TTS (if active; do nothing if not active).
 * 
 * @function cancel
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidSpeechSynthesisPlugin.prototype.cancel = function(successCallback, errorCallback) {
//     return exec(successCallback, errorCallback, "AndroidSpeechSynthesisPlugin", "cancel", []);
	if(errorCallback){
		errorCallback('Error for cancel(): not implemented');
	}
};

module.exports = new AndroidSpeechSynthesisPlugin();