package mahjong.mode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by pengyi
 * Date : 16-6-12.
 */
public class Card {
    public static int containSize(List<Integer> cardList, Integer containCard) {
        int size = 0;
        for (Integer card : cardList) {
            if (card.intValue() == containCard) {
                size++;
            }
        }
        return size;
    }

    public static List<Integer> getAllCard() {
        return new ArrayList<>(
                Arrays.asList(1, 1, 1, 1,
                        2, 2, 2, 2,
                        3, 3, 3, 3,
                        4, 4, 4, 4,
                        5, 5, 5, 5,
                        6, 6, 6, 6,
                        7, 7, 7, 7,
                        8, 8, 8, 8,
                        9, 9, 9, 9,
                        11, 11, 11, 11,
                        12, 12, 12, 12,
                        13, 13, 13, 13,
                        14, 14, 14, 14,
                        15, 15, 15, 15,
                        16, 16, 16, 16,
                        17, 17, 17, 17,
                        18, 18, 18, 18,
                        19, 19, 19, 19,
                        21, 21, 21, 21,
                        22, 22, 22, 22,
                        23, 23, 23, 23,
                        24, 24, 24, 24,
                        25, 25, 25, 25,
                        26, 26, 26, 26,
                        27, 27, 27, 27,
                        28, 28, 28, 28,
                        29, 29, 29, 29,
                        31, 31, 31, 31,
                        33, 33, 33, 33,
                        35, 35, 35, 35,
                        41, 41, 41, 41,
                        43, 43, 43, 43,
                        45, 45, 45, 45,
                        47, 47, 47, 47
                ));
    }

    public static boolean containAll(List<Integer> cardList, List<Integer> cards) {

        for (Integer card : cards) {
            if (!cardList.contains(card)) {
                return false;
            }
        }
        return true;
    }


    /**
     * 有相同颜色的牌
     *
     * @param color
     * @return
     */
    public static boolean hasSameColor(List<Integer> cardList, int color) {

        List<Integer> cards = getAllSameColor(color);
        for (Integer card : cardList) {
            if (cards.contains(card)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取相同颜色的所有牌
     *
     * @param color
     * @return
     */
    public static List<Integer> getAllSameColor(int color) {

        switch (color) {
            case 0:
                return Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
            case 1:
                return Arrays.asList(11, 12, 13, 14, 15, 16, 17, 18, 19);
            case 2:
                return Arrays.asList(21, 22, 23, 24, 25, 26, 27, 28, 29);
            case 3:
                return Arrays.asList(31, 33, 35);
            case 4:
                return Arrays.asList(41, 43, 45, 47);
        }
        return null;
    }

    /**
     * 十三幺
     *
     * @param cardList
     * @param guiSize
     * @return
     */
    public static boolean isSSY(List<Integer> cardList, int guiSize) {
        List<Integer> cards = new ArrayList<>(Arrays.asList(1, 9, 11, 19, 21, 29, 31, 33, 35, 41, 43, 45, 47));
        if (guiSize == 0) {
            return containAll(cardList, cards) && containAll(cards, cardList);
        }
        List<Integer> temp = new ArrayList<>();
        temp.addAll(cardList);
        for (Integer card : cards) {
            boolean find = false;
            for (Integer handCard : cardList) {
                if (card.intValue() == handCard) {
                    temp.remove(handCard);
                    find = true;
                    break;
                }
            }
            if (!find) {
                guiSize--;
                if (0 == guiSize) {
                    return false;
                }
            }
        }

        if (Card.containAll(cards, temp)) {
            return true;
        }
        return false;
    }

    /**
     * 幺九
     *
     * @param cardList
     * @return
     */
    public static boolean isYJ(List<Integer> cardList) {
        List<Integer> cards = Arrays.asList(1, 9, 11, 19, 21, 29, 31, 33, 35, 41, 43, 45, 47);
        return containAll(cards, cardList);
    }

    /**
     * 全风
     *
     * @param cardList
     * @return
     */
    public static boolean isQF(List<Integer> cardList) {
        List<Integer> cards = Arrays.asList(31, 33, 35, 41, 43, 45, 47);
        return containAll(cards, cardList);
    }

    public static boolean legal(int card) {
        return getAllCard().contains(card);
    }

    /**
     * 买中自己的
     *
     * @return
     */
    public static List<Integer> ma_my() {
        return Arrays.asList(1, 5, 9, 11, 15, 19, 21, 25, 29, 31, 41);
    }

    /**
     * 买中下家
     *
     * @return
     */
    public static List<Integer> ma_next() {
        return Arrays.asList(2, 6, 12, 16, 22, 26, 33, 43);
    }

    /**
     * 买中对家
     *
     * @return
     */
    public static List<Integer> ma_opposite() {
        return Arrays.asList(3, 7, 13, 17, 23, 27, 35, 45);
    }

    /**
     * 买中上家
     *
     * @return
     */
    public static List<Integer> ma_last() {
        return Arrays.asList(4, 8, 14, 18, 24, 28, 47);
    }

    public static void remove(List<Integer> cards, Integer card) {
        for (Integer card1 : cards) {
            if (card1.intValue() == card) {
                cards.remove(card1);
                return;
            }
        }
    }

    public static void removeAll(List<Integer> cards, List<Integer> removes) {
        for (Integer card : removes) {
            for (Integer card1 : cards) {
                if (card1.intValue() == card) {
                    cards.remove(card1);
                    break;
                }
            }
        }
    }
}
