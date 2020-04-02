package org.todo1.cordovaPlugins;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

//import org.cloudsky.cordovaPlugins.ZBarScannerActivity;

public class ZBar extends CordovaPlugin {


    // Configuration ---------------------------------------------------

    private static int SCAN_CODE = 1;


    // State -----------------------------------------------------------

    private boolean isInProgress = false;
    private CallbackContext scanCallbackContext;


    // Plugin API ------------------------------------------------------


    private boolean isError;
    public final static int QR_DESDE_IMAGEN = 888;
    private String error = "error";
    private Uri selectedImageTemp;


    @Override
    public boolean execute (String action, JSONArray args, CallbackContext callbackContext)
    throws JSONException
    {
        if(action.equals("scan")) {
            if(isInProgress) {
                callbackContext.error("A scan is already in progress!");
            } else {
                isInProgress = true;
                scanCallbackContext = callbackContext;
                JSONObject params = args.optJSONObject(0);

            }
            return true;
        } else if (action.equals("gallery")) {
            scanCallbackContext = callbackContext;
            openAndSelectFromGallery();
            return  true;
        } else {
                return false;
            }

    }

    private void openAndSelectFromGallery() {
        cordova.setActivityResultCallback (this);
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_DEFAULT);

        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);


        this.cordova.startActivityForResult((CordovaPlugin)this, Intent.createChooser(intent,
               "personas"),QR_DESDE_IMAGEN);


    }

    private void decodeImageFromGallery(InputStream is) {
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
        super.onActivityResult(requestCode, resultCode, result);
        if (requestCode == QR_DESDE_IMAGEN) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    selectedImageTemp = result.getData();
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
            decodeImageFromGallery(is);
        } catch (FileNotFoundException e) {
            scanCallbackContext.error(error);
        }
    }
}
