
package com.test;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Tetris extends Activity {
    private String TAG = "Tetris";

    private TetrisView tetrisView;

    private TextView highestScore;

    private TextView info;

    private TextView currentScore;

    private TextView currentLevel;

    String NAME = "score.txt";

    /**
     * 保存数据时用到的key
     */
    private static String ICICLE_KEY = "tetris-view";

    /**
     * 游戏的主要activity
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tetris_layout);
        tetrisView = (TetrisView)findViewById(R.id.tetris);
        highestScore = (TextView)findViewById(R.id.highest_score);
        currentScore = (TextView)findViewById(R.id.current_score);
        info = (TextView)findViewById(R.id.info);
        currentLevel = (TextView)findViewById(R.id.current_level);
        // 为view实例化这些文本框
        tetrisView.setTextView(currentScore, highestScore, info, currentLevel);
        if (savedInstanceState == null) {
            // 开始一个新游戏，将游戏的状态设为READY
            tetrisView.setMode(TetrisView.READY);
        } else {
            // 如果存储了一个游戏的状态，则将状态读出来，可以继续游戏
            Bundle map = savedInstanceState.getBundle(ICICLE_KEY);
            if (map != null) {
                tetrisView.restoreState(map);
            } else {
                tetrisView.setMode(TetrisView.PAUSE);
            }
        }
        // 读取历史最高分
        FileInputStream in;
        byte[] by = new byte[10];
        try {
            in = openFileInput(NAME);
            in.read(by);
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < 10; i++) {
                if (by[i] != 0) {
                    buffer.append(by[i] - 48);
                }
            }
            Log.d("dd", buffer.toString());
            tetrisView.historyScore = Long.valueOf(buffer.toString());
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    protected void onPause() {
        //如果分数超出了历史最高分，则保存数据
        if (tetrisView.overhistroy) {
            FileOutputStream fos;
            try {
                fos = openFileOutput(NAME, Context.MODE_PRIVATE);
                fos.write(String.valueOf(TetrisView.historyScore).getBytes());
                fos.close();

            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        super.onPause();
        // 暂停游戏
        tetrisView.setMode(TetrisView.PAUSE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // 保存游戏状态
        outState.putBundle(ICICLE_KEY, tetrisView.saveState());
    }

}
