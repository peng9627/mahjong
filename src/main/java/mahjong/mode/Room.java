package mahjong.mode;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import mahjong.constant.Constant;
import mahjong.entrance.MahjongTcpService;
import mahjong.redis.RedisService;
import mahjong.timeout.OperationTimeout;
import mahjong.timeout.PlayCardTimeout;
import mahjong.timeout.ReadyTimeout;
import mahjong.utils.HttpUtil;
import org.slf4j.LoggerFactory;

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
    private int ghost;//1.红中做鬼，2.无鬼，3.翻鬼，4.无鬼加倍
    private int gameRules;////游戏规则  高位到低位顺序（鸡胡，门清，天地和，幺九，全番，十三幺，对对胡，十八罗汉，七小对，清一色，混一色，海底捞，杠爆全包，庄硬）
    private Integer[] dice;//骰子
    private List<Record> recordList = new ArrayList<>();//战绩
    private int gameCount;

    private int initMaCount;

    private int roomOwner;
    private int continuityBanker;

    private Date startDate;
    private int fan;
    private int gui;
    private List<Integer> ma = new ArrayList<>();//买的马

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

    public int getGhost() {
        return ghost;
    }

    public void setGhost(int ghost) {
        this.ghost = ghost;
    }

    public int getGameRules() {
        return gameRules;
    }

    public void setGameRules(int gameRules) {
        this.gameRules = gameRules;
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

    public int getRoomOwner() {
        return roomOwner;
    }

    public void setRoomOwner(int roomOwner) {
        this.roomOwner = roomOwner;
    }

    public int getContinuityBanker() {
        return continuityBanker;
    }

    public void setContinuityBanker(int continuityBanker) {
        this.continuityBanker = continuityBanker;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public int getFan() {
        return fan;
    }

    public void setFan(int fan) {
        this.fan = fan;
    }

    public int getGui() {
        return gui;
    }

    public void setGui(int gui) {
        this.gui = gui;
    }

    public List<Integer> getMa() {
        return ma;
    }

    public void setMa(List<Integer> ma) {
        this.ma = ma;
    }

    public void addSeat(User user, int score) {
        Seat seat = new Seat();
        seat.setRobot(false);
        seat.setReady(false);
        seat.setAreaString(user.getArea());
        seat.setHead(user.getHead());
        seat.setNickname(user.getNickname());
        seat.setSex(user.getSex().equals("MAN"));
        seat.setScore(score);
        seat.setIp(user.getLastLoginIp());
        seat.setGameCount(user.getGameCount());
        seat.setSeatNo(seatNos.get(0));
        seatNos.remove(0);
        seat.setUserId(user.getUserId());
        seats.add(seat);
    }

    public void dealCard() {
        startDate = new Date();
        surplusCards = Card.getAllCard();
        //卖马 发牌
        for (Seat seat : seats) {
            seat.setReady(false);
            List<Integer> cardList = new ArrayList<>();
            int cardIndex;
//            if (seat.getSeatNo() == 1) {
//                cardIndex = new Random().nextInt(3);
//            } else {
//                cardIndex = (int) (Math.random() * surplusCards.size());
//            }
            for (int i = 0; i < 13; i++) {
                cardIndex = (int) (Math.random() * surplusCards.size());
//                cardIndex = new Random().nextInt(40);
                cardList.add(surplusCards.get(cardIndex));
                surplusCards.remove(cardIndex);
            }

            seat.setCards(cardList);
            seat.setInitialCards(cardList);

            if (seat.getUserId() == banker) {
                operationSeatNo = seat.getSeatNo();
                cardIndex = (int) (Math.random() * surplusCards.size());
                seat.getCards().add(surplusCards.get(cardIndex));
                surplusCards.remove(cardIndex);
            }
        }

        ma = new ArrayList<>();
        for (int i = 0; i < initMaCount; i++) {
            int cardIndex = (int) (Math.random() * surplusCards.size());
            ma.add(surplusCards.get(cardIndex));
            surplusCards.remove(cardIndex);
        }

        switch (ghost) {
            case 1://红中作鬼
                gui = 31;
                break;
            case 3://翻鬼
                int cardIndex = (int) (Math.random() * surplusCards.size());
                fan = surplusCards.remove(cardIndex);
                switch (fan / 10) {
                    case 0:
                    case 1:
                    case 2:
                        if (9 == fan % 10) {
                            gui = (fan / 10 * 10) + 1;
                        } else {
                            gui = fan + 1;
                        }
                        break;
                    case 3:
                        if (35 == fan) {
                            gui = 31;
                        } else {
                            gui = fan + 2;
                        }
                        break;
                    case 4:
                        if (47 == fan) {
                            gui = 41;
                        } else {
                            gui = fan + 2;
                        }
                        break;
                }
                break;
        }
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
        record.setBanker(banker);
        List<SeatRecord> seatRecords = new ArrayList<>();
        seats.forEach(seat -> {
            SeatRecord seatRecord = new SeatRecord();
            seatRecord.setUserId(seat.getUserId());
            seatRecord.setNickname(seat.getNickname());
            seatRecord.setHead(seat.getHead());
            seatRecord.setCardResult(seat.getCardResult());
            seatRecord.getMingGangResult().addAll(seat.getMingGangResult());
            seatRecord.getAnGangResult().addAll(seat.getAnGangResult());
            seatRecord.getInitialCards().addAll(seat.getInitialCards());
            seatRecord.getCards().addAll(seat.getCards());
            final int[] winOrLose = {0};
            seat.getMingGangResult().forEach(gameResult -> winOrLose[0] += gameResult.getScore());
            seat.getAnGangResult().forEach(gameResult -> winOrLose[0] += gameResult.getScore());
            if (null != seat.getCardResult()) {
                winOrLose[0] += seat.getCardResult().getScore();
            }
            seatRecord.setWinOrLose(winOrLose[0]);
            seatRecords.add(seatRecord);
        });
        record.setSeatRecordList(seatRecords);
        record.getHistoryList().addAll(historyList);
        recordList.add(record);

        historyList.clear();
        surplusCards.clear();
        gameStatus = GameStatus.READYING;
        lastOperation = 0;
        dice = null;
        seats.forEach(Seat::clear);
        startDate = new Date();
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

        GameBase.RoundResponse roundResponse = GameBase.RoundResponse.newBuilder().setID(username[0])
                .setTimeCounter(redisService.exists("room_match" + roomNo) ? 8 : 0).build();
        response.setOperationType(GameBase.OperationType.ROUND).setData(roundResponse.toByteString());
        seats.stream().filter(seat -> MahjongTcpService.userClients.containsKey(seat.getUserId()))
                .forEach(seat -> MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId()));

        checkSelfGetCard(response, operationSeat, redisService);
    }

    /**
     * 游戏结束
     *
     * @param response
     * @param redisService
     */
    public void gameOver(GameBase.BaseConnection.Builder response, RedisService redisService, int card) {

        Map<Integer, Integer> seatScore = new HashMap<>();
        Map<Integer, Integer> maScore = new HashMap<>();

        List<Integer> loseSeats = new ArrayList<>();

        Mahjong.MahjongResultResponse.Builder resultResponse = Mahjong.MahjongResultResponse.newBuilder();
        resultResponse.addAllMaCard(ma);
        resultResponse.setReadyTimeCounter(redisService.exists("room_match" + roomNo) ? 8 : 0);

        List<Integer> winSeats = new ArrayList<>();
        int maSeat = 0;
        for (Seat seat : seats) {
            if (banker == seat.getUserId()) {
                maSeat = seat.getSeatNo();
            }
            maScore.put(seat.getSeatNo(), 0);

            Mahjong.MahjongUserResult.Builder userResult = Mahjong.MahjongUserResult.newBuilder();
            userResult.setID(seat.getUserId());
            userResult.addAllCards(seat.getCards());
            userResult.addAllChiCards(seat.getChiCards());
            userResult.addAllPengCards(seat.getPengCards());
            userResult.addAllAnGangCards(seat.getAnGangCards());
            userResult.addAllMingGangCards(seat.getMingGangCards());
            if (null != seat.getCardResult()) {
                seatScore.put(seat.getSeatNo(), seat.getCardResult().getScore());
                userResult.setCardScore(seat.getCardResult().getScore());
                if (seat.getCardResult().getScore() > 0) {
                    winSeats.add(seat.getUserId());
                    userResult.setHuCard(card);
                } else {
                    loseSeats.add(seat.getSeatNo());
                }
                for (ScoreType scoreType : seat.getCardResult().getScoreTypes()) {
                    userResult.addScoreTypes(Mahjong.ScoreType.forNumber(scoreType.ordinal() - 3));
                }
            }
            int mingGangScore = 0;
            for (GameResult gameResult : seat.getMingGangResult()) {
                mingGangScore += gameResult.getScore();
            }
            int anGangScore = 0;
            for (GameResult gameResult : seat.getAnGangResult()) {
                anGangScore += gameResult.getScore();
            }
            userResult.setMingGangScore(mingGangScore);
            userResult.setAnGangScore(anGangScore);
            resultResponse.addUserResult(userResult);
        }

        int tempBanker = 0;
        if (0 == winSeats.size()) {
            resultResponse.clearMaCard();
            for (Mahjong.MahjongUserResult.Builder userResult : resultResponse.getUserResultBuilderList()) {
                userResult.setMingGangScore(0);
                userResult.setAnGangScore(0);
            }
        }

        if (1 == winSeats.size()) {
            tempBanker = winSeats.get(0);

//        } else if (1 == loseSeats.size()) {
//            tempBanker = loseSeats.get(0);
        } else {
            for (int i = 0; i < seats.size(); i++) {
                if (seats.get(i).getUserId() == banker) {
                    if (i == seats.size() - 1) {
                        tempBanker = seats.get(0).getUserId();
                    } else {
                        tempBanker = seats.get(i + 1).getUserId();
                    }
                    break;
                }
            }
        }

        if (banker == tempBanker) {
            continuityBanker++;
        } else {
            continuityBanker = 0;
        }
        banker = tempBanker;

        int zhongMa = 0;
        int maUser = 0;
        if (winSeats.size() == 1) {
            for (Seat seat : seats) {
                if (seat.getUserId() == winSeats.get(0)) {
                    zhongMa = MahjongUtil.GetLuckMa(seat.getSeatNo(), maSeat, count, ma).size();
                    break;
                }
            }
            maUser = winSeats.get(0);
        }
        if (winSeats.size() > 1) {
            maUser = loseSeats.get(0);
            zhongMa = MahjongUtil.GetLuckMa(loseSeats.get(0), maSeat, count, ma).size();
        }

//        if (0 != maUser) {
//            for (Seat seat : seats) {
//                if (maUser == seat.getUserId()) {
//                    //本家
//                    if (seat.getSeatNo() == maSeat) {
//                        for (Integer ma : ma) {
//                            if (1 == Card.containSize(Card.ma_my(), ma)) {
//                                zhongMa++;
//                            }
//                        }
//                    }
//                    //下家
//                    if (seat.getSeatNo() - 1 == maSeat || seat.getSeatNo() + 3 == maSeat) {
//                        for (Integer ma : ma) {
//                            if (1 == Card.containSize(Card.ma_next(), ma)) {
//                                zhongMa++;
//                            }
//                        }
//                    }
//                    //对家
//                    if (seat.getSeatNo() - 2 == maSeat || seat.getSeatNo() + 2 == maSeat) {
//                        for (Integer ma : ma) {
//                            if (1 == Card.containSize(Card.ma_my(), ma)) {
//                                zhongMa++;
//                            }
//                        }
//                    }
//                    //上家
//                    if (seat.getSeatNo() - 3 == maSeat || seat.getSeatNo() + 1 == maSeat) {
//                        for (Integer ma : ma) {
//                            if (1 == Card.containSize(Card.ma_my(), ma)) {
//                                zhongMa++;
//                            }
//                        }
//                    }
//                }
//            }
//        }

        if (winSeats.size() == 1) {
            for (Integer loseSeat : loseSeats) {
                maScore.put(loseSeat, -2 * zhongMa);
            }
            for (Seat seat : seats) {
                if (seat.getUserId() == winSeats.get(0)) {
                    maScore.put(seat.getSeatNo(), 2 * zhongMa * loseSeats.size());
                    break;
                }
            }
        }
        if (winSeats.size() > 1) {
            for (Seat seat : seats) {
                if (winSeats.contains(seat.getUserId())) {
                    maScore.put(seat.getSeatNo(), 2 * zhongMa);
                }
            }
            for (Seat seat : seats) {
                if (seat.getSeatNo() == loseSeats.get(0)) {
                    maScore.put(seat.getSeatNo(), -2 * zhongMa * winSeats.size());
                    break;
                }
            }
        }

//        for (Seat seat : seats) {
//            for (Integer ma : seat.getMa()) {
//                int maiSeat = getMaiSeat(seat.getSeatNo(), ma);
//                if (seatScore.containsKey(maiSeat)) {
//                    if (seatScore.get(maiSeat) > 0) {//买中赢家
//                        maScore.put(seat.getSeatNo(), maScore.get(seat.getSeatNo()) + (2 * loseSeats.size()));
//                        for (int loseSeat : loseSeats) {
//                            maScore.put(loseSeat, maScore.get(loseSeat) - 2);
//                        }
//                    } else {//买中输家
//                        maScore.put(seat.getSeatNo(), maScore.get(seat.getSeatNo()) - 2 * winSeats.size());
//                        for (Seat seat1 : seats) {
//                            if (winSeats.contains(seat1.getUserId())) {
//                                maScore.put(seat1.getSeatNo(), maScore.get(seat1.getSeatNo()) + 2);
//                            }
//                        }
//                    }
//                }
//            }
//        }

        for (Seat seat : seats) {
            if (maScore.containsKey(seat.getSeatNo())) {
                maScore.put(seat.getUserId(), maScore.get(seat.getSeatNo()));
                maScore.remove(seat.getSeatNo());
            }
        }

        for (Mahjong.MahjongUserResult.Builder userResult : resultResponse.getUserResultBuilderList()) {
            if (maScore.containsKey(userResult.getID())) {
                userResult.setMaScore(maScore.get(userResult.getID()));
            }
        }
        if (1 == (gameRules >> 12) % 2) {
            if (2 < historyList.size()) {
                OperationHistory operationHistory = historyList.get(historyList.size() - 2);
                if (0 == operationHistory.getHistoryType().compareTo(OperationHistoryType.DIAN_GANG)) {
                    int baoCardScore = 0;
                    int baoGangScore = 0;
                    int baoMaScore = 0;
                    for (Mahjong.MahjongUserResult.Builder userResult : resultResponse.getUserResultBuilderList()) {
                        if (operationHistory.getUserId() != userResult.getID()) {
                            if (userResult.getCardScore() < 0) {
                                baoCardScore += userResult.getCardScore();
                                userResult.setCardScore(0);
                            }
                            if (userResult.getMingGangScore() < 0) {
                                baoGangScore += userResult.getMingGangScore();
                                userResult.setMingGangScore(0);
                            }
                            if (userResult.getAnGangScore() < 0) {
                                baoGangScore += userResult.getAnGangScore();
                                userResult.setAnGangScore(0);
                            }
                            if (userResult.getMaScore() < 0) {
                                baoMaScore += userResult.getMaScore();
                                userResult.setMaScore(0);
                            }
                        }
                    }
                    for (Mahjong.MahjongUserResult.Builder userResult : resultResponse.getUserResultBuilderList()) {
                        if (operationHistory.getUserId() == userResult.getID()) {
                            userResult.getScoreTypesList().add(Mahjong.ScoreType.GANGBAO);
                            userResult.setCardScore(userResult.getCardScore() + baoCardScore);
                            userResult.setMingGangScore(userResult.getMingGangScore() + baoGangScore);
                            userResult.setAnGangScore(userResult.getAnGangScore() + baoGangScore);
                            userResult.setMaScore(userResult.getMaScore() + baoMaScore);
                        }
                    }
                    for (Seat seat : seats) {
                        if (seat.getUserId() == operationHistory.getUserId()) {
                            seat.getCardResult().getScoreTypes().add(ScoreType.GANGBAO);
                        }
                    }
                }
            }
        }

        for (Mahjong.MahjongUserResult.Builder userResult : resultResponse.getUserResultBuilderList()) {
            int win = userResult.getCardScore() + userResult.getMingGangScore() + userResult.getAnGangScore() + userResult.getMaScore();
            userResult.setWinOrLose(win);
            for (Seat seat : seats) {
                if (seat.getUserId() == userResult.getID()) {
                    seat.setScore(seat.getScore() + win);
                    userResult.setScore(seat.getScore());
                    break;
                }
            }
        }

        if (redisService.exists("room_match" + roomNo)) {
            GameBase.ScoreResponse.Builder scoreResponse = GameBase.ScoreResponse.newBuilder();
            for (Mahjong.MahjongUserResult.Builder userResult : resultResponse.getUserResultBuilderList()) {
                if (MahjongTcpService.userClients.containsKey(userResult.getID())) {
                    int win = userResult.getCardScore() + userResult.getMingGangScore() + userResult.getAnGangScore() + userResult.getMaScore();
                    GameBase.MatchResult matchResult;
                    if (gameCount != gameTimes) {
                        matchResult = GameBase.MatchResult.newBuilder().setResult(0).setCurrentScore(win)
                                .setTotalScore(userResult.getScore()).build();
                    } else {
                        matchResult = GameBase.MatchResult.newBuilder().setResult(2).setCurrentScore(win)
                                .setTotalScore(userResult.getScore()).build();
                    }
                    MahjongTcpService.userClients.get(userResult.getID()).send(response.setOperationType(GameBase.OperationType.MATCH_RESULT)
                            .setData(matchResult.toByteString()).build(), userResult.getID());
                }
                scoreResponse.addScoreResult(GameBase.ScoreResult.newBuilder().setID(userResult.getID()).setScore(userResult.getScore()));
            }
            for (Seat seat : seats) {
                if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                    MahjongTcpService.userClients.get(seat.getUserId()).send(response.setOperationType(GameBase.OperationType.MATCH_SCORE)
                            .setData(scoreResponse.build().toByteString()).build(), seat.getUserId());
                }
            }
        } else {
            response.setOperationType(GameBase.OperationType.RESULT).setData(resultResponse.build().toByteString());
            seats.stream().filter(seat -> MahjongTcpService.userClients.containsKey(seat.getUserId()))
                    .forEach(seat -> MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId()));
        }
        clear();
        //结束房间
        if (gameCount == gameTimes) {
            roomOver(response, redisService);
        } else {
            if (redisService.exists("room_match" + roomNo)) {
                new ReadyTimeout(Integer.valueOf(roomNo), redisService, gameCount).start();
            }
        }
    }

    public void roomOver(GameBase.BaseConnection.Builder response, RedisService redisService) {
        JSONObject jsonObject = new JSONObject();
        //是否竞技场
        if (redisService.exists("room_match" + roomNo)) {
            String matchNo = redisService.getCache("room_match" + roomNo);
            redisService.delete("room_match" + roomNo);
            if (redisService.exists("match_info" + matchNo)) {
                while (!redisService.lock("lock_match_info" + matchNo)) {
                }
                GameBase.MatchResult.Builder matchResult = GameBase.MatchResult.newBuilder();
                MatchInfo matchInfo = JSON.parseObject(redisService.getCache("match_info" + matchNo), MatchInfo.class);
                Arena arena = matchInfo.getArena();

                //移出当前桌
                List<Integer> rooms = matchInfo.getRooms();
                for (Integer integer : rooms) {
                    if (integer == Integer.parseInt(roomNo)) {
                        rooms.remove(integer);
                        break;
                    }
                }

                //等待的人
                List<MatchUser> waitUsers = matchInfo.getWaitUsers();
                if (null == waitUsers) {
                    waitUsers = new ArrayList<>();
                    matchInfo.setWaitUsers(waitUsers);
                }
                //在比赛中的人 重置分数
                List<MatchUser> matchUsers = matchInfo.getMatchUsers();
                for (Seat seat : seats) {
                    redisService.delete("reconnect" + seat.getUserId());
                    for (MatchUser matchUser : matchUsers) {
                        if (seat.getUserId() == matchUser.getUserId()) {
                            matchUser.setScore(seat.getScore());
                        }
                    }
//                    if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
//                        MahjongTcpService.userClients.get(seat.getUserId()).send(response.setOperationType(GameBase.OperationType.ROOM_INFO).clearData().build(), seat.getUserId());
//                        GameBase.RoomSeatsInfo.Builder roomSeatsInfo = GameBase.RoomSeatsInfo.newBuilder();
//                        GameBase.SeatResponse.Builder seatResponse = GameBase.SeatResponse.newBuilder();
//                        seatResponse.setSeatNo(1);
//                        seatResponse.setID(seat.getUserId());
//                        seatResponse.setScore(seat.getScore());
//                        seatResponse.setReady(false);
//                        seatResponse.setIp(seat.getIp());
//                        seatResponse.setGameCount(seat.getGameCount());
//                        seatResponse.setNickname(seat.getNickname());
//                        seatResponse.setHead(seat.getHead());
//                        seatResponse.setSex(seat.isSex());
//                        seatResponse.setOffline(false);
//                        seatResponse.setIsRobot(seat.isRobot());
//                        roomSeatsInfo.addSeats(seatResponse.build());
//                        MahjongTcpService.userClients.get(seat.getUserId()).send(response.setOperationType(GameBase.OperationType.SEAT_INFO).setData(roomSeatsInfo.build().toByteString()).build(), seat.getUserId());
//                    }
                }

                //用户对应分数
                Map<Integer, Integer> userIdScore = new HashMap<>();
                for (MatchUser matchUser : matchUsers) {
                    userIdScore.put(matchUser.getUserId(), matchUser.getScore());
                }

                GameBase.MatchData.Builder matchData = GameBase.MatchData.newBuilder();
                switch (matchInfo.getStatus()) {
                    case 1:

                        //根据金币排序
                        seats.sort(new Comparator<Seat>() {
                            @Override
                            public int compare(Seat o1, Seat o2) {
                                return o1.getScore() > o2.getScore() ? 1 : -1;
                            }
                        });

                        //本局未被淘汰的
                        List<MatchUser> thisWait = new ArrayList<>();
                        //循环座位，淘汰
                        for (Seat seat : seats) {
                            for (MatchUser matchUser : matchUsers) {
                                if (matchUser.getUserId() == seat.getUserId()) {
                                    if (seat.getScore() < matchInfo.getMatchEliminateScore() && matchUsers.size() > arena.getCount() / 2) {
                                        matchUsers.remove(matchUser);

                                        matchResult.setResult(3).setTotalScore(seat.getScore()).setCurrentScore(-1);
                                        response.setOperationType(GameBase.OperationType.MATCH_RESULT).setData(matchResult.build().toByteString());
                                        if (MahjongTcpService.userClients.containsKey(matchUser.getUserId())) {
                                            MahjongTcpService.userClients.get(matchUser.getUserId()).send(response.build(), matchUser.getUserId());
                                        }
                                        response.setOperationType(GameBase.OperationType.MATCH_BALANCE).setData(GameBase.MatchBalance.newBuilder()
                                                .setRanking(matchUsers.size()).setTotalScore(matchUser.getScore()).build().toByteString());
                                        if (MahjongTcpService.userClients.containsKey(matchUser.getUserId())) {
                                            MahjongTcpService.userClients.get(matchUser.getUserId()).send(response.build(), matchUser.getUserId());
                                            GameBase.OverResponse.Builder over = GameBase.OverResponse.newBuilder();
                                            String uuid = UUID.randomUUID().toString().replace("-", "");
                                            while (redisService.exists(uuid)) {
                                                uuid = UUID.randomUUID().toString().replace("-", "");
                                            }
                                            redisService.addCache("backkey" + uuid, seat.getUserId() + "", 1800);
                                            over.setBackKey(uuid);
                                            over.setDateTime(new Date().getTime());
                                            response.setOperationType(GameBase.OperationType.OVER).setData(over.build().toByteString());
                                            MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
                                        }

                                        redisService.delete("reconnect" + seat.getUserId());
                                    } else {
                                        thisWait.add(matchUser);
                                        redisService.addCache("reconnect" + seat.getUserId(), "xingning_mahjong," + matchNo);
                                    }
                                    break;
                                }
                            }
                        }

                        //淘汰人数以满
                        int count = matchUsers.size();
                        if (count == arena.getCount() / 2 && 0 == rooms.size()) {
                            waitUsers.clear();
                            List<User> users = new ArrayList<>();
                            StringBuilder stringBuilder = new StringBuilder();
                            for (MatchUser matchUser : matchUsers) {
                                stringBuilder.append(",").append(matchUser.getUserId());
                            }
                            jsonObject.clear();
                            jsonObject.put("userIds", stringBuilder.toString().substring(1));
                            ApiResponse<List<User>> usersResponse = JSON.parseObject(HttpUtil.urlConnectionByRsa(Constant.apiUrl + Constant.userListUrl, jsonObject.toJSONString()),
                                    new TypeReference<ApiResponse<List<User>>>() {
                                    });
                            if (0 == usersResponse.getCode()) {
                                users = usersResponse.getData();
                            }

                            //第二轮开始
                            matchInfo.setStatus(2);
                            matchData.setStatus(2);
                            matchData.setCurrentCount(matchUsers.size());
                            matchData.setRound(1);
                            while (4 <= users.size()) {
                                rooms.add(matchInfo.addRoom(matchNo, 2, redisService, users.subList(0, 4), userIdScore, response, matchData));
                            }
                        } else if (count > arena.getCount() / 2) {
                            //满四人继续匹配
                            waitUsers.addAll(thisWait);
                            while (4 <= waitUsers.size()) {
                                //剩余用户
                                List<User> users = new ArrayList<>();
                                StringBuilder stringBuilder = new StringBuilder();
                                for (int i = 0; i < 4; i++) {
                                    stringBuilder.append(",").append(waitUsers.remove(0).getUserId());
                                }
                                jsonObject.clear();
                                jsonObject.put("userIds", stringBuilder.toString().substring(1));
                                ApiResponse<List<User>> usersResponse = JSON.parseObject(HttpUtil.urlConnectionByRsa(Constant.apiUrl + Constant.userListUrl, jsonObject.toJSONString()),
                                        new TypeReference<ApiResponse<List<User>>>() {
                                        });
                                if (0 == usersResponse.getCode()) {
                                    users = usersResponse.getData();
                                }
                                matchData.setStatus(1);
                                matchData.setCurrentCount(matchUsers.size());
                                matchData.setRound(1);
                                rooms.add(matchInfo.addRoom(matchNo, 1, redisService, users, userIdScore, response, matchData));
                            }
                        }
                        break;
                    case 2:
                    case 3:
                        for (Seat seat : seats) {
                            redisService.addCache("reconnect" + seat.getUserId(), "xingning_mahjong," + matchNo);
                        }
                        if (0 == rooms.size()) {
                            matchInfo.setStatus(matchInfo.getStatus() + 1);
                            matchData.setStatus(2);

                            List<User> users = new ArrayList<>();
                            StringBuilder stringBuilder = new StringBuilder();
                            for (MatchUser matchUser : matchUsers) {
                                stringBuilder.append(",").append(matchUser.getUserId());
                            }
                            jsonObject.clear();
                            jsonObject.put("userIds", stringBuilder.toString().substring(1));
                            ApiResponse<List<User>> usersResponse = JSON.parseObject(HttpUtil.urlConnectionByRsa(Constant.apiUrl + Constant.userListUrl, jsonObject.toJSONString()),
                                    new TypeReference<ApiResponse<List<User>>>() {
                                    });
                            if (0 == usersResponse.getCode()) {
                                users = usersResponse.getData();
                            }
                            matchData.setCurrentCount(matchUsers.size());
                            matchData.setRound(matchInfo.getStatus() - 1);
                            while (4 <= users.size()) {
                                rooms.add(matchInfo.addRoom(matchNo, 2, redisService, users.subList(0, 4), userIdScore, response, matchData));
                            }
                        }
                        break;
                    case 4:
                        for (Seat seat : seats) {
                            MatchUser matchUser = new MatchUser();
                            matchUser.setUserId(seat.getUserId());
                            matchUser.setScore(seat.getScore());
                            waitUsers.add(matchUser);
                            redisService.addCache("reconnect" + seat.getUserId(), "xingning_mahjong," + matchNo);
                        }

                        waitUsers.sort(new Comparator<MatchUser>() {
                            @Override
                            public int compare(MatchUser o1, MatchUser o2) {
                                return o1.getScore() > o2.getScore() ? -1 : 1;
                            }
                        });
                        while (waitUsers.size() > 4) {
                            MatchUser matchUser = waitUsers.remove(waitUsers.size() - 1);

                            response.setOperationType(GameBase.OperationType.MATCH_BALANCE).setData(GameBase.MatchBalance.newBuilder()
                                    .setRanking(matchUsers.size()).setTotalScore(matchUser.getScore()).build().toByteString());
                            if (MahjongTcpService.userClients.containsKey(matchUser.getUserId())) {
                                MahjongTcpService.userClients.get(matchUser.getUserId()).send(response.build(), matchUser.getUserId());
                                GameBase.OverResponse.Builder over = GameBase.OverResponse.newBuilder();
                                String uuid = UUID.randomUUID().toString().replace("-", "");
                                while (redisService.exists(uuid)) {
                                    uuid = UUID.randomUUID().toString().replace("-", "");
                                }
                                redisService.addCache("backkey" + uuid, matchUser.getUserId() + "", 1800);
                                over.setBackKey(uuid);
                                over.setDateTime(new Date().getTime());
                                response.setOperationType(GameBase.OperationType.OVER).setData(over.build().toByteString());
                                MahjongTcpService.userClients.get(matchUser.getUserId()).send(response.build(), matchUser.getUserId());
                            }
                            redisService.delete("reconnect" + matchUser.getUserId());
                        }

                        if (0 == rooms.size()) {

                            matchUsers.clear();
                            matchUsers.addAll(waitUsers);
                            waitUsers.clear();

                            matchInfo.setStatus(5);
                            matchData.setStatus(3);

                            List<User> users = new ArrayList<>();
                            StringBuilder stringBuilder = new StringBuilder();
                            for (MatchUser matchUser : matchUsers) {
                                stringBuilder.append(",").append(matchUser.getUserId());
                            }
                            jsonObject.clear();
                            jsonObject.put("userIds", stringBuilder.toString().substring(1));
                            ApiResponse<List<User>> usersResponse = JSON.parseObject(HttpUtil.urlConnectionByRsa(Constant.apiUrl + Constant.userListUrl, jsonObject.toJSONString()),
                                    new TypeReference<ApiResponse<List<User>>>() {
                                    });
                            if (0 == usersResponse.getCode()) {
                                users = usersResponse.getData();
                            }
                            matchData.setCurrentCount(matchUsers.size());
                            matchData.setRound(1);
                            while (4 == users.size()) {
                                rooms.add(matchInfo.addRoom(matchNo, 2, redisService, users, userIdScore, response, matchData));
                            }
                        }
                        break;
                    case 5:
                        matchUsers.sort(new Comparator<MatchUser>() {
                            @Override
                            public int compare(MatchUser o1, MatchUser o2) {
                                return o1.getScore() > o2.getScore() ? -1 : 1;
                            }
                        });
                        for (int i = 0; i < matchUsers.size(); i++) {
                            matchResult.setResult(i == 0 ? 1 : 3).setTotalScore(matchUsers.get(i).getScore()).setCurrentScore(-1);
                            response.setOperationType(GameBase.OperationType.MATCH_RESULT).setData(matchResult.build().toByteString());
                            if (MahjongTcpService.userClients.containsKey(matchUsers.get(i).getUserId())) {
                                MahjongTcpService.userClients.get(matchUsers.get(i).getUserId()).send(response.build(), matchUsers.get(i).getUserId());
                            }
                            response.setOperationType(GameBase.OperationType.MATCH_BALANCE).setData(GameBase.MatchBalance.newBuilder()
                                    .setRanking(i + 1).setTotalScore(matchUsers.get(i).getScore()).build().toByteString());
                            if (MahjongTcpService.userClients.containsKey(matchUsers.get(i).getUserId())) {
                                MahjongTcpService.userClients.get(matchUsers.get(i).getUserId()).send(response.build(), matchUsers.get(i).getUserId());
                                GameBase.OverResponse.Builder over = GameBase.OverResponse.newBuilder();
                                String uuid = UUID.randomUUID().toString().replace("-", "");
                                while (redisService.exists(uuid)) {
                                    uuid = UUID.randomUUID().toString().replace("-", "");
                                }
                                redisService.addCache("backkey" + uuid, matchUsers.get(i).getUserId() + "", 1800);
                                over.setBackKey(uuid);
                                over.setDateTime(new Date().getTime());
                                response.setOperationType(GameBase.OperationType.OVER).setData(over.build().toByteString());
                                MahjongTcpService.userClients.get(matchUsers.get(i).getUserId()).send(response.build(), matchUsers.get(i).getUserId());
                            }
                        }
                        matchInfo.setStatus(-1);
                        break;
                }
                if (0 < matchInfo.getStatus()) {
                    matchInfo.setMatchUsers(matchUsers);
                    matchInfo.setRooms(rooms);
                    matchInfo.setWaitUsers(waitUsers);
                    redisService.addCache("match_info" + matchNo, JSON.toJSONString(matchInfo));
                }
                redisService.unlock("lock_match_info" + matchNo);
            }
        } else {
            if (0 == gameStatus.compareTo(GameStatus.WAITING)) {
                jsonObject.clear();
                jsonObject.put("flowType", 1);
                if (8 == gameTimes) {
                    jsonObject.put("money", 1);
                } else {
                    jsonObject.put("money", 2);
                }
                jsonObject.put("description", "开房间退回" + roomNo);
                jsonObject.put("userId", roomOwner);
                ApiResponse moneyDetail = JSON.parseObject(HttpUtil.urlConnectionByRsa(Constant.apiUrl + Constant.moneyDetailedCreate, jsonObject.toJSONString()), new TypeReference<ApiResponse<User>>() {
                });
                if (0 != moneyDetail.getCode()) {
                    LoggerFactory.getLogger(this.getClass()).error(Constant.apiUrl + Constant.moneyDetailedCreate + "?" + jsonObject.toJSONString());
                }
            }

            Mahjong.MahjongBalanceResponse.Builder balance = Mahjong.MahjongBalanceResponse.newBuilder();
            for (Seat seat : seats) {
                Mahjong.MahjongSeatGameBalance.Builder seatGameOver = Mahjong.MahjongSeatGameBalance.newBuilder()
                        .setID(seat.getUserId()).setMinggang(seat.getMinggang()).setAngang(seat.getAngang())
                        .setZimoCount(seat.getZimoCount()).setHuCount(seat.getHuCount())
                        .setDianpaoCount(seat.getDianpaoCount()).setWinOrLose(seat.getScore());
                balance.addGameBalance(seatGameOver);
            }

            StringBuilder people = new StringBuilder();

            GameBase.OverResponse.Builder over = GameBase.OverResponse.newBuilder();
            for (Seat seat : seats) {
                people.append(",").append(seat.getUserId());
                redisService.delete("reconnect" + seat.getUserId());
                if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                    String uuid = UUID.randomUUID().toString().replace("-", "");
                    while (redisService.exists(uuid)) {
                        uuid = UUID.randomUUID().toString().replace("-", "");
                    }
                    redisService.addCache("backkey" + uuid, seat.getUserId() + "", 1800);
                    over.setBackKey(uuid);
                    over.setDateTime(new Date().getTime());

                    response.setOperationType(GameBase.OperationType.BALANCE).setData(balance.build().toByteString());
                    MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
                    response.setOperationType(GameBase.OperationType.OVER).setData(over.build().toByteString());
                    MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
                }
            }

            if (0 != recordList.size()) {
                List<TotalScore> totalScores = new ArrayList<>();
                for (Seat seat : seats) {
                    TotalScore totalScore = new TotalScore();
                    totalScore.setHead(seat.getHead());
                    totalScore.setNickname(seat.getNickname());
                    totalScore.setUserId(seat.getUserId());
                    totalScore.setScore(seat.getScore());
                    totalScores.add(totalScore);
                }
                SerializerFeature[] features = new SerializerFeature[]{SerializerFeature.WriteNullListAsEmpty,
                        SerializerFeature.WriteMapNullValue, SerializerFeature.DisableCircularReferenceDetect,
                        SerializerFeature.WriteNullStringAsEmpty, SerializerFeature.WriteNullNumberAsZero,
                        SerializerFeature.WriteNullBooleanAsFalse};
                int feature = SerializerFeature.config(JSON.DEFAULT_GENERATE_FEATURE, SerializerFeature.WriteEnumUsingName, false);
                jsonObject.clear();
                jsonObject.put("gameType", 0);
                jsonObject.put("roomOwner", roomOwner);
                jsonObject.put("people", people.toString().substring(1));
                jsonObject.put("gameTotal", gameTimes);
                jsonObject.put("gameCount", gameCount);
                jsonObject.put("peopleCount", count);
                jsonObject.put("roomNo", Integer.parseInt(roomNo));
                JSONObject gameRule = new JSONObject();
                gameRule.put("ghost", ghost);
                gameRule.put("gameRule", gameRules);
                gameRule.put("initMaCount", initMaCount);
                jsonObject.put("gameRule", gameRule.toJSONString());
                jsonObject.put("gameData", JSON.toJSONString(recordList, feature, features).getBytes());
                jsonObject.put("scoreData", JSON.toJSONString(totalScores, feature, features).getBytes());

                ApiResponse apiResponse = JSON.parseObject(HttpUtil.urlConnectionByRsa(Constant.apiUrl + Constant.gamerecordCreateUrl, jsonObject.toJSONString()), ApiResponse.class);
                if (0 != apiResponse.getCode()) {
                    LoggerFactory.getLogger(this.getClass()).error(Constant.apiUrl + Constant.gamerecordCreateUrl + "?" + jsonObject.toJSONString());
                }
            }
        }

        //删除该桌
        redisService.delete("room" + roomNo);
        redisService.delete("room_type" + roomNo);
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
    public void checkSelfGetCard(GameBase.BaseConnection.Builder response, Seat seat, RedisService redisService) {
        GameBase.AskResponse.Builder builder = GameBase.AskResponse.newBuilder();
        builder.setTimeCounter(redisService.exists("room_match" + roomNo) ? 8 : 0);
        if (MahjongUtil.checkHu(seat.getCards(), gameRules, gui)) {
            builder.addOperationId(GameBase.ActionId.HU);
            if (redisService.exists("room_match" + roomNo)) {
                new OperationTimeout(seat.getUserId(), roomNo, historyList.size(), gameCount, redisService, true).start();
            }
        }
        //暗杠
        if (null != MahjongUtil.checkGang(seat.getCards()) && 0 < surplusCards.size()) {
            builder.addOperationId(GameBase.ActionId.AN_GANG);
        }
        //扒杠
        if (null != MahjongUtil.checkBaGang(seat.getCards(), seat.getPengCards()) && 0 < surplusCards.size()) {
            builder.addOperationId(GameBase.ActionId.BA_GANG);
        }
        if (0 != builder.getOperationIdCount()) {
            if (redisService.exists("room_match" + roomNo) && !builder.getOperationIdList().contains(GameBase.ActionId.HU)) {
                new OperationTimeout(seat.getUserId(), roomNo, historyList.size(), gameCount, redisService, false).start();
            }
            if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                response.clear();
                response.setOperationType(GameBase.OperationType.ASK).setData(builder.build().toByteString());
                MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
            }
        } else {
            if (redisService.exists("room_match" + roomNo)) {
                new PlayCardTimeout(seat.getUserId(), roomNo, historyList.size(), gameCount, redisService).start();
            }
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
        if (MahjongUtil.checkHu(huSeat[0].getCards(), gameRules, gui)) {
            if (0 < historyList.size()) {
                if (0 != historyList.get(historyList.size() - 1).getHistoryType().compareTo(OperationHistoryType.GET_CARD)
                        || historyList.get(historyList.size() - 1).getUserId() != userId) {
                    return;
                }
            }
            historyList.add(new OperationHistory(huSeat[0].getUserId(), OperationHistoryType.HU, huSeat[0].getCards().get(huSeat[0].getCards().size() - 1)));
            List<Integer> gangCards = new ArrayList<>();
            gangCards.addAll(huSeat[0].getAnGangCards());
            gangCards.addAll(huSeat[0].getMingGangCards());

            List<ScoreType> scoreTypes = MahjongUtil.getHuType(huSeat[0].getCards(), huSeat[0].getPengCards(), gangCards, gameRules, gui);
            int score = MahjongUtil.getScore(scoreTypes);
            if (0 == score) {
                if (1 == gameRules % 2) {
                    score = 1;
                } else {
                    score = 2;
                }
            }

            //天胡
            if (historyList.size() == 0 && score < 20 && 1 == (gameRules >> 2) % 2) {
                scoreTypes.clear();
                scoreTypes.add(ScoreType.TIAN_HU);
                score = 20;
            } else {
                if (0 == surplusCards.size() && 1 == (gameRules >> 11) % 2) {
                    scoreTypes.add(ScoreType.HAIDI);
                    score *= 2;
                }
            }
            if (banker == userId && 0 < continuityBanker) {
                score *= 2;
                scoreTypes.add(ScoreType.ZHUANGYING);
            }
            if (1 == (gameRules >> 14) % 2 && 0 == Card.containSize(huSeat[0].getCards(), gui)) {
                score *= 2;
            }
            int loseSize[] = {0};
            int finalScore = score;
            seats.stream().filter(seat -> seat.getUserId() != userId)
                    .forEach(seat -> {
                        seat.setCardResult(new GameResult(scoreTypes, huSeat[0].getCards().get(huSeat[0].getCards().size() - 1), -finalScore));
                        loseSize[0]++;
                    });

            huSeat[0].setCardResult(new GameResult(scoreTypes, huSeat[0].getCards().get(huSeat[0].getCards().size() - 1), loseSize[0] * score));
            huSeat[0].setZimoCount(huSeat[0].getZimoCount() + 1);

            response.setOperationType(GameBase.OperationType.ACTION).setData(GameBase.BaseAction.newBuilder().setOperationId(GameBase.ActionId.HU)
                    .setID(huSeat[0].getUserId()).setData(Mahjong.MahjongHuResponse.newBuilder().setCard(huSeat[0].getCards().size() - 1)
                            .build().toByteString()).build().toByteString());
            seats.stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserId()))
                    .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserId()).send(response.build(), seat1.getUserId()));

            gameOver(response, redisService, huSeat[0].getCards().get(huSeat[0].getCards().size() - 1));
            return;
        }

