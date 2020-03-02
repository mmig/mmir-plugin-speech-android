
var subFuncs2nd = ['startup', 'shutdown', 'getLanguage', 'getVoice', 'getLanguages', 'getVoices', 'getDefaultLanguage', 'getDefaultVoice', 'cancel'];
var subFuncs3rd = ['speak', 'silence', 'isLanguageAvailable', 'setLanguage', 'setVoice'];

function createStubs(func, errorCallbackIndex){
	return function(){
		var errCb = arguments[errorCallbackIndex];
		if(errCb){
			errCb('Error for '+func+'(): not implemented');
		}
	};
}

/**
 */
function AndroidTTSPlugin() {
}

AndroidTTSPlugin.STOPPED = 0;
AndroidTTSPlugin.INITIALIZING = 1;
AndroidTTSPlugin.STARTED = 2;

AndroidTTSPlugin.prototype.tts = createStubs('tts', 4);

subFuncs2nd.map(function(name){
	AndroidTTSPlugin.prototype[name] = createStubs(name, 2);
});

subFuncs3rd.map(function(name){
	AndroidTTSPlugin.prototype[name] = createStubs(name, 3);
});

module.exports = new AndroidTTSPlugin();
