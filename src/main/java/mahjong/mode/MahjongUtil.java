package mahjong.mode;

import java.util.*;

/**
 * Author pengyi
 * Date 17-2-14.
 */

public class MahjongUtil {

    List<Integer> cards = new ArrayList<>();

    public static List<Integer> dealIntegers(List<Integer> allIntegers) {
        List<Integer> cardList = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            int cardIndex = (int) (Math.random() * allIntegers.size());
            cardList.add(allIntegers.get(cardIndex));
            allIntegers.remove(cardIndex);
        }
        return cardList;
    }

    public static List<Integer> get_dui(List<Integer> cardList) {
        List<Integer> cards = new ArrayList<>();
        cards.addAll(cardList);
        List<Integer> dui_arr = new ArrayList<>();
        if (cards.size() >= 2) {
            for (int i = 0; i < cards.size() - 1; i++) {
                if (cards.get(i).intValue() == cardList.get(i + 1).intValue()) {
                    dui_arr.add(cards.get(i));
                    dui_arr.add(cards.get(i));
                    i++;
                }
            }
        }
        return dui_arr;
    }

    public static List<Integer> get_san(List<Integer> cardList) {
        List<Integer> cards = new ArrayList<>();
        cards.addAll(cardList);
        List<Integer> san_arr = new ArrayList<>();
        if (cards.size() >= 3) {
            for (int i = 0; i < cards.size() - 2; i++) {
                if (cards.get(i).intValue() == cards.get(i + 2).intValue()) {
                    san_arr.add(cards.get(i));
                    san_arr.add(cards.get(i));
                    san_arr.add(cards.get(i));
                    i += 2;
                }
            }
        }
        return san_arr;
    }

    public static List<Integer> get_si(List<Integer> cardList) {
        List<Integer> cards = new ArrayList<>();
        cards.addAll(cardList);
        List<Integer> san_arr = new ArrayList<>();
        if (cards.size() >= 4) {
            cards.sort(new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    return o1.compareTo(o2);
                }
            });
            for (int i = 0; i < cards.size() - 3; i++) {
                if (cards.get(i).intValue() == cards.get(i + 3).intValue()) {
                    san_arr.add(cards.get(i));
                    san_arr.add(cards.get(i));
                    san_arr.add(cards.get(i));
                    san_arr.add(cards.get(i));
                    i += 3;
                }
            }
        }
        return san_arr;
    }

    public static List<Integer> get_shun(List<Integer> cardList) {
        List<Integer> cards = new ArrayList<>();
        cards.addAll(cardList);
        cards.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        });
        List<Integer> sun_arr = new ArrayList<>();
        List<Integer> temp = new ArrayList<>();
        temp.addAll(cards);
        while (temp.size() > 2) {
            boolean find = false;
            for (int i = 0; i < temp.size() - 2; i++) {
                int start = temp.get(i);
                if (temp.get(i) < 30) {
                    if (temp.contains(start + 1) && temp.contains(start + 2)) {
                        sun_arr.add(start);
                        sun_arr.add(start + 1);
                        sun_arr.add(start + 2);
                        temp.remove(Integer.valueOf(start));
                        temp.remove(Integer.valueOf(start + 1));
                        temp.remove(Integer.valueOf(start + 2));
                        find = true;
                        break;
                    }
                }
            }
            if (!find) {
                break;
            }
        }
        return sun_arr;
    }

    /**
     * 传入手牌，找到可胡牌
     *
     * @param userCards
     * @return
     */
    public static List<Integer> ting(List<Integer> userCards, int gameRules) {
        List<Integer> ting_arr = new ArrayList<>();
        List<Integer> temp = new ArrayList<>();
        List<Integer> allCard = Card.getAllCard();
        for (Integer card : allCard) {
            temp.clear();
            temp.addAll(userCards);
            temp.add(card);
            if (checkHu(temp, gameRules)) {
                ting_arr.add(card);
            }
        }
        return ting_arr;
    }

    public static Integer checkGang(List<Integer> cards) {
        List<Integer> cardList = new ArrayList<>();
        cardList.addAll(cards);
        cardList.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        });
        for (int i = 0; i < cardList.size() - 3; i++) {
            if (cardList.get(i).intValue() == cardList.get(i + 3)) {
                return cardList.get(i);
            }
        }
        return null;
    }

    public static Integer checkBaGang(List<Integer> cards, List<Integer> cardList) {
        for (Integer card : cardList) {
            for (Integer card1 : cards) {
                if (card.intValue() == card1) {
                    return card;
                }
            }
        }
        return null;
    }

    /**
     * 牌型
     *
     * @param cards     手牌
     * @param pengCards 碰的牌
     * @param gangCard  杠的牌
     * @return
     */
    public static List<ScoreType> getHuType(List<Integer> cards, List<Integer> pengCards, List<Integer> gangCard, int gameRules, int gui) {
        List<ScoreType> scoreTypes = new ArrayList<>();

        //门清
        if (14 == cards.size() && 1 == (gameRules >> 1) % 2) {
            scoreTypes.add(ScoreType.MENQING_HU);
        }
        if (4 == gangCard.size() && 1 == (gameRules >> 7) % 2) {
            scoreTypes.add(ScoreType.SHIBALUOHAN);
            return scoreTypes;
        }
        List<Integer> cardList = new ArrayList<>();
        cardList.addAll(cards);
        cardList.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        });

        int guiSize = Card.containSize(cardList, gui);
        List<Integer> temp = new ArrayList<>();
        //碰碰胡
        if (1 == (gameRules >> 6) % 2) {
            temp.addAll(cardList);
            List<Integer> san = get_san(temp);
            Card.removeAll(temp, san);
            if (temp.size() == 2 && temp.get(0).intValue() == temp.get(1)) {
                scoreTypes.add(ScoreType.PENGPENG_HU);
            } else {
                //有鬼牌
                for (int i = 0; i < guiSize; i++) {
                    Card.remove(temp, gui);
                }
                List<Integer> dui = get_dui(temp);
                switch (guiSize) {
                    case 1:
                        if ((dui.size() == 4 && temp.size() == 4) || temp.size() == 1) {
                            scoreTypes.add(ScoreType.PENGPENG_HU);
                        }
                        break;
                    case 2:
                        if (dui.size() == 1 && temp.size() == 3) {
                            scoreTypes.add(ScoreType.PENGPENG_HU);
                        }
                        break;
                    case 3:
                        if ((dui.size() == 8 && temp.size() == 8) || (dui.size() == 2 && temp.size() == 5) || temp.size() == 2) {
                            scoreTypes.add(ScoreType.PENGPENG_HU);
                        }
                        break;
                    case 4:
                        if ((dui.size() == 10 && temp.size() == 10) || (dui.size() == 6 && temp.size() == 7) || (dui.size() > 1 && temp.size() == 4)) {
                            scoreTypes.add(ScoreType.PENGPENG_HU);
                        }
                        break;
                }
            }

        }

        List<Integer> allCard = new ArrayList<>();
        allCard.addAll(cardList);
        allCard.addAll(pengCards);
        allCard.addAll(gangCard);

        temp.clear();
        temp.addAll(allCard);
        if (guiSize > 0) {
            for (int i = 0; i < guiSize; i++) {
                Card.remove(temp, gui);
            }
        }
        //清一色，混一色
        if ((!Card.hasSameColor(temp, 0) && !Card.hasSameColor(temp, 1)) || (!Card.hasSameColor(temp, 0) && !Card.hasSameColor(temp, 2))
                || (!Card.hasSameColor(temp, 1) && !Card.hasSameColor(temp, 2))) {
            if (!Card.hasSameColor(temp, 3) && !Card.hasSameColor(temp, 4) && 1 == (gameRules >> 9) % 2) {
                scoreTypes.add(ScoreType.QINGYISE_HU);
            } else if (1 == (gameRules >> 10) % 2) {
                scoreTypes.add(ScoreType.HUNYISE_HU);
            }
        }

        //七对
