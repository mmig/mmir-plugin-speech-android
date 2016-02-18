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

/**
 * part of Cordova plugin: dfki-mmir-plugin-speech-android
 * @version 0.7.7
 * @ignore
 */
newMediaPlugin = {
		/**  @memberOf AndroidTextToSpeech# */
		initialize: function(callBack){//, mediaManager){//DISABLED this argument is currently un-used -> disabled
			
			/**  @memberOf AndroidTextToSpeech# */
			var _pluginName = 'androidTextToSpeech';
			/** 
			 * @type mmir.LanguageManager
			 * @memberOf AndroidTextToSpeech#
			 */
			var languageManager = require('languageManager');
			/** 
			 * @type mmir.CommonUtils
			 * @memberOf AndroidTextToSpeech#
			 */
			var commonUtils = require('commonUtils');
			/** 
			 * @type AndroidSpeechSynthesisPlugin
			 * @memberOf AndroidTextToSpeech#
			 */
			var androidTtsPlugin = window.cordova.plugins.androidTtsPlugin;
			/** 
			 * @type String
			 * @memberOf AndroidTextToSpeech#
			 */
			var language;
			
			/** 
			 * @type Enum<String>
			 * @memberOf AndroidTextToSpeech#
			 */
			var return_types = {
					"TTS_BEGIN": "TTS_BEGIN",
					"TTS_DONE": "TTS_DONE"
			};
			
			//initialize the TTS plugin (with the current language setting)
			androidTtsPlugin.startup(
				
				function(data){
					
					console.info('AndroidTTS.js.startup: success -> '+JSON.stringify(data));
					
					language = languageManager.getLanguageConfig(_pluginName);
					//TODO get & set voice (API in plugin is missing for that ... currently...)
					//var voice = languageManager.getLanguageConfig(_pluginName, 'voice');
					
					androidTtsPlugin.setLanguage(
							language,
						function(data){
							console.info('AndroidTTS.js.setLanguage('+language+'): success -> '+JSON.stringify(data));
						}, function(e){
							console.info('AndroidTTS.js.setLanguage('+language+'): error -> '+JSON.stringify(e));
							language = void(0);
						}
					);
					
				}, function(e){
					console.info('AndroidTTS.js.startup: error -> '+JSON.stringify(e));
				}
			);
			//TODO destructor: register onpause/exit handler that shuts down the TTS engine
			
			/** 
			 * @type Function
			 * @memberOf AndroidTextToSpeech#
			 */
			function createSuccessWrapper(onEnd, onStart){
				return function(msg){
					
					var isHandled = false;
					if(msg){
						
						if(msg.type === return_types.TTS_BEGIN){
							isHandled = true;
							if(onStart){
								onStart(msg.message);
							} else {
								console.debug('AndroidTTS.js: started.');//FIXME debug (use mediamanager's logger instead)
							}
						}
						else if(msg.type === return_types.TTS_DONE){
							isHandled = true;
							if(onEnd){
								onEnd(msg.message);
							} else {
								console.debug('AndroidTTS.js: finished.');//FIXME debug (use mediamanager's logger instead)
							}
						}
					}
					
					if(isHandled === false) {
						//DEFALT: treat callback-invocation as DONE callback
						
						console.warn('AndroidTTS.js: success-callback invoked without result / specific return-message.');//FIXME debug (use mediamanager's logger instead)
						
						if(onEnd){
							onEnd();
						} else {
							console.debug('AndroidTTS.js: finished.');//FIXME debug (use mediamanager's logger instead)
						}
					}
				};
			}
			
			//invoke the passed-in initializer-callback and export the public functions:
			callBack({
					/**
					 * @public
					 * @memberOf AndroidTextToSpeech.prototype
					 * @see mmir.MediaManager#textToSpeech
					 */
				    textToSpeech: function (parameter, endCallBack, failureCallBack, startCallBack){
				    	
//				    	var text;
//			    		if((typeof parameter !== 'undefined') && commonUtils.isArray(parameter) ){
//			    			text = parameter.join('\n');
//			    		}
//			    		else {
//			    			text = parameter;
//			    		}
			    		
			    		//FIXME implement evaluation / handling the parameter similar to treatment in maryTextToSpeech.js
		    			var text = parameter;
			    		
				    	try{
				    		var currentLanguage = languageManager.getLanguageConfig(_pluginName);
				    		currentLanguage = currentLanguage !== language? currentLanguage : void(0);
				    		
			    			androidTtsPlugin.tts(
					    			text,
					    			currentLanguage,
					    			createSuccessWrapper(endCallBack, startCallBack),
					    			failureCallBack
					    	);
				    		
				    	} catch(e){
				    		if(failureCallBack){
				    			failureCallBack(e);
				    		}
				    	}
				    	
				    },
				    /**
					 * @public
					 * @memberOf AndroidTextToSpeech.prototype
					 * @see mmir.MediaManager#cancelSpeech
					 */
	    			cancelSpeech: function(successCallBack,failureCallBack){
	    				
				    	androidTtsPlugin.cancel(
				    			successCallBack, 
				    			failureCallBack
				    	);
				    	
	    			}
				});	
		}
};