//        //找到那张牌
//        final Integer[] card = new Integer[1];
//        Seat operationSeat = null;
//        for (Seat seat : seats) {
//            if (seat.getSeatNo() == operationSeatNo) {
//                card[0] = seat.getPlayedCards().get(seat.getPlayedCards().size() - 1);
//                operationSeat = seat;
//                break;
//            }
//        }
//
//        //先检查胡，胡优先
//        boolean hu = false;
//        for (Seat seat : seats) {
//            if (seat.getSeatNo() != operationSeatNo) {
//                List<Integer> temp = new ArrayList<>();
//                temp.addAll(seat.getCards());
//
//                //当前玩家是否可以胡牌
//                temp.add(card[0]);
//                if (MahjongUtil.checkHu(temp, gameRules, gui)) {
//
//                    List<Integer> gangCards = new ArrayList<>();
//                    gangCards.addAll(seat.getAnGangCards());
//                    gangCards.addAll(seat.getMingGangCards());
//
//                    List<ScoreType> scoreTypes = MahjongUtil.getHuType(huSeat[0].getCards(), seat.getPengCards(), gangCards, gameRules);
//                    int score = MahjongUtil.getScore(scoreTypes);
//                    //地胡
//                    if (historyList.size() == 1 && score < 8 && 1 == (gameRules >> 2) % 2) {
//                        scoreTypes.clear();
//                        scoreTypes.add(ScoreType.DI_HU);
//                        score = 8;
//                    }
//                    if (banker == seat.getUserId() && 1 == continuityBanker) {
//                        score *= 2;
//                        scoreTypes.add(ScoreType.ZHUANGYING);
//                    }
//
//                    historyList.add(new OperationHistory(seat.getUserId(), OperationHistoryType.HU, card[0]));
//
//                    operationSeat.setCardResult(new GameResult(scoreTypes, card[0], -score));
//                    operationSeat.setDianpaoCount(operationSeat.getDianpaoCount() + 1);
//                    seat.setCardResult(new GameResult(scoreTypes, card[0], score));
//                    seat.setHuCount(seat.getHuCount() + 1);
//                    //胡牌
//                    hu = true;
//                }
//            }
//        }
//
//        if (hu) {
//            gameOver(response, redisService, huSeat[0].getSeatNo(), false);
//        }
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

                seat.getAnGangCards().add(card);

                List<ScoreType> scoreTypes = new ArrayList<>();
                scoreTypes.add(ScoreType.AN_GANG);

                final int[] loseScore = {0};
                if (banker == seat.getUserId() && 0 < continuityBanker) {
                    seats.stream().filter(seat1 -> seat1.getSeatNo() != seat.getSeatNo())
                            .forEach(seat1 -> {
                                seat1.getAnGangResult().add(new GameResult(scoreTypes, card, -4));
                                loseScore[0] += 4;
                            });
                    seat.getAnGangResult().add(new GameResult(scoreTypes, card, loseScore[0]));
                } else {
                    seats.stream().filter(seat1 -> seat1.getSeatNo() != seat.getSeatNo())
                            .forEach(seat1 -> {
                                if (banker == seat1.getUserId() && 0 < continuityBanker) {
                                    seat1.getAnGangResult().add(new GameResult(scoreTypes, card, -4));
                                    loseScore[0] += 4;
                                } else {
                                    seat1.getAnGangResult().add(new GameResult(scoreTypes, card, -2));
                                    loseScore[0] += 2;
                                }
                            });
                    seat.getAnGangResult().add(new GameResult(scoreTypes, card, loseScore[0]));
                }

                seat.setAngang(seat.getAngang() + 1);
                historyList.add(new OperationHistory(seat.getUserId(), OperationHistoryType.AN_GANG, card));

                actionResponse.setOperationId(GameBase.ActionId.AN_GANG).setData(Mahjong.MahjongGang.newBuilder().setCard(card).build().toByteString());
                response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                seats.stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserId()))
                        .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserId()).send(response.build(), seat1.getUserId()));
                getCard(response, seat.getSeatNo(), redisService);
            } else if (1 == Card.containSize(seat.getPengCards(), card) && 1 == Card.containSize(seat.getCards(), card)) {//扒杠
                Card.remove(seat.getCards(), card);
                Card.remove(seat.getPengCards(), card);

                seat.getMingGangCards().add(card);

                List<ScoreType> scoreTypes = new ArrayList<>();
                scoreTypes.add(ScoreType.BA_GANG);

                final int[] loseScore = {0};
                if (banker == seat.getUserId() && 0 < continuityBanker) {
                    seats.stream().filter(seat1 -> seat1.getSeatNo() != seat.getSeatNo())
                            .forEach(seat1 -> {
                                seat1.getMingGangResult().add(new GameResult(scoreTypes, card, -2));
                                loseScore[0] += 2;
                            });
                    seat.getMingGangResult().add(new GameResult(scoreTypes, card, loseScore[0]));
                } else {
                    seats.stream().filter(seat1 -> seat1.getSeatNo() != seat.getSeatNo())
                            .forEach(seat1 -> {
                                if (banker == seat1.getUserId() && 0 < continuityBanker) {
                                    seat1.getMingGangResult().add(new GameResult(scoreTypes, card, -2));
                                    loseScore[0] += 2;
                                } else {
                                    seat1.getMingGangResult().add(new GameResult(scoreTypes, card, -1));
                                    loseScore[0]++;
                                }
                            });
                    seat.getMingGangResult().add(new GameResult(scoreTypes, card, loseScore[0]));
                }

                seat.setMinggang(seat.getMinggang() + 1);
                historyList.add(new OperationHistory(seat.getUserId(), OperationHistoryType.BA_GANG, card));

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
        GameBase.AskResponse.Builder builder = GameBase.AskResponse.newBuilder();
        builder.setTimeCounter(redisService.exists("room_match" + roomNo) ? 8 : 0);
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
                if (0 < surplusCards.size()) {
                    builder.addOperationId(GameBase.ActionId.DIAN_GANG);
                }
            } else if (2 == containSize) {
                builder.addOperationId(GameBase.ActionId.PENG);
            }
            //当前玩家是否可以胡牌
