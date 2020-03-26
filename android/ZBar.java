package org.todo1.cordovaPlugins;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

//import org.cloudsky.cordovaPlugins.ZBarScannerActivity;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class ZBar extends CordovaPlugin {

    // Configuration ---------------------------------------------------

    private static int SCAN_CODE = 1;


    // State -----------------------------------------------------------

    private boolean isInProgress = false;
    private CallbackContext scanCallbackContext;


    // Plugin API ------------------------------------------------------


    private boolean isError;
    public final static int QR_DESDE_IMAGEN = 8;
    private String error = "error";
    private Uri selectedImageTemp;


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext)
            throws JSONException {
        if (action.equals("scan")) {
            if (isInProgress) {
                callbackContext.error("A scan is already in progress!");
            } else {
                isInProgress = true;
                scanCallbackContext = callbackContext;
                JSONObject params = args.optJSONObject(0);

            }
            return true;
        } else if (action.equals("gallery")) {
            scanCallbackContext = callbackContext;
            readFromGallery();
            return true;
        } else {
            return false;
        }

    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void readFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        cordova.startActivityForResult(this, intent, QR_DESDE_IMAGEN);
    }

    private void decodeImage(InputStream is) {
        int intArray[];
        Bitmap bMap;
        BufferedInputStream bis = new BufferedInputStream(is);
        ImageScanner scanner = new ImageScanner();
        if (isError) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 8;
            bMap = BitmapFactory.decodeStream(bis, null, options);
        } else {
            bMap = BitmapFactory.decodeStream(bis);
        }

        intArray = new int[bMap.getWidth() * bMap.getHeight()];
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());
        Image barcode = new Image(bMap.getWidth(), bMap.getHeight(), "RGB4");
        boolean hadErrorsWhileScanning = true;
        barcode.setData(intArray);
        int resultZbar = scanner.scanImage(barcode.convert("Y800"));
        if (resultZbar != 0) {
            hadErrorsWhileScanning = false;
            SymbolSet syms = scanner.getResults();
            for (Symbol sym : syms) {
                String data = sym.getData();
                scanCallbackContext.success(data);
                break;
            }
        }
        // Check wheter or not there were error while scanning the QR Code
        if (hadErrorsWhileScanning) {
            if (isError) {
                scanCallbackContext.error(error);
            } else {
                receiveImage(selectedImageTemp, true);
            }
        }
    }


    // External results handler ----------------------------------------

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == QR_DESDE_IMAGEN) {
            if (resultCode == Activity.RESULT_OK) {
                selectedImageTemp = result.getData();
                try {
                    receiveImage(selectedImageTemp, false);
                } catch (OutOfMemoryError e) {
                    receiveImage(selectedImageTemp, true);
                } catch (Exception e) {
                    scanCallbackContext.error(error);
                }
            }
        }
    }

    private void receiveImage(Uri selectedImage, boolean isErrorLocal) {
        InputStream is;
        isError = isErrorLocal;
        try {
            is = cordova.getContext().getContentResolver().openInputStream(selectedImage);
            decodeImage(is);
        } catch (FileNotFoundException e) {
            scanCallbackContext.error(error);
        }
    }
}
