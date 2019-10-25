/*
 * Copyright (C) 2018 SoftBank Robotics Europe
 * See COPYING for the license
 */
package uni.gesturedetectiononpepper.classifier;

import android.graphics.Bitmap;

import java.util.List;
import java.util.Locale;

/**
 * Generic interface for interacting with different recognition engines.
 */
public interface Classifier {
    /**
     * An immutable result returned by a Classifier describing what was recognized.
     */
    class Recognition {
        /**
         * A unique identifier for what has been recognized. Specific to the class, not the instance of
         * the object.
         */
        private final String id;

        public void setTitle(String title) {
            this.title = title;
        }

        /**
         * Display name for the recognition.
         */
        private  String title;

        /**
         * A sortable score for how good the recognition is relative to others. Higher should be better.
         */
        private final Float confidence;

        public Recognition(
                final String id, final String title, final Float confidence) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public Float getConfidence() {
            return confidence;
        }

        @Override
        public String toString() {
            String resultString = "";
            if (id != null) {
                resultString += "[" + id + "] ";
            }

            if (title != null) {
                resultString += title + " ";
            }

            if (confidence != null) {
                resultString += String.format(Locale.getDefault(), "(%.1f%%) ", confidence * 100.0f);
            }

            return resultString.trim();
        }
    }
    List<Recognition> recognizeImage(Bitmap bitmap);

    void close();
}
