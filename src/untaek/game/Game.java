package untaek.game;

import untaek.BasePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.*;
import java.util.Timer;

public class Game extends JPanel{
    static int rows = 25;   // 20 + 1 + 4
    static int columns = 12; // 10 + 2
    static int rows_savefield = 21;
    static int columns_savefield = 12;

    static int SIZE = 22;
    static int MAX_HEIGHT = SIZE * (rows + 2);
    static int MAX_WIDTH = SIZE * (columns + 1);
    static int HEIGHT = SIZE * (rows - 5) ;
    static int WIDTH = SIZE * (columns - 2);
    static int MARGIN_X = 13 * SIZE;
    static int MARGIN_Y = 5 * SIZE;

    public int score;

    static int OTHER_MARGIN_X = 25;                 // othter player field's margin X
    static int OTHER_MARGIN_Y = 25;                 // othter player field's margin Y

    Box [][] save_field = new Box[rows-4][columns];   // field's back up field
    Box [][] field = new Box[rows][columns];        // main field
    Box [][] field_ = new Box[rows][columns];       // contain block

    static public Box[][] send_field = new Box[20][10];

    public Block block;                             // now block
    public Block block_pre_1;                       // next block
    public Block block_pre_2;                       // next next block
    public Block block_pre_3;                       // next next next block
    public Block block_pre_4;                       // next // next next next block

    public boolean fall_complete;                   // block fall complete flag
    public boolean fall_block_result;               // fall_block() method result value
    public boolean timer_flag = true;               // normal or down(keyboard) fall block flag
    public boolean gameover_flag;           // game over flag
    public boolean attack_flag = false;

    public int combo=0;
    public int delay = 500;                         // normal block fall delay (0.5s)
    public int attack_point;                        // my attack point

    public  Timer timer;
    public TimerTask task;

    Queue queue;
    Thread thread;
    Color gray = new Color(210,209,208);

    public static KeyAdapter keyadapter;

    public Game() {
        // 초기화

        queue = new LinkedList();
        queue.add("");
        queue.remove();
        queue.poll();
        score = 0;
        gameover_flag = false;
        reset_save_field();
        reset_field();
        reset_field_();

        keyadapter = new MyKeyAdapter();
        this.addKeyListener(keyadapter);
        this.setFocusable(true);

        block = new Block();
        block_pre_1 = new Block();
        block_pre_2 = new Block();
        block_pre_3 = new Block();
        block_pre_4 = new Block();

        fill_field_();
        fill_field();

        task = new TimerTask(){
            @Override
            public void run(){
                // object 떨어짐
                fall_block();
            }
        };
        timer = new Timer();
        timer.schedule(task,0,delay);

        thread = new Thread(runnable);
        thread.start();
    }
    public void game() {
        // 초기화
        score = 0;
        gameover_flag = false;
        reset_save_field();
        reset_field();
        reset_field_();

        keyadapter = new MyKeyAdapter();
        this.addKeyListener(keyadapter);
        this.setFocusable(true);

        block = new Block();
        block_pre_1 = new Block();
        block_pre_2 = new Block();
        block_pre_3 = new Block();
        block_pre_4 = new Block();

        // field_ 에 block 넣기
        fill_field_();

        // field  = field + field_
        fill_field();

        task = new TimerTask(){
            @Override
            public void run(){
                // object 떨어짐
                fall_block();
            }
        };
        timer = new Timer();
        timer.schedule(task,0,delay);    }
    public void paintComponent(Graphics g){

        super.paintComponent(g);
        //draw_edge(g);
        if(gameover_flag){
            gameover_effect();
        }
            // my game
            draw_Preview(g);
            draw_Field(g);
            draw_Field_Border(g);

        if(attack_flag){
            //send attack_point to server
        }

        BasePanel.score.setText(String.valueOf(score));
        // send field to server

        repaint();
        invalidate();
    }

    public void send_Field_setting(){
        for(int x = 0; x< 10; x++){
            for (int y = 0; y<20; y++){
                send_field[y][x].num = field[y+4][x+1].num;
                send_field[y][x].color = field[y+4][x+1].color;
            }
        }
    }

