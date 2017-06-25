/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kanayo.cordova.idscanner.ocr;

import android.util.Log;
import android.util.SparseArray;

import com.kanayo.cordova.idscanner.camera.GraphicOverlay;
import com.kanayo.cordova.idscanner.iso7064.Mod11_2;
import com.kanayo.cordova.idscanner.iso7064.Mod37_2;
import com.kanayo.cordova.idscanner.iso7064.Mod97_10;
import com.kanayo.cordova.idscanner.iso7064.Mod661_26;
import com.kanayo.cordova.idscanner.iso7064.Mod1271_36;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;

import java.util.ArrayList;

/**
 * A very simple Processor which gets detected TextBlocks and adds them to the overlay
 * as OcrGraphics.
 */
public class OcrDetectorProcessor implements Detector.Processor<TextBlock> {

    private final  IdScanListener mListener;
    private GraphicOverlay<OcrGraphic> mGraphicOverlay;

    public static class VerifyResult {
        public String processedText;
        public boolean isCandidate;
        public boolean isVerified;
    }


    public interface IdScanListener {
        void onScanFinished(String message);
        VerifyResult verifyScannedText(String message);
    }

    public OcrDetectorProcessor(GraphicOverlay<OcrGraphic> ocrGraphicOverlay, IdScanListener listener) {
        mGraphicOverlay = ocrGraphicOverlay;
        mListener = listener;
    }

    /**
     IdScanListener* Called by the detector to deliver detection results.
     * If your application called for it, this could be a place to check for
     * equivalent detections by tracking TextBlocks that are similar in location and content from
     * previous frames, or reduce noise by eliminating TextBlocks that have not persisted through
     * multiple detections.
     */
    @Override
    public void receiveDetections(Detector.Detections<TextBlock> detections) {
        mGraphicOverlay.clear();
        SparseArray<TextBlock> items = detections.getDetectedItems();
        ArrayList<TextBlock> candidateItems = new ArrayList<TextBlock>();
        TextBlock foundItem = null;
        String foundText = null;

        for (int i = 0; i < items.size(); ++i) {
            TextBlock item = items.valueAt(i);
            if (item != null && item.getValue() != null) {
                Log.d("OcrDetectorProcessor", "Text detected! " + item.getValue());

                VerifyResult result = mListener.verifyScannedText( item.getValue() );
                if (result.isCandidate) {
                    candidateItems.add( item );
                }
                if( result.isVerified ){
                    foundItem = item;
                    foundText = result.processedText;
                }
            }

        }

        if( foundItem != null ){
            mGraphicOverlay.clear();
            OcrGraphic graphic = new OcrGraphic(mGraphicOverlay, foundItem, true);
            mGraphicOverlay.add(graphic);
        } else if(candidateItems.size() > 0 ){
            mGraphicOverlay.clear();
            for (int i = 0; i < candidateItems.size(); ++i) {
                TextBlock item = candidateItems.get(i);
                OcrGraphic graphic = new OcrGraphic(mGraphicOverlay, item, false);
                mGraphicOverlay.add(graphic);
            }
        }

        if( foundText != null){
            mListener.onScanFinished(foundText);
        }


    }

    /**
     * Frees the resources associated with this detection processor.
     */
    @Override
    public void release() {
        mGraphicOverlay.clear();
    }
}
