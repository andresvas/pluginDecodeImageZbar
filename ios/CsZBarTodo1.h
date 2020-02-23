#import <Cordova/CDV.h>

#import "ZBarSDK.h"
#import <UIKit/UIKit.h>

@interface CsZBarTodo1 : CDVPlugin <ZBarReaderDelegate>

- (void)scan: (CDVInvokedUrlCommand*)command;
- (void)toggleflash;




@end