    // draw my Field's Border
    public void draw_Field_Border(Graphics g){
        g.setColor(Color.BLACK);

        // top border
        g.fillRect(SIZE/2, 0, (columns-1) *SIZE,SIZE/2);
        g.drawRect(SIZE/2, 0, (columns-1) *SIZE,SIZE/2);

        // bottom border
        g.fillRect(SIZE/2, SIZE * (rows-4) -SIZE/2, (columns-1) *SIZE,SIZE/2);
        g.drawRect(SIZE/2, SIZE * (rows-4) -SIZE/2, (columns-1) *SIZE,SIZE/2);

        // left border
        g.fillRect(SIZE/2, 0, SIZE/2, (rows-4)*SIZE);
        g.drawRect(SIZE/2, 0, SIZE/2, (rows-4)*SIZE);

        // right border
        g.fillRect((columns-1) *SIZE, 0, SIZE/2, (rows-4)*SIZE);
        g.drawRect((columns-1) *SIZE, 0, SIZE/2, (rows-4)*SIZE);
    }

    // draw my Field
    public void draw_Field(Graphics g){
        // 눈금
        for(int x = 1; x < columns-1; x++){     // 0~ 11
            for (int y = 0; y < rows-1; y++) {   // 4~ 24
                g.setColor(gray);
                g.drawRect(SIZE * x, SIZE/2 + SIZE * (y-4), SIZE,SIZE);
            }
        }

        for(int x = 1; x < columns-1; x++){     // 0~ 11
            for (int y = 4; y < rows-1; y++){   // 4~ 24
                if(field[y][x].num == 1){
                    switch (field[y][x].color) {
                        case -2:    // Gray
                            g.setColor(Color.gray);
                            break;
                        case -1:    // Black
                            g.setColor(Color.BLACK);
                            break;
                        case 0:     //  White
                            g.setColor(Color.WHITE);
                            break;
                        case 1:     //  Red (255,0,0)
                            g.setColor(Color.RED);
                            break;
                        case 2:     //  Yellow (255, 212, 0)
                            g.setColor(Color.YELLOW);
                            break;
                        case 3:     //  Blue    (0, 153, 255)
                            g.setColor(Color.BLUE);
                            break;
                        case 4:     // Green    (0, 153, 0)
                            g.setColor(Color.GREEN);
                            break;
                        case 5:     //  Pink    (255, 144, 190)
                            g.setColor(Color.PINK);
                            break;
                        case 6:     //  Purple  (128, 0, 255)
                            g.setColor(new Color(128, 0, 255));
                            break;
                        case 7:     //  Orange  (255, 127, 0)
                            g.setColor(Color.ORANGE);
                            break;
                    }
                    g.fillRect(SIZE * x, SIZE/2 + SIZE * (y-4), SIZE,SIZE);
                    g.setColor(Color.BLACK);
                    g.drawRect(SIZE * x, SIZE/2 + SIZE * (y-4), SIZE,SIZE);
                }

            }
        }
    }

    // draw my Preview
    public void draw_Preview(Graphics g) {
        draw_Block(g,1);    // pre1
        draw_Block(g,2);    // pre2
        draw_Block(g,3);    // pre3
        draw_Block(g,4);    // pre4
    }

