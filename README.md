Cordova CameraPreview Plugin
====================

Cordova plugin that allows camera interaction from HTML code.<br/>
Show camera preview popup on top of the HTML.<br/>
<br/>
<p><b>Purpose of Fork:</b></p>
Published for testing integration specifically targeting the Meteor JavaScript App Platform. Use at your own Risk.<br/>

<p><b>Features:</b></p>
<ul>
  <li>Start a camera preview from HTML code.</li>
  <li>Send the preview box to back of the HTML content.</li>
  <li>Set a custom position for the camera preview box.</li>
  <li>Set a custom size for the preview box.</li>
  <li>Set a custom alpha for the preview box.</li>
  <li>Set the camera flash mode to off, on, or auto.</li>
  <li>Set the quality of the saved JPEG capture file.</li>
  <li>Set the camera zoom.</li>
  <li>Set auto focus with detection before picture taken.</li>
  <li>Set device orientation callback to allow independant ui changes that reflect the camera orientation.</li>
  <li>Set debug message callback to notify user of picture errors due to low memory and for plugin debugging on remote devices.</li>
  <li>Maintain HTML interactivity.</li>
</ul>

<p><b>Version 0.0.17</b></p>

<p><b>Installation:</b></p>

```
cordova plugin add https://github.com/clariontools/CordovaCameraPreview.git

  or

ionic plugin add https://github.com/clariontools/CordovaCameraPreview.git
```

<b>Phonegap Build:</b><br/>

```
<gap:plugin name="com.clariontools.camerapreview" version="0.0.17" source="plugins.cordova.io" />
```

<b>METEOR:</b><br/>

```
meteor add cordova:com.clariontools.camerapreview@https://github.com/clariontools/CordovaCameraPreview.git#[latest_commit_id]

```

<p><b>Methods:</b></p>


  <b>startCamera(rect, defaultCamera, toBack, maxCaptureLength, rotation, debugLevel, alpha, prefix)</b><br/>
  <info>
  	Starts the camera preview instance.
  	<br/>
	<br/>
	When setting the toBack to TRUE, remember to add the style bellow on your app's HTML body element:
```
style="background-color='transparent'"
```
</info>

Javascript:

```
var rect = {x: 100, y: 100, width: 200, height:200}; (required)
var defaultCamera = 'rear'  // rear or front default is rear (optional)
var toBack = true; // true indicates preview in back of html preview false forces preview on top with alpha value applied (optional)
var maxCaptureLength = 640;  // This controls the max length of capture width or height 640 is only value currently supported forces 640x480 capture and preview faster and no need to resize (optional)

var deviceRotation = cordova.plugins.camerapreview.ROTATION_FREE; // (optional)
  // Possible values for camera device rotation:
  // cordova.plugins.camerapreview.ROTATION_FREE  // default value
  // cordova.plugins.camerapreview.ROTATION_PORTRAIT
  // cordova.plugins.camerapreview.ROTATION_LANDSCAPE_RIGHT
  // cordova.plugins.camerapreview.ROTATION_PORTRAIT_UPSIDE_DOWN
  // cordova.plugins.camerapreview.ROTATION_LANDSCAPE_LEFT

var debugLevel = 0; // only send NOPIC: messages back to setOnCameraDebugMessageHandler(callback) function value 1 enables all active plugin debug messages
var alpha = 1.0 // alpha applied only when toBack = false default 1.0 (optional)
var prefix = 'pic-' // adds prefix to the .jpg filename default is 'picture' (optional)

cordova.plugins.camerapreview.startCamera(rect, defaultCamera, toBack, maxCaptureLength, rotation, debugLevel, alpha, prefix);

// or if called with a JavaScript promise:

var startCameraPromise = cordova.plugins.camerapreview.startCamera(rect, defaultCamera, toBack, maxCaptureLength, rotation, debugLevel, alpha, prefix);

startCameraPromise.then(function (result) {
    console.log('Result from startCamera: ' + result);
}, function (err) {
    console.log('Error from startCamera: ' + err);
});

```

<b>stopCamera()</b><br/>
<info>Stops the camera preview instance.</info><br/>

```
cordova.plugins.camerapreview.stopCamera();
```

