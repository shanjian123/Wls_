
package com.test;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Random;

public class TetrisView extends TileView {

    private static final String TAG = "TetrisView";

    /**
     * 游戏的四种状态
     */
    private int mMode = READY;

    public static final int PAUSE = 0;

    public static final int READY = 1;

    public static final int RUNNING = 2;

    public static final int LOSE = 3;

    /**
     * 三种背景图片，由这些图片拼装成游戏的基本界面
     */
    private static final int RED_STAR = 1;

    private static final int YELLOW_STAR = 2;

    private static final int GREEN_STAR = 3;

    /**
     * 产生随机数，根据随机数决定方块的形状
     */
    private static final Random RNG = new Random();

    /**
     * 当前游戏的分数
     */
    private long mScore = 0;

    /**
     * 历史最高分
     */
    public static long historyScore = 0;

    /**
     * 是否超出了最高分，如果超出了，则在退出时会保存数据
     */
    public static boolean overhistroy=false;
    /**
     * 方块移动的速度，数值越小则方块的速度越高，难度也就越大
     */
    private long mMoveDelay;

    /**
     * 当前方块移动的速度，和mMoveDelay配合使用，在游戏中更改游戏的速度都是更改此变量值
     * 在每个方块产生的时候，将此变量值赋给mMoveDelay。之所以采用两个变量是因为当按下加速 方块落下之后，下一个方块的速度还要保持原来的速度。
     */
    private long currentDelay;

    /**
     * 预先显示的方块，在前一个方块落下后，使其变为当前方块
     */
    private ArrayList<Coordinate> preShape = new ArrayList<Coordinate>();

    /**
     * 当前正在下落的方块
     */
    private ArrayList<Coordinate> mShape = new ArrayList<Coordinate>();

    private ArrayList<Coordinate> oldShape = new ArrayList<Coordinate>();

    /**
     * 显示历史最高分的文本框
     */
    private TextView highestScore;

    /**
     * 显示当前分数的文本框
     */
    private TextView currentScore;

    /**
     * 显示当前游戏级别的文本框
     */
    private TextView currentLevel;

    /**
     * 记录游戏的级别
     */
    private int gameLevel = 1;

    /**
     * 在屏幕中央显示提示信息的文本框
     */
    private TextView info;

    /**
     * 记录目前落下的方块的最高层数
     */
    private int highLevel = 0;

    /**
     * 当前方块类型，主要用来标示田形方块，从而在旋转方块的时候可以让田形方块不旋转
     */
    private int shapeType;

    /**
     * 预先显示方块的类型
     */
    private int preType;

