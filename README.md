Cordova ID Scanner 
==================

Cordova plugin that uses the camera and OCR to scan id numbers.


# Features

<ul>
  <li>Start a camera preview with a graphic overlay and use OCR to read an id from the image.</li>
</ul>

# Installation

To install the master version with latest fixes and features

```
cordova plugin add https://github.com/kanayo/kanayo-cordova-plugin-id-scanner.git

```

# Methods

### startScan(options, [successCallback, errorCallback])

Starts the Scanner. 

Text that matches the candidateExpression is displayed with a white border. 

Text that matches the verifyExpression && verifyChecksum is displayed with a green border 
and returned to the caller.
<br>

<strong>Options:</strong>
All options stated are optional and will default to values here

* `instructions` - Defaults to 'Aim the camera at the id card' - scanning instructions to display to user
* `cameraDirection` - Defaults to 'back' - or 'front'
* `cancelText` - Defaults to 'Cancel' - text for cancel scan button
* `switchText` - Defaults to 'Switch Cameras' - text for switch cameras button
* `candidateExpression` - Defaults to ""  - Regular expression to pre-verify ids
* `verifyExpression` - Defaults to ""  - Regular expression to verify ids
* `verifyChecksum` - Defaults to ""  - Name of checksum algorithm to use to verify ids. One of:
                     "Mod11_2", "Mod37_2", "Mod97_10", "Mod37_2", "Mod661_26", "Mod1271_36"


```javascript
let options = {
  cameraDirection: 'front',
  verifyExpression: '^[\d]{10}.*',
  verifyChecksum: 'Mod11_2'
};

IdScanner.startScan(options);
```

### stopScan([successCallback, errorCallback])

<info>Stops the camera preview view and scanning.</info><br/>

```javascript
IdScanner.stopScan();
```



# Credits

cordova-plugin-camera-preview Maintained by Weston Ganger - [@westonganger](https://github.com/westonganger)

cordova-plugin-camera-preview Created by Marcel Barbosa Pinto [@mbppower](https://github.com/mbppower)

mobile-vision-oc: (http://codelabs.developers.google.com/codelabs/mobile-vision-ocr)

iso7064 library: (https://github.com/danieltwagner/iso7064)
