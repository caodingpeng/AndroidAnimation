package com.xiaomi.router.ui;

import android.util.Log;
import android.view.Choreographer;
import android.view.View;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Flash animation player
 * <p/>
 * json format
 * {
 * "animation":
 * {
 * "frames":[
 * {
 * "obj1":["x","y","sx","sy","r"],
 * "obj1":["x","y","sx","sy","r"]
 * <p/>
 * },
 * {
 * "obj1":["x","y","sx","sy","r"],
 * "obj1":["x","y","sx","sy","r"]
 * }
 * ]
 * }
 * }
 * <p/>
 * 如何使用
 * try {
 * InputStream istream = getResources().openRawResource(R.raw.flashanimation);
 * byte[] buffer = new byte[istream.available()];
 * istream.read(buffer);
 * istream.close();
 * <p/>
 * JSONObject json = new JSONObject(new String(buffer));
 * FlashAnimationViewer viewer = new FlashAnimationViewer(this,json);
 * FlashAnimationViewer.FlashAnimation animation = viewer.getAnimation("testAnimation");
 * animation.bindViews();
 * animation.setRepeat(true);
 * animation.start();
 * }catch (IOException e){
 * <p/>
 * }catch (JSONException e){
 * <p/>
 * }
 * <p/>
 * <p/>
 * Created with IntelliJ IDEA.
 * User: caodingpeng
 * Date: 10/15/13
 * Time: 3:12 PM
 * To change this template use File | Settings | File Templates.
 */

public class FlashAnimationViewer {
    public static int X = 0;
    public static int Y = 1;
    public static int SCALE_X = 3;
    public static int SCALE_Y = 4;
    public static float ROTATION = 5;

    private HashMap<String, FlashAnimation> mAnimations;
    private HashMap<String, View> mTokenMap;

    private Class<?> mClassInfo;

    public FlashAnimationViewer(Object context, JSONObject json) {

        mClassInfo = context.getClass();
        mTokenMap = new HashMap<String, View>();
        mAnimations = new HashMap<String, FlashAnimation>();

        try {
            Field[] fields = mClassInfo.getFields();
            for (Field field : fields) {
                Object value = field.get(context);
                Log.d("cdp", field.getName());

                if (value instanceof View) {
                    mTokenMap.put(field.getName(), (View) value);
                }
            }
        } catch (IllegalAccessException e) {

        }

        parseJson(json);
    }

    public void replaceToken(String token, View view, Object... pairs) {
        mTokenMap.put(token, view);

        for (int i = 0; i < pairs.length; i += 2) {
            mTokenMap.put((String) pairs[i], (View) pairs[i + 1]);
        }
    }

    public HashMap<String, View> getTokenMap() {
        return mTokenMap;
    }

    public void playAnimation(String name) {
        FlashAnimation animation = mAnimations.get(name);
        if (animation != null) {
            animation.bindViews();
            animation.start();
        }
    }

    public View getViewByToken(String token) {
        return mTokenMap.get(token);
    }

    private void parseJson(JSONObject json) {
        try {
            Iterator it = json.keys();
            while (it.hasNext()) {
                String key = it.next().toString();

                FlashAnimation animation = FlashAnimation.parseAnimation(json.getJSONObject(key));
                animation.setAnimationViewer(this);
                mAnimations.put(key, animation);
            }
        } catch (JSONException e) {

        }
    }

    public FlashAnimation getAnimation(String animationName) {
        return mAnimations.get(animationName);
    }

    public static class FlashAnimation implements Choreographer.FrameCallback {

        private enum AnimationState {
            kNotStart,
            kStart,
            kPause,
            kStop,
            kCancel,
            kCount
        }

        /**
         * show animation repeat when finish
         */
        private Boolean mRepeat = false;

        protected ArrayList<Frame> mFrames;
        protected int mCurrentFrameIndex = -1;
        protected int mTotalFrames = 0;
        AnimationState mState = AnimationState.kNotStart;

        private FlashAnimatorListener mListener = null;
        private Boolean mAnimationScheduled = false;
        private final Choreographer mChoreographer;
        private HashMap<String, View> mTokenMap;
        private FlashAnimationViewer mAnimationViewer;

        public FlashAnimation() {
            mChoreographer = Choreographer.getInstance();

            mTokenMap = new HashMap<String, View>();
        }

        public void setAnimationViewer(FlashAnimationViewer animationViewer) {
            mAnimationViewer = animationViewer;
        }

        /**
         * @param map
         */
        public void extendTokenMap(HashMap<String, View> map) {
            mTokenMap.putAll(map);
        }

        public void replaceToken(String token, View view, Object... pairs) {
            mTokenMap.put(token, view);

            for (int i = 0; i < pairs.length; i += 2) {
                mTokenMap.put((String) pairs[i], (View) pairs[i + 1]);
            }
        }

        public void setRepeat(Boolean repeat) {
            mRepeat = repeat;
        }

        public View getViewByToken(String token) {
            return mTokenMap.get(token);
        }

        public void setFrames(ArrayList<Frame> frames) {
            mFrames = frames;
            mTotalFrames = frames.size();
        }