    // darw my Block
    public void draw_Block(Graphics g, int block_num){

        Block block_pre = null;
        int margin_y = 0;
        switch (block_num){
            case 1:
                block_pre = block_pre_1;
                margin_y = 0;
                break;
            case 2:
                block_pre= block_pre_2;
                margin_y = 5;
                break;
            case 3:
                block_pre = block_pre_3;
                margin_y = 10;
                break;
            case 4:
                block_pre = block_pre_4;
                margin_y = 15;
                break;
        }

        for(int x = 0; x < block_pre.area_length; x++){     // 0~ 11
            for (int y = 0; y < block_pre.area_length; y++){   // 4~ 24
                if(block_pre.area[y][x].num == 1) {
                    switch (block_pre.area[y][x].color) {
                        case 1:     //  Red (255,0,0)
                            g.setColor(Color.RED);
                            break;
                        case 2:     //  Yellow (255, 212, 0)
                            g.setColor(Color.YELLOW);
                            break;
                        case 3:     //  Blue    (0, 153, 255)
                            g.setColor(Color.BLUE);
                            break;
                        case 4:     // Green    (0, 153, 0)
                            g.setColor(Color.GREEN);
                            break;
                        case 5:     //  Pink    (255, 144, 190)
                            g.setColor(Color.PINK);
                            break;
                        case 6:     //  Purple  (128, 0, 255)
                            g.setColor(new Color(128, 0, 255));
                            break;
                        case 7:     //  Orange  (255, 127, 0)
                            g.setColor(Color.ORANGE);
                            break;
                    }

                    g.fillRect(SIZE / 2 + SIZE * x + MARGIN_X, SIZE / 2 + SIZE * (y + margin_y), SIZE, SIZE);
                    g.setColor(Color.BLACK);
                    g.drawRect(SIZE / 2 + SIZE * x + MARGIN_X, SIZE / 2 + SIZE * (y + margin_y), SIZE, SIZE);
                }

            }
        }
    }

    // draw other Field's Border
    public void draw_others_Field_Border(Graphics g){
        g.setColor(Color.BLACK);

        // top border
        g.fillRect(SIZE/4 +OTHER_MARGIN_X, SIZE/4+OTHER_MARGIN_Y, (columns-1) *(SIZE/2),SIZE/4);
        g.drawRect(SIZE/4+OTHER_MARGIN_X, SIZE/4+OTHER_MARGIN_Y, (columns-1) *(SIZE/2),SIZE/4);

        // bottom border
        g.fillRect(SIZE/4+OTHER_MARGIN_X, (SIZE/2) * (rows-4)+OTHER_MARGIN_Y, (columns-1) *(SIZE/2),SIZE/4);
        g.drawRect(SIZE/4+OTHER_MARGIN_X, (SIZE/2) * (rows-4)+OTHER_MARGIN_Y, (columns-1) *(SIZE/2),SIZE/4);

        // left border
        g.fillRect(SIZE/4+OTHER_MARGIN_X, SIZE/4+OTHER_MARGIN_Y, SIZE/4, (rows-4)*(SIZE/2));
        g.drawRect(SIZE/4+OTHER_MARGIN_X, SIZE/4+OTHER_MARGIN_Y, SIZE/4, (rows-4)*(SIZE/2));

        // right border
        g.fillRect((columns-1) *(SIZE/2)+OTHER_MARGIN_X, SIZE/4+OTHER_MARGIN_Y, SIZE/4, (rows-4)*(SIZE/2));
        g.drawRect((columns-1) *(SIZE/2)+OTHER_MARGIN_X, SIZE/4+OTHER_MARGIN_Y, SIZE/4, (rows-4)*(SIZE/2));
    }

