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
 * @version 0.8.0
 * @ignore
 */
newMediaPlugin = {
		/**  @memberOf AndroidTextToSpeech# */
		initialize: function(callBack){//, mediaManager){//DISABLED this argument is currently un-used -> disabled
			
			/**  @memberOf AndroidTextToSpeech# */
			var _pluginName = 'androidTextToSpeech';

			/** 
			 * legacy mode: use pre-v4 API of mmir-lib
			 * @memberOf AndroidTextToSpeech#
			 */
			var _isLegacyMode = true;
			/** 
			 * Reference to the mmir-lib core (only available in non-legacy mode)
			 * @type mmir
			 * @memberOf AndroidTextToSpeech#
			 */
			var _mmir = null;
			
			//get mmir-lib core from global namespace:
			_mmir = window[typeof MMIR_CORE_NAME === 'string'? MMIR_CORE_NAME : 'mmir'];
			if(_mmir){
				// set legacy-mode if version is < v4
				_isLegacyMode = _mmir? _mmir.isVersion(4, '<') : true;
			}
			
			/**
			 * HELPER for require(): 
			 * 		use module IDs (and require instance) depending on legacy mode
			 * 
			 * @param {String} id
			 * 			the require() module ID
			 * 
			 * @returns {any} the require()'ed module
			 * 
			 * @memberOf AndroidTextToSpeech#
			 */
			var _req = function(id){
				var name = (_isLegacyMode? '' : 'mmirf/') + id;
				return _mmir? _mmir.require(name) : require(name);
			};
			
			/** 
			 * @type mmir.LanguageManager
			 * @memberOf AndroidTextToSpeech#
			 */
			var languageManager = _req('languageManager');
			/** 
			 * @type mmir.CommonUtils
			 * @memberOf AndroidTextToSpeech#
			 */
			var commonUtils = _req('commonUtils');
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