//            temp.add(card);
//            if (MahjongUtil.checkHu(temp, gameRules, gui)) {
//                if (redisService.exists("room_match" + roomNo)) {
//                    new OperationTimeout(seat.getUserId(), roomNo, historyList.size(), gameCount, redisService, true).start();
//                }
//                builder.addOperationId(GameBase.ActionId.HU);
//            } else {
//                if (redisService.exists("room_match" + roomNo)) {
//                    new OperationTimeout(seat.getUserId(), roomNo, historyList.size(), gameCount, redisService, false).start();
//                }
//            }
            if (0 != builder.getOperationIdCount()) {
                if (redisService.exists("room_match" + roomNo)) {
                    new OperationTimeout(seat.getUserId(), roomNo, historyList.size(), gameCount, redisService, false).start();
                }
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
        }
    }

    /**
     * 重连时检查出牌后是否有人能胡、杠、碰
     *
     * @param card 当前出的牌
     * @param date
     */
    public void checkSeatCan(Integer card, GameBase.BaseConnection.Builder response, int userId, Date date, RedisService redisService) {
        GameBase.AskResponse.Builder builder = GameBase.AskResponse.newBuilder();
        int time = 0;
        if (redisService.exists("room_match" + roomNo)) {
            time = 8 - (int) ((new Date().getTime() - date.getTime()) / 1000);
        }
        builder.setTimeCounter(time > 0 ? time : 0);
        //先检查胡，胡优先
        seats.stream().filter(seat -> seat.getUserId() == userId).forEach(seat -> {
            builder.clearOperationId();
            List<Integer> temp = new ArrayList<>();
            temp.addAll(seat.getCards());

            //当前玩家手里有几张牌，3张可碰可杠，两张只能碰
            int containSize = Card.containSize(temp, card);
            if (3 == containSize && 0 < surplusCards.size()) {
                builder.addOperationId(GameBase.ActionId.PENG);
                if (0 < surplusCards.size()) {
                    builder.addOperationId(GameBase.ActionId.DIAN_GANG);
                }
            } else if (2 == containSize) {
                builder.addOperationId(GameBase.ActionId.PENG);
            }
            //当前玩家是否可以胡牌
//            temp.add(card);
//            if (MahjongUtil.checkHu(temp, gameRules, gui)) {
//                builder.addOperationId(GameBase.ActionId.HU);
//            }
            if (0 != builder.getOperationIdCount()) {
                if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                    response.setOperationType(GameBase.OperationType.ASK).setData(builder.build().toByteString());
                    MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
                }
            }
        });
    }

