
var subFuncs2nd = ['stopRecord', 'cancel', 'getLanguages'];
var subFuncs3rd = ['recognize', 'recognizeNoEOS', 'startRecord', 'stopRecord'];

function createStubs(func, errorCallbackIndex){
	return function(){
		var errCb = arguments[errorCallbackIndex];
		if(errCb){
			errCb('Error for '+func+'(): not implemented');
		}
	};
}

/**
 *
 * @return Instance of AndroidASRPlugin
 */
var AndroidASRPlugin = function() {
	//list of listeners for the "microphone levels changed" event
	this.__micListener = [];
};

subFuncs2nd.map(function(name){
	AndroidASRPlugin.prototype[name] = createStubs(name, 2);
});

subFuncs3rd.map(function(name){
	AndroidASRPlugin.prototype[name] = createStubs(name, 3);
});

/**
 *
 * @return Instance of AndroidASRPlugin
 */
var AndroidASRPlugin = function() {
	//list of listeners for the "microphone levels changed" event
	this.__micListener = [];
};

AndroidASRPlugin.prototype.fireMicLevelChanged = function(value){
	for(var i=0, size = this.__micListener.length; i < size; ++i){
		this.__micListener[i](value);
	}
};

AndroidASRPlugin.prototype.onMicLevelChanged = function(listener){
	var isStart = this.__micListener.length === 0;
	this.__micListener.push(listener);
};

AndroidASRPlugin.prototype.getMicLevelChangedListeners = function(){
	//return copy of listener-list
	return this.__micListener.slice(0,this.__micListener.length);
};

AndroidASRPlugin.prototype.offMicLevelChanged = function(listener){
	var isRemoved = false;
	var size = this.__micListener.length;
	if(size){
		for(var i = size - 1; i >= 0; --i){
			if(this.__micListener[i] ===  listener){
				this.__micListener.splice(i, 1);
				isRemoved = true;
				break;
			}
		}
	}
	return isRemoved;
};

module.exports = new AndroidASRPlugin();
