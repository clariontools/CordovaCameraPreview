var argscheck = require('cordova/argscheck'),
utils = require('cordova/utils'),
exec = require('cordova/exec');

var PLUGIN_NAME = "CameraPreview";

var CameraPreview = function() {};

CameraPreview.ROTATION_FREE = -1; // Do not lock rotation
CameraPreview.ROTATION_PORTRAIT = 0; // 0°
CameraPreview.ROTATION_LANDSCAPE_RIGHT = 1; // 90°
CameraPreview.ROTATION_PORTRAIT_UPSIDE_DOWN = 2; // 180°
CameraPreview.ROTATION_LANDSCAPE_LEFT = 3; // 270°

CameraPreview.FLASH_AUTO = 0;
CameraPreview.FLASH_ON = 1;
CameraPreview.FLASH_OFF = 2;
CameraPreview.FLASH_TORCH = 3;

CameraPreview.setOnPictureTakenHandler = function(onPictureTaken) {
    exec(onPictureTaken, onPictureTaken, PLUGIN_NAME, "setOnPictureTakenHandler", []);
};

CameraPreview.setOnCameraPreviewReadyHandler = function(onCameraPreviewReady) {
    exec(onCameraPreviewReady, onCameraPreviewReady, PLUGIN_NAME, "setOnCameraPreviewReadyHandler", []);
};

CameraPreview.setOnOrientationChangeHandler = function(onOrientationChange) {
    exec(onOrientationChange, onOrientationChange, PLUGIN_NAME, "setOnOrientationChangeHandler", []);
};

CameraPreview.setOnCameraDebugMessageHandler = function(onCameraDebugMessage) {
    exec(onCameraDebugMessage, onCameraDebugMessage, PLUGIN_NAME, "setOnCameraDebugMessageHandler", []);
};

//@param rect {x: 0, y: 0, width: 100, height:100}
//@param defaultCamera "front" | "back"
CameraPreview.startCamera = function(rect, defaultCamera, toBack, maxCaptureLength, rotation, alpha, prefix) {
    if (typeof(alpha) === 'undefined') {
        alpha = 1;
    }
    return new Promise( function(resolve, reject) {
                       exec(resolve, reject, PLUGIN_NAME, "startCamera", [rect.x, rect.y, rect.width, rect.height, defaultCamera, !!toBack, maxCaptureLength, rotation, alpha, prefix]);
                       });
};

CameraPreview.stopCamera = function() {
    exec(null, null, PLUGIN_NAME, "stopCamera", []);
};

//@param size {maxWidth: 100, maxHeight:100}
//@param quality 80
CameraPreview.takePicture = function(size, quality) {
    if (typeof size !== 'object' || typeof size === 'object' && (size.maxWidth !== undefined || size.maxHeight !== undefined)) {
        size = {maxWidth: 0, maxHeight: 0};
    }
    if (typeof quality === 'undefined') quality = 80;
    
    return new Promise( function(resolve, reject) {
                       exec(resolve, reject, PLUGIN_NAME, "takePicture", [size.maxWidth, size.maxHeight, quality]);
                       });
};

CameraPreview.switchCamera = function() {
    exec(null, null, PLUGIN_NAME, "switchCamera", []);
};

CameraPreview.hide = function() {
    exec(null, null, PLUGIN_NAME, "hideCamera", []);
};

CameraPreview.show = function() {
    exec(null, null, PLUGIN_NAME, "showCamera", []);
};

CameraPreview.disable = function(disable) {
    exec(null, null, PLUGIN_NAME, "disable", [disable]);
};

CameraPreview.setFlashMode = function(mode) {
    exec(null, null, PLUGIN_NAME, "setFlashMode", [mode]);
};

CameraPreview.setZoomLevel = function(zoom) {
    if ( typeof zoom === 'undefined') zoom = 0;
    exec(null, null, PLUGIN_NAME, "setZoomLevel", [zoom]);
};

CameraPreview.setCameraOrientation = function(orientation) {
    if ( typeof orientation === 'undefined') orientation = 1;
    exec(null, null, PLUGIN_NAME, "setCameraOrientation", [orientation]);
};

CameraPreview.setCameraDebugMessageLogging = function(debugLevel) {
    if ( typeof debugLevel === 'undefined') debugLevel = 0;
    exec(null, null, PLUGIN_NAME, "setCameraDebugMessageLogging", [debugLevel]);
};

//@param targetWholeNumbers false
//@param maxZoomLevels 0
//@param maxZoomRatio 0
CameraPreview.getZoomLevels = function(targetWholeNumbers, maxZoomLevels, maxZoomRatio) {
    if (typeof maxZoomLevels === 'undefined') {
        maxZoomLevels = 0;
    }
    if (typeof maxZoomRatio === 'undefined') {
        maxZoomRatio = 0;
    }
    return new Promise( function(resolve, reject) {
                       exec(resolve, reject, PLUGIN_NAME, "getZoomLevels", [!!targetWholeNumbers, maxZoomLevels, maxZoomRatio]);
                       });
};

module.exports = CameraPreview;
