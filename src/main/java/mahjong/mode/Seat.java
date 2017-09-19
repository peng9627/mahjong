package mahjong.mode;

import java.util.ArrayList;
import java.util.List;

/**
 * Author pengyi
 * Date 17-3-7.
 */
public class Seat {

    private int seatNo;                         //座位号
    private int userId;                         //用户名
    private String nickname;                    //昵称
    private String head;                        //头像
    private boolean sex;                        //性别
    private List<Integer> initialCards = new ArrayList<>();          //初始牌
    private List<Integer> cards = new ArrayList<>();                 //牌
    private List<Integer> pengCards = new ArrayList<>();             //碰牌
    private List<Integer> anGangCards = new ArrayList<>();           //杠的牌
    private List<Integer> mingGangCards = new ArrayList<>();         //杠的牌
    private List<Integer> chiCards = new ArrayList<>();              //吃的牌
    private List<Integer> playedCards = new ArrayList<>();           //出牌
    private int score;                          //输赢分数
    private String areaString;                  //地区
    private boolean isRobot;                    //是否托管
    private int operation;                      //标识，0.未操作，1.胡，2.杠，3.碰，4.过
    private boolean ready;                      //准备
    private boolean completed;                  //就绪
    private GameResult cardResult;              //结算
    private List<GameResult> mingGangResult = new ArrayList<>();        //明杠
    private List<GameResult> anGangResult = new ArrayList<>();        //暗杠

    private int huCount;//胡牌次数
    private int zimoCount; //自摸次数
    private int dianpaoCount; //点炮次数
    private int angang; //暗杠次数
    private int minggang; //明杠次数

    private String ip;
    private int gameCount;

    public int getSeatNo() {
        return seatNo;
    }

    public void setSeatNo(int seatNo) {
        this.seatNo = seatNo;
    }

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

    public boolean isSex() {
        return sex;
    }

    public void setSex(boolean sex) {
        this.sex = sex;
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

    public List<Integer> getPengCards() {
        return pengCards;
    }

    public void setPengCards(List<Integer> pengCards) {
        this.pengCards = pengCards;
    }

    public List<Integer> getAnGangCards() {
        return anGangCards;
    }

    public void setAnGangCards(List<Integer> anGangCards) {
        this.anGangCards = anGangCards;
    }

    public List<Integer> getMingGangCards() {
        return mingGangCards;
    }

    public void setMingGangCards(List<Integer> mingGangCards) {
        this.mingGangCards = mingGangCards;
    }

    public List<Integer> getChiCards() {
        return chiCards;
    }

    public void setChiCards(List<Integer> chiCards) {
        this.chiCards = chiCards;
    }

    public List<Integer> getPlayedCards() {
        return playedCards;
    }

    public void setPlayedCards(List<Integer> playedCards) {
        this.playedCards = playedCards;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getAreaString() {
        return areaString;
    }

    public void setAreaString(String areaString) {
        this.areaString = areaString;
    }

    public boolean isRobot() {
        return isRobot;
    }

    public void setRobot(boolean robot) {
        isRobot = robot;
    }

    public int getOperation() {
        return operation;
    }

    public void setOperation(int operation) {
        this.operation = operation;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
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

    public int getHuCount() {
        return huCount;
    }

    public void setHuCount(int huCount) {
        this.huCount = huCount;
    }

    public int getZimoCount() {
        return zimoCount;
    }

    public void setZimoCount(int zimoCount) {
        this.zimoCount = zimoCount;
    }

    public int getDianpaoCount() {
        return dianpaoCount;
    }

    public void setDianpaoCount(int dianpaoCount) {
        this.dianpaoCount = dianpaoCount;
    }

    public int getAngang() {
        return angang;
    }

    public void setAngang(int angang) {
        this.angang = angang;
    }

    public int getMinggang() {
        return minggang;
    }

    public void setMinggang(int minggang) {
        this.minggang = minggang;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getGameCount() {
        return gameCount;
    }

    public void setGameCount(int gameCount) {
        this.gameCount = gameCount;
    }

    public void clear() {
        initialCards.clear();
        cards.clear();
        pengCards.clear();
        anGangCards.clear();
        mingGangCards.clear();
        chiCards.clear();
        playedCards.clear();
        ready = false;
        completed = false;
        cardResult = null;
        mingGangResult.clear();
        anGangResult.clear();
    }
}