//    /**
//     * 当有人胡、碰、杠后，再次检查是否还有人胡、碰、杠
//     */
//    public boolean checkSurplus() {
//        //找到那张牌
//        final Integer[] card = new Integer[1];
//        seats.stream().filter(seat -> seat.getSeatNo() == operationSeatNo)
//                .forEach(seat -> card[0] = seat.getPlayedCards().get(seat.getPlayedCards().size() - 1));
//        final boolean[] hu = {false};
//        //先检查胡，胡优先
//        seats.stream().filter(seat -> seat.getSeatNo() != operationSeatNo).forEach(seat -> {
//            List<Integer> temp = new ArrayList<>();
//            temp.addAll(seat.getCards());
//
//            //当前玩家是否可以胡牌
//            temp.add(card[0]);
//            if (MahjongUtil.checkHu(temp, gameRules, gui) && seat.getOperation() == 0) {
//                hu[0] = true;
//            }
//        });
//        return !hu[0];
//    }

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
//            if (MahjongUtil.checkHu(temp, gameRules, gui) && seat.getOperation() != 4) {
//                hasNoOperation[0] = true;
//            }

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
                    seat.getMingGangCards().add(card[0]);

                    //添加结算
                    List<ScoreType> scoreTypes = new ArrayList<>();
                    scoreTypes.add(ScoreType.DIAN_GANG);

                    //庄家点闲家或者闲家点庄家并且连庄
                    if ((banker == seat.getUserId() || banker == operationSeat.getUserId()) && 0 < continuityBanker) {
                        operationSeat.getMingGangResult().add(new GameResult(scoreTypes, card[0], 2 * (-count + 1)));
                        seat.getMingGangResult().add(new GameResult(scoreTypes, card[0], 2 * (count - 1)));
                    } else if (0 < continuityBanker) {
                        operationSeat.getMingGangResult().add(new GameResult(scoreTypes, card[0], -count));
                        seat.getMingGangResult().add(new GameResult(scoreTypes, card[0], count));
                    } else {
                        operationSeat.getMingGangResult().add(new GameResult(scoreTypes, card[0], -count + 1));
                        seat.getMingGangResult().add(new GameResult(scoreTypes, card[0], count - 1));
                    }

                    seat.setMinggang(seat.getMinggang() + 1);
                    historyList.add(new OperationHistory(userId, OperationHistoryType.DIAN_GANG, card[0]));

                    operationSeat.getPlayedCards().remove(operationSeat.getPlayedCards().size() - 1);

                    actionResponse.setOperationId(GameBase.ActionId.DIAN_GANG).setData(Mahjong.MahjongGang.newBuilder()
                            .setCard(card[0]).build().toByteString());
                    response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                    seats.stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserId()))
                            .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserId()).send(response.build(), seat1.getUserId()));

                    //点杠后需要摸牌
                    getCard(response, seat.getSeatNo(), redisService);
                    return;
                } else if (2 <= containSize && seat.getOperation() == 3) {//碰
                    Card.remove(seat.getCards(), card[0]);
                    Card.remove(seat.getCards(), card[0]);
                    seat.getPengCards().add(card[0]);
                    operationSeatNo = seat.getSeatNo();
                    historyList.add(new OperationHistory(userId, OperationHistoryType.PENG, card[0]));

                    operationSeat.getPlayedCards().remove(operationSeat.getPlayedCards().size() - 1);

                    actionResponse.setOperationId(GameBase.ActionId.PENG).setData(Mahjong.MahjongPengResponse.newBuilder().setCard(card[0]).build().toByteString());
                    response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                    seats.stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserId()))
                            .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserId()).send(response.build(), seat1.getUserId()));

                    if (redisService.exists("room_match" + roomNo)) {
                        new PlayCardTimeout(seat.getUserId(), roomNo, historyList.size(), gameCount, redisService).start();
                    }
                    GameBase.RoundResponse roundResponse = GameBase.RoundResponse.newBuilder().setID(seat.getUserId())
                            .setTimeCounter(redisService.exists("room_match" + roomNo) ? 8 : 0).build();
                    response.setOperationType(GameBase.OperationType.ROUND).setData(roundResponse.toByteString());
                    seats.stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserId()))
                            .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserId()).send(response.build(), seat1.getUserId()));
                    return;
                }
            }
        }
    }

    public void start(GameBase.BaseConnection.Builder response, RedisService redisService) {
        gameCount = gameCount + 1;
        gameStatus = GameStatus.PLAYING;
        dealCard();
        //骰子
        int dice1 = new Random().nextInt(6) + 1;
        int dice2 = new Random().nextInt(6) + 1;
        dice = new Integer[]{dice1, dice2};
        Mahjong.MahjongStartResponse.Builder dealCard = Mahjong.MahjongStartResponse.newBuilder();
        dealCard.setBanker(banker).addDice(dice1).addDice(dice2).setRogue(fan == 0 ? gui : fan);
        dealCard.setSurplusCardsSize(surplusCards.size());
        response.setOperationType(GameBase.OperationType.START);
        for (Seat seat : seats) {
            if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                dealCard.clearCards();
                dealCard.addAllCards(seat.getCards());
                response.setData(dealCard.build().toByteString());
                MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
            }
        }

        Seat operationSeat = null;
        GameBase.RoundResponse roundResponse = GameBase.RoundResponse.newBuilder().setID(banker)
                .setTimeCounter(redisService.exists("room_match" + roomNo) ? 8 : 0).build();
        response.setOperationType(GameBase.OperationType.ROUND).setData(roundResponse.toByteString());

        if (redisService.exists("room_match" + roomNo)) {
            new PlayCardTimeout(banker, roomNo, historyList.size(), gameCount, redisService).start();
        }
        for (Seat seat : seats) {
            if (operationSeatNo == seat.getSeatNo()) {
                operationSeat = seat;
            }
            if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
            }
        }

        checkSelfGetCard(response, operationSeat, redisService);
    }

    public void playCard(Integer card, int userId, GameBase.BaseAction.Builder actionResponse, GameBase.BaseConnection.Builder response, RedisService redisService) {
        actionResponse.setID(userId);
        for (Seat seat : seats) {
            if (seat.getUserId() == userId) {
                if (operationSeatNo == seat.getSeatNo() && lastOperation != userId) {
                    if (seat.getCards().contains(card)) {
                        seat.getCards().remove(card);
                        if (null == seat.getPlayedCards()) {
                            seat.setPlayedCards(new ArrayList<>());
                        }
                        seat.getPlayedCards().add(card);
                        Mahjong.MahjongPlayCard.Builder builder = Mahjong.MahjongPlayCard.newBuilder().setCard(card);

                        actionResponse.setOperationId(GameBase.ActionId.PLAY_CARD).setData(builder.build().toByteString());

                        response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                        lastOperation = userId;
                        historyList.add(new OperationHistory(userId, OperationHistoryType.PLAY_CARD, card));
                        seats.stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserId()))
                                .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserId()).send(response.build(), seat1.getUserId()));
                        //先检查其它三家牌，是否有人能胡、杠、碰
                        checkCard(card, response, redisService, userId);
                    } else {
                        System.out.println("用户手中没有此牌" + userId);
                    }
                } else {
                    System.out.println("不该当前玩家操作" + userId);
                }
                break;
            }
        }
    }

    public void sendRoomInfo(GameBase.RoomCardIntoResponse.Builder roomCardIntoResponseBuilder, GameBase.BaseConnection.Builder response, int userId) {
        Xingning.XingningMahjongIntoResponse.Builder intoResponseBuilder = Xingning.XingningMahjongIntoResponse.newBuilder();
        intoResponseBuilder.setBaseScore(baseScore);
        intoResponseBuilder.setCount(count);
        intoResponseBuilder.setGameTimes(gameTimes);
        intoResponseBuilder.setGameRules(gameRules);
        intoResponseBuilder.setGhost(ghost);
        intoResponseBuilder.setMaCount(initMaCount);
        roomCardIntoResponseBuilder.setGameType(GameBase.GameType.MAHJONG_XINGNING);
        roomCardIntoResponseBuilder.setError(GameBase.ErrorCode.SUCCESS);
        roomCardIntoResponseBuilder.setData(intoResponseBuilder.build().toByteString());
        response.setOperationType(GameBase.OperationType.ROOM_INFO).setData(roomCardIntoResponseBuilder.build().toByteString());
        if (MahjongTcpService.userClients.containsKey(userId)) {
            MahjongTcpService.userClients.get(userId).send(response.build(), userId);
        }
    }

    public void sendSeatInfo(GameBase.BaseConnection.Builder response) {
        GameBase.RoomSeatsInfo.Builder roomSeatsInfo = GameBase.RoomSeatsInfo.newBuilder();
        for (Seat seat1 : seats) {
            GameBase.SeatResponse.Builder seatResponse = GameBase.SeatResponse.newBuilder();
            seatResponse.setSeatNo(seat1.getSeatNo());
            seatResponse.setID(seat1.getUserId());
            seatResponse.setScore(seat1.getScore());
            seatResponse.setReady(seat1.isReady());
            seatResponse.setNickname(seat1.getNickname());
            seatResponse.setHead(seat1.getHead());
            seatResponse.setSex(seat1.isSex());
            seatResponse.setOffline(seat1.isRobot());
            seatResponse.setIsRobot(seat1.isRobot());
            seatResponse.setIp(seat1.getIp());
            seatResponse.setGameCount(seat1.getGameCount());
            roomSeatsInfo.addSeats(seatResponse.build());
        }
        response.setOperationType(GameBase.OperationType.SEAT_INFO).setData(roomSeatsInfo.build().toByteString());
        for (Seat seat : seats) {
            if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
            }
        }
    }
}
