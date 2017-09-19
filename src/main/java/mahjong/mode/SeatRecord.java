package mahjong.mode;

import java.util.ArrayList;
import java.util.List;

public class SeatRecord {
    private int userId;                         //用户名
    private String nickname;
    private String head;
    private List<Integer> initialCards = new ArrayList<>();         //初始牌
    private List<Integer> cards = new ArrayList<>();                //牌
    private int winOrLose;                      //输赢分数
    private GameResult cardResult;              //结算
    private List<GameResult> mingGangResult = new ArrayList<>();        //明杠
    private List<GameResult> anGangResult = new ArrayList<>();        //暗杠

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getHead() {
        return head;
    }

    public void setHead(String head) {
        this.head = head;
    }

    public List<Integer> getInitialCards() {
        return initialCards;
    }

    public void setInitialCards(List<Integer> initialCards) {
        this.initialCards = initialCards;
    }

    public List<Integer> getCards() {
        return cards;
    }

    public void setCards(List<Integer> cards) {
        this.cards = cards;
    }

    public int getWinOrLose() {
        return winOrLose;
    }

    public void setWinOrLose(int winOrLose) {
        this.winOrLose = winOrLose;
    }

    public GameResult getCardResult() {
        return cardResult;
    }

    public void setCardResult(GameResult cardResult) {
        this.cardResult = cardResult;
    }

    public List<GameResult> getMingGangResult() {
        return mingGangResult;
    }

    public void setMingGangResult(List<GameResult> mingGangResult) {
        this.mingGangResult = mingGangResult;
    }

    public List<GameResult> getAnGangResult() {
        return anGangResult;
    }

    public void setAnGangResult(List<GameResult> anGangResult) {
        this.anGangResult = anGangResult;
    }
}
