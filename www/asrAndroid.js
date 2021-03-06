

/**
 * part of Cordova plugin: mmir-plugin-speech-android
 * @version 1.2.4
 * @ignore
 */

define(
	['mmirf/mediaManager', 'mmirf/configurationManager', 'mmirf/languageManager', 'mmirf/logger'],
	function(
		mediaManager, config, lang, Logger
){

return {

	/**  @memberOf AndroidAudioInput# */
	initialize: function(initCallBack, __mediaManager){

		/**  @memberOf AndroidAudioInput# */
		var _pluginName = 'asrAndroid';

		/**
		 * @type mmir.Logger
		 * @memberOf AndroidAudioInput#
		 */
		var logger = new Logger.create(_pluginName);

		/**
		 * @type AndroidASRPlugin
		 * @memberOf AndroidAudioInput#
		 */
		var asrPlugin;

		/** @memberOf AndroidAudioInput# */
		var DEFAULT_LANGUAGE = 'en-US';

		/** @memberOf AndroidAudioInput# */
		var DEFAULT_ALTERNATIVE_RESULTS = 1;

		/** @memberOf AndroidAudioInput# */
		var DEFAULT_LANGUAGE_MODEL = 'dictation';// 'dictation' | 'search'

		// /**  @memberOf AndroidAudioInput# */
		// var id = 0;

		/**
		 * @type Function
		 * @memberOf AndroidAudioInput#
		 */
		var currentSuccessCallback;
		/**
		 * @type Function
		 * @memberOf AndroidAudioInput#
		 */
		var currentFailureCallback;
		/**  @memberOf AndroidAudioInput# */
		var intermediate_results = true;
		/**  @memberOf AndroidAudioInput# */
		var repeat = false;
		/**
		 * The last received result (or undefined, if there is none).
		 *
		 * [ text : String, score : Number, type : result_types, alternatives : Array, unstable : String ]
		 *
		 * @type Array
		 * @memberOf AndroidAudioInput#
		 */
		var last_result = void(0);

		/**
		 * activate / deactivate improved feedback mode:
		 * <br>
		 * If activated, this will take effect during start-/stop-Record mode <em>with</em> intermediate results.
		 * <p>
		 *
		 * This deals with the fact, that the Nuance recognizer has a very noticeable pause between stopping the recording
		 * and re-starting the recording for the next voice input.
		 *
		 * The improved feedback mode will return recognition results NOT immediately when they are received, but
		 * when the recognition has restarted (i.e. listens again) - or when it stops
		 * (i.e. stopRecognition is called or error occurred).
		 *
		 *
		 * This can improve user interactions, since the results will only be shown, when the recognizer is active again,
		 * i.e. users do not have to actively interpret the START prompt (if it is active!) or other WAIT indicators
		 * during the time when recording stops and restarts again (i.e. when input-recording is inactive).
		 *
		 * Instead they are "prompted" by the appearing text of the last recognition result.
		 *
		 * @memberOf AndroidAudioInput#
		 * @default false: improved feedback mode is enabled by default
		 */
		var disable_improved_feedback_mode = false;

		/**
		 * Counter for error-in-a-row:
		 * each time an error is encountered, this counter is increased.
		 * On starting/canceling, or on an internal success/result callback,
		 * the counter is reset.
		 *
		 * Thus, this counter keeps track how many times in a row
		 * the (internal) error-callback was triggered.
		 *
		 * NOTE: this is currently used, to try restarting <code>max_error_retry</code>
		 * 		 times the ASR, even on "critical" errors (during repeat-mode).
		 *
		 * @see #max_error_retry
		 *
		 * @memberOf AndroidAudioInput#
		 */
		var error_counter = 0;

		/**
		 * Maximal number of errors-in-a-row for trying to restart
		 * recognition in repeat-mode.
		 *
		 * @see #error_counter
		 *
		 * @memberOf AndroidAudioInput#
		 * @default 5
		 */
		var max_error_retry = 5;

//					/**
//					* Time to wait, before restarting recognition in repeat-mode.
//					*
//					* The actual duration is calculated by multiplying this value with the #error_counter.
//					*
//					* @see #error_counter
//					*
//					* @memberOf AndroidAudioInput#
//					* @default 10
//					*/
//					var retry_delay = 50;


		//Nuance Error Codes:
//					var error_codes_enum = {
//					"UNKNOWN": 				0,
//					"SERVER_CONNECTION": 	1,
//					"SERVER_RETRY": 		2,
//					"RECOGNIZER": 			3,
//					"VOCALIZER": 			4,
//					"CANCEL": 				5
//					};

		//Android Error Codes:
//					/** Network operation timed out. */
//					public static final int ERROR_NETWORK_TIMEOUT = 1;
//					/** Other network related errors. */
//					public static final int ERROR_NETWORK = 2;
//					/** Audio recording error. */
//					public static final int ERROR_AUDIO = 3;
//					/** Server sends error status. */
//					public static final int ERROR_SERVER = 4;
//					/** Other client side errors. */
//					public static final int ERROR_CLIENT = 5;
//					/** No speech input */
//					public static final int ERROR_SPEECH_TIMEOUT = 6;
//					/** No recognition result matched. */
//					public static final int ERROR_NO_MATCH = 7;
//					/** RecognitionService busy. */
//					public static final int ERROR_RECOGNIZER_BUSY = 8;
//					/** Insufficient permissions */
//					public static final int ERROR_INSUFFICIENT_PERMISSIONS = 9;

		/**
		 * Error codes (returned by the native/Cordova plugin)
		 * @type Enum
		 * @constant
		 * @memberOf AndroidAudioInput#
		 */
		var error_codes_enum = {
				"NETWORK_TIMEOUT":			1, //Nuance: SERVER_CONNECTION
				"NETWORK":					2, //Nuance: SERVER_RETRY
				"AUDIO": 					3, //Nuance: RECOGNIZER
				"SERVER": 					4, //Nuance: VOCALIZER
				"CLIENT": 					5, //Nuance: CANCEL
				"SPEECH_TIMEOUT":			6, //Nuance -> na
				"NO_MATCH":					7, //Nuance -> na
				"RECOGNIZER_BUSY":			8, //Nuance -> na
				"INSUFFICIENT_PERMISSIONS":	9, //Nuance -> na
				// >= 10  --> UNKNOWN:
				"UNKNOWN":					10 //Nuance -> 0
		};

		/**
		 * Result types (returned by the native/Cordova plugin)
		 *
		 * @type Enum
		 * @constant
		 * @memberOf AndroidAudioInput#
		 */
		var result_types = {
				"FINAL": 				"FINAL",
				"INTERIM": 				"INTERIM",
				"INTERMEDIATE":			"INTERMEDIATE",
				"RECOGNITION_ERROR": 	"RECOGNITION_ERROR",
				"RECORDING_BEGIN": 		"RECORDING_BEGIN",
				"RECORDING_DONE": 		"RECORDING_DONE"
		};

		//set log-level from configuration (if there is setting)
		var loglevel = config.get([_pluginName, 'logLevel']);
		if(typeof loglevel !== 'undefined'){
			logger.setLevel(loglevel);
		}

		//backwards compatibility (pre v0.5.0)
		if(!mediaManager._preparing){
			mediaManager._preparing = function(name){logger.info(name + ' is preparing - NOTE: this is a stub-function. Overwrite MediaManager._preparing for setting custom implementation.');};
		}
		if(!mediaManager._ready){
			mediaManager._ready     = function(name){logger.info(name + ' is ready - NOTE: this is a stub-function. Overwrite MediaManager._ready for setting custom implementation.');};
		}

		/**
		 * MIC-LEVELS: Name for the event that is emitted, when the input-mircophone's level change.
		 *
		 * @private
		 * @constant
		 * @memberOf AndroidAudioInput#
		 */
		var MIC_CHANGED_EVT_NAME = 'miclevelchanged';

		/**
		 * MIC-LEVELS: start/stop audio-analysis if listeners get added/removed
		 *             in mediaManager, i.e. register via mediaManager._addListenerObserver()
		 *
		 * @private
		 * @memberOf AndroidAudioInput#
		 */
		function _updateMicLevelListeners(actionType, handler){
			//add to plugin-listener-list
			if(actionType=== 'added'){
				asrPlugin.onMicLevelChanged(handler);
			}
			//remove from plugin-listener-list
			else if(actionType === 'removed'){
				asrPlugin.offMicLevelChanged(handler);
			}
		}

		/**
		 * HELPER: finialize the initialization by
		 *  * retrieving the cordova plugin instance
		 *  * initialize listeners for mic-level-changes
		 *  * invoke the initCallBack() callback for the plugin
		 *
		 * @private
		 * @memberOf AndroidAudioInput#
		 *
		 * @param  {Function} initializerCallBack the initCallBack() of the mediaManager
		 * @param  {AndroidAudioInput} initModule the mmir android asr plugin instance
		 */
		function initAsrPlugin(initializerCallBack, initModule){

			if(!window.cordova || !window.cordova.plugins || !window.cordova.plugins.androidAsrPlugin){
				window.document.addEventListener('deviceready', function(){ initAsrPlugin(initializerCallBack, initModule); }, false);
				return;
			}

			asrPlugin = window.cordova.plugins.androidAsrPlugin;

			//add current listeners for mic-levels-changes:
			var list = mediaManager.getListeners(MIC_CHANGED_EVT_NAME);
			for(var i=0, size= list.length; i < size; ++i){
				asrPlugin.onMicLevelChanged(list[i]);
			}

			//observe changes on listener-list for mic-levels-changed-event
			mediaManager._addListenerObserver(MIC_CHANGED_EVT_NAME, _updateMicLevelListeners);

			initializerCallBack(initModule);
		}

		/**
		 * HELPER invoke current callback function with last recognition results.
		 * @private
		 * @memberOf AndroidAudioInput#
		 */
		var call_callback_with_last_result = function(){
			if(typeof last_result !== "undefined") {
				if (currentSuccessCallback){
					if(logger.isDebug()) logger.debug("last_result is " + JSON.stringify(last_result));
					currentSuccessCallback.apply(mediaManager, last_result);
					last_result = void(0);
				} else {
					logger.error("No callback function defined for success.");
				}
			} else {
				logger.info("last_result is undefined.");
			}
		};

		/**
		 * Creates the wrapper for the success-back:
		 *
		 * successcallback(asr_result, asr_score, asr_type, asr_alternatives, asr_unstable) OR in case of error:
		 * successcallback(asr_result, asr_score, asr_type, asr_error_code, asr_error_suggestion)
		 *
		 * @private
		 * @memberOf AndroidAudioInput#
		 */
		var successCallbackWrapper = function successCallbackWrapper (cb, options){

			return (function (res){

//							logger.log("asrAndroid "+(repeat? "(REPEAT_MODE)" : "")+": " + JSON.stringify(res));//FIXM DEBUG

				var asr_result = null;
				var asr_score = -1;
				var asr_type = -1;
				var asr_alternatives = [];
				var asr_unstable = null;

				error_counter = 0;

				if(res) {

					if(typeof res['result'] !== "undefined"){
						asr_result = res['result'];
					}
					if  (typeof res["score"] !== "undefined"){
						asr_score = res["score"];
					}
					if  (typeof res["type"] !== "undefined"){
						asr_type = res["type"];
					}
					if  (typeof res["alternatives"] !== "undefined"){
						asr_alternatives = res["alternatives"];
					}
					if  (typeof res["unstable"] !== "undefined"){
						asr_unstable = res["unstable"];
					}
				}

				//call voice recognition again, if repeat is set to true
				if (repeat === true) {
					if (asr_type === result_types.RECORDING_BEGIN){
						// only call success-callback, if we started recording again.
						mediaManager._ready(_pluginName);
						call_callback_with_last_result();
					} else if (asr_type === result_types.RECORDING_DONE){
						// Do nothing right now at the recording done event
//									logger.log("asrAndroid "+(repeat? "(REPEAT_MODE)" : "")+": RECORDING_DONE, last_result: " + JSON.stringify(last_result));//FIXM DEBUG

					} else if (asr_type === result_types.FINAL){
						// its the final result
						// post last result
						call_callback_with_last_result();
						last_result = [asr_result, asr_score, asr_type, asr_alternatives, asr_unstable];
						// post current result
						call_callback_with_last_result();
					} else if (asr_type === result_types.INTERIM){
						// its an interim result
						// -> save the last result
						call_callback_with_last_result();
						last_result = [asr_result, asr_score, asr_type, asr_alternatives, asr_unstable];
						// post current result
						call_callback_with_last_result();
					} else if (asr_type === result_types.INTERMEDIATE){
						mediaManager._preparing(_pluginName);
						// save the last result and start recognition again
						// (callback for intermediate results will be triggered on next RECORDING_BEGIN)
						last_result = [asr_result, asr_score, asr_type, asr_alternatives, asr_unstable];

						//if improved-feedback-mode is disabled: immediately call success-callback with results
						if(disable_improved_feedback_mode === true){
							call_callback_with_last_result();
						}

						asrPlugin.startRecord(
								options.language,
								successCallbackWrapper(currentSuccessCallback, options),
								failureCallbackWrapper(currentFailureCallback, options),
								intermediate_results,
								options.results,
								options.mode
						);

					} else {
						// save the last result and start recognition again
//									last_result = [asr_result, asr_score, asr_type, asr_alternatives];

//									asrPlugin.startRecord(
//									options.language,
//									successCallbackWrapper(currentSuccessCallback, options),
//									failureCallbackWrapper(currentFailureCallback, options),
//									intermediate_results,
//									options.results,
//									options.mode
//									);
//									logger.warn("Success - Repeat - Else\nType: " + asr_type+"\n"+JSON.stringify(res));
					}

				} else {
					// no repeat, there won't be another recording, so call callback right now with previous and current result

					mediaManager._ready(_pluginName);

					if (asr_type === result_types.INTERMEDIATE){

						//if we are in non-repeat mode, then INTERMEDIATE
						//results are actually FINAL ones
						// (since we normally have no stopRecording-callback)
						asr_type = result_types.FINAL;

					}
//								else if (asr_type === result_types.RECORDING_DONE){
//								//nothing to do (yet)
//								}
//								else if (asr_type === result_types.FINAL){
//								//nothing to do (yet)
//								}

					//send any previous results, if there are any (NOTE: there actually should be none!)
					call_callback_with_last_result();

					//invoke callback
					if (cb){
						cb(asr_result, asr_score, asr_type, asr_alternatives, asr_unstable);
					} else {
						logger.error("No callback function defined for success.");
					}

				}
			});
		};

		/**
		 * creates the wrapper for the failure callback
		 *
		 * @private
		 * @memberOf AndroidAudioInput#
		 */
		var failureCallbackWrapper = function failureCallbackWrapper (cb, options){

			return (function (res){
				var error_code = -1;
				var error_msg = "";
				var error_suggestion = "";
				var error_type = -1;

				if (typeof res !== "undefined"){
					if(typeof res['error_code'] !== "undefined") {

						error_code = res['error_code'];
					}
					if  (typeof res["msg"] !== "undefined"){
						error_msg = res["msg"];
					}

					if (typeof res["suggestion"] !== "undefined"){
						error_suggestion = res["suggestion"];
					}

					if  (typeof res["type"] !== "undefined"){
						error_type = res["type"];
					}
				}

				++error_counter;

				if(logger.isInfo()) logger.info("\""+error_msg+"\" (code "+error_code+", type "+error_type+"), repeat="+repeat+", error-counter: "+error_counter+"...");

				mediaManager._ready(_pluginName);

				// TEST: if there is still a pending last result, call successcallback first.
				call_callback_with_last_result();
				if (repeat === true){

					var restartFunc = function(){

						if (error_type !== result_types.FINAL){
							//show loader/signal "busy", so that the user knows it may take a while before (s)he can start talking again
							mediaManager._preparing(_pluginName);
						}

						//restart:
						asrPlugin.startRecord(
								options.language,
								successCallbackWrapper(currentSuccessCallback, options),
								failureCallbackWrapper(currentFailureCallback, options),
								intermediate_results,
								options.results,
								options.mode
						);
					};


//								"NETWORK_TIMEOUT":			1, //Nuance: SERVER_CONNECTION
//								"NETWORK":					2, //Nuance: SERVER_RETRY
//								"AUDIO": 					3, //Nuance: RECOGNIZER
//								"SERVER": 					4, //Nuance: VOCALIZER
//								"CLIENT": 					5, //Nuance: CANCEL
//								"SPEECH_TIMEOUT":			6  //Nuance -> na
//								"NO_MATCH":					7  //Nuance -> na
//								"RECOGNIZER_BUSY":			8  //Nuance -> na
//								"INSUFFICIENT_PERMISSIONS":	9  //Nuance -> na
//								// >= 10  --> UNKNOWN:
//								"UNKNOWN":					10 //Nuance -> 0


					//minor errors -> restart the recognition
					if (
							error_code === error_codes_enum.ERROR_RECOGNIZER_BUSY
							||	error_code === error_codes_enum.NETWORK_TIMEOUT
							||	error_code === error_codes_enum.SPEECH_TIMEOUT
							||	error_code === error_codes_enum.NO_MATCH

//										||	error_code === error_codes_enum.CLIENT		//TEST treat CLIENT error analogous to BUSY error: sometimes CLIENT will stop the recognizer, sometimes not ... so just cancel the recognizer in ANY case and restart
					){

						//SPECIAL CASE: recognizer already busy -> need to cancel before restarting
						if(
								error_code === error_codes_enum.ERROR_RECOGNIZER_BUSY
//											||	error_code === error_codes_enum.CLIENT
						){

							//-> first need to cancel current recognizer, then restart again:

//										var retryDelay = error_counter * retry_delay;
//										logger.info(" "+error_msg+" (code "+error_code+") - canceling and restarting ASR process in "+(2*retryDelay)+"ms...");

//										setTimeout( function(){
							asrPlugin.cancel(function(){

								//now we can restart the recognizer:
//											setTimeout( function(){
								restartFunc();
//											}, retryDelay);

							}, function(error){
								if (cb){
									logger.warn("while CANCELING due to ERROR_RECOGNIZER_BUSY: Calling error callback (" + error_code + ": " + error_msg + ").");
									cb(error_msg, error_code, error_suggestion);
								}
								else {
									logger.error("while CANCELING due to ERROR_RECOGNIZER_BUSY: No callback function defined for failure "+error);
								}

							});
//										}, retryDelay);

						}
						else {

							//just restart recognition:
							restartFunc();
						}

					}
					//on minor errors that do not stop the recognizer -> do nothing
					else if(
							error_code === error_codes_enum.CLIENT
					){
						if(logger.isInfo()) logger.info(error_msg+" (code "+error_code+") - continuing ASR process...");
					}
					// call error callback on "severe" errors
					else
//									if (		error_code < 1 //undefined error!!!
//									|| 	error_code === error_codes_enum.NETWORK
//									|| 	error_code === error_codes_enum.SERVER
//									|| 	error_code === error_codes_enum.AUDIO
//									|| 	error_code === error_codes_enum.INSUFFICIENT_PERMISSIONS
//									|| 	error_code >= error_codes_enum.UNKNOWN // >= : "catch all" for unknown/undefined errors
//									)
					{
						if(error_code !== error_codes_enum.INSUFFICIENT_PERMISSIONS && error_counter < max_error_retry){

							restartFunc();

						}
						else if (cb){
							logger.warn("Calling error callback (" + error_code + ": " + error_msg + ").");
							cb(error_msg, error_code, error_suggestion);
						}
						else {
							logger.error("No callback function defined for failure.");
						}
					}

				}
				// "one-time recogintion call", i.e. without intermediate results, that is no repeat-mode requested
				//  --> just call error callback:
				else {

					// do no repeat, just call errorCallback
					if (cb){
						if(logger.isDebug()) logger.debug("Calling error callback (" + error_code + ").");
						cb(error_msg, error_code, error_suggestion);
					} else {
						logger.error("No callback function defined for failure.");
					}
				}

			});
		};

		/**
		 * the mmir android ASR plugin instance
		 * @private
		 * @type AndroidAudioInput
		 */
		var mmirAsrPlugin = {
			/**
			 * Start speech recognition (without <em>end-of-speech</em> detection):
			 * after starting, the recognition continues until {@link #stopRecord} is called.
			 *
			 * @async
			 *
			 * @param {PlainObject} [options] OPTIONAL
			 * 		options for Automatic Speech Recognition:
			 * 		<pre>{
			 * 			  success: OPTIONAL Function, the status-callback (see arg statusCallback)
			 * 			, error: OPTIONAL Function, the error callback (see arg failureCallback)
			 * 			, language: OPTIONAL String, the language for recognition (if omitted, the current language setting is used)
			 * 			, intermediate: OTPIONAL Boolean, set true for receiving intermediate results (NOTE not all ASR engines may support intermediate results)
			 * 			, results: OTPIONAL Number, set how many recognition alternatives should be returned at most (NOTE not all ASR engines may support this option)
			 * 			, mode: OTPIONAL "search" | "dictation", set how many recognition alternatives should be returned at most (NOTE not all ASR engines may support this option)
			 * 			, eosPause: OTPIONAL "short" | "long", length of pause after speech for end-of-speech detection (NOTE not all ASR engines may support this option)
			 * 			, disableImprovedFeedback: OTPIONAL Boolean, disable improved feedback when using intermediate results (NOTE not all ASR engines may support this option)
			 * 		}</pre>
			 *
			 * @param {Function} [statusCallback] OPTIONAL
			 * 			callback function that is triggered when, recognition starts, text results become available, and recognition ends.
			 * 			The callback signature is:
			 * 				<pre>
			 * 				callback(
			 * 					text: String | "",
			 * 					confidence: Number | Void,
			 * 					status: "FINAL"|"INTERIM"|"INTERMEDIATE"|"RECORDING_BEGIN"|"RECORDING_DONE",
			 * 					alternatives: Array<{result: String, score: Number}> | Void,
			 * 					unstable: String | Void
			 * 				)
			 * 				</pre>
			 *
			 * 			Usually, for status <code>"FINAL" | "INTERIM" | "INTERMEDIATE"</code> text results are returned, where
			 * 			<pre>
			 * 			  "INTERIM": an interim result, that might still change
			 * 			  "INTERMEDIATE": a stable, intermediate result
			 * 			  "FINAL": a (stable) final result, before the recognition stops
			 * 			</pre>
			 * 			If present, the <code>unstable</code> argument provides a preview for the currently processed / recognized text.
			 *
			 * 			<br>NOTE that when using <code>intermediate</code> mode, status-calls with <code>"INTERMEDIATE"</code> may
			 * 			     contain "final intermediate" results, too.
			 *
			 * 			<br>NOTE: if used in combination with <code>options.success</code>, this argument will supersede the options
			 *
			 * @param {Function} [failureCallback] OPTIONAL
			 * 			callback function that is triggered when an error occurred.
			 * 			The callback signature is:
			 * 				<code>callback(error)</code>
			 *
			 * 			<br>NOTE: if used in combination with <code>options.error</code>, this argument will supersede the options
			 *
			 * @public
			 * @memberOf AndroidAudioInput.prototype
			 * @see mmir.MediaManager#startRecord
			 */
			startRecord: function(options, statusCallback, failureCallback, intermediateResults, isDisableImprovedFeedback){
				//argument intermediateResults is deprecated (use options.intermediate instead)
				//argument isDisableImprovedFeedback is deprecated (use options.disableImprovedFeedback instead)

				if(typeof options === 'function'){
					isDisableImprovedFeedback = intermediateResults;
					intermediateResults = failureCallback;
					failureCallback = statusCallback;
					statusCallback = options;
					options = void(0);
				}

				if(!options){
					options = {};
				}
				options.success = statusCallback? statusCallback : options.success;
				options.error = failureCallback? failureCallback : options.error;
				options.intermediate = typeof intermediateResults === 'boolean'? intermediateResults : !!options.intermediate;
				options.language = options.language? options.language : lang.getLanguageConfig(_pluginName) || DEFAULT_LANGUAGE;
				options.disableImprovedFeedback = typeof isDisableImprovedFeedback === 'boolean'? isDisableImprovedFeedback : !!options.disableImprovedFeedback;
				options.results = options.results? options.results : DEFAULT_ALTERNATIVE_RESULTS;
				options.mode = options.mode? options.mode : DEFAULT_LANGUAGE_MODEL;
				//TODO
//							options.eosPause =


				currentFailureCallback = options.error;
				currentSuccessCallback = options.success;
				repeat = true;

				error_counter = 0;

				intermediate_results = options.intermediate;

				//EXPERIMENTAL: allow disabling the improved feedback mode
				disable_improved_feedback_mode = options.disableImprovedFeedback;

				mediaManager._preparing(_pluginName);

				asrPlugin.startRecord(
						options.language,
						successCallbackWrapper(options.success, options),
						failureCallbackWrapper(options.error, options),
						intermediate_results,
						options.results,
						options.mode
				);
			},
			/**
			 * @public
			 * @memberOf AndroidAudioInput.prototype
			 * @see mmir.MediaManager#stopRecord
			 */
			stopRecord: function(options, statusCallback, failureCallback){

				repeat = false;

				if(typeof options === 'function'){
					failureCallback = statusCallback;
					statusCallback = options;
					options = void(0);
				}

				if(!options){
					options = {};
				}

				options.success = statusCallback? statusCallback : options.success;
				options.error = failureCallback? failureCallback : options.error;

				asrPlugin.stopRecord(
						successCallbackWrapper(options.success, options),
						failureCallbackWrapper(options.error, options)
				);
			},
			/**
			 * @public
			 * @memberOf AndroidAudioInput.prototype
			 * @see mmir.MediaManager#recognize
			 * @see #startRecord
			 */
			recognize: function(options, statusCallback,failureCallback){


				if(typeof options === 'function'){
					failureCallback = statusCallback;
					statusCallback = options;
					options = void(0);
				}

				if(!options){
					options = {};
				}
				options.success = statusCallback? statusCallback : options.success;
				options.error = failureCallback? failureCallback : options.error;
				options.intermediate = typeof intermediateResults === 'boolean'? intermediateResults : !!options.intermediate;
				options.language = options.language? options.language : lang.getLanguageConfig(_pluginName) || DEFAULT_LANGUAGE;
				options.disableImprovedFeedback = typeof isDisableImprovedFeedback === 'boolean'? isDisableImprovedFeedback : !!options.disableImprovedFeedback;
				options.results = options.results? options.results : DEFAULT_ALTERNATIVE_RESULTS;
				options.mode = options.mode? options.mode : DEFAULT_LANGUAGE_MODEL;
				//TODO
//							options.eosPause =


				repeat = false;
				error_counter = 0;
				mediaManager._preparing(_pluginName);

				asrPlugin.recognize(
						options.language,
						successCallbackWrapper(options.success, options),
						failureCallbackWrapper(options.error, options),
						options.results,
						options.mode
				);
			},
			/**
			 * @public
			 * @memberOf AndroidAudioInput.prototype
			 * @see mmir.MediaManager#cancelRecognition
			 */
			cancelRecognition: function(successCallBack,failureCallBack){
				last_result = void(0);
				repeat = false;
				error_counter = 0;

				mediaManager._ready(_pluginName);

				asrPlugin.cancel(successCallBack, failureCallBack);
			},
			/**
			 * @public
			 * @memberOf AndroidAudioInput.prototype
			 * @see mmir.MediaManager#getRecognitionLanguages
			 */
			getRecognitionLanguages: function(successCallBack,failureCallBack){
				asrPlugin.getLanguages(successCallBack, failureCallBack);
			}
		};//END mmirAsrPlugin = {...

		//invoke the passed-in initializer-callback and export the public functions:
		initAsrPlugin(initCallBack, mmirAsrPlugin);

	}//END: initialize()

};

});//END: define()
