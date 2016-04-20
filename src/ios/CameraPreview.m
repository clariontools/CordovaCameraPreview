#import <AssetsLibrary/AssetsLibrary.h>
#import <Cordova/CDV.h>
#import <Cordova/CDVPlugin.h>
#import <Cordova/CDVInvokedUrlCommand.h>

#import "CameraPreview.h"

@implementation CameraPreview

+ (UIDeviceOrientation) fromRotation: (NSInteger) rotation {
    
    switch (rotation) {
        case -1: return UIDeviceOrientationUnknown;
        case 0: return UIDeviceOrientationPortrait;
        case 1: return UIDeviceOrientationLandscapeRight;
        case 2: return UIDeviceOrientationPortraitUpsideDown;
        case 3: return UIDeviceOrientationLandscapeLeft;
        default: return UIDeviceOrientationUnknown;
    }
}

- (void) startCamera:(CDVInvokedUrlCommand*)command {
    
    CDVPluginResult *pluginResult;
    self.maxCaptureLength = 0;
    self.capturePresetUsed = false;
    
    if (self.sessionManager != nil) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Camera already started!"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    
    if (command.arguments.count > 3) {
        CGFloat x = (CGFloat)[command.arguments[0] floatValue] + self.webView.frame.origin.x;
        CGFloat y = (CGFloat)[command.arguments[1] floatValue] + self.webView.frame.origin.y;
        CGFloat width = (CGFloat)[command.arguments[2] floatValue];
        CGFloat height = (CGFloat)[command.arguments[3] floatValue];
        NSString *defaultCamera = command.arguments[4];
        //BOOL tapToTakePicture = (BOOL)[command.arguments[5] boolValue];
        //BOOL dragEnabled = (BOOL)[command.arguments[6] boolValue];
        BOOL toBack = (BOOL)[command.arguments[5] boolValue];
        self.maxCaptureLength = ([command.arguments[6] isEqual:[NSNull null]]) ? 0 : (long)[command.arguments[6] integerValue];
        self.lockOrientation = ([command.arguments[7] isEqual:[NSNull null]]) ? [CameraPreview fromRotation: -1] : [CameraPreview fromRotation: (NSInteger)[command.arguments[7] integerValue]];
        CGFloat containerAlpha = ([command.arguments[8] isEqual:[NSNull null]]) ? 1.0 : (CGFloat)[command.arguments[8] floatValue];
        self.filePrefix = ([command.arguments[9] isEqual:[NSNull null]]) ? @"picture" : command.arguments[9];
        
        //NSLog(@"maxCaptureLength: %ld", self.maxCaptureLength);
        //NSLog(@"lockOrientation: %ld", self.lockOrientation);
        //NSLog(@"containerAlpha: %f", containerAlpha);
        //NSLog(@"filePrefix: %@", self.filePrefix);

        // Create the session manager
        self.sessionManager = [[CameraSessionManager alloc] init];
        
        // Use more efficient capture format if larger is not needed and avoid resize
        // TESING FOR EFFECT OF sessionPreset
        //self.sessionManager.session.sessionPreset = AVCaptureSessionPresetPhoto;     // Max resolution of camera 4s - 6 3264x2448
        //self.sessionManager.session.sessionPreset = AVCaptureSessionPreset640x480;
        //self.sessionManager.session.sessionPreset = AVCaptureSessionPresetMedium;    // 320 x 480 on iPhone 8mp
        //self.sessionManager.session.sessionPreset = AVCaptureSessionPresetHigh;      // 1081 x 1980 on iPhone 8mp
        //self.sessionManager.session.sessionPreset = AVCaptureSessionPreset1280x720;
        if (self.maxCaptureLength == 640) {
            self.sessionManager.session.sessionPreset = AVCaptureSessionPreset640x480;
            self.capturePresetUsed = true;
        }
        
        //render controller setup
        self.cameraRenderController = [[CameraRenderController alloc] init];
        self.cameraRenderController.dragEnabled = false; // dragEnabled;
        self.cameraRenderController.tapToTakePicture = false; // tapToTakePicture;
        self.cameraRenderController.sessionManager = self.sessionManager;
        self.cameraRenderController.view.frame = CGRectMake(x, y, width, height);
        self.cameraRenderController.zoomLevel = 0;
        self.cameraRenderController.delegate = self;
        
        [self.viewController addChildViewController:self.cameraRenderController];
        //display the camera bellow the webview
        if (toBack) {
            //make transparent
            self.webView.opaque = NO;
            self.webView.backgroundColor = [UIColor clearColor];
            [self.viewController.view insertSubview:self.cameraRenderController.view atIndex:0];
        } else {
            self.cameraRenderController.view.alpha = containerAlpha;
            [self.viewController.view addSubview:self.cameraRenderController.view];
        }
        
        // Setup session
        self.sessionManager.delegate = self.cameraRenderController;
        [self.sessionManager setupSession:defaultCamera];
        
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"OK"];
        dispatch_async(self.sessionManager.sessionQueue, ^{
            [self.commandDelegate sendPluginResult:pluginResult callbackId:self.onCameraPreviewReadyHandlerId];
        });
        
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Invalid number of parameters"];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) setOnCameraPreviewReadyHandler:(CDVInvokedUrlCommand*)command {
    NSLog(@"setOnCameraPreviewReadyHandler");
    self.onCameraPreviewReadyHandlerId = command.callbackId;
}