    // draw ohter Field
    public void draw_others_Field(Graphics g){
        for(int x = 1; x < columns-1; x++){     // 0~ 11
            for (int y = 4; y < rows-1; y++){   // 4~ 24
                if(field[y][x].num == 1){
                    switch (field[y][x].color) {
                        case -2:    // Gray
                            g.setColor(Color.gray);
                            break;
                        case -1:    // Black
                            g.setColor(Color.BLACK);
                            break;
                        case 0:     //  White
                            g.setColor(Color.WHITE);
                            break;
                        case 1:     //  Red (255,0,0)
                            g.setColor(Color.RED);
                            break;
                        case 2:     //  Yellow (255, 212, 0)
                            g.setColor(Color.YELLOW);
                            break;
                        case 3:     //  Blue    (0, 153, 255)
                            g.setColor(Color.BLUE);
                            break;
                        case 4:     // Green    (0, 153, 0)
                            g.setColor(Color.GREEN);
                            break;
                        case 5:     //  Pink    (255, 144, 190)
                            g.setColor(Color.PINK);
                            break;
                        case 6:     //  Purple  (128, 0, 255)
                            g.setColor(new Color(128, 0, 255));
                            break;
                        case 7:     //  Orange  (255, 127, 0)
                            g.setColor(Color.ORANGE);
                            break;
                    }
                    g.fillRect((SIZE/2) * x+OTHER_MARGIN_X, SIZE/2 + (SIZE/2) * (y-4)+OTHER_MARGIN_Y, SIZE/2,SIZE/2);
                    g.setColor(Color.BLACK);
                    g.drawRect((SIZE/2) * x+OTHER_MARGIN_X, SIZE/2 + (SIZE/2) * (y-4)+OTHER_MARGIN_Y, SIZE/2,SIZE/2);
                }
            }
        }
    }

//    public void draw_edge(Graphics g){
//        g.setColor(Color.BLACK);
//        for(int y = 0; y<rows-4; y++){  //  0 ~ 20
//            g.fillRect(MARGINE + SIZE/2,MARGINE + SIZE* y + SIZE/2, SIZE, SIZE);
//            g.fillRect(MARGINE + SIZE * columns + SIZE/2,MARGINE + SIZE* y + SIZE/2, SIZE, SIZE);
//        }
//        for(int x = 0; x<columns; x++){   // 0 ~ 11
//            g.fillRect(MARGINE + SIZE *  x + SIZE/2, MARGINE + SIZE * (rows-5) + SIZE/2, SIZE, SIZE);
//        }
//    }

    // check to possible or Impossible
    public boolean confirm_field(){
        for(int i = 0; i<rows; i++){
            for(int j = 0; j<columns; j++){
                if(field[i][j].num == 2)
                    return false;
            }
        }
        return true;
    }

    // block fall complete
    public boolean fall_block(){
        block_event(0);
        return fall_block_result;
    }

    // print my Field (Console)
    public void print_field(Box[][] field){
        for (int i = 0; i < rows-1; i++) {
            System.out.print("□");
            for (int j = 1; j < columns-1; j++) {
                if(field[i][j].num == 1){
                    System.out.print(" ■");
                }else{
                    System.out.print("   ");
                }
            }
            System.out.println(" □");
        }
        for(int j = 0; j<columns;j++){
            System.out.print("□ ");
        }
        System.out.println("");


    }

    // field  = field + field_
    public void fill_field(){
        for(int i = 0; i<rows; i++){
            for (int j = 0; j<columns; j++){
                field[i][j].num = field[i][j].num + field_[i][j].num;
                field[i][j].color = field[i][j].color+ field_[i][j].color;
            }
        }
    }

    // field_ = field_ (0) + block
    public void fill_field_(){
        reset_field_();
        for(int y = 0; y<block.area_length; y++){
            for(int x = 0; x<block.area_length; x++){
                if(y + block.row >= rows || x +block.column <=-1 || x + block.column >= columns){
                }else {
                    field_[y + block.row][x + block.column].num = block.area[y][x].num;
                    field_[y + block.row][x + block.column].color = block.area[y][x].color;
                }
            }
        }
    }

    // field = 0
    public void reset_save_field(){
        for (int y = 0; y < rows_savefield; y++) {
            for(int x = 0; x<columns_savefield; x++) { // 0 ~ 11
                if (y == rows_savefield - 1 || x == 0 || x == columns_savefield - 1) {
                    save_field[y][x] = new Box(1, -1);
                } else {
                    save_field[y][x] = new Box(0, 0);
                }
            }
        }
    }

    // field = 0
    public void reset_field(){
        for (int y = 0; y < rows; y++) {
            for(int x = 0; x<columns; x++) { // 0 ~ 11
                if (y == rows - 1 || x == 0 || x == columns - 1) {
                    field[y][x] = new Box(1, -1);
                } else {
                    field[y][x] = new Box(0, 0);
                }
            }
        }
    }

    // field_ = 0
    public void reset_field_(){
        for (int y = 0; y<rows; y++){
            for(int x = 0 ; x<columns; x++){
                field_[y][x] = new Box(0,0);
            }
        }
    }

