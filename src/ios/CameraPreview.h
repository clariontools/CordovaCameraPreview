#import <Cordova/CDV.h>
#import <Cordova/CDVPlugin.h>
#import <Cordova/CDVInvokedUrlCommand.h>

#import "CameraSessionManager.h"
#import "CameraRenderController.h"

@interface CameraPreview : CDVPlugin <TakePictureDelegate>

- (void) startCamera:(CDVInvokedUrlCommand*)command;
- (void) stopCamera:(CDVInvokedUrlCommand*)command;
- (void) showCamera:(CDVInvokedUrlCommand*)command;
- (void) hideCamera:(CDVInvokedUrlCommand*)command;
- (void) switchCamera:(CDVInvokedUrlCommand*)command;
- (void) takePicture:(CDVInvokedUrlCommand*)command;
- (void) setOnPictureTakenHandler:(CDVInvokedUrlCommand*)command;
- (void) setColorEffect:(CDVInvokedUrlCommand*)command;
- (void) setFlashMode:(CDVInvokedUrlCommand*)command;

- (void) invokeTakePicture:(CGFloat) maxWidth withHeight:(CGFloat) maxHeight withQuality:(int) quality command:(CDVInvokedUrlCommand*)command;
- (void) invokeTakePicture:(CDVInvokedUrlCommand*)command;

@property (nonatomic) CameraSessionManager *sessionManager;
@property (nonatomic) CameraRenderController *cameraRenderController;
@property (nonatomic) NSString *onPictureTakenHandlerId;
@property (nonatomic) long maxCaptureLength;
@property (nonatomic) BOOL capturePresetUsed;
@property (nonatomic) BOOL imageWasRotated;
@property (nonatomic) UIDeviceOrientation lockOrientation;
@property (nonatomic) NSString *filePrefix;

@end