<b>takePicture(size)</b><br/>
<info>Take the picture, the parameter size is optional, the quality parameter is also optional but will default to 80.  If you use the new _maxCaptureLength_ parameter in the startCamera method then the takePicture maxWidth and maxHeight parameters are ignored.</info><br/>

```
cordova.plugins.camerapreview.takePicture({maxWidth:640, maxHeight:640}, 80);

// if only the quality parameter is to be set:

cordova.plugins.camerapreview.takePicture(undefined, 75);


// or if called with a JavaScript promise:

// EXAMPLE 1: without maxWidth and maxHeight...
var takePicturePromise = cordova.plugins.camerapreview.takePicture(undefined,72);

// EXAMPLE 2: original style with flakey maxWidth and maxHeight parms specified...
var takePicturePromise = cordova.plugins.camerapreview.takePicture({maxWidth:640, maxHeight:640});

// EXAMPLE 3: with the defaults of no maxWidth, maxHeight and quality of 80...
var takePicturePromise = cordova.plugins.camerapreview.takePicture();

takePicturePromise.then(function (result) {
    // Advise not to use this to make any important settings
    // the picture take handler callback may occur before the promise is
    // returned, there is no gaurantee, the err return is ok to use to handle error.
    // this is ok to determine the call was made, but the real action is in the callback.
    console.log('Result from takePicture: ' + result);
}, function (err) {
    console.log('Error from takePicture: ' + err);
});

```

<b>setOnCameraPreviewReadyHandler(callback)</b><br/>
<info>Register a callback function that receives notification that the camera preview is ready. Use this to make property settings to the preview that would otherwise fail if called before the preview has been instantiated.</info><br/>

```
cordova.plugins.camerapreview.setOnCameraPreviewReadyHandler(function(result){
    console.log('camera preview is ready!  Result: ' + result);
    setCameraFlashMode(cameraFlashMode);
    // getZoomLevels etc...
});
```

<b>setOnPictureTakenHandler(callback)</b><br/>
<info>Register a callback function that receives the original picture image captured from the camera.</info><br/>

```
cordova.plugins.camerapreview.setOnPictureTakenHandler(function(result){
	document.getElementById('originalPicture').src = result[0];//originalPicturePath;
});
```

<b>getZoomLevels(targetWholeNumbers, maxZoomLevel, maxZoomRatio)</b><br/>
<info>getZoomLevels returns a JSON object with the zoom ratios array, zoom level array, and zoom count to be used with setZoomLevel method and to build up a useful interface in your app.</info><br/>

```
var getZoomPromise = cordova.plugins.camerapreview.getZoomLevels(
Session.get('targetWholeNumbers'),
Session.get('maxZoomLevel'),
Session.get('maxZoomRatio')
);
//
// examples
// 
//var getZoomPromise = cordova.plugins.camerapreview.getZoomLevels(true,3);
//var getZoomPromise = cordova.plugins.camerapreview.getZoomLevels(false,0,200);
//var getZoomPromise = cordova.plugins.camerapreview.getZoomLevels(true,7,600);
//var getZoomPromise = cordova.plugins.camerapreview.getZoomLevels();

getZoomPromise.then(function (result) {
  console.log('Result from takePicture: ' + result);
  }, function (err) {
  console.log('Error from takePicture: ' + err);
});

```

<b>setZoomLevel(zoomLevel)</b><br/>
<info>Sets the zoom level based on valid level reported back from getZoomLevels object.</info><br/>

```
cordova.plugins.camerapreview.setZoomLevel(zoomObject.zoomLevels[currentZoomCount]);

```

<b>camerapreview.setOnCameraDebugMessageHandler(callback)</b><br/>
<info>Register a callback function that receives debug message notifications reporting standardized messages for memory issues causing no picture condition.  Also will return messages that have been placed in the plugin native code used to track down errors that occur on specifc devices in the field.  Currently only implemented for Android to facilitate debugging problems that cannot be recreated when testing locally.  The return messages can change over time depending on problems found, and if a specific batch of messages have helped to track down an issue, they should be removed from the native source upon the next plugin release.</info><br/>