    // field = save_field
    public void load_field(){
        for(int y = 0; y<rows; y++){
            if(y<4){
                for(int x = 0; x<columns; x++) {
                    field[y][x].num = 0;
                    field[y][x].color= 0;
                }
            }else{
                for(int x = 0; x<columns; x++){
                    field[y][x].num = save_field[y-4][x].num;
                    field[y][x].color = save_field[y-4][x].color;
                }
            }

        }
    }

    // save_field = field     (Back Up)
    public void save_field(){
        for(int i = 0; i<rows_savefield; i++){
            for (int j = 0; j<columns_savefield; j++){
                save_field[i][j].num = field[i+4][j].num;
                save_field[i][j].color = field[i+4][j].color;
            }
        }
    }

    // field = field - field_    (back up fill_field())
    public void return_field(){
        for (int i = 0; i<rows; i++){
            for(int j = 0 ; j<columns; j++){
                field[i][j].num = field[i][j].num - field_[i][j].num;
                field[i][j].color = field[i][j].color - field_[i][j].color;
            }
        }
    }

    // block move event
    public void block_event(int a){
        load_field();               // field = savefield
        reset_field_();             // field_ = 0

        switch(a){
            case 0:     // fall_block
                block.row= block.row + 1;
                break;
            case 1:     //  keyboard left
                block.column = block.column - 1;
                break;
            case 2:     //  keyboard right
                block.column = block.column + 1;
                break;
            case 3:     //  keyboard up
                block.turn_block();
                break;
        }
        fill_field_();              // field_ = field + block
        fill_field();               // field = field + field_

        if(confirm_field()) {        // 2가 없으면 그대로 진행.
            fall_block_result = true;
            //System.out.println("block.row = "+block.row + "  block.column = "+block.column);

        }else{                      // 2가 발견되면 다시 바꿈
            return_field();     // field - field_
            reset_field_();     // field_ = 0
            switch(a){
                case 0:             // fall block
                    fall_block_result = false;
                    fall_complete = true;
                    block.row= block.row - 1;
                    fill_field_();          //  field_ = 0 + block
                    fill_field();           // field = field + field_

                    explode_block();
                    save_field();

                    block_pre_change();
                    if (confirm_gameover()) {
                        System.out.println("*****************GAME OVER *****************");
                        this.removeKeyListener(keyadapter);
                        timer.cancel();
                        timer.purge();
                        return;
                    } else {
                    }
                    return;
                case 1:             // left
                    block.column = block.column + 1;
                    fill_field_();          //  field_ = 0 + block
                    fill_field();           // field = field + field_
                    break;
                case 2:             // right
                    block.column = block.column - 1;
                    fill_field_();          //  field_ = 0 + block
                    fill_field();           // field = field + field_
                    break;
                case 3:             // up
                    block.return_block();
                    fill_field_();          //  field_ = 0 + block
                    fill_field();           // field = field + field_
                    break;
            }

        }
        //print_field(field);
    }

    // confirm block explode
    public void explode_block(){
        int total;
        attack_point = -1;
        for(int a = rows -2; a > 3; a--){
            total = 0;
            for(int b = 1; b<columns-1; b++){
                total = total + field[a][b].num;
            }
            if(total == 10){
                //explode
                for(int i = a; i>3; i--){
                    for(int j = 1; j<columns-1; j++){
                        field[i][j].num = field[i-1][j].num;
                        field[i][j].color = field[i-1][j].color;
                    }
                }
                a = a+1;
                attack_point = attack_point+1;
                //combo = combo + 1;

            }else{

            }
        }

        if(attack_point>1){
            attack_flag = true;
        }
        score = score + (attack_point +1) * (attack_point +1) * 10;
    }

    // confirm game over
    public boolean confirm_gameover(){
        for(int y = 0; y<4; y++){
            for (int x = 1; x<columns-1; x++){
                if(field[y][x].num == 1){
                    gameover_flag = true;
                    return true;
                }
            }
        }
        return false;
    }

    // game over effect ( every block change color  >> gray )
    public void gameover_effect(){
        for(int y = 4; y<rows-1; y++) {
            for (int x = 1; x < columns - 1; x++) {
                if (field[y][x].num == 1) {
                    field[y][x].color = -2;
                }
            }
        }
    }