        public void addListener(FlashAnimatorListener listener) {
            mListener = listener;
        }

        public void bindViews() {
            if ((mAnimationViewer != null) && (mAnimationViewer.getTokenMap().size() > 0)) {
                HashMap<String, View> tmp = new HashMap<String, View>(mAnimationViewer.getTokenMap().size() + mTokenMap.size());

                tmp.putAll(mAnimationViewer.getTokenMap());
                tmp.putAll(mTokenMap);

                mTokenMap = tmp;
            }

            for (int i = 0; i < mTotalFrames; i++) {
                mFrames.get(i).bindViews();
            }
        }

        public void start() {

            scheduleAnimation();

            if (mListener != null) {
                mListener.onAnimationStart(this);
            }

            mState = AnimationState.kStart;
        }

        public void pause() {
            mState = AnimationState.kPause;

            unscheduleAnimation();
        }

        public void resume() {
            mState = AnimationState.kStart;

            scheduleAnimation();
        }

        public void cancel() {
            unscheduleAnimation();
            mCurrentFrameIndex = -1;
            mState = AnimationState.kCancel;
        }

        public void doFrame(long l) {

            mAnimationScheduled = false;

            if (hasNext()) {
                nextFrame().play();

                scheduleAnimation();
            } else {
                if (mRepeat) {
                    mCurrentFrameIndex = -1;
                    scheduleAnimation();

                    if (mListener != null) {
                        mListener.onAnimationRepeat(this);
                    }
                } else {
                    if (mListener != null) {
                        mListener.onAnimationEnd(this);

                        mState = AnimationState.kStop;
                    }
                }
            }
        }

        public Boolean hasNext() {
            return mCurrentFrameIndex + 1 < mTotalFrames;
        }

        public Frame nextFrame() {
            mCurrentFrameIndex++;
            return mFrames.get(mCurrentFrameIndex);
        }

        private void scheduleAnimation() {
            if (!mAnimationScheduled) {
                mChoreographer.postFrameCallback(this);
                mAnimationScheduled = true;
            }
        }

        private void unscheduleAnimation() {
            if (mAnimationScheduled) {
                mChoreographer.removeFrameCallback(this);
                mAnimationScheduled = false;
            }
        }

        public static FlashAnimation parseAnimation(JSONObject json) throws JSONException {

            FlashAnimation anim = new FlashAnimation();

            parse frames
            JSONArray frameData = json.getJSONArray("frames");
            ArrayList<Frame> frames = new ArrayList<Frame>(frameData.length());
            int length = frameData.length();
            for (int i = 0; i < length; i++) {
                frames.add(Frame.parseFrame(anim, frameData.getJSONObject(i)));
            }


            anim.setFrames(frames);
            return anim;
        }
    }

    public static class Frame {
        protected FlashAnimation mAnimation;

        protected ArrayList<Element> mElements;
        private int mElementsLength;

        public Frame(FlashAnimation animation) {
            mAnimation = animation;
        }

        public void setElements(ArrayList<Element> elements) {
            mElements = elements;
            mElementsLength = elements.size();
        }

        public void play() {
            for (Element element : mElements) {
                element.transform();
            }
        }

        public void bindViews() {
            for (int i = 0; i < mElementsLength; i++) {
                Element element = mElements.get(i);
                if (mAnimation.getViewByToken(element.token) == null) {
                    Boolean findError = true;
                }
                element.view = mAnimation.getViewByToken(element.token);
            }
        }

        public static Frame parseFrame(FlashAnimation animation, JSONObject json) throws JSONException {
            Frame frame = new Frame(animation);

            ArrayList<Element> elements = new ArrayList<Element>(json.length());
            Iterator it = json.keys();
            while (it.hasNext()) {
                String token = it.next().toString();
                elements.add(Element.parseElement(token, json.getJSONArray(token)));
            }
            frame.setElements(elements);
            return frame;
        }
    }

    public static class Element {
        public int x;
        public int y;
        public float scaleX;
        public float scaleY;
        public float rotation;

        public String token;
        todo add ignore flag
        int mIgnore = 0;

        public View view;

        public Element() {

        }

        public void transform() {
            view.setScaleX(scaleX);
            view.setScaleY(scaleY);
            view.setRotation(rotation);
            view.setX(x);
            view.setY(y);
        }

        /**
         * json format
         * ["x","y","sx","sy","r"],
         *
         * @param token
         * @param json
         * @return
         */
        public static Element parseElement(String token, JSONArray json) throws JSONException {
            Element element = new Element();
            element.token = token;
            element.x = json.getInt(0);
            element.y = json.getInt(1);
            element.scaleX = (float) json.getDouble(2);
            element.scaleY = (float) json.getDouble(3);
            element.rotation = (float) json.getDouble(4);

            return element;
        }
    }

    public static interface FlashAnimatorListener {
        void onAnimationStart(FlashAnimation animation);

        void onAnimationEnd(FlashAnimation animation);

        void onAnimationCancel(FlashAnimation animation);

        void onAnimationRepeat(FlashAnimation animation);
    }

}
