package com.audio.demo.audiointeraction;

import android.util.Log;

import com.audio.demo.audiointeraction.bean.Song;
import com.audio.demo.audiointeraction.bean.VideoViewObj;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void demo() {
        VideoViewObj videoViewObj = new VideoViewObj(10);

        Log.e("Test", videoViewObj.toString());
    }
}