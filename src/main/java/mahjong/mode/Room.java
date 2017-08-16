package mahjong.mode;


import mahjong.entrance.MahjongTcpService;
import mahjong.redis.RedisService;

import java.util.*;

/**
 * Author pengyi
 * Date 17-3-7.
 */
public class Room {

    private int baseScore; //基础分
    private String roomNo;  //桌号
    private List<Seat> seats = new ArrayList<>();//座位
    private List<Integer> seatNos;
    private int operationSeatNo;
    private List<OperationHistory> historyList = new ArrayList<>();
    private List<Integer> surplusCards;//剩余的牌
    private GameStatus gameStatus;

    private int lastOperation;

    private int banker;//庄家
    private int gameTimes; //游戏局数
    private int count;//人数
    private boolean dianpao;//点炮
    private Integer[] dice;//骰子
    private List<Record> recordList = new ArrayList<>();//战绩
    private int gameCount;

    private int initMaCount;

    public int getBaseScore() {
        return baseScore;
    }

    public void setBaseScore(int baseScore) {
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

    public List<Integer> getSeatNos() {
        return seatNos;
    }

    public void setSeatNos(List<Integer> seatNos) {
        this.seatNos = seatNos;
    }

    public int getOperationSeatNo() {
        return operationSeatNo;
    }

    public void setOperationSeatNo(int operationSeatNo) {
        this.operationSeatNo = operationSeatNo;
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

    public int getLastOperation() {
        return lastOperation;
    }

    public void setLastOperation(int lastOperation) {
        this.lastOperation = lastOperation;
    }

    public int getBanker() {
        return banker;
    }

    public void setBanker(int banker) {
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

    public List<Record> getRecordList() {
        return recordList;
    }

    public void setRecordList(List<Record> recordList) {
        this.recordList = recordList;
    }

    public int getGameCount() {
        return gameCount;
    }

    public void setGameCount(int gameCount) {
        this.gameCount = gameCount;
    }

    public int getInitMaCount() {
        return initMaCount;
    }

    public void setInitMaCount(int initMaCount) {
        this.initMaCount = initMaCount;
    }

    public void addSeat(User user) {
        Seat seat = new Seat();
        seat.setRobot(false);
        seat.setReady(false);
        seat.setAreaString("");
        seat.setGold(0);
        seat.setScore(0);
        seat.setSeatNo(seatNos.get(0));
        seatNos.remove(0);
        seat.setUserId(user.getId());
        seats.add(seat);
    }

    public void dealCard() {
        surplusCards = Card.getAllCard();
        //卖马 发牌
        for (Seat seat : seats) {
            if (seat.getMaCount() == 0) {
                seat.setMaCount(initMaCount);
            }
            if (seat.getUserId() == banker) {
                operationSeatNo = seat.getSeatNo();
            }

            if (seat.getUserId() == banker) {
                List<Integer> cardList = new ArrayList<>();
                cardList.add(1);
                surplusCards.remove(Integer.valueOf(1));
                cardList.add(1);
                surplusCards.remove(Integer.valueOf(1));
                cardList.add(1);
                surplusCards.remove(Integer.valueOf(1));
                cardList.add(2);
                surplusCards.remove(Integer.valueOf(2));
                cardList.add(2);
                surplusCards.remove(Integer.valueOf(2));
                cardList.add(2);
                surplusCards.remove(Integer.valueOf(2));
                cardList.add(3);
                surplusCards.remove(Integer.valueOf(3));
                cardList.add(3);
                surplusCards.remove(Integer.valueOf(3));
                cardList.add(3);
                surplusCards.remove(Integer.valueOf(3));
                cardList.add(4);
                surplusCards.remove(Integer.valueOf(4));
                cardList.add(4);
                surplusCards.remove(Integer.valueOf(4));
                cardList.add(4);
                surplusCards.remove(Integer.valueOf(4));
                cardList.add(5);
                surplusCards.remove(Integer.valueOf(5));
                cardList.add(5);
                surplusCards.remove(Integer.valueOf(5));
                seat.setCards(cardList);
                seat.setInitialCards(cardList);

                List<Integer> maList = new ArrayList<>();
                for (int i = 0; i < seat.getMaCount(); i++) {
                    int cardIndex = (int) (Math.random() * surplusCards.size());
                    maList.add(surplusCards.get(cardIndex));
                    surplusCards.remove(cardIndex);
                }
                seat.setMa(maList);
            } else {
                List<Integer> cardList = new ArrayList<>();
                for (int i = 0; i < 13; i++) {
                    int cardIndex = (int) (Math.random() * surplusCards.size());
                    cardList.add(surplusCards.get(cardIndex));
                    surplusCards.remove(cardIndex);
                }
                seat.setCards(cardList);
                seat.setInitialCards(cardList);

                List<Integer> maList = new ArrayList<>();
                for (int i = 0; i < seat.getMaCount(); i++) {
                    int cardIndex = (int) (Math.random() * surplusCards.size());
                    maList.add(surplusCards.get(cardIndex));
                    surplusCards.remove(cardIndex);
                }
                seat.setMa(maList);
            }


        }
//        int cardIndex = (int) (Math.random() * surplusCards.size());
//        seats.get(0).getCards().add(surplusCards.get(cardIndex));


//        seats.get(0).getCards().add(5);
//        surplusCards.remove(Integer.valueOf(5));

//        surplusCards.remove(cardIndex);
    }

    public int getNextSeat() {
        int next = operationSeatNo;
        if (count == next) {
            next = 1;
        } else {
            next += 1;
        }
        return next;
    }

    private void clear() {
        Record record = new Record();
        record.setDice(dice);
        List<SeatRecord> seatRecords = new ArrayList<>();
        seats.forEach(seat -> {
            SeatRecord seatRecord = new SeatRecord();
            seatRecord.setUserId(seat.getUserId());
            seatRecord.setCardResult(seat.getCardResult());
            seatRecord.setGangResult(seat.getGangResult());
            seatRecord.setInitialCards(seat.getInitialCards());
            seatRecord.setCards(seat.getCards());
            final int[] winOrLose = {0};
            seat.getGangResult().forEach(gameResult -> winOrLose[0] += gameResult.getScore());
            if (null != seat.getCardResult()) {
                winOrLose[0] += seat.getCardResult().getScore();
            }
            seatRecord.setWinOrLoce(winOrLose[0]);
            seatRecords.add(seatRecord);
        });
        record.setSeatRecordList(seatRecords);
        record.setHistoryList(historyList);
        recordList.add(record);

        historyList.clear();
        surplusCards.clear();
        gameStatus = GameStatus.READYING;
        lastOperation = 0;
        dice = null;
        seats.forEach(Seat::clear);
    }

    public void getCard(GameBase.BaseConnection.Builder response, int seatNo, RedisService redisService) {
        if (0 == surplusCards.size()) {
            gameOver(response, redisService, 0);
            return;
        }
        GameBase.BaseAction.Builder actionResponse = GameBase.BaseAction.newBuilder().setOperationId(GameBase.ActionId.GET_CARD);
        operationSeatNo = seatNo;
        int cardIndex = (int) (Math.random() * surplusCards.size());
        Integer card1 = surplusCards.get(cardIndex);
        surplusCards.remove(cardIndex);
        final Integer[] username = new Integer[1];
        seats.stream().filter(seat -> seat.getSeatNo() == seatNo).forEach(seat -> username[0] = seat.getUserId());
        actionResponse.setID(username[0]);

        historyList.add(new OperationHistory(username[0], OperationHistoryType.GET_CARD, card1));
        Mahjong.MahjongGetCardResponse.Builder builder1 = Mahjong.MahjongGetCardResponse.newBuilder();
        builder1.setCard(card1);
        Seat operationSeat = null;
        for (Seat seat : seats) {
            if (seat.getSeatNo() == seatNo) {
                seat.getCards().add(card1);
                operationSeat = seat;
                actionResponse.setData(builder1.build().toByteString());
            } else {
                actionResponse.clearData();
            }
            if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
            }
        }

        GameBase.RoundResponse roundResponse = GameBase.RoundResponse.newBuilder().setID(username[0]).build();
        response.setOperationType(GameBase.OperationType.ROUND).setData(roundResponse.toByteString());
        seats.stream().filter(seat -> MahjongTcpService.userClients.containsKey(seat.getUserId()))
                .forEach(seat -> MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId()));

        checkSelfGetCard(response, operationSeat);
    }

    /**
     * 游戏结束
     *
     * @param response
     * @param redisService
     */
    public void gameOver(GameBase.BaseConnection.Builder response, RedisService redisService, int winSeat) {

        Map<Integer, Integer> seatScore = new HashMap<>();
        Map<Integer, Integer> maScore = new HashMap<>();

        List<Integer> loseSeats = new ArrayList<>();
        final int[] score = {0};

        //TODO 扣款
        Xingning.XingningMahjongResultResponse.Builder resultResponse = Xingning.XingningMahjongResultResponse.newBuilder();
        seats.forEach(seat -> {
            maScore.put(seat.getSeatNo(), 0);
            if (seat.getSeatNo() == winSeat) {
                if (seat.getMaCount() != initMaCount + 6) {
                    seat.setMaCount(seat.getMaCount() + 2);
                }
            } else {
                seat.setMaCount(initMaCount);
            }

            Xingning.XingningMahjongUserResult.Builder userResult = Xingning.XingningMahjongUserResult.newBuilder();
            userResult.setID(seat.getUserId());
            userResult.addAllCards(seat.getCards());
            final int[] win = {0};
            if (null != seat.getCardResult()) {
                seatScore.put(seat.getSeatNo(), seat.getCardResult().getScore());
                userResult.setCardScore(seat.getCardResult().getScore());
                if (seat.getCardResult().getScore() > 0) {
                    if (seat.getCardResult().getScoreTypes().contains(ScoreType.ZIMO_HU) || seat.getCardResult().getScoreTypes().contains(ScoreType.TIAN_HU)) {
                        userResult.setCardScore(seat.getCardResult().getScore() * 3);
                    }
                } else {
                    loseSeats.add(seat.getSeatNo());
                    score[0] = seat.getCardResult().getScore();
                }
                for (ScoreType scoreType : seat.getCardResult().getScoreTypes()) {
                    userResult.addScoreTypes(Xingning.ScoreType.forNumber(scoreType.ordinal() - 4));
                }
            }
            List<Integer> gangCard = new ArrayList<>();
            int gangScore = 0;
            for (GameResult gameResult : seat.getGangResult()) {
                gangScore += gameResult.getScore();
                if (0 < gameResult.getScore()) {
                    gangCard.add(gameResult.getCard());
                }
            }
            userResult.setGangScore(gangScore);

            userResult.setWinOrLose(win[0]);
            userResult.addAllGangCards(gangCard);
            resultResponse.addUserResult(userResult);

            seat.setScore(seat.getScore() + win[0]);

        });

        for (Seat seat : seats) {
            for (Integer ma : seat.getMa()) {
                int maiSeat = getMaiSeat(seat.getSeatNo(), ma);
                if (seatScore.containsKey(maiSeat)) {
                    if (seatScore.get(maiSeat) > 0) {//买中赢家
                        maScore.put(seat.getSeatNo(), maScore.get(seat.getSeatNo()) + (score[0] * loseSeats.size()));
                        for (int loseSeat : loseSeats) {
                            maScore.put(loseSeat, maScore.get(loseSeat) - score[0]);
                        }
                    } else {//买中输家
                        maScore.put(seat.getSeatNo(), maScore.get(seat.getSeatNo()) - score[0] * loseSeats.size());
                        maScore.put(winSeat, maScore.get(winSeat) + score[0]);
                    }
                }
            }
        }

        for (Seat seat : seats) {
            if (maScore.containsKey(seat.getSeatNo())) {
                maScore.put(seat.getUserId(), maScore.get(seat.getSeatNo()));
                maScore.remove(seat.getSeatNo());
            }
        }

        for (Xingning.XingningMahjongUserResult.Builder userResult : resultResponse.getUserResultBuilderList()) {
            if (maScore.containsKey(userResult.getID())) {
                userResult.setMaScore(maScore.get(userResult.getID()));
            }
        }

        response.setOperationType(GameBase.OperationType.RESULT).setData(resultResponse.build().toByteString());
        seats.stream().filter(seat -> MahjongTcpService.userClients.containsKey(seat.getUserId()))
                .forEach(seat -> MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId()));

        clear();
        //结束房间
        if (gameCount == gameTimes) {
            roomOver(response, redisService);
        }
    }