    /**
     * 构造方法
     */
    public TetrisView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initGame();
    }

    public TetrisView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initGame();
    }

    /**
     * 通过一个handler来更新界面的显示，以及方块下落的速度
     */
    private RefreshHandler mRedrawHandler = new RefreshHandler();

    class RefreshHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            TetrisView.this.update();
            TetrisView.this.invalidate();

        }

        public void sleep(long delayMillis) {
            this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }
    };

    /**
     * 初始化游戏
     */
    private void initGame() {
        setFocusable(true);
        Resources r = this.getContext().getResources();
        resetTiles(4);
        loadTile(RED_STAR, r.getDrawable(R.drawable.redstar));
        loadTile(YELLOW_STAR, r.getDrawable(R.drawable.yellowstar));
        loadTile(GREEN_STAR, r.getDrawable(R.drawable.greenstar));

    }

    /**
     * 开始一个新游戏，重置各种数据
     */
    private void startGame() {
        clearTiles();
        gameLevel = 1;
        highLevel = 0;
        currentDelay = 600;
        mScore = 0;
        currentScore.setText("当前分数：\n" + mScore);
        currentLevel.setText("当前级别：\n" + gameLevel);
        highestScore.setText("最高分数：\n"+historyScore);
        setMode(RUNNING);
    }


    /**
     * 将arraylist转换为数组，从而可以将该部分数据保存起来
     */
    private int[] coordArrayListToArray(ArrayList<Coordinate> cvec) {
        int count = cvec.size();
        int[] rawArray = new int[count * 2];
        for (int index = 0; index < count; index++) {
            Coordinate c = cvec.get(index);
            rawArray[2 * index] = c.x;
            rawArray[2 * index + 1] = c.y;
        }
        return rawArray;
    }

    /**
     * 将已经落下来的方块的坐标保存在一个ArrayList中
     */
    private ArrayList<Coordinate> tailsToList(int[][] tileGrid) {
        ArrayList<Coordinate> tranList = new ArrayList<Coordinate>();
        for (int i = 0; i < mXTileCount - 6; i++) {
            for (int j = 1; j < mYTileCount - 1; j++) {
                if (tileGrid[i][j] == RED_STAR) {
                    Coordinate cor = new Coordinate(i, j);
                    tranList.add(cor);
                }
            }
        }
        return tranList;
    }

    /**
     * 保存游戏的状态，从而在切换游戏后还可以继续游戏
     *
     * @return a Bundle with this view's state
     */
    public Bundle saveState() {
        Bundle map = new Bundle();
        map.putIntArray("preShapeList", coordArrayListToArray(preShape));
        map.putIntArray("mShapeList", coordArrayListToArray(mShape));
        map.putLong("mMoveDelay", Long.valueOf(mMoveDelay));
        map.putLong("mScore", Long.valueOf(mScore));
        map.putLong("hisScore", Long.valueOf(historyScore));
        map.putInt("mLevel", highLevel);
        map.putIntArray("tailList", coordArrayListToArray(tailsToList(mTileGrid)));

        return map;
    }

    /**
     * 将数组转换为Arraylist，从而将保存的数据转化为游戏的状态
     *
     * @param rawArray : [x1,y1,x2,y2,...]
     * @return a ArrayList of Coordinates
     */
    private ArrayList<Coordinate> coordArrayToArrayList(int[] rawArray) {
        ArrayList<Coordinate> coordArrayList = new ArrayList<Coordinate>();
        int coordCount = rawArray.length;
        for (int index = 0; index < coordCount; index += 2) {
            Coordinate c = new Coordinate(rawArray[index], rawArray[index + 1]);
            coordArrayList.add(c);
        }
        return coordArrayList;
    }

    /**
     * 从保存的数据中读取已经落下的方块坐标，并重新在界面上画出这些方块
     */
    private void listToTail(ArrayList<Coordinate> cor) {
        int count = cor.size();

        for (int index = 0; index < count; index++) {
            Coordinate c = cor.get(index);
            mTileGrid[c.x][c.y] = RED_STAR;
        }
    }

    /**
     * 切换到游戏的时候，读取所保存的数据，重现游戏先前的状态
     *
     * @param icicle a Bundle containing the game state
     */
    public void restoreState(Bundle icicle) {
        setMode(PAUSE);
        historyScore = icicle.getLong("hisScore");
        highLevel = icicle.getInt("mLevel");
        mMoveDelay = icicle.getLong("mMoveDelay");
        mScore = icicle.getLong("mScore");
        preShape = coordArrayToArrayList(icicle.getIntArray("preShapeList"));
        mShape = coordArrayToArrayList(icicle.getIntArray("mShapeList"));
        listToTail(coordArrayToArrayList(icicle.getIntArray("tailList")));
    }

    /**
     * 对按键的响应
     */
