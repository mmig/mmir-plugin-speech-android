
/*********************************************************************
 * This file is automatically generated by mmir-plugins-export tools *
 *        Do not modify: ANY CHANGES WILL GET DISCARDED              *
 *********************************************************************/

var _id = "mmir-plugin-speech-android";
var _paths = {
  "mmir-plugin-speech-android/androidasr": "www/androidasr.js",
  "mmir-plugin-speech-android/androidcapabilities": "www/androidcapabilities.js",
  "mmir-plugin-speech-android/androidtts": "www/androidtts.js",
  "mmir-plugin-speech-android/asrAndroid": "www/asrAndroid.js",
  "mmir-plugin-speech-android/ttsAndroid": "www/ttsAndroid.js",
  "mmir-plugin-speech-android/asrAndroidCompat": "www/alt/asrAndroidCompat.js",
  "mmir-plugin-speech-android/asrAndroidWebpack": "www/alt/asrAndroidWebpack.js",
  "mmir-plugin-speech-android/ttsAndroidCompat": "www/alt/ttsAndroidCompat.js",
  "mmir-plugin-speech-android/ttsAndroidWebpack": "www/alt/ttsAndroidWebpack.js",
  "mmir-plugin-speech-android/androidasr_stub": "www/alt/stub/androidasr_stub.js",
  "mmir-plugin-speech-android/androidcapabilities_stub": "www/alt/stub/androidcapabilities_stub.js",
  "mmir-plugin-speech-android/androidtts_stub": "www/alt/stub/androidtts_stub.js",
  "mmir-plugin-speech-android": "www/asrAndroid.js"
};
var _workers = [];
var _exportedModules = [
  "mmir-plugin-speech-android",
  "mmir-plugin-speech-android/ttsAndroid"
];
var _dependencies = [];
var _exportedFiles = [];
var _modes = {};
var _buildConfig = "module-config.gen.js";
function _join(target, source, dupl){
  source.forEach(function(item){
    if(!dupl || !dupl.has(item)){
      dupl && dupl.add(item);
      target.push(item);
    }
  });
};
function _toDict(list){
  if(typeof list.has === 'function' && typeof list.add === 'function'){
    return list;
  }
  if(typeof list[Symbol.iterator] !== 'function'){
    list = Object.keys(list);
  }
  return new Set(list);
};
function _getAll(type, mode, isResolve){

  if(typeof mode === 'boolean'){
    isResolve = mode;
    mode = void(0);
  }

  var data = this[type];
  var isArray = Array.isArray(data);
  var result = isArray? [] : Object.assign({}, data);
  var dupl;
  var mod = mode && this.modes[mode];
  if(isArray){
    dupl = new Set();
    if(mod && mod[type]){
      _join(result, this.modes[mode][type], dupl);
    }
    _join(result, data, dupl);
  } else if(isResolve){
    var root = __dirname;
    Object.keys(result).forEach(function(field){
      var val = result[field];
      if(mod && mod[field]){
        val = _paths[mod[field]];
      }
      result[field] = root + '/' + val;
    });
  }
  this.dependencies.forEach(function(dep){
    var depExports = require(dep + '/module-ids.gen.js');
    var depData = depExports.getAll(type, mode, isResolve);
    if(isArray){
      _join(result, depData, dupl);
    } else {
      Object.assign(result, depData)
    }
  });

  return result;
};
function _getBuildConfig(pluginName, buildConfigsMap){
  if(pluginName && typeof pluginName !== 'string'){
    buildConfigsMap = pluginName;
    pluginName = void(0);
  }
  var buildConfigs = [];
  var dupl = buildConfigsMap? _toDict(buildConfigsMap) : new Set();
  if(_buildConfig){
    var buildConfigMod = require(__dirname+'/'+_buildConfig);
    var buildConfig = buildConfigMod.buildConfigs;
    if(Array.isArray(buildConfig)){
      _join(buildConfigs, buildConfig, dupl);
    } else if(buildConfig && !dupl.has(buildConfig)){
      dupl.add(buildConfig);
      buildConfigs.push(buildConfig);
    }
    if(Array.isArray(buildConfigMod.pluginName) && buildConfigMod.plugins){
      buildConfigMod.pluginName.forEach(function(name){
        if(!pluginName || pluginName === name){
          var pluginBuildConfig = buildConfigMod.plugins[name].buildConfigs;
          if(Array.isArray(pluginBuildConfig)){
            _join(buildConfigs, pluginBuildConfig, dupl);
          } else if(pluginBuildConfig && !dupl.has(pluginBuildConfig)){
            dupl.add(pluginBuildConfig);
            buildConfigs.push(pluginBuildConfig);
          }
        }
      });
    }
  }

  this.dependencies.forEach(function(dep){
    var depExports = require(dep + '/module-ids.gen.js');
    if(depExports.buildConfig){
      var depBuildConfigs = depExports.getBuildConfig(null, dupl);
      _join(buildConfigs, depBuildConfigs);
    }
  });

  return buildConfigs;
};
module.exports = {id: _id, paths: _paths, workers: _workers, modules: _exportedModules, files: _exportedFiles, dependencies: _dependencies, modes: _modes, buildConfig: _buildConfig, getAll: _getAll, getBuildConfig: _getBuildConfig};
