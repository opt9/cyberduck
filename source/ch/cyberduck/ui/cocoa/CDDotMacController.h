/*
 *  Copyright (c) 2005 David Kocher. All rights reserved.
 *  http://cyberduck.ch/
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Bug fixes, suggestions and comments should be sent to:
 *  dkocher@cyberduck.ch
 */

#import <Cocoa/Cocoa.h>
#import <DotMacKit/DotMacKit.h>

/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class ch_cyberduck_ui_cocoa_CDDotMacController */

#ifndef _Included_ch_cyberduck_ui_cocoa_CDDotMacController
#define _Included_ch_cyberduck_ui_cocoa_CDDotMacController
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     ch_cyberduck_ui_cocoa_CDDotMacController
 * Method:    downloadBookmarksFromDotMacActionNative
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ch_cyberduck_ui_cocoa_CDDotMacController_downloadBookmarksFromDotMacActionNative
  (JNIEnv *, jobject, jstring);

/*
 * Class:     ch_cyberduck_ui_cocoa_CDDotMacController
 * Method:    uploadBookmarksToDotMacActionNative
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_ch_cyberduck_ui_cocoa_CDDotMacController_uploadBookmarksToDotMacActionNative
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif

@interface CDDotMacController : NSObject {
	@private
	BOOL syncPreferences;
	BOOL syncBookmarks;
	NSString *tmpBookmarkFile;
	NSException *e;
}

- (DMMemberAccount*)getUserAccount;

- (IBAction)downloadBookmarksFromDotMacAction:(id)sender;
- (IBAction)uploadBookmarksToDotMacAction:(id)sender;

- (IBAction)downloadPreferencesFromDotMacAction:(id)sender;
- (IBAction)uploadPreferencesToDotMacAction:(id)sender;

- (void)downloadFromDotMac:(NSString *)remoteFile usingAccount:(DMMemberAccount*)account;
- (void)uploadToDotMac:(NSString *)localFile usingAccount:(DMMemberAccount*)account;

@end
