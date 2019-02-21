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

AndroidSpeechPlugin.prototype.recognize = function(language, successCallback, failureCallback, withIntermediateResults){
//	 return exec(successCallback,
//   					 failureCallback,
//   					 'AndroidSpeechPlugin',
//   					 'recognize',
//   					 [language, withIntermediateResults? true : false]);
	if(failureCallback){
		failureCallback('Error for recognize(): not implemented');
	}
};

/**
 * @deprecated use #startRecord instead
 */
AndroidSpeechPlugin.prototype.recognizeNoEOS = function(language, successCallback, failureCallback, withIntermediateResults){
//	this.startRecord(language, successCallback, failureCallback, withIntermediateResults);
	if(failureCallback){
		failureCallback('Error for recognizeNoEOS(): not implemented');
	}
};

AndroidSpeechPlugin.prototype.startRecord = function(language, successCallback, failureCallback, withIntermediateResults){
	
//	return exec(successCallback,
//					 failureCallback,
//					 'AndroidSpeechPlugin',
//					 'startRecording',
//					 [language, withIntermediateResults? true : false]);
	if(failureCallback){
		failureCallback('Error for startRecord(): not implemented');
	}
};

AndroidSpeechPlugin.prototype.stopRecord = function(successCallback, failureCallback){

//	 return exec(successCallback,
// 					 failureCallback,
// 					 'AndroidSpeechPlugin',
// 					 'stopRecording',
// 					 []);
	if(failureCallback){
		failureCallback('Error for stopRecord(): not implemented');
	}
};

AndroidSpeechPlugin.prototype.cancel = function(successCallback, failureCallback){

//	 return exec(successCallback,
//   					 failureCallback,
//   					 'AndroidSpeechPlugin',
//   					 'cancel',
//   					 []);

	if(failureCallback){
		failureCallback('Error for cancel(): not implemented');
	}
};

AndroidSpeechPlugin.prototype.getLanguages = function(successCallback, failureCallback){

//	 return exec(successCallback,
//   					 failureCallback,
//   					 'AndroidSpeechPlugin',
//   					 'getSupportedLanguages',
//   					 []);

		if(failureCallback){
			failureCallback('Error for getLanguages(): not implemented');
		}
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
	
//	if(isStart){
//		//start the RMS-changed processing (i.e. fire change-events for microphone-level changed events
//		return exec(function(){console.info('AndroidSpeechPlugin: started processing microphone-levels');},
//				 function(err){console.error('AndroidSpeechPlugin: Error on start processing microphone-levels! ' + err);},
//				 'AndroidSpeechPlugin',
//				 'setMicLevelsListener',
//				 [true]
//		);
//	}
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
	
//	if(isRemoved && this.__micListener.length === 0){
//		//stop RMS-changed processing (no handlers are listening any more!)
//		return exec(function(){console.info('AndroidSpeechPlugin: stopped processing microphone-levels');},
//				 function(err){console.error('AndroidSpeechPlugin: Error on stop processing microphone-levels! ' + err);},
//				 'AndroidSpeechPlugin',
//				 'setMicLevelsListener',
//				 [false]
//		);
//	}
	
	return isRemoved;
};

module.exports = new AndroidSpeechPlugin();

