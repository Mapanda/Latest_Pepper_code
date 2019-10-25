package uni.swt.gesturedetectionwithpepper;

public class ConstantValues {
    private ConstantValues()
    {
        throw new UnsupportedOperationException("Utility Class");
    }

    public static final int INPUT_SIZE =  224;
    public static final int IMAGE_MEAN = 117;
    public static final float IMAGE_STD =  1;
    public static final String INPUT_NAME =  "input_2";
    public static final String OUTPUT_NAME = "dense_2/Softmax"; //dense_2_target , dense_2_sample_weights
    public static final String MODEL_FILE =  "file:///android_asset/tf_model.pb";
    public static final String LABEL_FILE="file:///android_asset/gestures.txt";

}