//        if (get_dui(cardList).size() == 14 && 1 == (gameRules >> 8) % 2) {//无鬼
        if (cardList.size() == 14 && 14 - get_dui(temp).size() - guiSize <= 2 * guiSize && 1 == (gameRules >> 8) % 2 && 1 != gameRules % 2) {//有鬼
            if (scoreTypes.contains(ScoreType.PENGPENG_HU)) {
                scoreTypes.remove(ScoreType.PENGPENG_HU);
            }
            int size = guiSize;
            int siCount = 0;
            List<Integer> si = get_si(temp);
            if (scoreTypes.contains(ScoreType.MENQING_HU)) {
                scoreTypes.remove(ScoreType.MENQING_HU);
            }
            List<Integer> temp1 = new ArrayList<>();
            temp1.addAll(temp);
            Card.removeAll(temp1, si);
            siCount += si.size() / 4;

            List<Integer> san = get_san(temp1);
            Card.removeAll(temp1, san);
            size -= san.size() / 3;
            siCount += san.size() / 3;

            Card.removeAll(temp1, get_dui(temp1));
            size -= temp1.size();
            siCount += size / 2;

            switch (siCount) {
                case 0:
                    scoreTypes.add(ScoreType.QIXIAODUI_HU);
                    break;
                case 1:
                    scoreTypes.add(ScoreType.HAOHUAQIXIAODUI_HU);
                    break;
                case 2:
                    scoreTypes.add(ScoreType.SHUANGHAOHUAQIXIAODUI_HU);
                    break;
                case 3:
                    scoreTypes.add(ScoreType.SANHAOHUAQIXIAODUI_HU);
                    break;
            }
        }

        //幺九
        if (Card.isYJ(temp) && 1 == (gameRules >> 3) % 2) {
            if (!Card.hasSameColor(temp, 3) && !Card.hasSameColor(temp, 4)) {
                scoreTypes.add(ScoreType.QUANYAOJIU_HU);
            } else {
                scoreTypes.add(ScoreType.HUNYAOJIU_HU);
            }
        }

        //全番
        if (Card.isQF(temp) && 1 == (gameRules >> 4) % 2) {
            scoreTypes.add(ScoreType.QUANFAN_HU);
            if (scoreTypes.contains(ScoreType.HUNYISE_HU)) {
                scoreTypes.remove(ScoreType.HUNYISE_HU);
            }
            if (scoreTypes.contains(ScoreType.HUNYAOJIU_HU)) {
                scoreTypes.remove(ScoreType.HUNYAOJIU_HU);
            }
        }

        //十三幺
        if (Card.isSSY(cardList, guiSize) && 1 == (gameRules >> 5) % 2) {
            scoreTypes.add(ScoreType.SHISANYAO_HU);
        }

        if (scoreTypes.contains(ScoreType.SHISANYAO_HU)) {
            scoreTypes.clear();
            scoreTypes.add(ScoreType.SHISANYAO_HU);
        }

        return scoreTypes;
    }

    /**
     * 算分
     *
     * @param scoreTypes
     * @return
     */
    public static int getScore(List<ScoreType> scoreTypes) {
        int score = 0;
        for (ScoreType scoreType : scoreTypes) {
            switch (scoreType) {
                case HUNYISE_HU:
                case MENQING_HU:
                case PENGPENG_HU:
                    score += 4;
                    break;
                case QIXIAODUI_HU:
                    score += 6;
                    break;
                case QINGYISE_HU:
                case HUNYAOJIU_HU:
                    score += 8;
                    break;
                case HAOHUAQIXIAODUI_HU:
                    score += 12;
                    break;
                case QUANYAOJIU_HU:
                case SHUANGHAOHUAQIXIAODUI_HU:
                    score += 18;
                    break;
                case QUANFAN_HU:
                    score += 20;
                    break;
                case SANHAOHUAQIXIAODUI_HU:
                    score += 24;
                    break;
                case SHISANYAO_HU:
                    score += 26;
                    break;
                case SHIBALUOHAN:
                    score += 36;
                    break;
            }
        }
        //十三幺不与其它牌型叠加
        if (scoreTypes.contains(ScoreType.SHISANYAO_HU) && score < 20) {
            score = 20;
        }
        return score;
    }

    private static Map<Integer, Integer> cardsSize(List<Integer> mahjongs) {
        Map<Integer, Integer> dic = new HashMap<>();
        for (Integer card : mahjongs) {
            if (dic.containsKey(card)) {
                dic.put(card, dic.get(card) + 1);
            } else {
                dic.put(card, 1);
            }
        }
        return dic;
    }


    public static int findPairNumber(List<Integer> mahjongs) {
        int number = 0;
        int single = 0;
        Map<Integer, Integer> dic = cardsSize(mahjongs);
        for (int i = 0; i < mahjongs.size(); i++) {
            int mahjong = mahjongs.get(i);
            if (dic.containsKey(mahjong)) {
                int count = dic.get(mahjong);
                if (count > 1) {
                    if (count == 2 || count == 4) number++;
                    else single++;
                }
            }
        }
        return number / 2 + single / 3;
    }

    public static ArrayList<Integer> getComputePossible(List<Integer> hand_list, int number) {
        Set<Integer> ret = new HashSet<>();
        for (int i = 0; i < hand_list.size(); i++) {
            int mahjong = hand_list.get(i);
            if (!ret.contains(mahjong)) {
                ret.add(mahjong);
            }
            int stepNum = 1;
            do {
                if (!ret.contains(mahjong - stepNum) && Card.legal(mahjong - stepNum)) {
                    ret.add(mahjong - stepNum);
                }
                if (!ret.contains(mahjong + stepNum) && Card.legal(mahjong + stepNum)) {
                    ret.add(mahjong + stepNum);
                }
                stepNum++;
            } while (stepNum <= number);
        }
        ArrayList<Integer> cards = new ArrayList<>();
        cards.addAll(ret);
        return cards;
    }

    /**
     * 传入14张牌，判断是否可胡牌
     *
     * @param cardList
     * @return
     */
    public static boolean checkHu(List<Integer> cardList, int gameRules, int gui) {
        List<Integer> cards = new ArrayList<>();
        int guiSize = 0;
        for (Integer card : cardList) {
            if (card != gui) {
                cards.add(card);
            } else {
                guiSize++;
            }
        }
        if (cardList.size() > 0 && cards.size() == 0) {
            return true;
        }
        List<Integer> guiCan = getComputePossible(cards, 2);
        List<Integer> temp = new ArrayList<>();
        switch (guiSize) {
            case 0:
                return checkHu(cardList, gameRules);
            case 1:
                for (int aBaoCan : guiCan) {
                    temp.clear();
                    temp.addAll(cards);
                    temp.add(aBaoCan);
                    if (checkHu(temp, gameRules)) {
                        return true;
                    }
                }
                break;
            case 2:
                for (int i = 0; i < guiCan.size(); i++) {
                    temp.clear();
                    temp.addAll(cards);
                    temp.add(guiCan.get(i));
                    for (int j = i; j < guiCan.size(); j++) {
                        temp.add(guiCan.get(j));
                        if (checkHu(temp, gameRules)) {
                            return true;
                        }
                        Card.remove(temp, guiCan.get(j));
                    }
                }
                break;
            case 3:
                for (int i = 0; i < guiCan.size(); i++) {
                    temp.clear();
                    temp.addAll(cards);
                    temp.add(guiCan.get(i));
                    for (int j = i; j < guiCan.size(); j++) {
                        temp.add(guiCan.get(j));
                        for (int k = j; k < guiCan.size(); k++) {
                            temp.add(guiCan.get(k));
                            if (checkHu(temp, gameRules)) {
                                return true;
                            }
                            Card.remove(temp, guiCan.get(k));
                        }
                        Card.remove(temp, guiCan.get(j));
                    }
                }
                break;
            case 4:
                for (int i = 0; i < guiCan.size(); i++) {
                    temp.clear();
                    temp.addAll(cards);
                    temp.add(guiCan.get(i));
                    for (int j = i; j < guiCan.size(); j++) {
                        temp.add(guiCan.get(j));
                        for (int k = j; k < guiCan.size(); k++) {
                            temp.add(guiCan.get(k));
                            for (int l = k; l < guiCan.size(); l++) {
                                temp.add(guiCan.get(l));
                                if (checkHu(temp, gameRules)) {
                                    return true;
                                }
                                Card.remove(temp, guiCan.get(l));
                            }
                            Card.remove(temp, guiCan.get(k));
                        }
                        Card.remove(temp, guiCan.get(j));
                    }
                }
                break;
        }
        return false;
    }

    /**
     * 传入14张牌，判断是否可胡牌
     *
     * @param cardList
     * @return
     */
    private static boolean checkHu(List<Integer> cardList, int gameRules) {
        List<Integer> handVals = new ArrayList<>();
        handVals.addAll(cardList);
        handVals.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        });

        List<Integer> pairs = get_dui(handVals);

        //鸡胡没有七对
        if (1 != gameRules % 2) {
            //检查七对
            if (pairs.size() == 14) {
                return true;
            }
        }

        //检测十三幺
        if (Card.isSSY(handVals, 0)) {
            return true;
        }

        for (int i = 0; i < pairs.size(); i += 2) {
            int md_val = pairs.get(i);
            List<Integer> hand = new ArrayList<>(handVals);
            hand.remove(Integer.valueOf(md_val));
            hand.remove(Integer.valueOf(md_val));
            if (CheckLug(hand)) {
                return true;
            }
        }
        return false;
    }

    protected static boolean CheckLug(List<Integer> handVals) {
        if (handVals.size() == 0) return true;
        int md_val = handVals.get(0);
        handVals.remove(0);
        if (Card.containSize(handVals, md_val) == 2) {
            handVals.remove(Integer.valueOf(md_val));
            handVals.remove(Integer.valueOf(md_val));
            return CheckLug(handVals);
        } else {
            if (handVals.contains(md_val + 1) && handVals.contains(md_val + 2)) {
                handVals.remove(Integer.valueOf(md_val + 1));
                handVals.remove(Integer.valueOf(md_val + 2));
                return CheckLug(handVals);
            }
        }
        return false;
    }

    public static List<Integer> GetLuckMa(int targetIdx, int bankerIdx, int playerCount, List<Integer> maVals) {
        List<Integer> ret = new ArrayList<>();
        int idx = targetIdx - bankerIdx;
        if (idx < 0) idx += playerCount;
        for (Integer ma : maVals) {
            int mj_val = ma % 10;
            int mj_color = ma / 10;
            if ((mj_color < 3 && ((mj_val - 1) % playerCount == idx)) || (mj_color == 3 && LuckMaZi(idx, playerCount, ma, Card.getAllSameColor(3))) || (mj_color == 4 && LuckMaZi(idx, playerCount, ma, Card.getAllSameColor(4)))) {
                ret.add(ma);
            }
        }
        return ret;
    }

    static boolean LuckMaZi(int idx, int playerCount, int mjVal, List<Integer> zis) {
        if (idx < zis.size()) {
            for (int i = 0; i < zis.size(); i++) {
                if (zis.get(i) == mjVal) {
                    return (i % playerCount) == idx;
                }
            }
        }
        return false;
    }
}
