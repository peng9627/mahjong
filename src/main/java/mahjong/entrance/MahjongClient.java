package mahjong.entrance;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.google.protobuf.InvalidProtocolBufferException;
import mahjong.constant.Constant;
import mahjong.mode.*;
import mahjong.redis.RedisService;
import mahjong.timeout.DissolveTimeout;
import mahjong.timeout.MatchScoreTimeout;
import mahjong.timeout.PlayCardTimeout;
import mahjong.timeout.ReadyTimeout;
import mahjong.utils.HttpUtil;
import mahjong.utils.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created date 2016/3/25
 * Author pengyi
 */
public class MahjongClient {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    public int userId;
    private RedisService redisService;

    private GameBase.BaseConnection.Builder response;
    private MessageReceive messageReceive;


    MahjongClient(RedisService redisService, MessageReceive messageReceive) {
        this.redisService = redisService;
        this.messageReceive = messageReceive;
        this.response = GameBase.BaseConnection.newBuilder();
    }

    void close() {
        if (0 != userId) {
            synchronized (MahjongTcpService.userClients) {
                if (MahjongTcpService.userClients.containsKey(userId) && messageReceive == MahjongTcpService.userClients.get(userId)) {
                    MahjongTcpService.userClients.remove(userId);
                    if (redisService.exists("room" + messageReceive.roomNo)) {
                        while (!redisService.lock("lock_room" + messageReceive.roomNo)) {
                        }
                        Room room = JSON.parseObject(redisService.getCache("room" + messageReceive.roomNo), Room.class);
                        if (null == room) {
                            return;
                        }
                        for (Seat seat : room.getSeats()) {
                            if (seat.getUserId() == userId && !seat.isRobot()) {
                                seat.setRobot(true);
                                response.setOperationType(GameBase.OperationType.ONLINE).setData(GameBase.Online.newBuilder()
                                        .setOnline(false).setUserId(userId).build().toByteString());
                                for (Seat seat1 : room.getSeats()) {
                                    if (MahjongTcpService.userClients.containsKey(seat1.getUserId())) {
                                        messageReceive.send(response.build(), seat1.getUserId());
                                    }
                                }
                                break;
                            }
                        }
                        redisService.addCache("room" + messageReceive.roomNo, JSON.toJSONString(room));
                        redisService.unlock("lock_room" + messageReceive.roomNo);
                    }
                }
            }

        }
    }

