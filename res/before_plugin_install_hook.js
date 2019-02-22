#!/usr/bin/env node

var path = require('path');
var fs = require('fs');

var configUtil = require('./configUtil.js');
var modeUtil = require('./modeUtil.js');


module.exports = function(ctx){
//	console.log('plugin-hook -> ', ctx, ctx.opts, ctx.opts.plugin.pluginInfo);

//	var platforms = ctx.opts.platforms? ctx.opts.platforms : ctx.opts.cordova.platforms;
//	if (platforms.indexOf('android') < 0) {
//		return;
//	}

	var plugin = ctx.opts.plugin;
	var fs = ctx.requireCordovaModule('fs'),
		path = ctx.requireCordovaModule('path')//,
//		deferral = ctx.requireCordovaModule('q').defer();


	var pluginDir = plugin.dir;

	var pluginInfo = plugin.pluginInfo;
//	console.log('prefs -> ', pluginInfo.getPreferences());
//	console.log('assests -> ', pluginInfo.getAssets());
//	console.log('files & frameworks -> ', pluginInfo.getFilesAndFrameworks());
//	console.log('source files -> ', pluginInfo.getSourceFiles());
//	console.log('js modules -> ', pluginInfo.getJsModules());
//	console.log('lib files -> ', pluginInfo.getLibFiles());

//	var variables = configUtil.getVariables(ctx, pluginInfo, true);
//	var compatMode = /^compat$/.test(variables.COMPAT_MODE);

	var mode = configUtil.getVariable(ctx, 'MMIR_PLUGIN_MODE', plugin.platform, pluginInfo, true);
	
	modeUtil.applyMode(mode, pluginDir, {targetFile: 'www/asrAndroid.js', sourcePath: 'www/alt'}, ctx);
	modeUtil.applyMode(mode, pluginDir, {targetFile: 'www/ttsAndroid.js', sourcePath: 'www/alt'}, ctx);
	
//
////	pluginInfo.getAssets().forEach(function(asset){
////		if(/asrAndroid\.js$/i.test(asset.target)){
////			var source = compatMode? compatFile : implFile;
////			console.log('  setting asset source for compatMode '+compatMode+' -> ', source, '...');
////			asset.src = source;
////			console.log('  did set asset source for compatMode '+compatMode+' -> ', asset);
////		} 
////	});

}
