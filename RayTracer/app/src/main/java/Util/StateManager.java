package Util;

import android.content.Context;

import java.util.ArrayList;
import Scenes.Scene;
import Scenes.SceneA;
import Scenes.SceneC;
import Scenes.SceneB;
import Scenes.SceneD;

/**
 * Created by Andreas on 11.05.2020.
 */

public class StateManager {

    // Level
    private static ArrayList<Scene> sceneList;
    private static int activeSceneIndex;

    // Timer
    private static ArrayList<Timer> timerList;

    // Activity
    private static Context context;
    private static int width, height;

    public static boolean isLoaded = false;

    public static void load(int firstSceneIndex, Context newContext) {
        timerList = new ArrayList<>();
        sceneList = new ArrayList<>();
        activeSceneIndex = firstSceneIndex;
        context = newContext;
        isLoaded = true;

        sceneList.add(new SceneA());    // 0
        sceneList.add(new SceneB());    // 1
        sceneList.add(new SceneC());    // 2
        sceneList.add(new SceneD());    // 3
    }

    public static void loadDimensions(int newWidth, int newHeight) {
        width = newWidth;
        height = newHeight;
    }

    public static void setActiveSceneIndex(int newActiveLevel) {
        timerList.clear();
        sceneList.get(newActiveLevel).onSurfaceCreated(context);
        sceneList.get(newActiveLevel).onSurfaceChanged(width, height);
        activeSceneIndex = newActiveLevel;
    }

    public static void resetActiveScene(int oldActiveLevel) {
        sceneList.get(oldActiveLevel).onReload();
        activeSceneIndex = oldActiveLevel;
    }

    public static void registerTimer (Timer timer) {
        timerList.add(timer);
    }

    public static void updateAllTimers () {
        for(Timer timer : timerList) {
            timer.update();
        }
    }

    public static Scene getActiveScene() {
        return sceneList.get(activeSceneIndex);
    }

    public static int getActiveSceneIndex() {
        return activeSceneIndex;
    }

    public static Context getContext() {
        return context;
    }

    public static int getWidth() {
        return width;
    }

    public static int getHeight() {
        return height;
    }
}
