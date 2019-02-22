;(function (root, factory) {

	//mmir legacy mode: use pre-v4 API of mmir-lib
	var _isLegacyMode3 = true;
	var _isLegacyMode4 = true;
	var mmirName = typeof MMIR_CORE_NAME === 'string'? MMIR_CORE_NAME : 'mmir';
	var _mmir = root[mmirName];
	if(_mmir){
		//set legacy-mode if version is < v4 (isVersion() is available since v4)
		_isLegacyMode3 = _mmir.isVersion? _mmir.isVersion(4, '<') : true;
		_isLegacyMode4 = _mmir.isVersion? _mmir.isVersion(5, '<') : true;
	}
	var _req = _mmir? _mmir.require : require;

	if(_isLegacyMode3){

		// HELPER: backwards compatibility v3 for module IDs
		var isArray = _req('util/isArray');
		var getId = function(ids){
			if(ids){
				if(isArray(ids)){
					return ids.map(function(id){ return id.replace(/^mmirf\//, '');})
				}
				return ids.replace(/^mmirf\//, '');
			}
			return ids;
		};
		var __req = _req;
		_req = function(deps, id, success, error){
			var args = [getId(deps), getId(id), success, error];
			return __req.apply(null, args);
		};

		//HELPER: backwards compatibility v3 for configurationManager.get():
		var config = _req('configurationManager');
		if(!config.__get){
			config.__get = config.get;
			config.get = function(propertyName, useSafeAccess, defaultValue){
				return this.__get(propertyName, defaultValue, useSafeAccess);
			};
		}

	}

	if(_isLegacyMode3 || _isLegacyMode4){

		//backwards compatibility v3 and v4:
		//  plugin instance is "exported" to global var newMediaPlugin
		root['newMediaPlugin'] = factory(_req);

	} else {

		if (typeof define === 'function' && define.amd) {
				// AMD. Register as an anonymous module.
				define(['require'], function (require) {
						return factory(require);
				});
		} else if (typeof module === 'object' && module.exports) {
				// Node. Does not work with strict CommonJS, but
				// only CommonJS-like environments that support module.exports,
				// like Node.
				module.exports = factory(_req);
		}
	}

}(typeof window !== 'undefined' ? window : typeof self !== 'undefined' ? self : typeof global !== 'undefined' ? global : this, function (require) {

	
/**
 * part of Cordova plugin: mmir-plugin-speech-android
 * @version 1.0.0
 * @ignore
 */


	
	return {initialize: function (){
	  var origArgs = arguments;
	  require(['mmirf/mediaManager', 'mmirf/configurationManager', 'mmirf/languageManager', 'mmirf/util/isArray', 'mmirf/logger'], function (mediaManager, config, lang, isArray, Logger){
    var origInit = (function(){
      {

return {
	/**  @memberOf AndroidTextToSpeech# */
	initialize: function(callBack, mediaManager, contextId){

		/**  @memberOf AndroidTextToSpeech# */
		var _pluginName = 'ttsAndroid';

		/** 
		 * @type mmir.Logger
		 * @memberOf AndroidTextToSpeech#
		 */
		var logger = Logger.create(_pluginName);

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

		//set log-level from configuration (if there is setting)
		var loglevel = config.get([_pluginName, 'logLevel']);
		if(typeof loglevel !== 'undefined'){
			logger.setLevel(loglevel);
		}

		//initialize the TTS plugin (with the current language setting)
		androidTtsPlugin.startup(

				function(data){

					logger.info('AndroidTTS.js.startup: success -> '+JSON.stringify(data));

					language = lang.getLanguageConfig(_pluginName);
					//TODO get & set voice (API in plugin is missing for that ... currently...)
					//var voice = lang.getLanguageConfig(_pluginName, 'voice');

					androidTtsPlugin.setLanguage(
							language,
							function(data){
								logger.info('AndroidTTS.js.setLanguage('+language+'): success -> '+JSON.stringify(data));
							}, function(e){
								logger.warn('AndroidTTS.js.setLanguage('+language+'): error -> '+JSON.stringify(e));
								language = void(0);
							}
					);

				}, function(e){
					logger.info('AndroidTTS.js.startup: error -> '+JSON.stringify(e));
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
							logger.debug('AndroidTTS.js: started.');//FIXME debug (use mediamanager's logger instead)
						}
					}
					else if(msg.type === return_types.TTS_DONE){
						isHandled = true;
						if(onEnd){
							onEnd(msg.message);
						} else {
							logger.debug('AndroidTTS.js: finished.');//FIXME debug (use mediamanager's logger instead)
						}
					}
				}

				if(isHandled === false) {
					//DEFALT: treat callback-invocation as DONE callback

					logger.warn('AndroidTTS.js: success-callback invoked without result / specific return-message.');//FIXME debug (use mediamanager's logger instead)

					if(onEnd){
						onEnd();
					} else {
						logger.debug('AndroidTTS.js: finished.');//FIXME debug (use mediamanager's logger instead)
					}
				}
			};
		}

		//invoke the passed-in initializer-callback and export the public functions:
		callBack({
			/**
			 * @deprecated use {@link #tts} instead
			 * @memberOf AndroidTextToSpeech.prototype
			 */
			textToSpeech: function(){
				return mediaManager.perform(contextId, 'tts', arguments);
			},
			/**
			 * Synthesizes ("read out loud") text.
			 * 
			 * @param {String|Array<String>|PlainObject} [options] OPTIONAL
			 * 		if <code>String</code> or <code>Array</code> of <code>String</code>s
			 * 			  synthesizes the text of the String(s).
			 * 			  <br>For an Array: each entry is interpreted as "sentence";
			 * 				after each sentence, a short pause is inserted before synthesizing the
			 * 				the next sentence<br>
			 * 		for a <code>PlainObject</code>, the following properties should be used:
			 * 		<pre>{
			 * 			  text: String | String[], text that should be read aloud
			 * 			, pauseDuration: OPTIONAL Number, the length of the pauses between sentences (i.e. for String Arrays) in milliseconds
			 * 			, language: OPTIONAL String, the language for synthesis (if omitted, the current language setting is used)
			 * 			, voice: OPTIONAL String, the voice (language specific) for synthesis; NOTE that the specific available voices depend on the TTS engine
			 * 			, success: OPTIONAL Function, the on-playing-completed callback (see arg onPlayedCallback)
			 * 			, error: OPTIONAL Function, the error callback (see arg failureCallback)
			 * 			, ready: OPTIONAL Function, the audio-ready callback (see arg onReadyCallback)
			 * 		}</pre>
			 * 
			 * @param {Function} [onPlayedCallback] OPTIONAL
			 * 			callback that is invoked when the audio of the speech synthesis finished playing:
			 * 			<pre>onPlayedCallback()</pre>
			 * 
			 * 			<br>NOTE: if used in combination with <code>options.success</code>, this argument will supersede the options
			 * 
			 * @param {Function} [failureCallback] OPTIONAL
			 * 			callback that is invoked in case an error occurred:
			 * 			<pre>failureCallback(error: String | Error)</pre>
			 * 
			 * 			<br>NOTE: if used in combination with <code>options.error</code>, this argument will supersede the options
			 * 
			 * @param {Function} [onReadyCallback] OPTIONAL
			 * 			callback that is invoked when audio becomes ready / is starting to play.
			 * 			If, after the first invocation, audio is paused due to preparing the next audio,
			 * 			then the callback will be invoked with <code>false</code>, and then with <code>true</code>
			 * 			(as first argument), when the audio becomes ready again, i.e. the callback signature is:
			 * 			<pre>onReadyCallback(isReady: Boolean, audio: IAudio)</pre>
			 * 
			 * 			<br>NOTE: if used in combination with <code>options.ready</code>, this argument will supersede the options
			 * 
			 * @public
			 * @memberOf AndroidTextToSpeech.prototype
			 * @see mmir.MediaManager#textToSpeech
			 */
			tts: function (options, endCallBack, failureCallback, onReadyCallback){

				//convert first argument to options-object, if necessary
				if(typeof options === 'string' || isArray(options)){
					options = {text: options};
				}

				if(endCallBack){
					options.success = endCallBack;
				}

				if(failureCallback){
					options.error = failureCallback;
				}

				if(onReadyCallback){
					options.ready = onReadyCallback;
				}

				options.language = options.language? options.language : lang.getLanguageConfig(_pluginName);

				options.pauseDuration = options.pauseDuration? options.pauseDuration : void(0);
				options.voice = options.voice? options.voice : lang.getLanguageConfig(_pluginName, 'voice');

//							var text;
//							if((typeof options !== 'undefined') && isArray(options) ){
//							text = options.join('\n');
//							}
//							else {
//							text = options;
//							}

				var text = options.text;

				try{
					//only set language in native plugin, if necessary
					var locale = options.language !== language? options.language : void(0);

					//TODO handle more options: voice

					androidTtsPlugin.tts(
							text, locale,
							createSuccessWrapper(options.success, options.ready),
							failureCallback,
							options.pauseDuration,
							options.voice
					);

				} catch(e){
					if(options.error){
						options.error(e);
					} else {
						logger.error(e);
					}
				}

			},
			/**
			 * @public
			 * @memberOf AndroidTextToSpeech.prototype
			 * @see mmir.MediaManager#cancelSpeech
			 */
			cancelSpeech: function(successCallback,failureCallback){

				androidTtsPlugin.cancel(
						successCallback, 
						failureCallback
				);

			}
		});	
		
	}//END: initialize()

};

}
    })();
    origInit.initialize.apply(null, origArgs);
});;
	}};


	//END: define()


}));