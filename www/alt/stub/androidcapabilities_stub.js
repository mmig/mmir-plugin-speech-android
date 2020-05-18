
function AndroidSpeechCapabilityPlugin() {
}

AndroidSpeechCapabilityPlugin.prototype.check = function(functionality, successCallback, errorCallback) {
	if(errorCallback){
		errorCallback('Error for check(): not implemented');
	}
};

AndroidSpeechCapabilityPlugin.prototype.request = function(functionality, successCallback, errorCallback) {
	if(errorCallback){
		errorCallback('Error for check(): not implemented');
	}
};

module.exports = new AndroidSpeechCapabilityPlugin();