    // block preview change
    public void block_pre_change(){
        block = block_pre_1.clone();
        block_pre_1 = block_pre_2.clone();
        block_pre_2 = block_pre_3.clone();
        block_pre_3 = block_pre_4.clone();
        block_pre_4 = new Block();
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                System.out.println("스ㅔㄹ드 시작");
                while (true) {
                    if(gameover_flag){
                        Thread.sleep(1000);
                        thread.interrupt();
                    }
                    if (!queue.isEmpty()) {
                        System.out.println("not empty");
                        switch ((int) queue.peek()) {
                            case 37:    // left
                                block_event(1);
                                break;
                            case 39:    //right
                                block_event(2);
                                break;
                            case 38:    // up
                                block_event(3);
                                break;
                            case 40:    // down
                                if (timer_flag) {
                                    timer.cancel();
                                    timer.purge();
                                    timer_flag = false;

                                    timer = new Timer();
                                    task = new TimerTask() {
                                        @Override
                                        public void run() {
                                            // object 떨어짐
                                            fall_block();
                                        }
                                    };
                                    timer.schedule(task, 0, delay / 4);
                                }
                                break;
                            case -40:   // down release
                                System.out.println("down release");
                                timer.cancel();
                                timer.purge();
                                timer_flag = true;

                                task = new TimerTask() {
                                    @Override
                                    public void run() {
                                        // object 떨어짐
                                        fall_block();
                                    }
                                };
                                timer = new Timer();
                                timer.schedule(task, 0, delay);
                                break;
                            case 32:
                                while (fall_block()) {
                                }
                                break;
                        }

                        queue.remove();
                        queue.poll();
                        System.out.println("큐 사용");
//                        try {
//                            Thread.sleep(Long.valueOf(50));
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
                    }

                    Thread.sleep(50);
                }
            }catch (InterruptedException e){
                e.printStackTrace();
            }finally {
                System.out.println("thread dead");
            }
        }
    };

    public class MyKeyAdapter extends KeyAdapter {
        @Override
        public void keyReleased(KeyEvent e){
            super.keyReleased(e);
            int key = e.getKeyCode() * (-1);
            System.out.println(key);
            queue.add(key);

        }

        @Override
        public void keyPressed(KeyEvent e) {
            super.keyPressed(e);
            int key = e.getKeyCode();
            System.out.println(key);
            queue.add(key);
        }

    }

//    public class MyKeyAdapter extends KeyAdapter {
//        @Override
//        public void keyReleased(KeyEvent e) {
//            super.keyReleased(e);
//            int key = e.getKeyCode();
//            if (key == KeyEvent.VK_DOWN) {
//                timer.cancel();
//                timer.purge();
//                timer_flag = true;
//
//                task = new TimerTask() {
//                    @Override
//                    public void run() {
//                        // object 떨어짐
//                        fall_block();
//                    }
//                };
//                timer = new Timer();
//                timer.schedule(task, 0, delay);
//            }
//        }
//
//
//        @Override
//        public void keyPressed(KeyEvent e) {
//
//            super.keyPressed(e);
//            int key = e.getKeyCode();
//            switch (key) {
//                case KeyEvent.VK_LEFT:
//                    block_event(1);
//                    break;
//                case KeyEvent.VK_RIGHT:
//                    block_event(2);
//                    break;
//                case KeyEvent.VK_UP:
//                    block_event(3);
//                    break;
//                case KeyEvent.VK_DOWN:
//                    if (timer_flag) {
//                        timer.cancel();
//                        timer.purge();
//                        timer_flag = false;
//
//                        timer = new Timer();
//                        task = new TimerTask() {
//                            @Override
//                            public void run() {
//                                // object 떨어짐
//                                fall_block();
//                            }
//                        };
//                        timer.schedule(task, 0, delay / 4);
//                    }
//
//                    break;
//
//                case KeyEvent.VK_SPACE:
//                    while (fall_block()) {
//                    }
//
//                    break;
//            }
//        }
//    }
}