    synchronized void receive(GameBase.BaseConnection request) {
        try {
            logger.info("接收" + userId + request.getOperationType().toString());
            switch (request.getOperationType()) {
                case HEARTBEAT:
                    messageReceive.send(response.setOperationType(GameBase.OperationType.HEARTBEAT).clearData().build(), userId);
                    break;
                case CONNECTION:
                    //加入玩家数据
                    if (redisService.exists("maintenance")) {
                        break;
                    }
                    GameBase.RoomCardIntoRequest intoRequest = GameBase.RoomCardIntoRequest.parseFrom(request.getData());
                    GameBase.RoomCardIntoResponse.Builder roomCardIntoResponseBuilder = GameBase.RoomCardIntoResponse.newBuilder();
                    roomCardIntoResponseBuilder.setGameType(GameBase.GameType.MAHJONG_XINGNING).setRoomNo(intoRequest.getRoomNo());

                    userId = intoRequest.getID();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("userId", userId);
                    ApiResponse<User> userResponse = JSON.parseObject(HttpUtil.urlConnectionByRsa(Constant.apiUrl + Constant.userInfoUrl, jsonObject.toJSONString()), new TypeReference<ApiResponse<User>>() {
                    });
                    if (0 == userResponse.getCode()) {
                        messageReceive.roomNo = Integer.valueOf(intoRequest.getRoomNo());
                        if (MahjongTcpService.userClients.containsKey(userId) && MahjongTcpService.userClients.get(userId) != messageReceive) {
                            MahjongTcpService.userClients.get(userId).close(false);
                        }
                        synchronized (this) {
                            try {
                                wait(10);
                            } catch (InterruptedException e) {
                                logger.error(e.toString(), e);
                            }
                        }
                        MahjongTcpService.userClients.put(userId, messageReceive);
                        if (redisService.exists("room" + messageReceive.roomNo)) {
                            while (!redisService.lock("lock_room" + messageReceive.roomNo)) {
                            }

                            Room room = JSON.parseObject(redisService.getCache("room" + messageReceive.roomNo), Room.class);
                            roomCardIntoResponseBuilder.setRoomOwner(room.getRoomOwner());
                            roomCardIntoResponseBuilder.setStarted(0 != room.getGameStatus().compareTo(GameStatus.READYING) && 0 != room.getGameStatus().compareTo(GameStatus.WAITING));
                            if (0 == room.getGameStatus().compareTo(GameStatus.READYING) && redisService.exists("room_match" + messageReceive.roomNo)) {
                                int time = 0;
                                if (redisService.exists("room_match" + messageReceive.roomNo)) {
                                    time = 8 - (int) ((new Date().getTime() - room.getStartDate().getTime()) / 1000);
                                }
                                roomCardIntoResponseBuilder.setReadyTimeCounter(time > 0 ? time : 0);
                            }

                            //房间是否已存在当前用户，存在则为重连
                            final boolean[] find = {false};
                            room.getSeats().stream().filter(seat -> seat.getUserId() == userId).forEach(seat -> {
                                find[0] = true;
                                seat.setRobot(false);
                            });
                            if (!find[0]) {
                                if (room.getCount() > room.getSeats().size()) {
                                    room.addSeat(userResponse.getData(), 0);
                                } else {
                                    roomCardIntoResponseBuilder.setError(GameBase.ErrorCode.COUNT_FULL);
                                    response.setOperationType(GameBase.OperationType.ROOM_INFO).setData(roomCardIntoResponseBuilder.build().toByteString());
                                    messageReceive.send(response.build(), userId);
                                    redisService.unlock("lock_room" + messageReceive.roomNo);
                                    break;
                                }
                            }
                            redisService.addCache("reconnect" + userId, "xingning_mahjong," + messageReceive.roomNo);
                            room.sendRoomInfo(roomCardIntoResponseBuilder, response, userId);
                            room.sendSeatInfo(response);

                            //是否竞技场
                            if (redisService.exists("room_match" + messageReceive.roomNo)) {
                                String matchNo = redisService.getCache("room_match" + messageReceive.roomNo);
                                if (redisService.exists("match_info" + matchNo)) {
                                    while (!redisService.lock("lock_match_info" + matchNo)) {
                                    }
                                    MatchInfo matchInfo = JSON.parseObject(redisService.getCache("match_info" + matchNo), MatchInfo.class);
                                    Arena arena = matchInfo.getArena();
                                    GameBase.MatchInfo matchInfoResponse = GameBase.MatchInfo.newBuilder().setArenaType(arena.getArenaType())
                                            .setCount(arena.getCount()).setEntryFee(arena.getEntryFee()).setName(arena.getName())
                                            .setReward(arena.getReward()).build();
                                    messageReceive.send(response.setOperationType(GameBase.OperationType.MATCH_INFO)
                                            .setData(matchInfoResponse.toByteString()).build(), userId);

                                    int status = matchInfo.getStatus();
                                    int round = 1;
                                    if (status == 3) {
                                        round = 2;
                                    }
                                    if (status == 4) {
                                        round = 3;
                                    }
                                    if (status > 2) {
                                        status = status == 5 ? 3 : 2;
                                    }
                                    GameBase.MatchData matchData = GameBase.MatchData.newBuilder()
                                            .setCurrentCount(matchInfo.getMatchUsers().size())
                                            .setStatus(status).setRound(round).build();
                                    messageReceive.send(response.setOperationType(GameBase.OperationType.MATCH_DATA)
                                            .setData(matchData.toByteString()).build(), userId);

                                    if (!matchInfo.isStart()) {
                                        List<Integer> roomNos = matchInfo.getRooms();
                                        for (Integer roomNo : roomNos) {
                                            new ReadyTimeout(roomNo, redisService, 0).start();
                                        }
                                        matchInfo.setStart(true);
                                        new MatchScoreTimeout(Integer.valueOf(matchNo), redisService).start();
                                    }
                                    redisService.addCache("match_info" + matchNo, JSON.toJSONString(matchInfo));
                                    redisService.unlock("lock_match_info" + matchNo);
                                }
                            }

                            if (0 != room.getGameStatus().compareTo(GameStatus.WAITING)) {

                                if (0 == room.getGameStatus().compareTo(GameStatus.PLAYING)) {
                                    for (Seat seat : room.getSeats()) {
                                        if (seat.getSeatNo() == room.getOperationSeatNo()) {
                                            int time = 0;
                                            if (redisService.exists("room_match" + messageReceive.roomNo)) {
                                                if (0 == room.getHistoryList().size()) {
                                                    time = 8 - (int) ((new Date().getTime() - room.getStartDate().getTime() / 1000));
                                                } else {
                                                    time = 8 - (int) ((new Date().getTime() - room.getHistoryList().get(room.getHistoryList().size() - 1).getDate().getTime() / 1000));
                                                }
                                            }
                                            GameBase.RoundResponse roundResponse = GameBase.RoundResponse.newBuilder().setTimeCounter(time > 0 ? time : 0).setID(seat.getUserId()).build();
                                            response.setOperationType(GameBase.OperationType.ROUND).setData(roundResponse.toByteString());
                                            messageReceive.send(response.build(), userId);
                                            break;
                                        }
                                    }
                                }

                                Mahjong.MahjongGameInfo.Builder gameInfo = Mahjong.MahjongGameInfo.newBuilder();
                                gameInfo.setSurplusCardsSize(room.getSurplusCards().size());
                                gameInfo.setBanker(room.getBanker());
                                gameInfo.setRogue(room.getFan() == 0 ? room.getGui() : room.getFan());
                                if (null != room.getDice() && 0 < room.getDice().length) {
                                    gameInfo.addAllDice(Arrays.asList(room.getDice()));
                                }
                                Seat operationSeat = null;
                                for (Seat seat : room.getSeats()) {
                                    if (seat.getSeatNo() == room.getOperationSeatNo()) {
                                        operationSeat = seat;
                                        break;
                                    }
                                }
                                gameInfo.setGameCount(room.getGameCount());
                                gameInfo.setGameTimes(room.getGameTimes());
                                addSeat(room, gameInfo);

                                int lastPlayedUser = 0;
                                if (0 < room.getHistoryList().size()) {
                                    for (int i = room.getHistoryList().size() - 1; i > -1; i--) {
                                        OperationHistory operationHistory = room.getHistoryList().get(i);
                                        if (0 == operationHistory.getHistoryType().compareTo(OperationHistoryType.PLAY_CARD)) {
                                            lastPlayedUser = operationHistory.getUserId();
                                            break;
                                        }
                                        if (0 == operationHistory.getHistoryType().compareTo(OperationHistoryType.PENG)
                                                || 0 == operationHistory.getHistoryType().compareTo(OperationHistoryType.DIAN_GANG)) {
                                            break;
                                        }
                                    }
                                }
                                gameInfo.setLastPlayedUser(lastPlayedUser);
                                response.setOperationType(GameBase.OperationType.GAME_INFO).setData(gameInfo.build().toByteString());
                                messageReceive.send(response.build(), userId);

                                //才开始的时候检测是否该当前玩家出牌
                                if (0 == room.getHistoryList().size()) {
                                    for (Seat seat : room.getSeats()) {
                                        if (seat.getSeatNo() == room.getOperationSeatNo() && seat.getUserId() == userId && 0 == room.getGameStatus().compareTo(GameStatus.PLAYING)) {
                                            room.checkSelfGetCard(response, operationSeat, redisService);
                                            break;
                                        }
                                    }
                                } else if (room.getHistoryList().size() > 0) {
                                    OperationHistory operationHistory = room.getHistoryList().get(room.getHistoryList().size() - 1);
                                    switch (operationHistory.getHistoryType()) {
                                        case GET_CARD:
                                            if (operationHistory.getUserId() == userId) {
                                                for (Seat seat : room.getSeats()) {
                                                    if (seat.getUserId() == userId) {
                                                        room.checkSelfGetCard(response, seat, redisService);
                                                        break;
                                                    }
                                                }
                                            }
                                            break;
                                        case PLAY_CARD:
                                            if (operationHistory.getUserId() != userId) {
                                                room.checkSeatCan(operationHistory.getCards().get(0), response, userId, operationHistory.getDate(), redisService);
                                            }
                                            break;
                                    }
                                }
                            }
                            redisService.addCache("room" + messageReceive.roomNo, JSON.toJSONString(room));
                            redisService.unlock("lock_room" + messageReceive.roomNo);

                            if (redisService.exists("dissolve" + messageReceive.roomNo)) {

                                String dissolveStatus = redisService.getCache("dissolve" + messageReceive.roomNo);
                                String[] users = dissolveStatus.split("-");
                                String user = "0";
                                for (String s : users) {
                                    if (s.startsWith("1")) {
                                        user = s.substring(1);
                                        break;
                                    }
                                }

                                GameBase.DissolveApply dissolveApply = GameBase.DissolveApply.newBuilder()
                                        .setError(GameBase.ErrorCode.SUCCESS).setUserId(Integer.valueOf(user)).build();
                                response.setOperationType(GameBase.OperationType.DISSOLVE).setData(dissolveApply.toByteString());
                                if (MahjongTcpService.userClients.containsKey(userId)) {
                                    messageReceive.send(response.build(), userId);
                                }

                                GameBase.DissolveReplyResponse.Builder replyResponse = GameBase.DissolveReplyResponse.newBuilder();
                                for (Seat seat : room.getSeats()) {
                                    if (dissolveStatus.contains("-1" + seat.getUserId())) {
                                        replyResponse.addDissolve(GameBase.Dissolve.newBuilder().setUserId(seat.getUserId()).setAgree(true));
                                    } else if (dissolveStatus.contains("-2" + seat.getUserId())) {
                                        replyResponse.addDissolve(GameBase.Dissolve.newBuilder().setUserId(seat.getUserId()).setAgree(false));
                                    }
                                }
                                response.setOperationType(GameBase.OperationType.DISSOLVE_REPLY).setData(replyResponse.build().toByteString());
                                messageReceive.send(response.build(), userId);
                            }
                        } else if (redisService.exists("match_info" + messageReceive.roomNo)) {
                            while (!redisService.lock("lock_match_info" + messageReceive.roomNo)) {
                            }
                            MatchInfo matchInfo = JSON.parseObject(redisService.getCache("match_info" + messageReceive.roomNo), MatchInfo.class);
                            int score = 0;
                            for (MatchUser m : matchInfo.getMatchUsers()) {
                                if (m.getUserId() == userId) {
                                    score = m.getScore();
                                    break;
                                }
                            }
                            messageReceive.send(response.setOperationType(GameBase.OperationType.ROOM_INFO).clearData().build(), userId);
                            GameBase.RoomSeatsInfo.Builder roomSeatsInfo = GameBase.RoomSeatsInfo.newBuilder();
                            GameBase.SeatResponse.Builder seatResponse = GameBase.SeatResponse.newBuilder();
                            seatResponse.setSeatNo(1);
                            seatResponse.setID(userId);
                            seatResponse.setScore(score);
                            seatResponse.setReady(false);
                            seatResponse.setIp(userResponse.getData().getLastLoginIp());
                            seatResponse.setGameCount(userResponse.getData().getGameCount());
                            seatResponse.setNickname(userResponse.getData().getNickname());
                            seatResponse.setHead(userResponse.getData().getHead());
                            seatResponse.setSex(userResponse.getData().getSex().equals("1"));
                            seatResponse.setOffline(false);
                            seatResponse.setIsRobot(false);
                            roomSeatsInfo.addSeats(seatResponse.build());
                            messageReceive.send(response.setOperationType(GameBase.OperationType.SEAT_INFO).setData(roomSeatsInfo.build().toByteString()).build(), userId);
                            redisService.unlock("lock_match_info" + messageReceive.roomNo);
                        } else {
                            roomCardIntoResponseBuilder.setError(GameBase.ErrorCode.ROOM_NOT_EXIST);
                            response.setOperationType(GameBase.OperationType.ROOM_INFO).setData(roomCardIntoResponseBuilder.build().toByteString());
                            messageReceive.send(response.build(), userId);
                        }
                    } else {
                        roomCardIntoResponseBuilder.setError(GameBase.ErrorCode.ROOM_NOT_EXIST);
                        response.setOperationType(GameBase.OperationType.ROOM_INFO).setData(roomCardIntoResponseBuilder.build().toByteString());
                        messageReceive.send(response.build(), userId);
                    }
                    break;
                case READY:
                    if (redisService.exists("room" + messageReceive.roomNo)) {
                        while (!redisService.lock("lock_room" + messageReceive.roomNo)) {
                        }
                        Room room = JSON.parseObject(redisService.getCache("room" + messageReceive.roomNo), Room.class);
                        if (0 == room.getGameStatus().compareTo(GameStatus.READYING) || 0 == room.getGameStatus().compareTo(GameStatus.WAITING)) {
                            room.getSeats().stream().filter(seat -> seat.getUserId() == userId && !seat.isReady()).forEach(seat -> {
                                seat.setReady(true);
                                response.setOperationType(GameBase.OperationType.READY).setData(GameBase.ReadyResponse.newBuilder().setID(userId).build().toByteString());
                                room.getSeats().stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserId())).forEach(seat1 ->
                                        MahjongTcpService.userClients.get(seat1.getUserId()).send(response.build(), seat1.getUserId()));
                            });
                            boolean allReady = true;
                            for (Seat seat : room.getSeats()) {
                                if (!seat.isReady()) {
                                    allReady = false;
                                    break;
                                }
                            }
                            if (allReady && room.getCount() == room.getSeats().size()) {
                                room.start(response, redisService);
                            }
                        }

                        redisService.addCache("room" + messageReceive.roomNo, JSON.toJSONString(room));
                        redisService.unlock("lock_room" + messageReceive.roomNo);
                    } else {
                        logger.warn("房间不存在");
                    }
                    break;
                case COMPLETED:
                    if (redisService.exists("room" + messageReceive.roomNo)) {
                        while (!redisService.lock("lock_room" + messageReceive.roomNo)) {
                        }
                        Room room = JSON.parseObject(redisService.getCache("room" + messageReceive.roomNo), Room.class);
                        room.getSeats().stream().filter(seat -> seat.getUserId() == userId && !seat.isCompleted())
                                .forEach(seat -> seat.setCompleted(true));
                        boolean allCompleted = true;
                        for (Seat seat : room.getSeats()) {
                            if (!seat.isCompleted()) {
                                allCompleted = false;
                                break;
                            }
                        }
                        if (allCompleted) {
                            //TODO 出牌超时
                        }
                        redisService.addCache("room" + messageReceive.roomNo, JSON.toJSONString(room));
                        redisService.unlock("lock_room" + messageReceive.roomNo);
                    } else {
                        logger.warn("房间不存在");
                    }
                    break;
                case ACTION:
                    GameBase.BaseAction actionRequest = GameBase.BaseAction.parseFrom(request.getData());
                    logger.info("xingning 接收 " + actionRequest.getOperationId() + userId);
                    GameBase.BaseAction.Builder actionResponse = GameBase.BaseAction.newBuilder().setID(userId);
                    if (redisService.exists("room" + messageReceive.roomNo)) {
                        while (!redisService.lock("lock_room" + messageReceive.roomNo)) {
                        }
                        Room room = JSON.parseObject(redisService.getCache("room" + messageReceive.roomNo), Room.class);
                        switch (actionRequest.getOperationId()) {
                            case PLAY_CARD:
                                Mahjong.CardsData playCardRequest = Mahjong.CardsData.parseFrom(actionRequest.getData());
                                Integer card = playCardRequest.getCards(0);
                                room.playCard(card, userId, actionResponse, response, redisService);
                                break;
                            case PENG:
                                if (0 < room.getHistoryList().size()) {
                                    if (0 != room.getHistoryList().get(room.getHistoryList().size() - 1).getHistoryType().compareTo(OperationHistoryType.PLAY_CARD)) {
                                        return;
                                    }
                                }
                                room.getSeats().stream().filter(seat -> seat.getUserId() == userId &&
                                        room.getOperationSeatNo() != seat.getSeatNo()).forEach(seat -> seat.setOperation(3));
//                                if (room.checkSurplus()) { //如果可以碰、杠牌，则碰、杠
                                room.pengOrGang(actionResponse, response, redisService, userId);
//                                }
                                break;
                            case AN_GANG:
                            case BA_GANG:
                                if (0 < room.getHistoryList().size()) {
                                    if (0 != room.getHistoryList().get(room.getHistoryList().size() - 1).getHistoryType().compareTo(OperationHistoryType.GET_CARD)
                                            || room.getHistoryList().get(room.getHistoryList().size() - 1).getUserId() != userId) {
                                        return;
                                    }
                                }
                                Mahjong.CardsData gangRequest = Mahjong.CardsData.parseFrom(actionRequest.getData());
                                room.selfGang(actionResponse, gangRequest.getCardsList(), response, redisService, userId);
                                break;
                            case DIAN_GANG:
                                if (0 < room.getHistoryList().size()) {
                                    if (0 != room.getHistoryList().get(room.getHistoryList().size() - 1).getHistoryType().compareTo(OperationHistoryType.PLAY_CARD)) {
                                        return;
                                    }
                                }
                                room.getSeats().stream().filter(seat -> seat.getUserId() == userId &&
                                        room.getOperationSeatNo() != seat.getSeatNo()).forEach(seat -> seat.setOperation(2));
//                                if (room.checkSurplus()) { //如果可以碰、杠牌，则碰、杠
                                room.pengOrGang(actionResponse, response, redisService, userId);
//                                }
                                break;
                            case HU:
                                room.getSeats().stream().filter(seat -> seat.getUserId() == userId).forEach(seat -> seat.setOperation(1));
                                if (0 < room.getHistoryList().size()) {
                                    if (0 == room.getHistoryList().get(room.getHistoryList().size() - 1).getHistoryType().compareTo(OperationHistoryType.BA_GANG)) {
                                        if (room.checkAllQiangGang(room.getHistoryList().get(room.getHistoryList().size() - 1).getCards().get(0), response, redisService)) {
                                            room.hu(userId, response, redisService);//胡
                                        }
                                        break;
                                    }
                                }
                                room.hu(userId, response, redisService);//胡
                                break;
                            case PASS:
                                room.getSeats().stream().filter(seat -> seat.getUserId() == userId).forEach(seat -> {
                                    if (room.getOperationSeatNo() != seat.getSeatNo()) {

                                        if (0 < room.getHistoryList().size()) {
                                            if (0 == room.getHistoryList().get(room.getHistoryList().size() - 1).getHistoryType().compareTo(OperationHistoryType.BA_GANG)) {
                                                seat.setOperation(4);
                                                room.checkQiangGangPass(room.getHistoryList().get(room.getHistoryList().size() - 1).getCards().get(0), response, redisService);
                                                if (room.checkAllQiangGang(room.getHistoryList().get(room.getHistoryList().size() - 1).getCards().get(0), response, redisService)) {
                                                    room.hu(userId, response, redisService);
                                                }
                                                return;
                                            }
                                        }

                                        if (0 < room.getHistoryList().size()) {
                                            if (0 != room.getHistoryList().get(room.getHistoryList().size() - 1).getHistoryType().compareTo(OperationHistoryType.PLAY_CARD)) {
                                                return;
                                            }
                                        }
                                        actionResponse.setOperationId(GameBase.ActionId.PASS).clearData();
                                        messageReceive.send(response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString()).build(), userId);
                                        seat.setOperation(4);
                                        if (!room.passedChecked()) {//如果都操作完了，继续摸牌
                                            room.getSeats().forEach(seat1 -> {
                                                seat1.setOperation(0);
                                            });
                                            room.getCard(response, room.getNextSeat(), redisService);
                                        } else {//if (room.checkSurplus()) { //如果可以碰、杠牌，则碰、杠
                                            room.pengOrGang(actionResponse, response, redisService, userId);
                                        }
                                    } else {
                                        actionResponse.setOperationId(GameBase.ActionId.PASS).clearData();
                                        messageReceive.send(response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString()).build(), userId);
                                        if (redisService.exists("room_match" + messageReceive.roomNo)) {
                                            new PlayCardTimeout(seat.getUserId(), String.valueOf(messageReceive.roomNo), room.getHistoryList().size(), room.getGameCount(), redisService).start();
                                        }
                                    }
                                });
                                break;

                        }
                        if (null != room.getRoomNo()) {
                            redisService.addCache("room" + messageReceive.roomNo, JSON.toJSONString(room));
                        }
                        redisService.unlock("lock_room" + messageReceive.roomNo);
                    } else {
                        logger.warn("房间不存在");
                    }
                    break;
                case EXIT:
                    if (redisService.exists("room" + messageReceive.roomNo) && !redisService.exists("room_match" + messageReceive.roomNo)) {
                        while (!redisService.lock("lock_room" + messageReceive.roomNo)) {
                        }
                        GameBase.ExitRoom.Builder exitRoom = GameBase.ExitRoom.newBuilder();
                        Room room = JSON.parseObject(redisService.getCache("room" + messageReceive.roomNo), Room.class);
                        if (0 == room.getGameStatus().compareTo(GameStatus.WAITING)) {
                            for (Seat seat : room.getSeats()) {
                                if (seat.getUserId() == userId) {
                                    exitRoom.setUserId(userId);
                                    String uuid = UUID.randomUUID().toString().replace("-", "");
                                    while (redisService.exists(uuid)) {
                                        uuid = UUID.randomUUID().toString().replace("-", "");
                                    }
                                    redisService.addCache("backkey" + uuid, seat.getUserId() + "", 1800);
                                    exitRoom.setBackKey(uuid);
                                    response.setOperationType(GameBase.OperationType.EXIT).setData(exitRoom.build().toByteString());
                                    messageReceive.send(response.build(), userId);
                                    room.getSeatNos().add(seat.getSeatNo());
                                    room.getSeats().remove(seat);
                                    redisService.delete("reconnect" + seat.getUserId());
                                    room.sendSeatInfo(response);
                                    redisService.addCache("room" + messageReceive.roomNo, JSON.toJSONString(room));
                                    break;
                                }
                            }
                        } else {
                            exitRoom.setError(GameBase.ErrorCode.SHOUND_NOT_OPERATION);
                            response.setOperationType(GameBase.OperationType.EXIT).setData(exitRoom.build().toByteString());
                            messageReceive.send(response.build(), userId);
                        }
                        redisService.unlock("lock_room" + messageReceive.roomNo);
                    }
                    break;
                case DISSOLVE:
                    if (redisService.exists("room" + messageReceive.roomNo) && !redisService.exists("room_match" + messageReceive.roomNo)) {
                        while (!redisService.lock("lock_room" + messageReceive.roomNo)) {
                        }
                        if (!redisService.exists("dissolve" + messageReceive.roomNo) && !redisService.exists("delete_dissolve" + messageReceive.roomNo)) {
                            GameBase.DissolveApply dissolveApply = GameBase.DissolveApply.newBuilder()
                                    .setError(GameBase.ErrorCode.SUCCESS).setUserId(userId).build();
                            Room room = JSON.parseObject(redisService.getCache("room" + messageReceive.roomNo), Room.class);
                            redisService.addCache("dissolve" + messageReceive.roomNo, "-1" + userId);
                            response.setOperationType(GameBase.OperationType.DISSOLVE).setData(dissolveApply.toByteString());
                            for (Seat seat : room.getSeats()) {
                                if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                                    messageReceive.send(response.build(), seat.getUserId());
                                }
                            }

                            GameBase.DissolveReplyResponse.Builder replyResponse = GameBase.DissolveReplyResponse.newBuilder();
                            replyResponse.addDissolve(GameBase.Dissolve.newBuilder().setUserId(userId).setAgree(true));
                            response.setOperationType(GameBase.OperationType.DISSOLVE_REPLY).setData(dissolveApply.toByteString());
                            for (Seat seat : room.getSeats()) {
                                if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                                    messageReceive.send(response.build(), seat.getUserId());
                                }
                            }

                            if (1 == room.getSeats().size()) {
                                GameBase.DissolveConfirm dissolveConfirm = GameBase.DissolveConfirm.newBuilder().setDissolved(true).build();
                                response.setOperationType(GameBase.OperationType.DISSOLVE_CONFIRM).setData(dissolveConfirm.toByteString());
                                for (Seat seat : room.getSeats()) {
                                    if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                                        messageReceive.send(response.build(), seat.getUserId());
                                    }
                                }
                                room.roomOver(response, redisService);
                            } else {
                                new DissolveTimeout(messageReceive.roomNo, redisService).start();
                            }
                        } else {
                            response.setOperationType(GameBase.OperationType.DISSOLVE).setData(GameBase.DissolveApply.newBuilder()
                                    .setError(GameBase.ErrorCode.AREADY_DISSOLVE).build().toByteString());
                            messageReceive.send(response.build(), userId);
                        }
                        redisService.unlock("lock_room" + messageReceive.roomNo);
                    }
                    break;
                case DISSOLVE_REPLY:
                    GameBase.DissolveReplyRequest dissolveReply = GameBase.DissolveReplyRequest.parseFrom(request.getData());
                    if (redisService.exists("room" + messageReceive.roomNo)) {
                        while (!redisService.lock("lock_room" + messageReceive.roomNo)) {
                        }
                        while (!redisService.lock("lock_dissolve" + messageReceive.roomNo)) {
                        }
                        if (redisService.exists("dissolve" + messageReceive.roomNo)) {
                            Room room = JSON.parseObject(redisService.getCache("room" + messageReceive.roomNo), Room.class);
                            String dissolveStatus = redisService.getCache("dissolve" + messageReceive.roomNo);
                            if (dissolveReply.getAgree()) {
                                dissolveStatus = dissolveStatus + "-1" + userId;
                            } else {
                                dissolveStatus = dissolveStatus + "-2" + userId;
                            }
                            redisService.addCache("dissolve" + messageReceive.roomNo, dissolveStatus);
                            int disagree = 0;
                            int agree = 0;
                            GameBase.DissolveReplyResponse.Builder replyResponse = GameBase.DissolveReplyResponse.newBuilder();
                            for (Seat seat : room.getSeats()) {
                                if (dissolveStatus.contains("-1" + seat.getUserId())) {
                                    replyResponse.addDissolve(GameBase.Dissolve.newBuilder().setUserId(userId).setAgree(true));
                                    agree++;
                                } else if (dissolveStatus.contains("-2" + seat.getUserId())) {
                                    replyResponse.addDissolve(GameBase.Dissolve.newBuilder().setUserId(userId).setAgree(false));
                                    disagree++;
                                }
                            }
                            response.setOperationType(GameBase.OperationType.DISSOLVE_REPLY).setData(replyResponse.build().toByteString());
                            for (Seat seat : room.getSeats()) {
                                if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                                    messageReceive.send(response.build(), seat.getUserId());
                                }
                            }

                            if (disagree >= room.getSeats().size() / 2) {
                                GameBase.DissolveConfirm dissolveConfirm = GameBase.DissolveConfirm.newBuilder().setDissolved(false).build();
                                response.setOperationType(GameBase.OperationType.DISSOLVE_CONFIRM).setData(dissolveConfirm.toByteString());
                                for (Seat seat : room.getSeats()) {
                                    if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                                        messageReceive.send(response.build(), seat.getUserId());
                                    }
                                }
                                redisService.delete("dissolve" + messageReceive.roomNo);
                                redisService.addCache("delete_dissolve" + messageReceive.roomNo, "", 60);
                            } else if (agree > room.getSeats().size() / 2) {
                                GameBase.DissolveConfirm dissolveConfirm = GameBase.DissolveConfirm.newBuilder().setDissolved(true).build();
                                response.setOperationType(GameBase.OperationType.DISSOLVE_CONFIRM).setData(dissolveConfirm.toByteString());
                                for (Seat seat : room.getSeats()) {
                                    if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                                        messageReceive.send(response.build(), seat.getUserId());
                                    }
                                }
                                room.roomOver(response, redisService);
                                redisService.delete("dissolve" + messageReceive.roomNo);
                            }
                        }
                        redisService.unlock("lock_dissolve" + messageReceive.roomNo);
                        redisService.unlock("lock_room" + messageReceive.roomNo);
                    }
                    break;
                case MESSAGE:
                    if (redisService.exists("room" + messageReceive.roomNo)) {
                        while (!redisService.lock("lock_room" + messageReceive.roomNo)) {
                        }
                        Room room = JSON.parseObject(redisService.getCache("room" + messageReceive.roomNo), Room.class);
                        GameBase.Message message = GameBase.Message.parseFrom(request.getData());

                        GameBase.Message messageResponse = GameBase.Message.newBuilder().setUserId(userId)
                                .setMessageType(message.getMessageType()).setContent(message.getContent()).build();

                        for (Seat seat : room.getSeats()) {
                            if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                                messageReceive.send(response.setOperationType(GameBase.OperationType.MESSAGE)
                                        .setData(messageResponse.toByteString()).build(), seat.getUserId());
                            }
                        }
                        redisService.unlock("lock_room" + messageReceive.roomNo);
                    }
                    break;
                case INTERACTION:
                    if (redisService.exists("room" + messageReceive.roomNo)) {
                        while (!redisService.lock("lock_room" + messageReceive.roomNo)) {
                        }
                        Room room = JSON.parseObject(redisService.getCache("room" + messageReceive.roomNo), Room.class);
                        GameBase.AppointInteraction appointInteraction = GameBase.AppointInteraction.parseFrom(request.getData());

                        GameBase.AppointInteraction appointInteractionResponse = GameBase.AppointInteraction.newBuilder().setUserId(userId)
                                .setToUserId(appointInteraction.getToUserId()).setContentIndex(appointInteraction.getContentIndex()).build();
                        for (Seat seat : room.getSeats()) {
                            if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                                messageReceive.send(response.setOperationType(GameBase.OperationType.MESSAGE)
                                        .setData(appointInteractionResponse.toByteString()).build(), seat.getUserId());
                            }
                        }
                        redisService.unlock("lock_room" + messageReceive.roomNo);
                    }
                    break;
                case ONLINE:
                    if (redisService.exists("room" + messageReceive.roomNo)) {
                        while (!redisService.lock("lock_room" + messageReceive.roomNo)) {
                        }
                        Room room = JSON.parseObject(redisService.getCache("room" + messageReceive.roomNo), Room.class);
                        GameBase.Online online = GameBase.Online.parseFrom(request.getData());
                        for (Seat seat : room.getSeats()) {
                            if (seat.getUserId() == userId) {
                                if (online.getOnline() && seat.isRobot()) {
                                    seat.setRobot(false);
                                } else if (!online.getOnline() && !seat.isRobot()) {
                                    seat.setRobot(true);
                                } else {
                                    break;
                                }
                                response.setOperationType(GameBase.OperationType.ONLINE).setData(online.toBuilder().setUserId(userId).build().toByteString());
                                for (Seat seat1 : room.getSeats()) {
                                    if (MahjongTcpService.userClients.containsKey(seat1.getUserId())) {
                                        messageReceive.send(response.build(), seat1.getUserId());
                                    }
                                }
                                redisService.addCache("room" + messageReceive.roomNo, JSON.toJSONString(room));
                            }
                        }
                        redisService.unlock("lock_room" + messageReceive.roomNo);
                    }
                    break;
                case LOGGER:
                    GameBase.LoggerRequest loggerRequest = GameBase.LoggerRequest.parseFrom(request.getData());
                    LoggerUtil.logger(userId + "----" + loggerRequest.getLogger());
                    break;
            }
        } catch (InvalidProtocolBufferException e) {
            logger.error(e.toString(), e);
        }
    }

    private void addSeat(Room room, Mahjong.MahjongGameInfo.Builder gameInfo) {
        for (Seat seat1 : room.getSeats()) {
            Mahjong.MahjongSeatGameInfo.Builder seatResponse = Mahjong.MahjongSeatGameInfo.newBuilder();
            seatResponse.setID(seat1.getUserId());
            if (null != seat1.getInitialCards()) {
                if (seat1.getUserId() == userId) {
                    seatResponse.addAllInitialCards(seat1.getInitialCards());
                }
            }
            if (null != seat1.getCards()) {
                if (seat1.getUserId() == userId) {
                    seatResponse.addAllCards(seat1.getCards());
                } else {
                    seatResponse.setCardsSize(seat1.getCards().size());
                }
            }

            if (null != seat1.getPengCards()) {
                seatResponse.addAllPengCards(seat1.getPengCards());
            }
            if (null != seat1.getAnGangCards()) {
                seatResponse.addAllAnGangCards(seat1.getAnGangCards());
            }
            if (null != seat1.getMingGangCards()) {
                seatResponse.addAllMingGangCards(seat1.getMingGangCards());
            }
            if (null != seat1.getChiCards()) {
                seatResponse.addAllChiCards(seat1.getChiCards());
            }

            if (null != seat1.getPlayedCards()) {
                seatResponse.addAllPlayedCards(seat1.getPlayedCards());
            }
            gameInfo.addSeats(seatResponse.build());
        }
    }
}