//    public boolean onTouchEvent(MotionEvent event){
//        if(event.equals(MotionEvent.ACTION_DOWN)){
//            if (mMode == READY | mMode == LOSE) {
//
//                startGame();
//                setMode(RUNNING);
//                return (true);
//            } else if (mMode == RUNNING) {
//                transShape();
//                update();
//                return (true);
//            }
//            if (mMode == PAUSE) {
//                setMode(RUNNING);
//                update();
//                return (true);
//            }
//        }
//        if (event.equals(MotionEvent.ACTION_POINTER_1_UP)) {
//            moveDown();
//
//            return (true);
//        }
//        // 按下了左键
//        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
//            moveLeft();
//            updateShape();
//            return (true);
//        }
//        // 按下了右键
//        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
//            moveRight();
//            updateShape();
//            return (true);
//        }
//
//    }
    public boolean onKeyDown(int keyCode, KeyEvent msg) {
        // 按下了上键
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (mMode == READY | mMode == LOSE) {

                startGame();
                setMode(RUNNING);
                return (true);
            } else if (mMode == RUNNING) {
                transShape();
                update();
                return (true);
            }
            if (mMode == PAUSE) {
                setMode(RUNNING);
                update();
                return (true);
            }
        }
        // 按下了下键
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            moveDown();

            return (true);
        }
        // 按下了左键
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            moveLeft();
            updateShape();
            return (true);
        }
        // 按下了右键
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            moveRight();
            updateShape();
            return (true);
        }
        return super.onKeyDown(keyCode, msg);
    }

    /**
     * 加速方块的下落
     */
    private void moveDown() {
        mMoveDelay = 25;
    }

    /**
     * 将方块向右移动，在移动前要判断一下是否可以移动
     */
    private void moveRight() {
        newToOld(mShape, oldShape);
        for (Coordinate c : mShape) {
            c.x = c.x + 1;
        }
        // 如果不可以移动，则保持位置不变
        if (!isMoveAble(mShape)) {
            for (Coordinate c : mShape) {
                c.x = c.x - 1;
            }
        }
    }

    /**
     * 将方块向左移动
     */
    private void moveLeft() {
        newToOld(mShape, oldShape);
        for (Coordinate c : mShape) {
            c.x = c.x - 1;
        }
        if (!isMoveAble(mShape)) {
            for (Coordinate c : mShape) {
                c.x = c.x + 1;
            }
        }
    }

    /**
     * 旋转方块
     */
    private void transShape() {
        Coordinate core = mShape.get(0);
        if (shapeType == 3) {
            // 田形，不用做任何事情
        } else {
            newToOld(mShape, oldShape);
            for (Coordinate c : mShape) {
                int x = core.x + (core.y - c.y);
                int y = core.y + (c.x - core.x);
                c.x = x;
                c.y = y;
            }
            // 如果可以旋转，则旋转90度并更新显示
            if (isMoveAble(mShape)) {
                updateShape();
                TetrisView.this.invalidate();
            } else {
                for (Coordinate c : mShape) {
                    int x = core.x + (c.y - core.y);
                    int y = core.y + (core.x - c.x);
                    c.x = x;
                    c.y = y;
                }
            }

        }

    }

    /**
     * 将一个方块的形状赋给另外一个
     *
     * @param mShape2
     * @param oldShape2
     */
    private void newToOld(ArrayList<Coordinate> mShape2, ArrayList<Coordinate> oldShape2) {
        oldShape2.clear();
        for (int i = 0; i < 4; i++) {
            Coordinate c1 = mShape2.get(i);
            Coordinate c2 = new Coordinate(c1.x, c1.y);
            oldShape2.add(c2);
        }

    }

    /**
     * 处理游戏的更新
     */
    public void update() {
        if (mMode == RUNNING) {
            // clearTiles();
            updateBlackGround();
            if (mXTileCount != 0) {
                // 如果是刚开始游戏，则初始化方块形状
                if (mShape.size() == 0) {
                    shapeType = RNG.nextInt(7);
                    mShape = getShape(shapeType);
                    preType = RNG.nextInt(7);
                    preShape = getShape(preType);
                    mMoveDelay = currentDelay;
                    // 设置方块的初始位置
                    for (Coordinate c : mShape) {
                        c.x = c.x + mXTileCount / 3;
                        c.y = c.y + 1;
                    }
                }
                // 将方块往下移动一格
                newToOld(mShape, oldShape);
                for (Coordinate c : mShape) {
                    c.y++;
                }
                // 如果方块可以往下移动，则更新图形
                if (canMoveDown(mShape)) {
                    updateShape();
                } else {
                    // 如果不可以往下移动则将方块的状态改为已落下并开始落下新方块
                    updateBlew();
                    mShape = preShape;
                    shapeType = preType;
                    for (Coordinate c : mShape) {
                        c.x = c.x + mXTileCount / 3;
                        c.y = c.y + 1;
                    }
                    preType = RNG.nextInt(7);
                    preShape = getShape(preType);
                    mMoveDelay = currentDelay;
                }
                updatePreShape();
            }
        }
        // 设置方块落下的速度
        mRedrawHandler.sleep(mMoveDelay);

    }

    // 方块落下过程中颜色为黄色
    private void updateShape() {
        for (Coordinate c : oldShape) {
            setTile(0, c.x, c.y);
            Log.d(TAG, "old" + c.x + c.y);
        }
        for (Coordinate c : mShape) {
            Log.d(TAG, "new" + c.x + c.y);
            if (mXTileCount != 0) {
                setTile(YELLOW_STAR, c.x, c.y);
            }

        }

    }

    // 方块落下后颜色为红色
    private void updateBlew() {
        for (Coordinate c : mShape) {
            if (mXTileCount != 0) {
                setTile(RED_STAR, c.x, c.y - 1);
                if (mYTileCount - c.y > highLevel) {
                    highLevel = mYTileCount - c.y;

                }
            }

        }
        // GAME OVER
        if (highLevel > mYTileCount - 3) {
            setMode(LOSE);
        }
        // 已经消去的行数
        int deleRows = 0;
        for (int i = 1; i <= highLevel + 1; i++) {
            int redCount = 0;
            for (int x = 1; x < mXTileCount - 7; x++) {
                if (mTileGrid[x][mYTileCount - i] == RED_STAR) {
                    redCount++;
                }
            }
            // 如果某一行的红色方格数等于列总数，则该列需要消去
            if (redCount == mXTileCount -8 ) {
                deleRows++;
                continue;
            } else {
                // 将不需要消去的行向下移动，移动的幅度取决于前面消去的行数
                if (deleRows != 0) {
                    for (int x = 1; x < mXTileCount - 6; x++) {
                        mTileGrid[x][mYTileCount - i + deleRows] = mTileGrid[x][mYTileCount - i];
                        mTileGrid[x][mYTileCount - i] = 0;
                    }
                }
            }
        }
        // 更改分数，一次性消去的行数越多，得到的分数就越多
        switch (deleRows) {
            case 1:
                mScore = mScore + 100;
                break;
            case 2:
                mScore = mScore + 300;
                break;
            case 3:
                mScore = mScore + 500;
                break;
            case 4:
                mScore = mScore + 800;
                break;
        }
        // 更新最高分
        if (mScore > historyScore) {
            overhistroy=true;
            historyScore = mScore;
            highestScore.setText("最高分数: \n" + historyScore);
        }
        // 更新当前分
        currentScore.setText("当前分数：\n" + mScore);
        // 当级别达到一定的程度后不再增加方块下落的速度
        if (mScore >= (500 * (gameLevel * 2 - 1))) {
            gameLevel++;
            currentDelay -= 50;
            if (currentDelay < 50)
                currentDelay = 50;
        }
        // 更新当前级别
        currentLevel.setText("当前级别：\n" + gameLevel);
    }

    /**
     * 设置游戏的状态
     *
     * @param newMode
     */
    public void setMode(int newMode) {
        int oldMode = mMode;
        mMode = newMode;
        if (newMode == RUNNING & oldMode != RUNNING) {
            info.setVisibility(View.INVISIBLE);
            update();
            return;
        }

        Resources res = getContext().getResources();
        CharSequence str = "";
        if (newMode == PAUSE) {
            str = res.getText(R.string.mode_pause);
        }
        if (newMode == READY) {
            str = res.getText(R.string.mode_ready);
        }
        if (newMode == LOSE) {
            str = res.getString(R.string.mode_lose_prefix) + mScore
                    + res.getString(R.string.mode_lose_suffix);
        }
        // 显示提示信息
        info.setText(str);
        info.setVisibility(View.VISIBLE);
    }

    /**
     * 更新预先显示方块
     */
    private void updatePreShape() {
        for (int x = mXTileCount - 4; x < mXTileCount; x++) {
            for (int y = 1; y < 6; y++) {
                setTile(0, x, y);

            }
        }
        for (Coordinate c : preShape) {
            if (mXTileCount != 0) {
                setTile(YELLOW_STAR, c.x + mXTileCount - 3, c.y + 2);
            }
        }
    }

    /**
     * 判断方块是否可以移动
     *
     * @param list
     * @return
     */
    private boolean isMoveAble(ArrayList<Coordinate> list) {
        boolean moveAble = true;
        for (Coordinate c : list) {

            if (mTileGrid[c.x][c.y] != GREEN_STAR && mTileGrid[c.x][c.y] != RED_STAR) {
                continue;
            } else {
                moveAble = false;
                break;
            }
        }
        return moveAble;

    }

    /**
     * 判断方块是否可以往下移动
     *
     * @param list
     * @return
     */
    private boolean canMoveDown(ArrayList<Coordinate> list) {
        boolean moveAble = true;
        for (Coordinate c : list) {
            if (c.y < mYTileCount - 1 && mTileGrid[c.x][c.y] != RED_STAR) {
                continue;
            } else {
                moveAble = false;
                break;
            }
        }
        return moveAble;

    }

    /**
     * 画出游戏的背景.即游戏的边框
     *
     */
    private void updateBlackGround() {
        for (int x = 0; x < mXTileCount - 5; x++) {
            setTile(GREEN_STAR, x, 0);
            setTile(GREEN_STAR, x, mYTileCount - 1);
        }
        for (int y = 1; y < mYTileCount - 1; y++) {
            setTile(GREEN_STAR, 0, y);
            setTile(GREEN_STAR, mXTileCount - 6, y);
        }
    }

    /**
     * 根据随机数产生各种形状的方块
     *
     * @param n
     * @return
     */
    private ArrayList<Coordinate> getShape(int n) {
        ArrayList<Coordinate> shape = new ArrayList<Coordinate>();
        switch (n) {
            case 1:
                // 反Z拐角
                shape.add(new Coordinate(0, 0));
                shape.add(new Coordinate(1, 0));
                shape.add(new Coordinate(0, 1));
                shape.add(new Coordinate(-1, 1));
                break;
            case 2:
                // 正Z拐角
                shape.add(new Coordinate(0, 0));
                shape.add(new Coordinate(-1, 0));
                shape.add(new Coordinate(0, 1));
                shape.add(new Coordinate(1, 1));
                break;
            case 3:
                // 田形
                shape.add(new Coordinate(0, 0));
                shape.add(new Coordinate(0, 1));
                shape.add(new Coordinate(1, 0));
                shape.add(new Coordinate(1, 1));
                break;
            case 4:
                // 长条
                shape.add(new Coordinate(0, 0));
                shape.add(new Coordinate(-1, 0));
                shape.add(new Coordinate(1, 0));
                shape.add(new Coordinate(2, 0));
                break;
            case 5:
                // 长左拐形
                shape.add(new Coordinate(0, 0));
                shape.add(new Coordinate(-1, 0));
                shape.add(new Coordinate(0, 1));
                shape.add(new Coordinate(0, 2));
                break;
            case 6:
                // 长右拐形
                shape.add(new Coordinate(0, 0));
                shape.add(new Coordinate(1, 0));
                shape.add(new Coordinate(0, 1));
                shape.add(new Coordinate(0, 2));
                break;
            case 0:
                // 凸形
                shape.add(new Coordinate(0, 0));
                shape.add(new Coordinate(-1, 0));
                shape.add(new Coordinate(1, 0));
                shape.add(new Coordinate(0, 1));
                break;
        }
        return shape;
    }

    public void setTextView(TextView curView, TextView higView, TextView infView, TextView levView) {
        currentScore = curView;
        highestScore = higView;
        info = infView;
        currentLevel = levView;
    }

    /**
     * 用来标示某一坐标上的方块
     *
     */
    private class Coordinate {
        public int x;

        public int y;

        public Coordinate(int newX, int newY) {
            x = newX;
            y = newY;
        }

        public boolean equals(Coordinate other) {
            if (x == other.x && y == other.y) {
                return true;
            }
            return false;
        }

        public String toString() {
            return "Coordinate: [" + x + "," + y + "]";
        }
    }
}