- (void) stopCamera:(CDVInvokedUrlCommand*)command {
    NSLog(@"stopCamera");
    CDVPluginResult *pluginResult;
    
    if(self.sessionManager != nil) {
        [self.cameraRenderController.view removeFromSuperview];
        [self.cameraRenderController removeFromParentViewController];
        self.cameraRenderController = nil;
        
        //let session stopping occur in viewWillDissappear
        //[self.sessionManager.session stopRunning];
        self.sessionManager = nil;
        
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }
    else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Camera not started"];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) hideCamera:(CDVInvokedUrlCommand*)command {
    NSLog(@"hideCamera");
    CDVPluginResult *pluginResult;
    
    if (self.cameraRenderController != nil) {
        [self.cameraRenderController.view setHidden:YES];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Camera not started"];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) showCamera:(CDVInvokedUrlCommand*)command {
    NSLog(@"showCamera");
    CDVPluginResult *pluginResult;
    
    if (self.cameraRenderController != nil) {
        [self.cameraRenderController.view setHidden:NO];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Camera not started"];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) switchCamera:(CDVInvokedUrlCommand*)command {
    NSLog(@"switchCamera");
    CDVPluginResult *pluginResult;
    
    if (self.sessionManager != nil) {
        [self.sessionManager switchCamera];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Camera not started"];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) getZoomLevels:(CDVInvokedUrlCommand*)command {
    NSLog(@"getZoomLevels");
    CDVPluginResult *pluginResult;
    
    if (self.cameraRenderController != NULL) {
        bool targetWholeNumbers = [command.arguments[0] boolValue];
        long maxZoomLevels = [command.arguments[1] integerValue];
        long maxZoomRatio = [command.arguments[2] integerValue];
        //{"zoomRatios":["1.00","2.00","3.00","4.00"],"zoomLevels":[0,2,3,4],"zoomCount":3}
        //
        // For now not bothering to replicate Android interface just use the zoom level
        // long values to indicate whole number zooms could replicate Android interface
        // to set a default or build up ratios and levels based on parameters passed
        //
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"{\"zoomRatios\":[\"1.00\",\"2.00\",\"3.00\",\"4.00\"],\"zoomLevels\":[0,2,3,4],\"zoomCount\":3}"];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Camera not started"];
    }
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) setZoomLevel:(CDVInvokedUrlCommand*)command {
    NSLog(@"setZoomLevel");
    CDVPluginResult *pluginResult;
    
    if (self.cameraRenderController != NULL) {
        long zoom = [command.arguments[0] integerValue];

        if (zoom >= 0) {
            NSLog(@"zoom level requested %ld", zoom);
            self.cameraRenderController.zoomLevel = zoom;
            
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"OK"];
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Invalid zoom value requested"];
        }
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Camera not started"];
    }
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) takePicture:(CDVInvokedUrlCommand*)command {
    NSLog(@"takePicture");
    CDVPluginResult *pluginResult;
    
    if (self.cameraRenderController != NULL) {
        CGFloat maxW = (CGFloat)[command.arguments[0] floatValue];
        CGFloat maxH = (CGFloat)[command.arguments[1] floatValue];
        int quality = (int)[command.arguments[2] integerValue];
        [self invokeTakePicture:maxW withHeight:maxH withQuality:quality command:command];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"OK"];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Camera not started"];
    }
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) setOnPictureTakenHandler:(CDVInvokedUrlCommand*)command {
    NSLog(@"setOnPictureTakenHandler");
    self.onPictureTakenHandlerId = command.callbackId;
}

