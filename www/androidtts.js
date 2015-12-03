/*
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
	
     return exec(successCallback, errorCallback, "AndroidSpeechSynthesisPlugin", "speak", [text, language]);
};

/**
 * @deprecated use #tts function instead (NOTE the different order of the arguments!)
 */
AndroidSpeechSynthesisPlugin.prototype.speak = function(text, successCallback, errorCallback, language) {
    return this.tts(text, language, successCallback, errorCallback);
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
     return exec(successCallback, errorCallback, "AndroidSpeechSynthesisPlugin", "silence", [duration]);
};

/**
 * Starts up the AndroidSpeechSynthesisPlugin Service
 * 
 * @function startup 
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidSpeechSynthesisPlugin.prototype.startup = function(successCallback, errorCallback) {
	
     return exec(successCallback, errorCallback, "AndroidSpeechSynthesisPlugin", "startup", []);
};

/**
 * Shuts down the AndroidSpeechSynthesisPlugin Service if you no longer need it.
 * 
 * @function shutdown 
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidSpeechSynthesisPlugin.prototype.shutdown = function(successCallback, errorCallback) {
     return exec(successCallback, errorCallback, "AndroidSpeechSynthesisPlugin", "shutdown", []);
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
     return exec(successCallback, errorCallback, "AndroidSpeechSynthesisPlugin", "isLanguageAvailable", [lang]);
};

/**
 * Finds out the current language of the AndroidSpeechSynthesisPlugin service.
 * 
 * @function successCallback 
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidSpeechSynthesisPlugin.prototype.getLanguage = function(successCallback, errorCallback) {
     return exec(successCallback, errorCallback, "AndroidSpeechSynthesisPlugin", "getLanguage", []);
};

/**
 * Sets the language of the AndroidSpeechSynthesisPlugin service.
 * 
 * @function setLanguage 
 * @param {DOMString} lang
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidSpeechSynthesisPlugin.prototype.setLanguage = function(lang, successCallback, errorCallback) {
     return exec(successCallback, errorCallback, "AndroidSpeechSynthesisPlugin", "setLanguage", [lang]);
};

/**
 * Cancel AndroidSpeechSynthesisPlugin TTS (if active; do nothing if not active).
 * 
 * @function cancel
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidSpeechSynthesisPlugin.prototype.cancel = function(successCallback, errorCallback) {
     return exec(successCallback, errorCallback, "AndroidSpeechSynthesisPlugin", "cancel", []);
};

module.exports = new AndroidSpeechSynthesisPlugin();