```
cordova.plugins.camerapreview.setOnCameraDebugMessageHandler(function (result) {
// plugin will return messages starting with "NOPIC: ..." if you implement this handler
// and set the debug level to 1 you will start getting all debug messages set in the plugin native code
// set debug level back to 0 to reset back to only "NOPIC: ..." messages NOPIC means no picture was taken.
// ON NOPIC: you may want to tell the user what happened.
console.log( 'Debug Message: ' + result[0] );
});
cordova.plugins.camerapreview.setCameraDebugMessageLogging(debugLevel);  // Good place to turn full debug on, must be after the callback handler

```

<b>camerapreview.setCameraDebugMessageLogging(debugLevel);</b><br/>
<info>Sets the level of messages returned to the debug message handler callback (setOnCameraDebugMessageHandler). Default value is 0 and will always issue "NOPIC: ..." errors when picture cannot be created due to out of memory conditions.  Set to 1 to turn on extra (mainly Android based) debug messages that have been added to the native code to help track down exceptions in the field.</info><br/>

```
cordova.plugins.camerapreview.setCameraDebugMessageLogging(debugLevel);

```

<b>camerapreview.setOnOrientationChangeHandler(callback)</b><br/>
<info>Register a callback function that receives stanrdized notification of device orientation changes.  Callback will not fire on FaceDown, FaceUp, or Unknown orientation values.  Generally tested with the preview set in the default ROTATION_FREE mode and return values are based on values returned by iOS (return 1 = Portrait, 3 = Rotate Left Landscape, 2 = Upside Down Portrait, 4 = Rotate Right Landscape).  Returns the standardized values in both iOS and Android.  Use this callback to set ui controls to indicate the orientation of the picture that will be captured.</info><br/>

```
cordova.plugins.camerapreview.setOnOrientationChangeHandler(function (result) {
// plugin will return 1 = Portrait, 3 = Rotate Left Landscape, 2 = Upside Down Portrait, 4 = Rotate Right Landscape
// don't forget to set the starting orientation of your layout, default is 1 portrait
// but you have to call setCameraOrientation(cameraOrientation); on preview ready to set
console.log( 'Detected Device Orientation Change. Result: ' + result[0] );
});

```

<b>camerapreview.setCameraOrientation(orientation)</b><br/>
<info>Set the default orientation of the camera, this is important if the camera were started in FaceUp, FaceDown, or Unknown orientation values that could be returned from the device.  Set this to match the starting orientation of your ui camera preview overlay, default is portrait.</info><br/>

```
cordova.plugins.camerapreview.setCameraOrientation(orientation);

```

<b>switchCamera()</b><br/>
<info>Switch from the rear camera and front camera, if available.</info><br/>

```
cordova.plugins.camerapreview.switchCamera();
```

<b>show()</b><br/>
<info>Show the camera preview box.</info><br/>

```
cordova.plugins.camerapreview.show();
```

<b>hide()</b><br/>
<info>Hide the camera preview box.</info><br/>

```
cordova.plugins.camerapreview.hide();
```

<b>setFlashMode(mode)</b><br/>
<info>Set the camera flash mode use flash mode variables</info><br/>
<ul>
<li>cordova.plugins.camerapreview.FLASH_AUTO</li>
<li>cordova.plugins.camerapreview.FLASH_ON</li>
<li>cordova.plugins.camerapreview.FLASH_OFF</li>
<li>cordova.plugins.camerapreview.FLASH_TORCH</li>
</ul>

```
cordova.plugins.camerapreview.(cordova.plugins.camerapreview.FLASH_AUTO);
```

<b>Base64 image:</b><br/>
Use the cordova-file in order to read the picture file and them get the base64.<br/>
Please, refer to this documentation: http://docs.phonegap.com/en/edge/cordova_file_file.md.html<br/>
Method <i>readAsDataURL</i>: Read file and return data as a base64-encoded data URL.

<b>Sample:</b><br/>
Please see the <a href="https://github.com/mbppower/CordovaCameraPreviewApp">CordovaCameraPreviewApp</a> for a complete working example for Android and iOS platforms.  NOTE: This forked version of the Camera Preview Plugin will not work with the mbppower example without some modification.

<p><b>Android Screenshots:</b></p>
<p><img src="https://raw.githubusercontent.com/mbppower/CordovaCameraPreview/master/docs/img/android-1.png"/></p>
<p><img src="https://raw.githubusercontent.com/mbppower/CordovaCameraPreview/master/docs/img/android-2.png"/></p>