- (void) setColorEffect:(CDVInvokedUrlCommand*)command {
    NSLog(@"setColorEffect");
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    NSString *filterName = command.arguments[0];
    
    if ([filterName isEqual: @"none"]) {
        dispatch_async(self.sessionManager.sessionQueue, ^{
            [self.sessionManager setCiFilter:nil];
        });
    } else if ([filterName isEqual: @"mono"]) {
        dispatch_async(self.sessionManager.sessionQueue, ^{
            CIFilter *filter = [CIFilter filterWithName:@"CIColorMonochrome"];
            [filter setDefaults];
            [self.sessionManager setCiFilter:filter];
        });
    } else if ([filterName isEqual: @"negative"]) {
        dispatch_async(self.sessionManager.sessionQueue, ^{
            CIFilter *filter = [CIFilter filterWithName:@"CIColorInvert"];
            [filter setDefaults];
            [self.sessionManager setCiFilter:filter];
        });
    } else if ([filterName isEqual: @"posterize"]) {
        dispatch_async(self.sessionManager.sessionQueue, ^{
            CIFilter *filter = [CIFilter filterWithName:@"CIColorPosterize"];
            [filter setDefaults];
            [self.sessionManager setCiFilter:filter];
        });
    } else if ([filterName isEqual: @"sepia"]) {
        dispatch_async(self.sessionManager.sessionQueue, ^{
            CIFilter *filter = [CIFilter filterWithName:@"CISepiaTone"];
            [filter setDefaults];
            [self.sessionManager setCiFilter:filter];
        });
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Filter not found"];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) setFlashMode:(CDVInvokedUrlCommand*)command {
    NSInteger mode = [command.arguments[0] integerValue];
    [self.sessionManager setFlashMode:mode];
}

- (void) invokeTakePicture:(CDVInvokedUrlCommand *)command {
    [self invokeTakePicture:0.0 withHeight:0.0 withQuality:80 command:command];
}
+ (NSString *) applicationDocumentsDirectory
{
    NSString *path = [NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    return [path stringByAppendingPathComponent:@"NoCloud"]; // cordova.file.dataDirectory
}
+ (NSString *)saveImage:(UIImage *)image withName:(NSString*)name withQuality:(int)quality {
    NSData *data = UIImageJPEGRepresentation(image, (CGFloat)quality * 0.01);
    NSFileManager *fileManager = [NSFileManager defaultManager];
    NSString *fullPath = [[CameraPreview applicationDocumentsDirectory] stringByAppendingPathComponent:name];
    [fileManager createFileAtPath:fullPath contents:data attributes:nil];
    
    return fullPath;
}

- (void) invokeTakePicture:(CGFloat) maxWidth withHeight:(CGFloat) maxHeight withQuality:(int) quality command:(CDVInvokedUrlCommand*)command {
    AVCaptureConnection *connection = [self.sessionManager.stillImageOutput connectionWithMediaType:AVMediaTypeVideo];
    [self.sessionManager.stillImageOutput captureStillImageAsynchronouslyFromConnection:connection completionHandler:^(CMSampleBufferRef sampleBuffer, NSError *error) {

        NSLog(@"Done creating still image");
        NSLog(@"Quality parameter is: %d.", quality);

        if (error) {
            NSLog(@"Error taking picture: %@", error);
            CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Camera not started"];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        } else {
            // Capture a preview image from the latest video frame
            // remove preview capture but could add back if had switch to save to the Gallery
            /*
             [self.cameraRenderController.renderLock lock];
             CIImage *previewCImage = self.cameraRenderController.latestFrame;
             [self.cameraRenderController.renderLock unlock];
             */
            
            // Get the current device orientation or lock with a requested orientation
            UIDeviceOrientation currentOrientation = self.lockOrientation != UIDeviceOrientationUnknown ? self.lockOrientation : [[UIDevice currentDevice] orientation];
            
            BOOL imageRotated = false;
            CIImage *capturedCImage;
            NSData *imageData = [AVCaptureStillImageOutput jpegStillImageNSDataRepresentation:sampleBuffer];
            UIImage *capturedImage  = [CameraPreview rotateImage:[[UIImage alloc] initWithData:imageData] rotateTo:currentOrientation wasRotated:&imageRotated];
            NSLog(@"Image size is %f x %f", capturedImage.size.width, capturedImage.size.height);
            NSLog(@"Image orientation is %ld", (long)capturedImage.imageOrientation);
            
            // Image resize
            self.imageWasRotated = imageRotated;
            BOOL skipResize = false;
            if (self.capturePresetUsed == true) {
                if (maxWidth > 0 && maxHeight > 0) {
                    // if any side of the capture rectangle is larger than the maxCaptureLength skip resize
                    if (maxWidth >= maxHeight) {
                        if ((long)maxWidth >= self.maxCaptureLength) {
                            skipResize = true;
                        }
                    } else {
                        if ((long)maxHeight >= self.maxCaptureLength) {
                            skipResize = true;
                        }
                    }
                }
            }
            
            CGFloat maxWidthResize = maxWidth;
            CGFloat maxHeightResize = maxHeight;
            CGFloat capturedWidth = capturedImage.size.width;
            CGFloat capturedHeight = capturedImage.size.height;
            CGFloat scaleHeightAdjustment = 0.0f;
            CGFloat scaleWidthAdjustment = 0.0f;
            CGFloat frameWidth = 0.0f;
            CGFloat frameHeight = 0.0f;
            long adjustX;
            long adjustY;
            long zoomLevel = self.cameraRenderController.zoomLevel;
            
            if (self.imageWasRotated == true) {
                NSLog(@"image WAS rotaed.");
                maxWidthResize = maxHeight;
                maxHeightResize = maxWidth;
            }
            
            if (zoomLevel > 0) {
                skipResize = false;
                maxWidthResize = capturedWidth * zoomLevel;
                maxHeightResize = capturedHeight * zoomLevel;
                
                frameWidth = self.cameraRenderController.view.frame.size.width;
                frameHeight = self.cameraRenderController.view.frame.size.height;
                
                scaleWidthAdjustment = 1.00000000000000000f - (frameWidth / capturedWidth);
                scaleHeightAdjustment = 1.00000000000000000f - (frameHeight / capturedHeight);
                
                adjustX = frameWidth * scaleWidthAdjustment;
                adjustY = frameHeight * scaleHeightAdjustment;
            }
            
            if (skipResize == false && maxWidthResize > 0 && maxWidthResize > 0) {
                NSLog(@"Resizing image");
                CIImage *resizedCImage;
                
                CGFloat scaleHeight = maxHeightResize/capturedHeight;
                CGFloat scaleWidth = maxWidthResize/capturedWidth;
                CGFloat scale = scaleHeight > scaleWidth ? scaleWidth : scaleHeight;
                
                NSLog(@"Scale = %f", scale);
                
                CIFilter *resizeFilter = [CIFilter filterWithName:@"CILanczosScaleTransform"];
                [resizeFilter setValue:[[CIImage alloc] initWithCGImage:[capturedImage CGImage]] forKey:kCIInputImageKey];
                [resizeFilter setValue:[NSNumber numberWithFloat:1.0f] forKey:@"inputAspectRatio"];
                [resizeFilter setValue:[NSNumber numberWithFloat:scale] forKey:@"inputScale"];
                
                if (zoomLevel > 0) {
                    resizedCImage = [resizeFilter outputImage];
                    
                    long xPos = (maxWidthResize - capturedWidth) / 2.00000000000000000f;
                    long yPos = (maxHeightResize - capturedHeight) / 2.00000000000000000f;
                    
                    long adjustX = self.cameraRenderController.view.frame.size.width * scaleWidthAdjustment;
                    long adjustY = self.cameraRenderController.view.frame.size.height * scaleHeightAdjustment;
                    
                    xPos += adjustX;   // Make adjustments for preview versus actual capture aspect ratio
                    yPos -= adjustY;
                    
                    CIFilter *cropFilter = [CIFilter filterWithName:@"CICrop"];
                    CIVector *cropRect = [CIVector vectorWithX:xPos Y:yPos Z:capturedWidth W:capturedHeight];
                    [cropFilter setValue:resizedCImage forKey:kCIInputImageKey];
                    [cropFilter setValue:cropRect forKey:@"inputRectangle"];
                    
                    capturedCImage = [cropFilter outputImage];
                } else {
                    capturedCImage = [resizeFilter outputImage];
                }
                
            } else {
                capturedCImage = [[CIImage alloc] initWithCGImage:[capturedImage CGImage]];
            }
            
            CIImage *imageToFilter;
            CIImage *finalCImage;
            
            // Fix front mirroring
            if (self.sessionManager.defaultCamera == AVCaptureDevicePositionFront) {
                CGAffineTransform matrix = CGAffineTransformTranslate(CGAffineTransformMakeScale(1, -1), 0, capturedCImage.extent.size.height);
                imageToFilter = [capturedCImage imageByApplyingTransform:matrix];
            } else {
                imageToFilter = capturedCImage;
            }
            
            CIFilter *filter = [self.sessionManager ciFilter];
            if (filter != nil) {
                [self.sessionManager.filterLock lock];
                [filter setValue:imageToFilter forKey:kCIInputImageKey];
                finalCImage = [filter outputImage];
                [self.sessionManager.filterLock unlock];
            } else {
                finalCImage = imageToFilter;
            }
            
            __block NSString *originalPicturePath;
            //__block NSString *previewPicturePath;

            dispatch_group_t group = dispatch_group_create();

            // Task 1 preview image save
            /*
            dispatch_group_enter(group);
            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
                // Process preview on a background queue
                NSString *fileName = [self.filePrefix stringByAppendingString:[[[NSUUID UUID] UUIDString] stringByAppendingString:@".jpg"]];
                CIContext *context = [CIContext contextWithOptions:nil];
                UIImage *saveUIImage = [UIImage imageWithCGImage:[context createCGImage:previewCImage fromRect:previewCImage.extent]];
                previewPicturePath = [CameraPreview saveImage: saveUIImage withName: fileName];
                
                NSLog(@"previewPicturePath: %@", previewPicturePath);
                dispatch_group_leave(group);
            });
            */
            // Task 2 orginal image save
            dispatch_group_enter(group);
            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
                // Process orginal image on a background queue
                NSString *fileName = [self.filePrefix stringByAppendingString:[[[NSUUID UUID] UUIDString] stringByAppendingString:@".jpg"]];
                CIContext *context = [CIContext contextWithOptions:nil];
                UIImage *saveUIImage = [UIImage imageWithCGImage:[context createCGImage:finalCImage fromRect:finalCImage.extent]];

                originalPicturePath = [CameraPreview saveImage: saveUIImage withName: fileName withQuality: quality];

                NSLog(@"originalPicturePath: %@", originalPicturePath);
                dispatch_group_leave(group);
            });
            
            dispatch_group_notify(group, dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
                NSMutableArray *params = [[NSMutableArray alloc] init];
                
                // Success returns two elements with path to original .jpg in [0]
                // REMOVE PREVIEW UNLESS ADD OPTION TO SAVE TO GALLERY and preview .jpg in [1]
                [params addObject:originalPicturePath];
                //[params addObject:previewPicturePath];
                
                CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:params];
                [pluginResult setKeepCallbackAsBool:true];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:self.onPictureTakenHandlerId];
            });
        }
    }];
}

