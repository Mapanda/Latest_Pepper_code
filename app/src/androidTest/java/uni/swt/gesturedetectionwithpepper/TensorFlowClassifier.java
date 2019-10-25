package uni.swt.gesturedetectionwithpepper;


import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Trace;
import android.util.Log;

import com.aldebaran.qi.sdk.QiContext;

import org.tensorflow.Operation;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Vector;

import static org.opencv.features2d.FastFeatureDetector.THRESHOLD;

public class TensorFlowClassifier implements Classifier
        //extends RobotActivity implements RobotLifecycleCallbacks,Classifier
    {
        String TAG = "Tensorflow Classifier ";
        QiContext qiContext;
        private TensorFlowInferenceInterface tensorFlowInferenceInterface;
        private String inputName ;
        private String outputName;
        private int inputsize;
        private int imageMean;
        private float imageStd;

        private Vector<String> labels = new Vector<String>();
        private int[] intValues;
        private float[] floatValues;
        private float[] outputs;
        private String[] outputNames;
        private boolean logstats = false;
        private static final int MAX_RESULTS =3;

        private TensorFlowClassifier()
        {

        }
        public static Classifier create(AssetManager assetManager , String modelFile , String labelFileName , int inputSize , int imageMean , float imageStd , String inputName , String outputName) throws IOException {

            TensorFlowClassifier classifier = new TensorFlowClassifier();
            classifier.inputName = inputName;
            classifier.outputName = outputName;
            String actualFileName = labelFileName.split("file:///android_asset/")[1];
            System.out.print("File name : " +actualFileName);
            BufferedReader bufferReader = null;
            try{
                bufferReader = new BufferedReader(new InputStreamReader(assetManager.open(actualFileName)));
                String line;
                while((line = bufferReader.readLine()) !=null){
                    classifier.labels.add(line);

                }

            }catch(IOException e){
                throw new RuntimeException("Problem reading label file!",e);
            }finally {
                bufferReader.close();
            }
            classifier.tensorFlowInferenceInterface = new TensorFlowInferenceInterface(assetManager,modelFile);

            final Operation operation = classifier.tensorFlowInferenceInterface.graphOperation(outputName);
            final int numClasses = (int) operation.output(0).shape().size(1);
            Log.i("","Read" +classifier.labels.size()+"labels , output layer size is " + numClasses);

            classifier.inputsize = inputSize;
            classifier.imageMean = imageMean;
            classifier.imageStd = imageStd;

            classifier.outputNames = new String[]{outputName};
            classifier.intValues = new int[inputSize * inputSize];
            classifier.floatValues = new float[inputSize*inputSize*3];
            classifier.outputs = new float[numClasses];

            return classifier;
        }

        @Override

        public List<Classifier.Recognize> recognizeImage(Bitmap bitmap) {
            // Log this method so that it can be analyzed with systrace.
            Trace.beginSection("recognizeImage");

            Trace.beginSection("preprocessBitmap");
            // Preprocess the image data from 0-255 int to normalized float based
            // on the provided parameters.
            bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
            for (int i = 0; i < intValues.length; ++i) {
                final int val = intValues[i];
                floatValues[i * 3 + 0] = (((val >> 16) & 0xFF) - imageMean) / imageStd;
                floatValues[i * 3 + 1] = (((val >> 8) & 0xFF) - imageMean) / imageStd;
                floatValues[i * 3 + 2] = ((val & 0xFF) - imageMean) / imageStd;
            }
            Trace.endSection();

            // Copy the input data into TensorFlow.
            Trace.beginSection("feed");
            tensorFlowInferenceInterface.feed(inputName, floatValues, 1, inputsize, inputsize, 3);
            Trace.endSection();

            // Run the inference call.
            Trace.beginSection("run");
            tensorFlowInferenceInterface.run(outputNames, logstats);
            Trace.endSection();

            // Copy the output Tensor back into the output array.
            Trace.beginSection("fetch");
            tensorFlowInferenceInterface.fetch(outputName, outputs);
            Trace.endSection();

            // Find the best classifications.
            PriorityQueue<Recognize> pq =
                    new PriorityQueue<Recognize>(
                            3,
                            new Comparator<Recognize>() {
                                @Override
                                public int compare(Recognize lhs, Recognize rhs) {
                                    // Intentionally reversed to put high confidence at the head of the queue.
                                    return Float.compare(rhs.getGestureConfidence(), lhs.getGestureConfidence());
                                }
                            });
            for (int i = 0; i < outputs.length; ++i) {
                if (outputs[i] > THRESHOLD) {
                    pq.add(new Recognize("" + i, labels.size() > i ? labels.get(i) : "unknown", outputs[i]));
                }
            }
            final ArrayList<Recognize> recognitions = new ArrayList<Recognize>();
            int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
            for (int i = 0; i < recognitionsSize; ++i) {
                recognitions.add(pq.poll());
            }
            Trace.endSection(); // "recognizeImage"
            return recognitions;
        }

        @Override
        public void close() {
                tensorFlowInferenceInterface.close();
        }
    }
