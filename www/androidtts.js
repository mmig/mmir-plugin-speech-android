
var exec = require('cordova/exec');

/**
 */
function AndroidTTSPlugin() {
}

AndroidTTSPlugin.STOPPED = 0;
AndroidTTSPlugin.INITIALIZING = 1;
AndroidTTSPlugin.STARTED = 2;

/**
 * Play the passed in text as synthesized speech
 * @function speak
 * @param {String|Array<String>} text
 * @param {String} language
 * @param {Object} successCallback
 * @param {Object} errorCallback
 * @param {Number} [pauseDuration]
 * @param {String} [voice]
 */
AndroidTTSPlugin.prototype.tts = function(text, language, successCallback, errorCallback, pauseDuration, voice) {

	var args = [text, language];
	if(typeof pauseDuration === 'number'){
		args.push(pauseDuration);
	} else if(pauseDuration === 'string'){
		//shift arg
		voice = pauseDuration;
	}
	if(voice){
		if(args.length === 2){
			//args for native plugin are positional: add default pause if necessary
			args.push(-1);
		}
		args.push(voice);
	}

	return exec(successCallback, errorCallback, "AndroidTTSPlugin", "speak", args);
};

/**
 * @deprecated use #tts function instead (NOTE the different order of the arguments!)
 */
AndroidTTSPlugin.prototype.speak = function(text, successCallback, errorCallback, language, pauseDuration) {
	return this.tts(text, language, successCallback, errorCallback, pauseDuration);
};

/**
 * Play silence for the number of ms passed in as duration
 *
 * @function silence
 * @param {long} duration
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidTTSPlugin.prototype.silence = function(duration, successCallback, errorCallback) {
	return exec(successCallback, errorCallback, "AndroidTTSPlugin", "silence", [duration]);
};

/**
 * Starts up the AndroidTTSPlugin Service
 *
 * @function startup
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidTTSPlugin.prototype.startup = function(successCallback, errorCallback) {

	return exec(successCallback, errorCallback, "AndroidTTSPlugin", "startup", []);
};

/**
 * Shuts down the AndroidTTSPlugin Service if you no longer need it.
 *
 * @function shutdown
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidTTSPlugin.prototype.shutdown = function(successCallback, errorCallback) {
	return exec(successCallback, errorCallback, "AndroidTTSPlugin", "shutdown", []);
};

/**
 * Finds out if the language is currently supported by the AndroidTTSPlugin service.
 *
 * @function isLanguageAvailable
 * @param {Sting} lang
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidTTSPlugin.prototype.isLanguageAvailable = function(lang, successCallback, errorCallback) {
	return exec(successCallback, errorCallback, "AndroidTTSPlugin", "isLanguageAvailable", [lang]);
};

/**
 * Finds out the current language of the AndroidTTSPlugin service.
 *
 * @function getLanguage
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidTTSPlugin.prototype.getLanguage = function(successCallback, errorCallback) {
	return exec(successCallback, errorCallback, "AndroidTTSPlugin", "getLanguage", []);
};

/**
 * Finds out the current voice of the AndroidTTSPlugin service.
 *
 * @function getVoice
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidTTSPlugin.prototype.getVoice = function(successCallback, errorCallback) {
	return exec(successCallback, errorCallback, "AndroidTTSPlugin", "getVoice", []);
};

/**
 * Sets the language of the AndroidTTSPlugin service.
 *
 * @function setLanguage
 * @param {String} lang
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidTTSPlugin.prototype.setLanguage = function(lang, successCallback, errorCallback) {
	return exec(successCallback, errorCallback, "AndroidTTSPlugin", "setLanguage", [lang]);
};

/**
 * Sets the voice of the AndroidTTSPlugin service.
 *
 * @function setVoice
 * @param {String} voice
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidTTSPlugin.prototype.setVoice = function(voice, successCallback, errorCallback) {
	return exec(successCallback, errorCallback, "AndroidTTSPlugin", "setVoice", [voice]);
};

/**
 * Cancel AndroidTTSPlugin TTS (if active; do nothing if not active).
 *
 * @function cancel
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidTTSPlugin.prototype.cancel = function(successCallback, errorCallback) {
	return exec(successCallback, errorCallback, "AndroidTTSPlugin", "cancel", []);
};

/**
 * Get all available languages of the AndroidTTSPlugin service:
 * <code>successCallback(languageList: Array<string>)</code>
 *
 * @function getLanguages
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidTTSPlugin.prototype.getLanguages = function(successCallback, errorCallback) {
	return exec(successCallback, errorCallback, "AndroidTTSPlugin", "languageList", []);
};

/**
 * Get all available voices of the AndroidTTSPlugin service:
 * <code>successCallback(voiceList: Array<string>)</code>
 *
 * @function getVoices
 * @param {String} [language] language code: if specified, only voices for matching the language will be returned
 * @param {Boolean} [includeDetails] if TRUE, the returned list will be comprised of entries with {name: STRING, language: STRING, gender: "female" | "male"}
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidTTSPlugin.prototype.getVoices = function(language, includeDetails, successCallback, errorCallback) {

	if(typeof language === 'function'){
		errorCallback = includeDetails;
		successCallback = language;
		language = '';
		includeDetails = false;
	} else if(typeof includeDetails === 'function'){
		errorCallback = successCallback;
		successCallback = includeDetails;
		if(typeof language === 'boolean'){
			includeDetails = language;
			language = '';
		} else {
			includeDetails = false;
		}
	}
	var args = [language, includeDetails];

	return exec(successCallback, errorCallback, "AndroidTTSPlugin", "voiceList", args);
};

/**
 * Get the default language of the AndroidTTSPlugin service:
 * <code>successCallback(language: string)</code>
 *
 * @function getDefaultLanguage
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidTTSPlugin.prototype.getDefaultLanguage = function(successCallback, errorCallback) {
	return exec(successCallback, errorCallback, "AndroidTTSPlugin", "defaultLanguage", []);
};

/**
 * Get the default voice of the AndroidTTSPlugin service:
 * <code>successCallback(voice: string)</code>
 *
 * @function getDefaultVoice
 * @param {Object} successCallback
 * @param {Object} errorCallback
 */
AndroidTTSPlugin.prototype.getDefaultVoice = function(successCallback, errorCallback) {
	return exec(successCallback, errorCallback, "AndroidTTSPlugin", "defaultVoice", []);
};

module.exports = new AndroidTTSPlugin();