+ (UIImage *) rotateImage: (UIImage *) imageIn rotateTo:(UIDeviceOrientation) orientation wasRotated:(BOOL*) imageWasRotated {
    // Camera defaults to UIImageOrientationRight (3)
    // rotate the image to match the device orientation at the time of taking the picture
    
    CGImageRef        imgRef    = imageIn.CGImage;
    CGFloat           width     = CGImageGetWidth(imgRef);
    CGFloat           height    = CGImageGetHeight(imgRef);
    
    // Calculate the size of the rotated view
    UIView *rotatedViewBox = [[UIView alloc] initWithFrame:CGRectMake(0, 0, width, height)];
    
    CGFloat angle = 0.0;
    switch (orientation) {
        case UIDeviceOrientationPortrait:
            // rotate 90° right
            angle = M_PI_2;
            break;
        case UIDeviceOrientationPortraitUpsideDown:
            // rotate 90° left
            angle = -M_PI_2;
            break;
        case UIDeviceOrientationLandscapeLeft:
            // no rotation
            angle = 0.0;
            break;
        case UIDeviceOrientationLandscapeRight:
            // rotate 180°
            angle = M_PI;
            break;
        default:
            // no rotation
            angle = 0.0;
    }
    
    rotatedViewBox.transform = CGAffineTransformMakeRotation(angle);;
    CGSize rotatedSize = rotatedViewBox.frame.size;
    
    // Create bitmap context;
    UIGraphicsBeginImageContext(rotatedSize);
    CGContextRef bitmap = UIGraphicsGetCurrentContext();
    
    // Move origin to the middle to rotate/scale around center
    CGContextTranslateCTM(bitmap, rotatedSize.width/2, rotatedSize.height/2);
    
    // Rotate image
    CGContextRotateCTM(bitmap, angle);
    
    // Scale and draw image
    CGContextScaleCTM(bitmap, 1.0, -1.0);
    CGContextDrawImage(bitmap, CGRectMake(-width/2, -height/2, width, height), imgRef);
    
    UIImage *rotated = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    if (angle == 0.0) {
        *imageWasRotated = false;
    } else {
        *imageWasRotated = true;
    }
    
    return rotated;
}

@end
