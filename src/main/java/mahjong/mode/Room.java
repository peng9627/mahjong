package mahjong.mode;


import java.util.ArrayList;
import java.util.List;

/**
 * Author pengyi
 * Date 17-3-7.
 */
public class Room {

    private Double baseScore; //基础分
    private String roomNo;  //桌号
    private List<Seat> seats;//座位
    private int operationSeat;
    private List<OperationHistory> historyList;
    private List<Integer> surplusCards;//剩余的牌
    private GameStatus gameStatus;

    private String lastOperation;

    private String banker;//庄家
    private int gameTimes; //游戏局数
    private int count;//人数
    private boolean dianpao;//点炮
    private Integer[] dice;//骰子

    public Room(Double baseScore, String roomNo, String banker, int gameTimes, int count, boolean dianpao) {
        this.baseScore = baseScore;
        this.roomNo = roomNo;
        this.banker = banker;
        this.gameTimes = gameTimes;
        this.count = count;
        this.dianpao = dianpao;
        this.gameStatus = GameStatus.WAITING;
    }

    public Double getBaseScore() {
        return baseScore;
    }

    public void setBaseScore(Double baseScore) {
        this.baseScore = baseScore;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public void setSeats(List<Seat> seats) {
        this.seats = seats;
    }

    public int getOperationSeat() {
        return operationSeat;
    }

    public void setOperationSeat(int operationSeat) {
        this.operationSeat = operationSeat;
    }

    public List<OperationHistory> getHistoryList() {
        return historyList;
    }

    public void setHistoryList(List<OperationHistory> historyList) {
        this.historyList = historyList;
    }

    public List<Integer> getSurplusCards() {
        return surplusCards;
    }

    public void setSurplusCards(List<Integer> surplusCards) {
        this.surplusCards = surplusCards;
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(GameStatus gameStatus) {
        this.gameStatus = gameStatus;
    }

    public String getLastOperation() {
        return lastOperation;
    }

    public void setLastOperation(String lastOperation) {
        this.lastOperation = lastOperation;
    }

    public String getBanker() {
        return banker;
    }

    public void setBanker(String banker) {
        this.banker = banker;
    }

    public int getGameTimes() {
        return gameTimes;
    }

    public void setGameTimes(int gameTimes) {
        this.gameTimes = gameTimes;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isDianpao() {
        return dianpao;
    }

    public void setDianpao(boolean dianpao) {
        this.dianpao = dianpao;
    }

    public Integer[] getDice() {
        return dice;
    }

    public void setDice(Integer[] dice) {
        this.dice = dice;
    }

    public void addSeat(User user) {
        Seat seat = new Seat();
        seat.setRobot(false);
        seat.setReady(false);
        seat.setAreaString("");
        seat.setGold(0);
        seat.setScore(0);
        seat.setSeatNo(seats.size() + 1);
        seat.setUserName(user.getUsername());
        seats.add(seat);
    }

    public void dealCard() {
        if (operationSeat == 0) {
            surplusCards = Card.getAllCard();
            for (Seat seat : seats) {
                List<Integer> cardList = new ArrayList<>();
                for (int i = 0; i < 13; i++) {
                    int cardIndex = (int) (Math.random() * surplusCards.size());
                    cardList.add(surplusCards.get(cardIndex));
                    surplusCards.remove(cardIndex);
                }
                seat.setCards(cardList);
            }
            int cardIndex = (int) (Math.random() * surplusCards.size());
            seats.get(0).getCards().add(surplusCards.get(cardIndex));
            surplusCards.remove(cardIndex);
            operationSeat = 1;
        }
    }

    public int getNextSeat() {
        int next = operationSeat;
        if (count == next) {
            next = 1;
        } else {
            next += 1;
        }
        return next;
    }
}
