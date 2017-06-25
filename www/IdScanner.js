var argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec');

var PLUGIN_NAME = "IdScanner";

var IdScanner = function() {};

IdScanner.startScan = function(options, onSuccess, onError) {
    options = options || {};
    options.instructions = options.instructions || "Aim camera at ID card";
    options.candidateExpression = options.candidateExpression || "";
    options.verifyExpression = options.verifyExpression || "Mod11_2";
    options.verifyChecksum = options.verifyChecksum || "";
    options.cameraDirection = options.cameraDirection || 'back';
    options.cancelText = options.cancelText || 'cancel';
    options.switchText = options.switchText || 'switch cameras';

    exec(onSuccess, onError, PLUGIN_NAME, "startScan", [options.instructions, options.candidateExpression, options.verifyExpression, options.verifyChecksum,
                                                        options.cameraDirection, options.cancelText, options.switchText]);
};

IdScanner.stopScan = function(onSuccess, onError) {
    exec(onSuccess, onError, PLUGIN_NAME, "stopScan", []);
};


module.exports = IdScanner;