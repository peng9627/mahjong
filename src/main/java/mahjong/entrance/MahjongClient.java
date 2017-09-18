package mahjong.entrance;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.google.protobuf.InvalidProtocolBufferException;
import mahjong.constant.Constant;
import mahjong.mode.*;
import mahjong.redis.RedisService;
import mahjong.timeout.DissolveTimeout;
import mahjong.timeout.PlayCardTimeout;
import mahjong.timeout.ReadyTimeout;
import mahjong.utils.HttpUtil;
import mahjong.utils.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created date 2016/3/25
 * Author pengyi
 */
public class MahjongClient {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    public int userId;
    private RedisService redisService;
    private Integer roomNo;

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
                    if (redisService.exists("room" + roomNo)) {
                        while (!redisService.lock("lock_room" + roomNo)) {
                        }
                        Room room = JSON.parseObject(redisService.getCache("room" + roomNo), Room.class);
                        if (null == room) {
                            return;
                        }
                        for (Seat seat : room.getSeats()) {
                            if (seat.getUserId() == userId) {
                                seat.setRobot(true);
                                break;
                            }
                        }
                        room.sendSeatInfo(response);
                        redisService.addCache("room" + roomNo, JSON.toJSONString(room));
                        redisService.unlock("lock_room" + roomNo);
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
                        roomNo = Integer.valueOf(intoRequest.getRoomNo());
                        if (MahjongTcpService.userClients.containsKey(userId) && MahjongTcpService.userClients.get(userId) != messageReceive) {
                            MahjongTcpService.userClients.get(userId).close(false);
                        }
                        synchronized (this) {
                            try {
                                wait(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        MahjongTcpService.userClients.put(userId, messageReceive);
                        if (redisService.exists("room" + roomNo)) {
                            while (!redisService.lock("lock_room" + roomNo)) {
                            }

                            redisService.addCache("reconnect" + userId, "xingning_mahjong," + roomNo);

                            //是否竞技场
                            if (redisService.exists("room_match" + roomNo)) {
                                String matchNo = redisService.getCache("room_match" + roomNo);
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

                                    GameBase.MatchData matchData = GameBase.MatchData.newBuilder()
                                            .setCurrentCount(matchInfo.getMatchUsers().size())
                                            .setStartDate(matchInfo.getStartDate().getTime())
                                            .setStatus(matchInfo.getStatus()).build();
                                    messageReceive.send(response.setOperationType(GameBase.OperationType.MATCH_DATA)
                                            .setData(matchData.toByteString()).build(), userId);

                                    if (!matchInfo.isStart()) {
                                        List<Integer> roomNos = matchInfo.getRooms();
                                        for (Integer roomNo1 : roomNos) {
                                            new ReadyTimeout(roomNo1, redisService).start();
                                        }
                                    }
                                    matchInfo.setStart(true);
                                    redisService.addCache("match_info" + matchNo, JSON.toJSONString(matchInfo));
                                    redisService.unlock("lock_match_info" + matchNo);
                                }
                            }

                            Room room = JSON.parseObject(redisService.getCache("room" + roomNo), Room.class);
                            roomCardIntoResponseBuilder.setRoomOwner(room.getRoomOwner());
                            roomCardIntoResponseBuilder.setStarted(0 != room.getGameStatus().compareTo(GameStatus.READYING) && 0 != room.getGameStatus().compareTo(GameStatus.WAITING));
                            if (0 == room.getGameStatus().compareTo(GameStatus.READYING) && redisService.exists("room_match" + roomNo)) {
                                int time = 0;
                                if (redisService.exists("room_match" + roomNo)) {
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
                                    redisService.unlock("lock_room" + roomNo);
                                    break;
                                }
                            }
                            room.sendRoomInfo(roomCardIntoResponseBuilder, response, userId);
                            room.sendSeatInfo(response);
                            if (0 != room.getGameStatus().compareTo(GameStatus.WAITING)) {

                                if (0 == room.getGameStatus().compareTo(GameStatus.PLAYING)) {
                                    for (Seat seat : room.getSeats()) {
                                        if (seat.getSeatNo() == room.getOperationSeatNo()) {
                                            int time = 0;
                                            if (redisService.exists("room_match" + roomNo)) {
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
                                gameInfo.addAllDice(Arrays.asList(room.getDice()));
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
                                response.setOperationType(GameBase.OperationType.GAME_INFO).setData(gameInfo.build().toByteString());
                                messageReceive.send(response.build(), userId);

                                //才开始的时候检测是否该当前玩家出牌
                                if (0 == room.getHistoryList().size()) {
                                    for (Seat seat : room.getSeats()) {
                                        if (seat.getSeatNo() == room.getOperationSeatNo() && seat.getUserId() == userId) {
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
                                                room.checkSeatCan(operationHistory.getCard(), response, userId, operationHistory.getDate(), redisService);
                                            }
                                            break;
                                    }
                                }
                            }
                            redisService.addCache("room" + roomNo, JSON.toJSONString(room));
                            redisService.unlock("lock_room" + roomNo);

                            if (redisService.exists("dissolve" + roomNo)) {

                                String dissolveStatus = redisService.getCache("dissolve" + roomNo);
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
                                for (Seat seat : room.getSeats()) {
                                    if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                                        messageReceive.send(response.build(), seat.getUserId());
                                    }
                                }

                                GameBase.DissolveReplyResponse.Builder replyResponse = GameBase.DissolveReplyResponse.newBuilder();
                                for (Seat seat : room.getSeats()) {
                                    if (dissolveStatus.contains("-1" + seat.getUserId())) {
                                        replyResponse.addDissolve(GameBase.Dissolve.newBuilder().setUserId(userId).setAgree(true));
                                    } else if (dissolveStatus.contains("-2" + seat.getUserId())) {
                                        replyResponse.addDissolve(GameBase.Dissolve.newBuilder().setUserId(userId).setAgree(false));
                                    }
                                }
                                response.setOperationType(GameBase.OperationType.DISSOLVE_REPLY).setData(replyResponse.build().toByteString());
                                messageReceive.send(response.build(), userId);
                            }
                        } else if (redisService.exists("match_info" + roomNo)) {
                            while (!redisService.lock("lock_match_info" + roomNo)) {
                            }
                            MatchInfo matchInfo = JSON.parseObject(redisService.getCache("match_info" + roomNo), MatchInfo.class);
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
                            seatResponse.setSex(userResponse.getData().getSex().equals("MAN"));
                            seatResponse.setOffline(false);
                            roomSeatsInfo.addSeats(seatResponse.build());
                            messageReceive.send(response.setOperationType(GameBase.OperationType.SEAT_INFO).setData(roomSeatsInfo.build().toByteString()).build(), userId);
                            redisService.unlock("lock_match_info" + roomNo);
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
                    if (redisService.exists("room" + roomNo)) {
                        while (!redisService.lock("lock_room" + roomNo)) {
                        }
                        Room room = JSON.parseObject(redisService.getCache("room" + roomNo), Room.class);
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

                        redisService.addCache("room" + roomNo, JSON.toJSONString(room));
                        redisService.unlock("lock_room" + roomNo);
                    } else {
                        logger.warn("房间不存在");
                    }
                    break;
                case COMPLETED:
                    if (redisService.exists("room" + roomNo)) {
                        while (!redisService.lock("lock_room" + roomNo)) {
                        }
                        Room room = JSON.parseObject(redisService.getCache("room" + roomNo), Room.class);
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
                        redisService.addCache("room" + roomNo, JSON.toJSONString(room));
                        redisService.unlock("lock_room" + roomNo);
                    } else {
                        logger.warn("房间不存在");
                    }
                    break;
                case ACTION:
                    GameBase.BaseAction actionRequest = GameBase.BaseAction.parseFrom(request.getData());
                    logger.info("xingning 接收 " + actionRequest.getOperationId() + userId);
                    GameBase.BaseAction.Builder actionResponse = GameBase.BaseAction.newBuilder().setID(userId);
                    if (redisService.exists("room" + roomNo)) {
                        while (!redisService.lock("lock_room" + roomNo)) {
                        }
                        Room room = JSON.parseObject(redisService.getCache("room" + roomNo), Room.class);
                        switch (actionRequest.getOperationId()) {
                            case PLAY_CARD:
                                Mahjong.MahjongPlayCard playCardRequest = Mahjong.MahjongPlayCard.parseFrom(actionRequest.getData());
                                Integer card = playCardRequest.getCard();
                                room.playCard(card, userId, actionResponse, response, redisService);
                                break;
                            case PENG:
                                room.getSeats().stream().filter(seat -> seat.getUserId() == userId &&
                                        room.getOperationSeatNo() != seat.getSeatNo()).forEach(seat -> seat.setOperation(3));
                                if (room.checkSurplus()) { //如果可以碰、杠牌，则碰、杠
                                    room.pengOrGang(actionResponse, response, redisService, userId);
                                }
                                break;
                            case AN_GANG:
                            case BA_GANG:
                                Mahjong.MahjongGang gangRequest = Mahjong.MahjongGang.parseFrom(actionRequest.getData());
                                room.selfGang(actionResponse, gangRequest.getCard(), response, redisService, userId);
                                break;
                            case DIAN_GANG:
                                room.getSeats().stream().filter(seat -> seat.getUserId() == userId &&
                                        room.getOperationSeatNo() != seat.getSeatNo()).forEach(seat -> seat.setOperation(2));
                                if (room.checkSurplus()) { //如果可以碰、杠牌，则碰、杠
                                    room.pengOrGang(actionResponse, response, redisService, userId);
                                }
                                break;
                            case HU:
                                room.getSeats().stream().filter(seat -> seat.getUserId() == userId).forEach(seat -> seat.setOperation(1));
                                room.hu(userId, response, redisService);//胡
                                break;
                            case PASS:
                                room.getSeats().stream().filter(seat -> seat.getUserId() == userId).forEach(seat -> {
                                    if (room.getOperationSeatNo() != seat.getSeatNo()) {
                                        seat.setOperation(4);
                                        if (!room.passedChecked()) {//如果都操作完了，继续摸牌
                                            room.getCard(response, room.getNextSeat(), redisService);
                                        } else if (room.checkSurplus()) { //如果可以碰、杠牌，则碰、杠
                                            room.pengOrGang(actionResponse, response, redisService, userId);
                                        }
                                    } else {
                                        if (redisService.exists("room_match" + roomNo)) {
                                            new PlayCardTimeout(seat.getUserId(), String.valueOf(roomNo), room.getHistoryList().size(), room.getGameCount(), redisService).start();
                                        }
                                    }
                                });
                                break;

                        }
                        if (null != room.getRoomNo()) {
                            redisService.addCache("room" + roomNo, JSON.toJSONString(room));
                        }
                        redisService.unlock("lock_room" + roomNo);
                    } else {
                        logger.warn("房间不存在");
                    }
                    break;
                case REPLAY:
                    Xingning.XingningMahjongReplayResponse.Builder replayResponse = Xingning.XingningMahjongReplayResponse.newBuilder();
                    if (redisService.exists("room" + roomNo)) {
                        while (!redisService.lock("lock_room" + roomNo)) {
                        }
                        Room room = JSON.parseObject(redisService.getCache("room" + roomNo), Room.class);

                        Mahjong.MahjongStartResponse.Builder dealCard = Mahjong.MahjongStartResponse.newBuilder();
                        dealCard.setBanker(room.getBanker()).addAllDice(Arrays.asList(room.getDice()));
                        replayResponse.setStart(dealCard);

                        for (OperationHistory operationHistory : room.getHistoryList()) {
                            GameBase.OperationHistory.Builder builder = GameBase.OperationHistory.newBuilder();
                            builder.setID(operationHistory.getUserId());
                            builder.addCard(operationHistory.getCard());
                            switch (operationHistory.getHistoryType()) {
                                case GET_CARD:
                                    builder.setOperationId(GameBase.ActionId.GET_CARD);
                                    break;
                                case PLAY_CARD:
                                    builder.setOperationId(GameBase.ActionId.PLAY_CARD);
                                    break;
                                case PENG:
                                    builder.setOperationId(GameBase.ActionId.PENG);
                                    break;
                                case AN_GANG:
                                    builder.setOperationId(GameBase.ActionId.AN_GANG);
                                    break;
                                case DIAN_GANG:
                                    builder.setOperationId(GameBase.ActionId.DIAN_GANG);
                                    break;
                                case BA_GANG:
                                    builder.setOperationId(GameBase.ActionId.BA_GANG);
                                    break;
                                case HU:
                                    builder.setOperationId(GameBase.ActionId.HU);
                                    break;
                            }
                            replayResponse.addHistory(builder);
                        }
                        response.setOperationType(GameBase.OperationType.REPLAY).setData(replayResponse.build().toByteString());
                        messageReceive.send(response.build(), userId);
                        redisService.unlock("lock_room" + roomNo);
                    }
                    break;
                case EXIT:
                    break;
                case DISSOLVE:
                    if (redisService.exists("room" + roomNo)) {
                        while (!redisService.lock("lock_room" + roomNo)) {
                        }
                        if (!redisService.exists("dissolve" + roomNo) && !redisService.exists("delete_dissolve" + roomNo)) {
                            GameBase.DissolveApply dissolveApply = GameBase.DissolveApply.newBuilder()
                                    .setError(GameBase.ErrorCode.SUCCESS).setUserId(userId).build();
                            Room room = JSON.parseObject(redisService.getCache("room" + roomNo), Room.class);
                            redisService.addCache("dissolve" + roomNo, "-1" + userId);
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
                                new DissolveTimeout(roomNo, redisService).start();
                            }
                        } else {
                            response.setOperationType(GameBase.OperationType.DISSOLVE).setData(GameBase.DissolveApply.newBuilder()
                                    .setError(GameBase.ErrorCode.AREADY_DISSOLVE).build().toByteString());
                            messageReceive.send(response.build(), userId);
                        }
                        redisService.unlock("lock_room" + roomNo);
                    }
                    break;
                case DISSOLVE_REPLY:
                    GameBase.DissolveReplyRequest dissolveReply = GameBase.DissolveReplyRequest.parseFrom(request.getData());
                    if (redisService.exists("room" + roomNo)) {
                        while (!redisService.lock("lock_room" + roomNo)) {
                        }
                        while (!redisService.lock("lock_dissolve" + roomNo)) {
                        }
                        if (redisService.exists("dissolve" + roomNo)) {
                            Room room = JSON.parseObject(redisService.getCache("room" + roomNo), Room.class);
                            String dissolveStatus = redisService.getCache("dissolve" + roomNo);
                            if (dissolveReply.getAgree()) {
                                dissolveStatus = dissolveStatus + "-1" + userId;
                            } else {
                                dissolveStatus = dissolveStatus + "-2" + userId;
                            }
                            redisService.addCache("dissolve" + roomNo, dissolveStatus);
                            boolean confirm = true;
                            int disagree = 0;
                            GameBase.DissolveReplyResponse.Builder replyResponse = GameBase.DissolveReplyResponse.newBuilder();
                            for (Seat seat : room.getSeats()) {
                                if (dissolveStatus.contains("-1" + seat.getUserId())) {
                                    replyResponse.addDissolve(GameBase.Dissolve.newBuilder().setUserId(userId).setAgree(true));
                                } else if (dissolveStatus.contains("-2" + seat.getUserId())) {
                                    replyResponse.addDissolve(GameBase.Dissolve.newBuilder().setUserId(userId).setAgree(false));
                                    disagree++;
                                } else {
                                    confirm = false;
                                }
                            }
                            response.setOperationType(GameBase.OperationType.DISSOLVE_REPLY).setData(replyResponse.build().toByteString());
                            for (Seat seat : room.getSeats()) {
                                if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                                    messageReceive.send(response.build(), seat.getUserId());
                                }
                            }

                            if (disagree > 0) {
                                GameBase.DissolveConfirm dissolveConfirm = GameBase.DissolveConfirm.newBuilder().setDissolved(false).build();
                                response.setOperationType(GameBase.OperationType.DISSOLVE_CONFIRM).setData(dissolveConfirm.toByteString());
                                for (Seat seat : room.getSeats()) {
                                    if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                                        messageReceive.send(response.build(), seat.getUserId());
                                    }
                                }
                                redisService.delete("dissolve" + roomNo);
                                redisService.addCache("delete_dissolve" + roomNo, "", 60);
                            } else if (confirm) {
                                GameBase.DissolveConfirm dissolveConfirm = GameBase.DissolveConfirm.newBuilder().setDissolved(true).build();
                                response.setOperationType(GameBase.OperationType.DISSOLVE_CONFIRM).setData(dissolveConfirm.toByteString());
                                for (Seat seat : room.getSeats()) {
                                    if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                                        messageReceive.send(response.build(), seat.getUserId());
                                    }
                                }
                                room.roomOver(response, redisService);
                                redisService.delete("dissolve" + roomNo);
                            }
                        }
                        redisService.unlock("lock_dissolve" + roomNo);
                        redisService.unlock("lock_room" + roomNo);
                    }
                    break;
                case MESSAGE:
                    if (redisService.exists("room" + roomNo)) {
                        while (!redisService.lock("lock_room" + roomNo)) {
                        }
                        Room room = JSON.parseObject(redisService.getCache("room" + roomNo), Room.class);
                        GameBase.Message message = GameBase.Message.parseFrom(request.getData());

                        GameBase.Message messageResponse = GameBase.Message.newBuilder().setUserId(userId)
                                .setMessageType(message.getMessageType()).setContent(message.getContent()).build();

                        for (Seat seat : room.getSeats()) {
                            if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                                messageReceive.send(response.setOperationType(GameBase.OperationType.MESSAGE)
                                        .setData(messageResponse.toByteString()).build(), seat.getUserId());
                            }
                        }
                        redisService.unlock("lock_room" + roomNo);
                    }
                    break;
                case INTERACTION:
                    if (redisService.exists("room" + roomNo)) {
                        while (!redisService.lock("lock_room" + roomNo)) {
                        }
                        Room room = JSON.parseObject(redisService.getCache("room" + roomNo), Room.class);
                        GameBase.AppointInteraction appointInteraction = GameBase.AppointInteraction.parseFrom(request.getData());

                        GameBase.AppointInteraction appointInteractionResponse = GameBase.AppointInteraction.newBuilder().setUserId(userId)
                                .setToUserId(appointInteraction.getToUserId()).setContentIndex(appointInteraction.getContentIndex()).build();
                        for (Seat seat : room.getSeats()) {
                            if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                                messageReceive.send(response.setOperationType(GameBase.OperationType.MESSAGE)
                                        .setData(appointInteractionResponse.toByteString()).build(), seat.getUserId());
                            }
                        }
                        redisService.unlock("lock_room" + roomNo);
                    }
                    break;
                case LOGGER:
                    GameBase.LoggerRequest loggerRequest = GameBase.LoggerRequest.parseFrom(request.getData());
                    LoggerUtil.logger(userId + "----" + loggerRequest.getLogger());
                    break;
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    private void addSeat(Room room, Mahjong.MahjongGameInfo.Builder gameInfo) {
        for (Seat seat1 : room.getSeats()) {
            Mahjong.MahjongSeatGameInfo.Builder seatResponse = Mahjong.MahjongSeatGameInfo.newBuilder();
            seatResponse.setID(seat1.getUserId());
            seatResponse.setIsRobot(seat1.isRobot());
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