    public void roomOver(GameBase.BaseConnection.Builder response, RedisService redisService) {
        Xingning.XingningMahjongOverResponse.Builder over = Xingning.XingningMahjongOverResponse.newBuilder();

        for (Seat seat : seats) {
            //TODO 统计
            Xingning.XingningMahjongSeatGameOver.Builder seatGameOver = Xingning.XingningMahjongSeatGameOver.newBuilder()
                    .setID(seat.getUserId()).setMinggang(seat.getMinggang()).setAngang(seat.getAngang())
                    .setZimoCount(seat.getZimoCount()).setHuCount(seat.getHuCount()).setDianpaoCount(seat.getDianpaoCount());
            over.addGameOver(seatGameOver);
        }

        for (Seat seat : seats) {
            redisService.delete("reconnect" + seat.getUserId());
            if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                String uuid = UUID.randomUUID().toString().replace("-", "");
                while (redisService.exists(uuid)) {
                    uuid = UUID.randomUUID().toString().replace("-", "");
                }
                redisService.addCache("backkey" + uuid, seat.getUserId() + "", 10);
                over.setBackKey(uuid);
                response.setOperationType(GameBase.OperationType.OVER).setData(over.build().toByteString());
                MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
            }
        }

        //删除该桌
        redisService.delete("room" + roomNo);
        roomNo = null;
    }

    private int getMaiSeat(int seatNo, Integer ma) {
        if (Card.ma_my().contains(ma)) {
            return seatNo;
        }
        if (Card.ma_next().contains(ma)) {
            return (seatNo + 1) % count;
        }
        if (Card.ma_opposite().contains(ma)) {
            return (seatNo + 2) % count;
        }
        if (Card.ma_last().contains(ma)) {
            return (seatNo + 3) % count;
        }
        return 0;
    }

    /**
     * 摸牌后检测是否可以自摸、暗杠、扒杠
     *
     * @param seat 座位
     */
    public void checkSelfGetCard(GameBase.BaseConnection.Builder response, Seat seat) {
        System.out.println("摸牌后检测是否可以自摸、暗杠、扒杠");
        GameBase.AskResponse.Builder builder = GameBase.AskResponse.newBuilder();
        if (MahjongUtil.hu(seat.getCards())) {
            builder.addOperationId(GameBase.ActionId.HU);
        }
        //暗杠
        if (null != MahjongUtil.checkGang(seat.getCards())) {
            builder.addOperationId(GameBase.ActionId.AN_GANG);
        }
        //扒杠
        if (null != MahjongUtil.checkBaGang(seat.getCards(), seat.getPengCards())) {
            builder.addOperationId(GameBase.ActionId.BA_GANG);
        }
        if (0 != builder.getOperationIdCount()) {
            if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                response.clear();
                response.setOperationType(GameBase.OperationType.ASK).setData(builder.build().toByteString());
                MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
            }
            //TODO 出牌超时
//                    new SelfOperationTimeout(deskNo).start();
        } else {
            //TODO 出牌超时
//                    new PlayTimeout(seat.getSeatNo(), deskNo).start();
        }
    }

    /**
     * 和牌
     *
     * @param userId
     * @param response
     * @param redisService
     */
    public void hu(int userId, GameBase.BaseConnection.Builder response, RedisService redisService) {
        //和牌的人
        final Seat[] huSeat = new Seat[1];
        seats.stream().filter(seat -> seat.getUserId() == userId)
                .forEach(seat -> huSeat[0] = seat);
        //检查是自摸还是点炮,自摸输家是其它三家
        if (MahjongUtil.hu(huSeat[0].getCards())) {

            List<ScoreType> scoreTypes = MahjongUtil.getHuType(huSeat[0].getCards(), huSeat[0].getPengCards(), huSeat[0].getGangCards());
            int score = MahjongUtil.getScore(scoreTypes);

            //天胡
            if (historyList.size() == 0 && score < 20) {
                scoreTypes.clear();
                scoreTypes.add(ScoreType.TIAN_HU);
                score = 20;
            } else {
                scoreTypes.add(ScoreType.ZIMO_HU);
                score += 2;
            }
            int loseSize[] = {0};
            int finalScore = score;
            seats.stream().filter(seat -> seat.getUserId() != userId)
                    .forEach(seat -> {
                        seat.setCardResult(new GameResult(scoreTypes, huSeat[0].getCards().get(huSeat[0].getCards().size() - 1), finalScore));
                        loseSize[0]++;
                    });

            huSeat[0].setCardResult(new GameResult(scoreTypes, huSeat[0].getCards().get(huSeat[0].getCards().size() - 1), loseSize[0] * score));
            huSeat[0].setZimoCount(huSeat[0].getZimoCount() + 1);

            gameOver(response, redisService, huSeat[0].getSeatNo());
            return;
        }

        //找到那张牌
        final Integer[] card = new Integer[1];
        Seat operationSeat = null;
        for (Seat seat : seats) {
            if (seat.getSeatNo() == operationSeatNo) {
                card[0] = seat.getPlayedCards().get(seat.getPlayedCards().size() - 1);
                operationSeat = seat;
                break;
            }
        }

        //先检查胡，胡优先
        Seat finalOperationSeat = operationSeat;
        seats.stream().filter(seat -> seat.getSeatNo() != operationSeatNo && seat.getUserId() == userId).forEach(seat -> {
            List<Integer> temp = new ArrayList<>();
            temp.addAll(seat.getCards());

            //当前玩家是否可以胡牌
            temp.add(card[0]);
            if (MahjongUtil.hu(temp) && seat.getOperation() == 1) {

                List<ScoreType> scoreTypes = MahjongUtil.getHuType(huSeat[0].getCards(), huSeat[0].getPengCards(), huSeat[0].getGangCards());
                int score = MahjongUtil.getScore(scoreTypes);
                //地胡
                if (historyList.size() == 1 && score < 20) {
                    scoreTypes.clear();
                    scoreTypes.add(ScoreType.DI_HU);
                    score = 20;
                }

                finalOperationSeat.setCardResult(new GameResult(scoreTypes, card[0], -score));
                finalOperationSeat.setCardResult(new GameResult(scoreTypes, card[0], -score));
                finalOperationSeat.setDianpaoCount(finalOperationSeat.getDianpaoCount() + 1);
                seat.setCardResult(new GameResult(scoreTypes, card[0], score));
                huSeat[0].setHuCount(huSeat[0].getHuCount() + 1);
                //胡牌
                gameOver(response, redisService, huSeat[0].getSeatNo());

                //TODO 检查是否游戏是否结束
//                checkEnd(room);
            }
        });
    }

    /**
     * 暗杠或者扒杠
     *
     * @param actionResponse
     * @param card
     * @param response
     * @param redisService
     */
    public void selfGang(GameBase.BaseAction.Builder actionResponse, Integer card, GameBase.BaseConnection.Builder response, RedisService redisService, int userId) {
        //碰或者杠
        seats.stream().filter(seat -> seat.getSeatNo() == operationSeatNo).forEach(seat -> {
            if (4 == Card.containSize(seat.getCards(), card)) {//暗杠
                Card.remove(seat.getCards(), card);
                Card.remove(seat.getCards(), card);
                Card.remove(seat.getCards(), card);
                Card.remove(seat.getCards(), card);

                seat.getGangCards().add(card);

                List<ScoreType> scoreTypes = new ArrayList<>();
                scoreTypes.add(ScoreType.AN_GANG);

                final int[] loseSize = {0};
                seats.stream().filter(seat1 -> seat1.getSeatNo() != seat.getSeatNo())
                        .forEach(seat1 -> {
                            seat.getGangResult().add(new GameResult(scoreTypes, card, -2));
                            loseSize[0]++;
                        });
                seat.getGangResult().add(new GameResult(scoreTypes, card, 2 * loseSize[0]));
                seat.setAngang(seat.getAngang() + 1);


                actionResponse.setOperationId(GameBase.ActionId.AN_GANG).setData(Mahjong.MahjongGang.newBuilder().setCard(card).build().toByteString());
                response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                seats.stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserId()))
                        .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserId()).send(response.build(), seat1.getUserId()));
                getCard(response, seat.getSeatNo(), redisService);
            } else if (1 == Card.containSize(seat.getPengCards(), card) && 1 == Card.containSize(seat.getCards(), card)) {//扒杠
                Card.remove(seat.getCards(), card);
                Card.remove(seat.getPengCards(), card);

                seat.getGangCards().add(card);

                List<ScoreType> scoreTypes = new ArrayList<>();
                scoreTypes.add(ScoreType.BA_GANG);

                final int[] loseSize = {0};
                seats.stream().filter(seat1 -> seat1.getSeatNo() != seat.getSeatNo())
                        .forEach(seat1 -> {
                            seat1.getGangResult().add(new GameResult(scoreTypes, card, -1));
                            loseSize[0]++;
                        });
                seat.getGangResult().add(new GameResult(scoreTypes, card, loseSize[0]));
                seat.setMinggang(seat.getMinggang() + 1);


                actionResponse.setOperationId(GameBase.ActionId.BA_GANG).setData(Mahjong.MahjongGang.newBuilder().setCard(card).build().toByteString());
                response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                seats.stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserId()))
                        .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserId()).send(response.build(), seat1.getUserId()));
                getCard(response, seat.getSeatNo(), redisService);
            }
        });
    }

    /**
     * 出牌后检查是否有人能胡、杠、碰
     *
     * @param card         当前出的牌
     * @param response
     * @param redisService
     * @param userId
     */

    public void checkCard(Integer card, GameBase.BaseConnection.Builder response, RedisService redisService, int userId) {
        System.out.println("出牌后检查是否有人能胡、杠、碰");
        GameBase.AskResponse.Builder builder = GameBase.AskResponse.newBuilder();
        //先检查胡，胡优先
        final boolean[] cannotOperation = {false};
        seats.stream().filter(seat -> seat.getSeatNo() != operationSeatNo).forEach(seat -> {
            builder.clearOperationId();
            List<Integer> temp = new ArrayList<>();
            temp.addAll(seat.getCards());

            //当前玩家手里有几张牌，3张可碰可杠，两张只能碰
            int containSize = Card.containSize(temp, card);
            if (3 == containSize && 0 < surplusCards.size()) {
                builder.addOperationId(GameBase.ActionId.PENG);
                builder.addOperationId(GameBase.ActionId.DIAN_GANG);
            } else if (2 == containSize) {
                builder.addOperationId(GameBase.ActionId.PENG);
            }
            //当前玩家是否可以胡牌
            temp.add(card);
            if (MahjongUtil.hu(temp)) {
                builder.addOperationId(GameBase.ActionId.HU);
            }
            if (0 != builder.getOperationIdCount()) {
                if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                    response.setOperationType(GameBase.OperationType.ASK).setData(builder.build().toByteString());
                    MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
                }
                cannotOperation[0] = true;
            }
        });

        if (!cannotOperation[0]) {
            //如果没有人可以胡、碰、杠，游戏继续，下家摸牌；
            getCard(response, getNextSeat(), redisService);
            //TODO 出牌超时
//                new OperationTimeout(deskNo, card).start();
        }
    }

    /**
     * 出牌后检查是否有人能胡、杠、碰
     *
     * @param card 当前出的牌
     */
    public void checkSeatCan(Integer card, GameBase.BaseConnection.Builder response, int userId) {
        GameBase.AskResponse.Builder builder = GameBase.AskResponse.newBuilder();
        //先检查胡，胡优先
        seats.stream().filter(seat -> seat.getUserId() == userId).forEach(seat -> {
            builder.clearOperationId();
            List<Integer> temp = new ArrayList<>();
            temp.addAll(seat.getCards());

            //当前玩家手里有几张牌，3张可碰可杠，两张只能碰
            int containSize = Card.containSize(temp, card);
            if (3 == containSize && 0 < surplusCards.size()) {
                builder.addOperationId(GameBase.ActionId.PENG);
                builder.addOperationId(GameBase.ActionId.DIAN_GANG);
            } else if (2 == containSize) {
                builder.addOperationId(GameBase.ActionId.PENG);
            }
            //当前玩家是否可以胡牌
            temp.add(card);
            if (MahjongUtil.hu(temp)) {
                builder.addOperationId(GameBase.ActionId.HU);
            }
            if (0 != builder.getOperationIdCount()) {
                if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                    response.setOperationType(GameBase.OperationType.ASK).setData(builder.build().toByteString());
                    MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
                }
                //TODO 出牌超时
//                new OperationTimeout(deskNo, card).start();
            }
        });
    }

    /**
     * 当有人胡、碰、杠后，再次检查是否还有人胡、碰、杠
     */
    public boolean checkSurplus() {
        //找到那张牌
        final Integer[] card = new Integer[1];
        seats.stream().filter(seat -> seat.getSeatNo() == operationSeatNo)
                .forEach(seat -> card[0] = seat.getPlayedCards().get(seat.getPlayedCards().size() - 1));
        final boolean[] hu = {false};
        //先检查胡，胡优先
        seats.stream().filter(seat -> seat.getSeatNo() != operationSeatNo).forEach(seat -> {
            List<Integer> temp = new ArrayList<>();
            temp.addAll(seat.getCards());

            //当前玩家是否可以胡牌
            temp.add(card[0]);
            if (MahjongUtil.hu(temp) && seat.getOperation() == 0) {
                hu[0] = true;
            }
        });
        return !hu[0];
    }

    /**
     * 检查是否还需要操作
     */
    public boolean passedChecked() {
        //找到那张牌
        final Integer[] card = new Integer[1];
        seats.stream().filter(seat -> seat.getSeatNo() == operationSeatNo)
                .forEach(seat -> card[0] = seat.getPlayedCards().get(seat.getPlayedCards().size() - 1));
        final boolean[] hasNoOperation = {false};
        //先检查胡，胡优先
        seats.stream().filter(seat -> seat.getSeatNo() != operationSeatNo).forEach(seat -> {
            List<Integer> temp = new ArrayList<>();
            temp.addAll(seat.getCards());

            //当前玩家是否可以胡牌
            temp.add(card[0]);
            if (MahjongUtil.hu(temp) && seat.getOperation() != 4) {
                hasNoOperation[0] = true;
            }

            //当前玩家手里有几张牌，3张可碰可杠，两张只能碰
            int containSize = Card.containSize(temp, card[0]);
            if (4 == containSize && 0 < surplusCards.size() && seat.getOperation() != 4) {
                hasNoOperation[0] = true;
            } else if (3 <= containSize && seat.getOperation() != 4) {
                hasNoOperation[0] = true;
            }
        });

        return hasNoOperation[0];
    }

    /**
     * 检测单个玩家是否可以碰或者港
     *
     * @param actionResponse
     * @param response
     * @param redisService
     * @param userId
     */
    public void pengOrGang(GameBase.BaseAction.Builder actionResponse, GameBase.BaseConnection.Builder response, RedisService redisService, int userId) {
        //找到那张牌
        final Integer[] card = new Integer[1];
        Seat operationSeat = null;
        for (Seat seat : seats) {
            if (seat.getSeatNo() == operationSeatNo) {
                card[0] = seat.getPlayedCards().get(seat.getPlayedCards().size() - 1);
                operationSeat = seat;
                break;
            }
        }

        for (Seat seat : seats) {
            if (seat.getSeatNo() != operationSeatNo) {
                List<Integer> temp = new ArrayList<>();
                temp.addAll(seat.getCards());

                //当前玩家手里有几张牌，3张可碰可杠，两张只能碰
                int containSize = Card.containSize(temp, card[0]);
                if (3 == containSize && 0 < surplusCards.size() && seat.getOperation() == 2) {//杠牌
                    Card.remove(seat.getCards(), card[0]);
                    Card.remove(seat.getCards(), card[0]);
                    Card.remove(seat.getCards(), card[0]);
                    seat.getGangCards().add(card[0]);

                    //添加结算
                    List<ScoreType> scoreTypes = new ArrayList<>();
                    scoreTypes.add(ScoreType.DIAN_GANG);
                    operationSeat.getGangResult().add(new GameResult(scoreTypes, card[0], -3));
                    seat.getGangResult().add(new GameResult(scoreTypes, card[0], 3));
                    seat.setMinggang(seat.getMinggang() + 1);
                    historyList.add(new OperationHistory(userId, OperationHistoryType.DIAN_GANG, card[0]));

                    actionResponse.setOperationId(GameBase.ActionId.DIAN_GANG).setData(Mahjong.MahjongGang.newBuilder()
                            .setCard(card[0]).build().toByteString());
                    response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                    seats.stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserId()))
                            .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserId()).send(response.build(), seat1.getUserId()));

                    operationSeat.getPlayedCards().remove(seat.getPlayedCards().size() - 1);
                    //点杠后需要摸牌
                    getCard(response, seat.getSeatNo(), redisService);
                    return;
                } else if (2 <= containSize && seat.getOperation() == 3) {//碰
                    Card.remove(seat.getCards(), card[0]);
                    Card.remove(seat.getCards(), card[0]);
                    seat.getPengCards().add(card[0]);
                    operationSeatNo = seat.getSeatNo();
                    historyList.add(new OperationHistory(userId, OperationHistoryType.PENG, card[0]));

                    actionResponse.setOperationId(GameBase.ActionId.PENG).setData(Mahjong.MahjongPengResponse.newBuilder().setCard(card[0]).build().toByteString());
                    response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                    seats.stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserId()))
                            .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserId()).send(response.build(), seat1.getUserId()));

                    GameBase.RoundResponse roundResponse = GameBase.RoundResponse.newBuilder().setID(seat.getUserId()).build();
                    response.setOperationType(GameBase.OperationType.ROUND).setData(roundResponse.toByteString());
                    seats.stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserId()))
                            .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserId()).send(response.build(), seat1.getUserId()));
                    return;
                }
            }
        }
    }

}
