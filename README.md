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
  <li>Maintain HTML interactivity.</li>
</ul>

<p><b>Version 0.0.11</b></p>

<p><b>Installation:</b></p>

```
cordova plugin add https://github.com/clariontools/CordovaCameraPreview.git
```

<b>Phonegap Build:</b><br/>

```
<gap:plugin name="com.clariontools.camerapreview" version="0.0.11" source="plugins.cordova.io" />
```

<p><b>Methods:</b></p>


  <b>startCamera(rect, defaultCamera, tapEnabled, dragEnabled, toBack)</b><br/>
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

var alpha = 1.0 // alpha applied only when toBack = false default 1.0 (optional)
var prefix = 'pic-' // adds prefix to the .jpg filename default is 'picture' (optional)

cordova.plugins.camerapreview.startCamera(rect, defaultCamera, toBack, maxCaptureLength, rotation, alpha, prefix);

// or if called with a JavaScript promise:

var startCameraPromise = cordova.plugins.camerapreview.startCamera(rect, defaultCamera, toBack, maxCaptureLength, rotation, alpha, prefix);

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
    console.log('Result from takePicture: ' + result);
}, function (err) {
    console.log('Error from takePicture: ' + err);
});

```

<b>setOnPictureTakenHandler(callback)</b><br/>
<info>Register a callback function that receives the original picture image captured from the camera.</info><br/>

```
cordova.plugins.camerapreview.setOnPictureTakenHandler(function(result){
	document.getElementById('originalPicture').src = result[0];//originalPicturePath;
});
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
