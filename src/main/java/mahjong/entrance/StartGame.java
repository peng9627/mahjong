package mahjong.entrance;

import mahjong.constant.Constant;

/**
 * Author pengyi
 * Date 17-7-24.
 */

public class StartGame {

    public static void main(String[] args) {
        Constant.init();
        new Thread(new MahjongTcpService()).start();
    }
}
