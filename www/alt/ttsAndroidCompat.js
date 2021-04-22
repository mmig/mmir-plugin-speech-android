;(function (root, factory) {

	//mmir legacy mode: use pre-v4 API of mmir-lib
	var _isLegacyMode3 = true;// v3 or below
	var _isLegacyMode4 = true;// v4 or below
	var _isLegacyMode6 = true;// v6 or below
	var mmirName = typeof MMIR_CORE_NAME === 'string'? MMIR_CORE_NAME : 'mmir';
	var _mmir = root[mmirName];
	if(_mmir){
		//set legacy-mode if version is < v4, or < v5, or < v7 (isVersion() is available since v4)
		_isLegacyMode3 = _mmir.isVersion? _mmir.isVersion(4, '<') : true;
		_isLegacyMode4 = _mmir.isVersion? _mmir.isVersion(5, '<') : true;
		_isLegacyMode6 = _mmir.isVersion? _mmir.isVersion(7, '<') : true;
	}
	var _req = _mmir? _mmir.require : require;

	var isArray = _req((_isLegacyMode3? '': 'mmirf/') + 'util/isArray');
	var getId;
	if(_isLegacyMode4){
		// HELPER: backwards compatibility v4 for module IDs
		getId = function(ids){
			if(isArray(ids)){
				return ids.map(function(id){ return getId(id);});
			}
			return ids? ids.replace(/\bresources$/, 'constants') : ids;
		};
		var __req = _req;
		_req = function(deps, success, error, completed){
			var args = [getId(deps), success, error, completed];
			return __req.apply(null, args);
		};
	} else if(!_isLegacyMode3) {
		getId = function(ids){ return ids; };
	}

	if(_isLegacyMode3){
		// HELPER: backwards compatibility v3 for module IDs
		var __getId = getId;
		getId = function(ids){
			if(isArray(ids)) return __getId(ids);
			return ids? __getId(ids).replace(/^mmirf\//, '') : ids;
		};
	}

	var extend, replacedMod;
	if(_isLegacyMode6) {
		extend = _req('mmirf/util/extend');
		//upgrage mmir < v7:
		// proxy require calls from within the wrapped module to replaced
		// implementations if necessary (i.e. isolated changed modules)
		replacedMod = {};
		var ___req = _req;
		_req = function(deps, success, error, completed){
			if(typeof deps === 'string' && replacedMod[getId(deps)]) return replacedMod[getId(deps)];
			if(success){
				var _success = success;
				success = function(){
					deps = getId(deps);
					for(var i=deps.length-1; i >= 0; --i){
						if(deps[i]==='require') arguments[i] = _req;
						else if(replacedMod[deps[i]]) arguments[i] = replacedMod[deps[i]];
					}
					_success.apply(null, arguments);
				};
			}
			return ___req.apply(null, [deps, success, error, completed]);
		}
	}

	if(_isLegacyMode3){
		//HELPER: backwards compatibility v3 for configurationManager.get():
		var config = _req('mmirf/configurationManager');
		if(!config.__get){
			config = extend({}, config);
			replacedMod[getId('mmirf/configurationManager')] = config;
			config.__get = config.get;
			config.get = function(propertyName, useSafeAccess, defaultValue){
				return this.__get(propertyName, defaultValue, useSafeAccess);
			};
		}
	}

	if(_isLegacyMode6) {
		//upgrage mmir < v7: add impl. for mediaManager.loadPlugin()
		var mediaManager = _req('mmirf/mediaManager');
		if(!mediaManager.loadPlugin && mediaManager.loadFile){
			mediaManager.loadPlugin = mediaManager.loadFile;
		}
		//patch changed interpretation of 3rd parameter in mmir.config.get(paht, defaultValue, boolean):
		var config = _req('mmirf/configurationManager');
		if(!config.___get){
			config = extend({}, config);
			replacedMod[getId('mmirf/configurationManager')] = config;
			config.___get = config.get;
			config.get = function(name, defaultValue, setDefaultIfUnset){
				var res = this.___get(isArray(name) ? name.slice() : name);
				if(typeof res === 'undefined' && defaultValue !== 'undefined'){
					res = defaultValue;
					if(setDefaultIfUnset){
						this.set(isArray(name) ? name.slice() : name, defaultValue);
					}
				}
				return res;
			};
		}
	}

	if(_isLegacyMode4){

		//backwards compatibility v3 and v4:
		// plugin instance is "exported" to global var newMediaPlugin
		root['newMediaPlugin'] = factory(_req);

	} else {

		if (typeof define === 'function' && define.amd) {
			// AMD. Register as an anonymous module.
			define(['require'], function (require) {
				//replace with modified require if necessary;
				if(__req) __req = require;
				else if(___req) ___req = require;
				return factory(_req);
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
 * @version 1.2.4
 * @ignore
 */


	
  return {initialize: function (){
    var origArgs = arguments;
    require(['mmirf/mediaManager', 'mmirf/configurationManager', 'mmirf/languageManager', 'mmirf/util/isArray', 'mmirf/logger'], function (_mediaManager, config, lang, isArray, Logger){
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
		 * @type AndroidTTSPlugin
		 * @memberOf AndroidTextToSpeech#
		 */
		var ttsPlugin;
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

		/**
		 * HELPER: finialize the initialization by
		 *  * retrieving the cordova plugin instance
		 *  * startup TTS engine & initialize language
		 *  * invoke the initCallBack() callback for the plugin
		 *
		 * @private
		 * @memberOf AndroidTextToSpeech#
		 *
		 * @param  {Function} initializerCallBack the callBack() of the mediaManager
		 * @param  {AndroidTextToSpeech} initModule the mmir android tts plugin instance
		 */
		function initTtsPlugin(initializerCallBack, initModule){

			if(!window.cordova || !window.cordova.plugins || !window.cordova.plugins.androidTtsPlugin){
				window.document.addEventListener('deviceready', function(){ initTtsPlugin(initializerCallBack, initModule); }, false);
				return;
			}

			ttsPlugin = window.cordova.plugins.androidTtsPlugin;

			//initialize the TTS plugin (with the current language setting)
			ttsPlugin.startup(

					function(data){

						logger.info('AndroidTTS.js.startup: success -> '+JSON.stringify(data));

						language = lang.getLanguageConfig(_pluginName);
						//TODO get & set voice (API in plugin is missing for that ... currently...)
						//var voice = lang.getLanguageConfig(_pluginName, 'voice');

						ttsPlugin.setLanguage(
								language,
								function(data){
									logger.info('AndroidTTS.js.setLanguage('+language+'): success -> '+JSON.stringify(data));
									initializerCallBack(initModule);
								}, function(e){
									logger.warn('AndroidTTS.js.setLanguage('+language+'): error -> '+JSON.stringify(e));
									language = void(0);
									initializerCallBack(initModule);
								}
						);

					}, function(e){
						logger.error('AndroidTTS.js.startup: error -> '+JSON.stringify(e));

						//TODO should this fail instead, e.g. throw error?
						initializerCallBack(initModule);
					}
			);

		}
		//TODO destructor: register onpause/exit handler that shuts down the TTS engine?

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

		/**
		 * the mmir android TTS plugin instance
		 * @private
		 * @type AndroidTextToSpeech
		 */
		var mmirTtsPlugin = {
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
			 * 			, ready: OPTIONAL Function, the audio-ready callback (see arg onReadyCallback),
			 * 			, fileUri: OPTIONAL String, [CUSTOM PARAMETER] file URI for storing the synthesized text instead of playing it
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

				var text = options.text;

				try{
					//only set language in native plugin, if necessary
					var locale = options.language !== language? options.language : void(0);

					//update var language if neccessary:
					if(locale){
						language = locale;
					}

					ttsPlugin.tts(
							text, locale,
							createSuccessWrapper(options.success, options.ready),
							options.error,
							options.pauseDuration,
							options.voice,
							options.fileUri
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

				ttsPlugin.cancel(
						successCallback,
						failureCallback
				);

			},
			/**
			 * @requires Android SDK >= 21
			 *
			 * @public
			 * @memberOf AndroidTextToSpeech.prototype
			 * @see mmir.MediaManager#getSpeechLanguages
			 */
			getSpeechLanguages: function(successCallback,failureCallback){

				ttsPlugin.getLanguages(
						successCallback,
						failureCallback
				);

			},
			/**
			 * @requires Android SDK >= 21
			 *
			 * @public
			 * @memberOf AndroidTextToSpeech.prototype
			 * @see mmir.MediaManager#getVoices
			 */
			getVoices: function(options, successCallback, failureCallback){

				var args = [];
				if(typeof options === 'function'){

					failureCallback = successCallback;
					successCallback = options;

				} else if(options){

					if(typeof options === 'string'){

						args.push(options);

					} else {

						if(typeof options.language !== 'undefined'){
							args.push(options.language);
						}
						if(typeof options.details !== 'undefined'){
							args.push(!!options.details);
						}
					}
				}
				args.push(successCallback, failureCallback);
				ttsPlugin.getVoices.apply(ttsPlugin, args);

			}
		};//END: mmirTtsPlugin = {...

		//invoke the passed-in initializer-callback and export the public functions:
		initTtsPlugin(callBack, mmirTtsPlugin);

	}//END: initialize()

};

}
    })();
    
    //backwards compatibility for media plugins that target mmir-lib < 7.x:
    //remove mediaManager instance from arguments
    var __mmir__ = require('mmirf/core');
    if(!__mmir__.isVersion || __mmir__.isVersion(7, '<')){
      var __mediaManager__ = require('mmirf/mediaManager');
      var modifiedArgs = [];
      for(var i=0, offset=0, size=origArgs.length; i < size; ++i){
        if(origArgs[i] === __mediaManager__){
          ++offset;
        } else {
          modifiedArgs[i - offset] = origArgs[i];
        }
      }
      origArgs = modifiedArgs;
    }

    origInit.initialize.apply(null, origArgs);
});;
  }};


	//END: define()


}));
