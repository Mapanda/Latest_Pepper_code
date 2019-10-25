package uni.gesturedetectiononpepper;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.builder.AnimateBuilder;
import com.aldebaran.qi.sdk.builder.AnimationBuilder;
import com.aldebaran.qi.sdk.builder.GoToBuilder;
import com.aldebaran.qi.sdk.builder.ListenBuilder;
import com.aldebaran.qi.sdk.builder.PhraseSetBuilder;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.builder.TransformBuilder;
import com.aldebaran.qi.sdk.object.actuation.Actuation;
import com.aldebaran.qi.sdk.object.actuation.Animate;
import com.aldebaran.qi.sdk.object.actuation.Animation;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.actuation.FreeFrame;
import com.aldebaran.qi.sdk.object.actuation.GoTo;
import com.aldebaran.qi.sdk.object.actuation.Mapping;
import com.aldebaran.qi.sdk.object.conversation.Listen;
import com.aldebaran.qi.sdk.object.conversation.ListenResult;
import com.aldebaran.qi.sdk.object.conversation.PhraseSet;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.aldebaran.qi.sdk.object.geometry.Transform;
import com.aldebaran.qi.sdk.object.geometry.Vector3;
import com.aldebaran.qi.sdk.object.touch.Touch;
import com.aldebaran.qi.sdk.object.touch.TouchSensor;
import com.aldebaran.qi.sdk.util.PhraseSetUtil;

import java.util.List;



public class FunctionalityAfterGesture extends AppCompatActivity {
private static String TAG = "Functionality Handled After Gesture";
private static TouchSensor touchSensor;
private static Animate animate;
private static MediaPlayer playerStart;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playerStart = MediaPlayer.create(this, R.raw.ready_sound);
    }
    public static void goToActionOnLAndPalm(QiContext qiContext, Vector3 vec){
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Actuation actuation = qiContext.getActuation();
                    Frame robotFrame = actuation.robotFrame();
                    Transform transform = TransformBuilder.create().fromTranslation(vec);
                    Mapping mapping = qiContext.getMapping();
                    FreeFrame targetFrame = mapping.makeFreeFrame();
                    // Pass a 0-timestamp to publish target relatively to the
                    // last known location of robotFrame.
                    targetFrame.update(robotFrame, transform, 0L);
                    GoTo goTo = GoToBuilder.with(qiContext)
                            .withFrame(targetFrame.frame())
                            .build();
                    goTo.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
    public static void createAnimationOnPeace(QiContext qiContext){
        Animation animation = AnimationBuilder.with(qiContext) // Create the builder with the context.
                .withResources(R.raw.raise_right_hand_b002) // Set the animation resource.
                .build(); // Build the animation.

        // Create an animate action.
        animate = AnimateBuilder.with(qiContext) // Create the builder with the context.
                .withAnimation(animation) // Set the animation.
                .build(); // Build the animate action.
        animate.addOnLabelReachedListener((label, time) -> {
            Say sayLabel = SayBuilder.with(qiContext)
                    .withText(label)
                    .build();

            sayLabel.async().run();
        });
        // Add an on started listener to the animate action.
        animate.addOnStartedListener(() -> {
            String message = "Animation started.";
            Log.i(TAG, message);
            //displayLine(message);
            playerStart.start();
        });

        // Run the animate action asynchronously.
        Future<Void> animateFuture = animate.async().run();
        // Add a lambda to the action execution.
        animateFuture.thenConsume(future -> {
            if (future.isSuccess()) {
                String message = "Animation finished with success.";
                Log.i(TAG, message);
                //displayLine(message);
            } else if (future.hasError()) {
                String message = "Animation finished with error.";
                Log.e(TAG, message, future.getError());
                //displayLine(message);
            }
        });
    }
    public static void getTouchSensorsOnOkay(QiContext qiContext){
        Touch touch = qiContext.getTouch();
        List<String> sensorNames = touch.getSensorNames();
        Say sayOnLTouch = SayBuilder.with(qiContext).withText("Start touching my sensors.").build();
        sayOnLTouch.run();
        for(String touchSensorName: sensorNames ){
            touchSensor = touch.getSensor(touchSensorName);
            touchSensor.addOnStateChangedListener(touchState -> {
                Log.i(TAG, "Sensor " + (touchState.getTouched() ? "touched" : "released") + " at " + touchState.getTime());
                Say sayOnL = SayBuilder.with(qiContext).withText(touchSensorName).build();
                sayOnL.run();
            });
        }
    }

/*    private void displayLine(final String text) {
        runOnUiThread(() -> conversationView.setText(text));
    }*/
    public static void listenToFunctionality(QiContext qiContext){
        // Create the PhraseSet 1.
        Say say = SayBuilder.with(qiContext)
                .withText("I can listen to you: say \"Yes\" or \"No\" to try.")
                .build();

        say.run();
        PhraseSet phraseSetYes = PhraseSetBuilder.with(qiContext) // Create the builder using the QiContext.
                .withTexts("yes", "OK", "alright", "let's do this") // Add the phrases Pepper will listen to.
                .build(); // Build the PhraseSet.

        // Create the PhraseSet 2.
        PhraseSet phraseSetNo = PhraseSetBuilder.with(qiContext) // Create the builder using the QiContext.
                .withTexts("no", "Sorry", "I can't") // Add the phrases Pepper will listen to.
                .build(); // Build the PhraseSet.

        Listen listen = ListenBuilder.with(qiContext) // Create the builder with the QiContext.
                .withPhraseSets(phraseSetYes, phraseSetNo) // Set the PhraseSets to listen to.
                .build(); // Build the listen action.
        ListenResult listenResult = listen.run();
        PhraseSet matchedPhraseSet = listenResult.getMatchedPhraseSet();
        if (PhraseSetUtil.equals(matchedPhraseSet, phraseSetYes)) {
            Say sayPhrase = SayBuilder.with(qiContext)
                    .withText(phraseSetYes.toString())
                    .build();

            sayPhrase.run();
        } else if (PhraseSetUtil.equals(matchedPhraseSet, phraseSetNo)) {
            Say sayPhrase = SayBuilder.with(qiContext)
                    .withText(phraseSetNo.toString())
                    .build();

            sayPhrase.run();
        }
    }
}
