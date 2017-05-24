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
 *  
 * @return Instance of AndroidSpeechPlugin
 */
var AndroidSpeechPlugin = function() {
	//list of listeners for the "microphone levels changed" event
	this.__micListener = [];
};

AndroidSpeechPlugin.prototype.recognize = function(language, successCallback, failureCallback, withIntermediateResults, maxAlternatives, languageModel){
	
	var args = [language, withIntermediateResults? true : false];

	if(typeof maxAlternatives === 'number'){
		args.push(maxAlternatives);
	}
	
	if(typeof languageModel === 'string' && languageModel){
		args.push(languageModel);
	}
	
	return exec(successCallback,
					failureCallback,
					'AndroidSpeechPlugin',
					'recognize',
					args);
};

/**
 * @deprecated use #startRecord instead
 */
AndroidSpeechPlugin.prototype.recognizeNoEOS = function(language, successCallback, failureCallback, withIntermediateResults){
	return this.startRecord.apply(this, arguments);
};

AndroidSpeechPlugin.prototype.startRecord = function(language, successCallback, failureCallback, withIntermediateResults, maxAlternatives, languageModel){
	
	var args = [language, withIntermediateResults? true : false];
	
	if(typeof maxAlternatives === 'number'){
		args.push(maxAlternatives);
	}
	
	if(typeof languageModel === 'string' && languageModel){
		args.push(languageModel);
	}
	
	return exec(successCallback,
					 failureCallback,
					 'AndroidSpeechPlugin',
					 'startRecording',
					 args);
};

AndroidSpeechPlugin.prototype.stopRecord = function(successCallback, failureCallback){

	 return exec(successCallback,
 					 failureCallback,
 					 'AndroidSpeechPlugin',
 					 'stopRecording',
 					 []);
};

AndroidSpeechPlugin.prototype.cancel = function(successCallback, failureCallback){

	 return exec(successCallback,
   					 failureCallback,
   					 'AndroidSpeechPlugin',
   					 'cancel',
   					 []);
};

AndroidSpeechPlugin.prototype.getLanguages = function(successCallback, failureCallback){

	 return exec(successCallback,
   					 failureCallback,
   					 'AndroidSpeechPlugin',
   					 'getSupportedLanguages',
   					 []);
};

///**
// * Get the microphone levels ("recording levels") when recording (i.e. when voice recognition is active).
// * 
// * @EXPERIMENTAL
// * 
// * @param successCallback
// * 			callback function which takes one parameter ARRAY:
// * 			An Array of Float values from range [0,90] that were gathered since
// *             the recording was started / the last call of getMicLevels()
// * @param failureCallback
// */
//AndroidSpeechPlugin.prototype.getMicLevels = function(successCallback, failureCallback){
//
//	 return exec(successCallback,
//  					 failureCallback,
//  					 'AndroidSpeechPlugin',
//  					 'getMicLevels',
//  					 []);
//};

/**
 * Functions for listening to the microphone levels
 * 
 * register a handler:
 * 	onMicLevelChanged(listener: Function)
 * 
 * remove a handler:
 *  offMicLevelChanged(listener: Function)
 *  
 * get the list of all currently registered listeners
 *  getMicLevelChangedListeners() : Array[Function]
 * 
 * @EXPERIMENTAL
 */

AndroidSpeechPlugin.prototype.fireMicLevelChanged = function(value){
	for(var i=0, size = this.__micListener.length; i < size; ++i){
		this.__micListener[i](value);
	}
};

AndroidSpeechPlugin.prototype.onMicLevelChanged = function(listener){
	var isStart = this.__micListener.length === 0; 
	this.__micListener.push(listener);
	
	if(isStart){
		//start the RMS-changed processing (i.e. fire change-events for microphone-level changed events
		return exec(function(){console.info('AndroidSpeechPlugin: started processing microphone-levels');},
				 function(err){console.error('AndroidSpeechPlugin: Error on start processing microphone-levels! ' + err);},
				 'AndroidSpeechPlugin',
				 'setMicLevelsListener',
				 [true]
		);
	}
};

AndroidSpeechPlugin.prototype.getMicLevelChangedListeners = function(){
	//return copy of listener-list
	return this.__micListener.slice(0,this.__micListener.length);
};

AndroidSpeechPlugin.prototype.offMicLevelChanged = function(listener){
	var isRemoved = false;
	var size = this.__micListener.length;
	if(size){
		for(var i = size - 1; i >= 0; --i){
			if(this.__micListener[i] ===  listener){
				
				//move all handlers after i by 1 index-position ahead:
				for(var j = size - 1; j > i; --j){
					this.__micListener[j-1] = this.__micListener[j];
				}
				//remove last array-element
				this.__micListener.splice(size-1, 1);
				
				isRemoved = true;
				break;
			}
		}
	}
	
	if(isRemoved && this.__micListener.length === 0){
		//stop RMS-changed processing (no handlers are listening any more!)
		return exec(function(){console.info('AndroidSpeechPlugin: stopped processing microphone-levels');},
				 function(err){console.error('AndroidSpeechPlugin: Error on stop processing microphone-levels! ' + err);},
				 'AndroidSpeechPlugin',
				 'setMicLevelsListener',
				 [false]
		);
	}
	
	return isRemoved;
};

////////////// back-channel from native implementation: ////////////////////////

/**
 * Handles messages from native implementation.
 * 
 * Supported messages:
 * 
 * <ul>
 * 
 * 	<li><u>plugin status</u>:<br>
 * 		<pre>{action: "plugin", "status": STRING}</pre>
 * 	</li>
 * 	<li><u>miclevels</u>:<br>
 * 		<pre>{action: "miclevels", value: NUMBER}</pre>
 * 	</li>
 * </ul>
 */
function onMessageFromNative(msg) {
	
    if (msg.action == 'miclevels') {
    	
    	_instance.fireMicLevelChanged(msg.value);
    	
    } else if (msg.action == 'plugin') {
    	
    	//TODO handle plugin status messages (for now there is only an init-completed message...)
    	
    	console.log('[AndroidSpeechPlugin] Plugin status: "' + msg.status+'"');
    	
    } else {
    	
        throw new Error('[AndroidSpeechPlugin] Unknown action "' + msg.action+'": ', msg);
    }
}

//register back-channel for native plugin when cordova gets available:
if (cordova.platformId === 'android' || cordova.platformId === 'amazon-fireos' || cordova.platformId === 'windowsphone') {

    var channel = require('cordova/channel');

    channel.createSticky('onAndroidSpeechPluginReady');
    channel.waitForInitialization('onAndroidSpeechPluginReady');

    channel.onCordovaReady.subscribe(function() {
        exec(onMessageFromNative, undefined, 'AndroidSpeechPlugin', 'msg_channel', []);
        channel.initializationComplete('onAndroidSpeechPluginReady');
    });
}

var _instance = new AndroidSpeechPlugin();
module.exports = _instance;

