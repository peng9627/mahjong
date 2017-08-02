package mahjong.mode;

import java.util.ArrayList;
import java.util.List;

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
            cards.sort(Integer::compareTo);
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
            cards.sort(Integer::compareTo);
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
            cards.sort(Integer::compareTo);
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
        cards.sort(Integer::compareTo);
        List<Integer> san_arr = new ArrayList<>();
        List<Integer> temp = new ArrayList<>();
        temp.addAll(cards);
        while (temp.size() > 2) {
            boolean find = false;
            for (int i = 0; i < temp.size() - 2; i++) {
                int start = temp.get(i);
                if (temp.get(i) < 30) {
                    if (cards.contains(start + 1) && cards.contains(start + 2)) {
                        san_arr.add(start);
                        san_arr.add(start);
                        san_arr.add(start);
                        temp.remove(Integer.valueOf(start));
                        temp.remove(Integer.valueOf(start));
                        temp.remove(Integer.valueOf(start));
                        find = true;
                        break;
                    }
                }
            }
            if (!find) {
                break;
            }
        }
        return san_arr;
    }

    /**
     * 传入14张牌，判断是否可胡牌
     *
     * @param cardList
     * @return
     */
    public static boolean hu(List<Integer> cardList) {
        List<Integer> cards = new ArrayList<>();
        cards.addAll(cardList);
        cards.sort(Integer::compareTo);
        List<Integer> temp = new ArrayList<>();
        temp.addAll(cards);

        //检查七对
        List<Integer> dui = get_dui(cards);
        if (dui.size() == 14) {
            return true;
        }

        //检测十三幺
        temp.clear();
        temp.addAll(cards);
        if (Card.isSSY(cards)) {
            return true;
        }

        //正常的胡
        temp.clear();
        temp.addAll(cards);

        //非七对先检查三个的,没个三个的都可以拆分成顺着听不同的牌
        List<Integer> san = get_san(cards);
        List<Integer> dui_temp = new ArrayList<>();
        List<Integer> cai = new ArrayList<>();
        if (0 != san.size()) {
            switch (san.size() / 3) {
                case 1:
                    //拆分三个的可能会影响听牌，先拆分
                    dui = get_dui(temp);
                    dui_temp.clear();
                    for (int i = 0; i < dui.size() / 2; i++) {
                        dui_temp.clear();
                        dui_temp.addAll(temp);
                        dui_temp.remove(dui.get(2 * i));
                        dui_temp.remove(dui.get(2 * i + 1));
                        if (dui_temp.size() == get_shun(dui_temp).size()) {
                            return true;
                        }
                    }

                    //不拆分
                    temp.removeAll(san);
                    //拆分三个的可能会影响听牌，先拆分
                    dui = get_dui(temp);
                    dui_temp.clear();
                    for (int i = 0; i < dui.size() / 2; i++) {
                        dui_temp.clear();
                        dui_temp.addAll(temp);
                        dui_temp.remove(dui.get(2 * i));
                        dui_temp.remove(dui.get(2 * i + 1));
                        if (dui_temp.size() == get_shun(dui_temp).size()) {
                            return true;
                        }
                    }

                    break;

                case 2:

                    //拆分三个的可能会影响听牌，先全部拆分
                    dui = get_dui(temp);
                    dui_temp.clear();
                    for (int i = 0; i < dui.size() / 2; i++) {
                        dui_temp.clear();
                        dui_temp.addAll(temp);
                        dui_temp.remove(dui.get(2 * i));
                        dui_temp.remove(dui.get(2 * i + 1));
                        if (dui_temp.size() == get_shun(dui_temp).size()) {
                            return true;
                        }
                    }

                    //拆分三个的可能会影响听牌，拆分第一个
                    cai.clear();
                    cai.addAll(temp);
                    cai.removeAll(san.subList(3, 6));
                    dui = get_dui(cai);
                    dui_temp.clear();
                    for (int i = 0; i < dui.size() / 2; i++) {
                        dui_temp.clear();
                        dui_temp.addAll(cai);
                        dui_temp.remove(dui.get(2 * i));
                        dui_temp.remove(dui.get(2 * i + 1));
                        if (dui_temp.size() == get_shun(dui_temp).size()) {
                            return true;
                        }
                    }

                    //拆分三个的可能会影响听牌，拆分第二个
                    cai.clear();
                    cai.addAll(temp);
                    cai.removeAll(san.subList(0, 3));
                    dui = get_dui(cai);
                    dui_temp.clear();
                    for (int i = 0; i < dui.size() / 2; i++) {
                        dui_temp.clear();
                        dui_temp.addAll(cai);
                        dui_temp.remove(dui.get(2 * i));
                        dui_temp.remove(dui.get(2 * i + 1));
                        if (dui_temp.size() == get_shun(dui_temp).size()) {
                            return true;
                        }
                    }

                    //不拆分
                    temp.removeAll(san);
                    dui = get_dui(temp);
                    dui_temp.clear();
                    for (int i = 0; i < dui.size() / 2; i++) {
                        dui_temp.clear();
                        dui_temp.addAll(temp);
                        dui_temp.remove(dui.get(2 * i));
                        dui_temp.remove(dui.get(2 * i + 1));
                        if (dui_temp.size() == get_shun(dui_temp).size()) {
                            return true;
                        }
                    }

                    break;
                case 3:
                    //拆分三个的可能会影响听牌，先全部拆分
                    dui = get_dui(temp);
                    dui_temp.clear();
                    for (int i = 0; i < dui.size() / 2; i++) {
                        dui_temp.clear();
                        dui_temp.addAll(temp);
                        dui_temp.remove(dui.get(2 * i));
                        dui_temp.remove(dui.get(2 * i + 1));
                        if (dui_temp.size() == get_shun(dui_temp).size()) {
                            return true;
                        }
                    }

                    //拆分三个的可能会影响听牌，拆分前两个
                    cai.clear();
                    cai.addAll(temp);
                    cai.removeAll(san.subList(6, 9));
                    dui = get_dui(cai);
                    dui_temp.clear();
                    for (int i = 0; i < dui.size() / 2; i++) {
                        dui_temp.clear();
                        dui_temp.addAll(cai);
                        dui_temp.remove(dui.get(2 * i));
                        dui_temp.remove(dui.get(2 * i + 1));
                        if (dui_temp.size() == get_shun(dui_temp).size()) {
                            return true;
                        }
                    }

                    //拆分三个的可能会影响听牌，拆分第一个和第三个
                    cai.clear();
                    cai.addAll(temp);
                    cai.removeAll(san.subList(3, 6));
                    dui = get_dui(cai);
                    dui_temp.clear();
                    for (int i = 0; i < dui.size() / 2; i++) {
                        dui_temp.clear();
                        dui_temp.addAll(cai);
                        dui_temp.remove(dui.get(2 * i));
                        dui_temp.remove(dui.get(2 * i + 1));
                        if (dui_temp.size() == get_shun(dui_temp).size()) {
                            return true;
                        }
                    }

                    //拆分三个的可能会影响听牌，拆分后两个
                    cai.clear();
                    cai.addAll(temp);
                    cai.removeAll(san.subList(0, 3));
                    dui = get_dui(cai);
                    dui_temp.clear();
                    for (int i = 0; i < dui.size() / 2; i++) {
                        dui_temp.clear();
                        dui_temp.addAll(cai);
                        dui_temp.remove(dui.get(2 * i));
                        dui_temp.remove(dui.get(2 * i + 1));
                        if (dui_temp.size() == get_shun(dui_temp).size()) {
                            return true;
                        }
                    }

                    //拆分三个的可能会影响听牌，拆分第一个
                    cai.clear();
                    cai.addAll(temp);
                    cai.removeAll(san.subList(3, 9));
                    dui = get_dui(cai);
                    dui_temp.clear();
                    for (int i = 0; i < dui.size() / 2; i++) {
                        dui_temp.clear();
                        dui_temp.addAll(cai);
                        dui_temp.remove(dui.get(2 * i));
                        dui_temp.remove(dui.get(2 * i + 1));
                        if (dui_temp.size() == get_shun(dui_temp).size()) {
                            return true;
                        }
                    }

                    //拆分三个的可能会影响听牌，拆分第二个
                    cai.clear();
                    cai.addAll(temp);
                    cai.removeAll(san.subList(0, 3));
                    cai.removeAll(san.subList(6, 9));
                    dui = get_dui(cai);
                    dui_temp.clear();
                    for (int i = 0; i < dui.size() / 2; i++) {
                        dui_temp.clear();
                        dui_temp.addAll(cai);
                        dui_temp.remove(dui.get(2 * i));
                        dui_temp.remove(dui.get(2 * i + 1));
                        if (dui_temp.size() == get_shun(dui_temp).size()) {
                            return true;
                        }
                    }

                    //拆分三个的可能会影响听牌，拆分第三个
                    cai.clear();
                    cai.addAll(temp);
                    cai.removeAll(san.subList(0, 6));
                    dui = get_dui(cai);
                    dui_temp.clear();
                    for (int i = 0; i < dui.size() / 2; i++) {
                        dui_temp.clear();
                        dui_temp.addAll(cai);
                        dui_temp.remove(dui.get(2 * i));
                        dui_temp.remove(dui.get(2 * i + 1));
                        if (dui_temp.size() == get_shun(dui_temp).size()) {
                            return true;
                        }
                    }


                    //不拆分
                    temp.removeAll(san);
                    dui = get_dui(temp);
                    dui_temp.clear();
                    for (int i = 0; i < dui.size() / 2; i++) {
                        dui_temp.clear();
                        dui_temp.addAll(temp);
                        dui_temp.remove(dui.get(2 * i));
                        dui_temp.remove(dui.get(2 * i + 1));
                        if (dui_temp.size() == get_shun(dui_temp).size()) {
                            return true;
                        }
                    }

                    break;

                case 4:
                    //拆分三个的可能会影响听牌，先拆分
                    dui = get_dui(temp);
                    dui_temp.clear();
                    for (int i = 0; i < dui.size() / 2; i++) {
                        dui_temp.clear();
                        dui_temp.addAll(temp);
                        dui_temp.remove(dui.get(2 * i));
                        dui_temp.remove(dui.get(2 * i + 1));
                        if (dui_temp.size() == get_shun(dui_temp).size()) {
                            return true;
                        }
                    }

                    //拆分三个的可能会影响听牌，拆分前三个
                    cai.clear();
                    cai.addAll(temp);
                    cai.removeAll(san.subList(0, 3));
                    dui = get_dui(cai);
                    dui_temp.clear();
                    for (int i = 0; i < dui.size() / 2; i++) {
                        dui_temp.clear();
                        dui_temp.addAll(cai);
                        dui_temp.remove(dui.get(2 * i));
                        dui_temp.remove(dui.get(2 * i + 1));
                        if (dui_temp.size() == get_shun(dui_temp).size()) {
                            return true;
                        }
                    }

                    //拆分三个的可能会影响听牌，拆分后三个
                    cai.clear();
                    cai.addAll(temp);
                    cai.removeAll(san.subList(9, 12));
                    dui = get_dui(cai);
                    dui_temp.clear();
                    for (int i = 0; i < dui.size() / 2; i++) {
                        dui_temp.clear();
                        dui_temp.addAll(cai);
                        dui_temp.remove(dui.get(2 * i));
                        dui_temp.remove(dui.get(2 * i + 1));
                        if (dui_temp.size() == get_shun(dui_temp).size()) {
                            return true;
                        }
                    }

                    for (int i = 0; i < 4; i++) {
                        for (int j = 0; j < 4; j++) {
                            if (i != j) {
                                cai.clear();
                                cai.addAll(temp);
                                cai.removeAll(san.subList(3 * i, 3 * i + 3));
                                cai.removeAll(san.subList(3 * j, 3 * j + 3));
                                dui = get_dui(cai);
                                dui_temp.clear();
                                for (int k = 0; k < dui.size() / 2; k++) {
                                    dui_temp.clear();
                                    dui_temp.addAll(cai);
                                    dui_temp.remove(dui.get(2 * i));
                                    dui_temp.remove(dui.get(2 * i + 1));
                                    if (dui_temp.size() == get_shun(dui_temp).size()) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }

                    //拆分三个的可能会影响听牌，拆分第一个
                    cai.clear();
                    cai.addAll(temp);
                    cai.removeAll(san.subList(3, 12));
                    dui = get_dui(cai);
                    dui_temp.clear();
                    for (int i = 0; i < dui.size() / 2; i++) {
                        dui_temp.clear();
                        dui_temp.addAll(cai);
                        dui_temp.remove(dui.get(2 * i));
                        dui_temp.remove(dui.get(2 * i + 1));
                        if (dui_temp.size() == get_shun(dui_temp).size()) {
                            return true;
                        }
                    }

                    //拆分三个的可能会影响听牌，拆分第二个
                    cai.clear();
                    cai.addAll(temp);
                    cai.removeAll(san.subList(0, 3));
                    cai.removeAll(san.subList(6, 12));
                    dui = get_dui(cai);
                    dui_temp.clear();
                    for (int i = 0; i < dui.size() / 2; i++) {
                        dui_temp.clear();
                        dui_temp.addAll(cai);
                        dui_temp.remove(dui.get(2 * i));
                        dui_temp.remove(dui.get(2 * i + 1));
                        if (dui_temp.size() == get_shun(dui_temp).size()) {
                            return true;
                        }
                    }

                    //拆分三个的可能会影响听牌，拆分第三个
                    cai.clear();
                    cai.addAll(temp);
                    cai.removeAll(san.subList(0, 6));
                    cai.removeAll(san.subList(9, 12));
                    dui = get_dui(cai);
                    dui_temp.clear();
                    for (int i = 0; i < dui.size() / 2; i++) {
                        dui_temp.clear();
                        dui_temp.addAll(cai);
                        dui_temp.remove(dui.get(2 * i));
                        dui_temp.remove(dui.get(2 * i + 1));
                        if (dui_temp.size() == get_shun(dui_temp).size()) {
                            return true;
                        }
                    }

                    //拆分三个的可能会影响听牌，拆分第四个
                    cai.clear();
                    cai.addAll(temp);
                    cai.removeAll(san.subList(0, 9));
                    dui = get_dui(cai);
                    dui_temp.clear();
                    for (int i = 0; i < dui.size() / 2; i++) {
                        dui_temp.clear();
                        dui_temp.addAll(cai);
                        dui_temp.remove(dui.get(2 * i));
                        dui_temp.remove(dui.get(2 * i + 1));
                        if (dui_temp.size() == get_shun(dui_temp).size()) {
                            return true;
                        }
                    }

                    //不拆分
                    temp.removeAll(san);
                    dui = get_dui(temp);
                    dui_temp.clear();
                    for (int i = 0; i < dui.size() / 2; i++) {
                        dui_temp.clear();
                        dui_temp.addAll(temp);
                        dui_temp.remove(dui.get(2 * i));
                        dui_temp.remove(dui.get(2 * i + 1));
                        if (dui_temp.size() == get_shun(dui_temp).size()) {
                            return true;
                        }
                    }
                    break;
            }
        } else {
            //拆分三个的可能会影响听牌，拆分第一个
            dui = get_dui(temp);
            dui_temp.clear();
            for (int i = 0; i < dui.size() / 2; i++) {
                dui_temp.clear();
                dui_temp.addAll(temp);
                dui_temp.remove(dui.get(2 * i));
                dui_temp.remove(dui.get(2 * i + 1));
                if (dui_temp.size() == get_shun(dui_temp).size()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 传入手牌，找到可胡牌
     *
     * @param userCards
     * @return
     */
    public static List<Integer> ting(List<Integer> userCards) {
        List<Integer> ting_arr = new ArrayList<>();
        List<Integer> temp = new ArrayList<>();
        List<Integer> allCard = Card.getAllCard();
        for (Integer card : allCard) {
            temp.clear();
            temp.addAll(userCards);
            temp.add(card);
            if (hu(temp)) {
                ting_arr.add(card);
            }
        }
        return ting_arr;
    }

    public static Integer checkGang(List<Integer> cards) {
        List<Integer> cardList = new ArrayList<>();
        cardList.addAll(cards);
        cardList.sort(Integer::compareTo);
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
     * @param cards         手牌
     * @param invertedCards 碰或杠的牌
     * @return
     */
    public static List<ScoreType> getHuType(List<Integer> cards, List<Integer> invertedCards) {
        List<ScoreType> scoreTypes = new ArrayList<>();

        //门清
        if (14 == cards.size()) {
            scoreTypes.add(ScoreType.MENQING_HU);
        }
        List<Integer> cardList = new ArrayList<>();
        cardList.addAll(cards);

        //碰碰胡
        if (get_san(cardList).size() + 2 == cards.size()) {
            scoreTypes.add(ScoreType.PENGPENG_HU);
        }

        List<Integer> allCard = new ArrayList<>();
        allCard.addAll(cards);
        allCard.addAll(invertedCards);

        //清一色，混一色
        if ((!Card.hasTong(allCard) && !Card.hasTiao(allCard)) || (!Card.hasTong(allCard) && !Card.hasWan(allCard))
                || (!Card.hasTiao(allCard) && !Card.hasWan(allCard))) {
            if (!Card.hasFeng(allCard) && !Card.hasZi(allCard)) {
                scoreTypes.add(ScoreType.QINGYISE_HU);
            } else {
                scoreTypes.add(ScoreType.HUNYISE_HU);
            }
        }

        //七对
        if (get_dui(cardList).size() == 14) {
            List<Integer> si = get_si(cardList);
            switch (si.size() / 4) {
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
        if (Card.isYJ(allCard)) {
            if (!Card.hasFeng(allCard) && !Card.hasZi(allCard)) {
                scoreTypes.add(ScoreType.QUANYAOJIU_HU);
            } else {
                scoreTypes.add(ScoreType.HUNYAOJIU_HU);
            }
        }

        //十三幺
        if (Card.isSSY(cardList)) {
            scoreTypes.add(ScoreType.SHISANYAO_HU);
        }

        //全风
        if (Card.isQF(allCard)) {
            scoreTypes.add(ScoreType.QUANFENG_HU);
        }

        //无红中
        if (!allCard.contains(31)) {
            scoreTypes.add(ScoreType.WUHONGZHONG_HU);
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
        int score = 1;
        for (ScoreType scoreType : scoreTypes) {
            switch (scoreType) {
                case MENQING_HU:
                    score += 2;
                    break;
                case PENGPENG_HU:
                case HUNYISE_HU:
                    score += 4;
                    break;
                case QINGYISE_HU:
                case QIXIAODUI_HU:
                    score += 8;
                    break;
                case HUNYAOJIU_HU:
                    score += 10;
                    break;
                case HAOHUAQIXIAODUI_HU:
                    score += 14;
                    break;
                case QUANYAOJIU_HU:
                case QUANFENG_HU:
                    score += 20;
                    break;
                case SHUANGHAOHUAQIXIAODUI_HU:
                    score += 28;
                    break;
                case SANHAOHUAQIXIAODUI_HU:
                    score += 42;
                    break;
                case WUHONGZHONG_HU:
                    score += 1;
                    break;
            }
        }
        //十三幺不与其它牌型叠加
        if (scoreTypes.contains(ScoreType.SHISANYAO_HU) && score < 20) {
            score = 20;
        }
        return score;
    }
}
