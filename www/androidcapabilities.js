
var exec = require('cordova/exec');

/**
 * Plugin for checking Speech Recognition (ASR) and Synthesis (TTS) capabilities,
 * and for installing necessary apps / requesting necessary permissions.
 */
function AndroidSpeechCapabilityPlugin() {
}

/**
 * Check if functionality (or permissions) are available (granted)
 *
 * @function check
 * @param {String} functionality	the functionality to ckeck: on of
 * 										"asr": speech recognition (app) available
 * 										"tts": speech synthesis (app) available
 * 										"tts-language": speech synthesis language(s) available
 * 										"tts-permission": speech synthesis permission granted
 * 										"asr-permission": speech recognition permission granted
 * @param {Object} successCallback	success callback: <code>success(result)</code> where result is
 * 										<pre>{
 * 											  "asrAvailable": BOOLEAN | UNDEFINED
 * 											  "ttsAvailable": BOOLEAN | UNDEFINED
 * 											  "ttsLanguageAvailable": BOOLEAN | UNDEFINED
 * 											  "asrPermission": BOOLEAN | UNDEFINED
 * 											  "ttsPermission": BOOLEAN | UNDEFINED
 * 											  "description": STRING | UNDEFINED
 * 										}
 * 										</pre>
 * 										With <code>boolean</code> values corresponding the <code>functionality</code> input argument.
 * 										The "description" field may contain details, e.g. the reason why some functionality is not available.
 * @param {Object} errorCallback
 */
AndroidSpeechCapabilityPlugin.prototype.check = function(functionality, successCallback, errorCallback) {

	var action;
	switch(functionality){
		case "asr":
			action = "asr_check";
			break;
		case "tts":
			action = "tts_check";
			break;
		case "tts-language":
			action = "language_tts_check";
			break;
		case "tts-permission":
			action = "tts_permissions_check";
			break;
		case "asr-permission":
			action = "asr_permissions_check";
			break;
		default:
			var msg = "AndroidSpeechCapabilityPlugin.check: trying to check unknown functionality "+functionality;
			return errorCallback? errorCallback(msg) : console.error(msg);
	}

	//TODO support custom dialog labels
	var args = [];

	return exec(successCallback, errorCallback, "AndroidSpeechCapabilityPlugin", action, args);
};


/**
 * Request required permissions from user
 *
 * @function check
 * @param {String} functionality	the functionality to ckeck: on of
 * 										"asr": request speech recognition permission(s)
 * 										"tts": request speech synthesis permission(s)
 * @param {Object} successCallback	success callback: <code>success(result)</code> where result is
 * 										<pre>{
 * 											  "asrPermission": BOOLEAN | UNDEFINED
 * 											  "ttsPermission": BOOLEAN | UNDEFINED
 * 											  "description": STRING | UNDEFINED
 * 										}
 * 										</pre>
 * 										the "description" field may contain details, e.g. the reason why some functionality is not available
 * @param {Object} errorCallback
 */
AndroidSpeechCapabilityPlugin.prototype.request = function(functionality, successCallback, errorCallback) {

	var action;
	switch(functionality){
		case "asr":
			action = "asr_ask_permissions";
			break;
		case "tts":
			action = "tts_ask_permissions";
			break;
		default:
			var msg = "AndroidSpeechCapabilityPlugin.request: requested unknown functionality "+functionality;
			return errorCallback? errorCallback(msg) : console.error(msg);
	}

	//TODO support custom dialog labels
	var args = [];

	return exec(successCallback, errorCallback, "AndroidSpeechCapabilityPlugin", action, args);
};

module.exports = new AndroidSpeechCapabilityPlugin();
