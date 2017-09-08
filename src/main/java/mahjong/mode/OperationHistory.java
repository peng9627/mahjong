package mahjong.mode;

import java.util.Date;

/**
 * Created by pengyi
 * Date 2017/7/28.
 */
public class OperationHistory {

    private int userId;
    private OperationHistoryType historyType;
    private Integer card;
    private Date date;

    public OperationHistory() {
    }

    public OperationHistory(int userId, OperationHistoryType historyType, Integer card) {
        this.userId = userId;
        this.historyType = historyType;
        this.card = card;
        this.date = new Date();
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public OperationHistoryType getHistoryType() {
        return historyType;
    }

    public void setHistoryType(OperationHistoryType historyType) {
        this.historyType = historyType;
    }

    public Integer getCard() {
        return card;
    }

    public void setCard(Integer card) {
        this.card = card;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
