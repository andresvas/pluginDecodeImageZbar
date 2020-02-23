package org.cloudsky.cordovaPlugins;

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
import android.os.Build;
import android.provider.MediaStore;
import android.widget.Toast;

import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import org.cloudsky.cordovaPlugins.ZBarScannerActivity;

import java.io.BufferedInputStream;
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

                Context appCtx = cordova.getActivity().getApplicationContext();
                Intent scanIntent = new Intent(appCtx, ZBarScannerActivity.class);
                scanIntent.putExtra(ZBarScannerActivity.EXTRA_PARAMS, params.toString());
                cordova.startActivityForResult(this, scanIntent, SCAN_CODE);
            }
            return true;
        } else if (action.equals("gallery")) {
            scanCallbackContext = callbackContext;
            readFromGallery();
            return  true;
        } else {
                return false;
            }

    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void readFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        cordova.startActivityForResult(this,intent, QR_DESDE_IMAGEN);
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
                Toast.makeText(cordova.getActivity(), data, Toast.LENGTH_SHORT).show();
                scanCallbackContext.success(data);
                //consultServiceQR(data);
                // Prevent to scan multiple qr codes
                break;
            }
        }
        // Check wheter or not there were error while scanning the QR Code
        if (hadErrorsWhileScanning) {
            //showQrInvalidate();
            Toast.makeText(cordova.getActivity(), "No se pudo leeer el QR", Toast.LENGTH_SHORT).show();
        }
    }



    // External results handler ----------------------------------------

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent result)
    {
        if(requestCode == SCAN_CODE) {
            switch(resultCode) {
                case Activity.RESULT_OK:
                    String barcodeValue = result.getStringExtra(ZBarScannerActivity.EXTRA_QRVALUE);
                    scanCallbackContext.success(barcodeValue);
                    break;
                case Activity.RESULT_CANCELED:
                    scanCallbackContext.error("cancelled");
                    break;
                case ZBarScannerActivity.RESULT_ERROR:
                    scanCallbackContext.error("Scan failed due to an error");
                    break;
                default:
                    scanCallbackContext.error("Unknown error");
            }
            isInProgress = false;
            scanCallbackContext = null;
        } else if (requestCode == QR_DESDE_IMAGEN) {
            if (resultCode == RESULT_OK) {
                Uri selectedImage = intent.getData();
                InputStream is;
                try {
                    isError = false;
                    is = getContentResolver().openInputStream(selectedImage);
                    decodeImage(is);
                } catch (FileNotFoundException e) {
                    Log.d("Todo1", e.getMessage());
                } catch (OutOfMemoryError e) {
                    try {
                        isError = true;
                        is = getContentResolver().openInputStream(selectedImage);
                        decodeImage(is);
                    } catch (FileNotFoundException ex) {
                        Log.d("Todo1", e.getMessage());
                    }
                } catch (Exception e) {
                    Log.d("Todo1", e.getMessage());
                }
            }
        }
    }
}
