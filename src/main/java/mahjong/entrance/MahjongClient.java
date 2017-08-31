package mahjong.entrance;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.google.protobuf.InvalidProtocolBufferException;
import mahjong.mode.*;
import mahjong.redis.RedisService;
import mahjong.utils.HttpUtil;
import mahjong.utils.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Created date 2016/3/25
 * Author pengyi
 */
public class MahjongClient {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private int userId;
    private RedisService redisService;
    private String roomNo;

    private GameBase.BaseConnection.Builder response;
    private MessageReceive messageReceive;


    MahjongClient(RedisService redisService, MessageReceive messageReceive) {
        this.redisService = redisService;
        this.messageReceive = messageReceive;
        this.response = GameBase.BaseConnection.newBuilder();
    }

    void close() {
        if (0 != userId) {
            if (redisService.exists("room" + roomNo)) {
                while (!redisService.lock("lock_room" + roomNo)) {
                    Room room = JSON.parseObject(redisService.getCache("room" + roomNo), Room.class);

                    for (Seat seat : room.getSeats()) {
                        if (seat.getUserId() == userId) {
                            seat.setRobot(true);
                            break;
                        }
                    }

                    Mahjong.MahjongGameInfo.Builder gameInfo = Mahjong.MahjongGameInfo.newBuilder().setGameStatus(GameBase.GameStatus.PLAYING);
                    addSeat(room, gameInfo);
                    response.setOperationType(GameBase.OperationType.GAME_INFO).setData(gameInfo.build().toByteString());
                    messageReceive.send(response.build(), userId);

                    redisService.addCache("room" + roomNo, JSON.toJSONString(room));
                    redisService.unlock("lock_room" + roomNo);
                }
            }
        }
    }

    synchronized void receive(GameBase.BaseConnection request) {
        try {
            logger.info(userId + "接收\n" + request + "\n");
            switch (request.getOperationType()) {
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
                    ApiResponse<User> userResponse = JSON.parseObject(HttpUtil.urlConnectionByRsa("http://127.0.0.1:9999/api/user/info", jsonObject.toJSONString()), new TypeReference<ApiResponse<User>>() {
                    });
                    if (0 == userResponse.getCode()) {
                        roomNo = intoRequest.getRoomNo();
                        if (MahjongTcpService.userClients.containsKey(userId) && MahjongTcpService.userClients.get(userId) != messageReceive) {
                            MahjongTcpService.userClients.get(userId).close();
                        }
                        synchronized (this) {
                            try {
                                wait(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        MahjongTcpService.userClients.put(userId, messageReceive);
                        Xingning.XingningMahjongIntoResponse.Builder intoResponseBuilder = Xingning.XingningMahjongIntoResponse.newBuilder();
                        if (redisService.exists("room" + roomNo)) {
                            while (!redisService.lock("lock_room" + roomNo)) {
                            }

                            redisService.addCache("reconnect" + userId, "xingning_mahjong," + roomNo);

                            Room room = JSON.parseObject(redisService.getCache("room" + roomNo), Room.class);
                            //房间是否已存在当前用户，存在则为重连
                            final boolean[] find = {false};
                            room.getSeats().stream().filter(seat -> seat.getUserId() == userId).forEach(seat -> find[0] = true);
                            if (!find[0]) {
                                if (room.getCount() > room.getSeats().size()) {
                                    room.addSeat(userResponse.getData());
                                } else {
                                    roomCardIntoResponseBuilder.setError(GameBase.ErrorCode.COUNT_FULL);
                                    response.setOperationType(GameBase.OperationType.ROOM_INFO).setData(roomCardIntoResponseBuilder.build().toByteString());
                                    messageReceive.send(response.build(), userId);
                                    redisService.unlock("lock_room" + roomNo);
                                    break;
                                }
                            }
                            intoResponseBuilder.setBaseScore(room.getBaseScore());
                            intoResponseBuilder.setCount(room.getCount());
                            intoResponseBuilder.setGameTimes(room.getGameTimes());
                            intoResponseBuilder.setGameRules(room.getGameRules());
                            intoResponseBuilder.setGhost(room.getGhost());
                            intoResponseBuilder.setMaCount(room.getInitMaCount());
                            roomCardIntoResponseBuilder.setError(GameBase.ErrorCode.SUCCESS);
                            roomCardIntoResponseBuilder.setData(intoResponseBuilder.build().toByteString());
                            response.setOperationType(GameBase.OperationType.ROOM_INFO).setData(roomCardIntoResponseBuilder.build().toByteString());
                            messageReceive.send(response.build(), userId);

                            GameBase.RoomSeatsInfo.Builder roomSeatsInfo = GameBase.RoomSeatsInfo.newBuilder();
                            for (Seat seat1 : room.getSeats()) {
                                seat1.setRobot(false);
                                GameBase.SeatResponse.Builder seatResponse = GameBase.SeatResponse.newBuilder();
                                seatResponse.setSeatNo(seat1.getSeatNo());
                                seatResponse.setID(seat1.getUserId());
                                seatResponse.setScore(seat1.getScore());
                                seatResponse.setReady(seat1.isReady());
                                seatResponse.setAreaString(seat1.getAreaString());
                                seatResponse.setNickname(seat1.getNickname());
                                seatResponse.setHead(seat1.getHead());
                                seatResponse.setSex(seat1.isSex());
                                roomSeatsInfo.addSeats(seatResponse.build());
                            }
                            response.setOperationType(GameBase.OperationType.SEAT_INFO).setData(roomSeatsInfo.build().toByteString());
                            for (Seat seat : room.getSeats()) {
                                if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                                    messageReceive.send(response.build(), seat.getUserId());
                                }
                            }
                            if (0 != room.getGameStatus().compareTo(GameStatus.WAITING)) {

                                if (0 == room.getGameStatus().compareTo(GameStatus.PLAYING)) {
                                    for (Seat seat : room.getSeats()) {
                                        if (seat.getSeatNo() == room.getOperationSeatNo()) {
                                            GameBase.RoundResponse roundResponse = GameBase.RoundResponse.newBuilder().setID(seat.getUserId()).build();
                                            response.setOperationType(GameBase.OperationType.ROUND).setData(roundResponse.toByteString());
                                            messageReceive.send(response.build(), userId);
                                            break;
                                        }
                                    }
                                }

                                Mahjong.MahjongGameInfo.Builder gameInfo = Mahjong.MahjongGameInfo.newBuilder();
                                if (0 == room.getGameStatus().compareTo(GameStatus.READYING)) {
                                    gameInfo.setGameStatus(GameBase.GameStatus.READYING);
                                } else {
                                    gameInfo.setGameStatus(GameBase.GameStatus.PLAYING);
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
                                response.setOperationType(GameBase.OperationType.GAME_INFO).setData(gameInfo.build().toByteString());
                                messageReceive.send(response.build(), userId);

                                //才开始的时候检测是否该当前玩家出牌
                                if (0 == room.getHistoryList().size()) {
                                    room.checkSelfGetCard(response, operationSeat);
                                } else if (room.getHistoryList().size() > 0) {
                                    OperationHistory operationHistory = room.getHistoryList().get(room.getHistoryList().size() - 1);
                                    switch (operationHistory.getHistoryType()) {
                                        case GET_CARD:
                                            if (operationHistory.getUserId() == userId) {
                                                for (Seat seat : room.getSeats()) {
                                                    if (seat.getUserId() == userId) {
                                                        room.checkSelfGetCard(response, seat);
                                                        break;
                                                    }
                                                }
                                            }
                                            break;
                                        case PLAY_CARD:
                                            if (operationHistory.getUserId() != userId) {
                                                room.checkSeatCan(operationHistory.getCard(), response, userId);
                                            }
                                            break;
                                    }
                                }
                            }
                            redisService.addCache("room" + roomNo, JSON.toJSONString(room));
                            redisService.unlock("lock_room" + roomNo);
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
                                room.setGameCount(room.getGameCount() + 1);
                                room.setGameStatus(GameStatus.PLAYING);
                                room.dealCard();
                                //骰子
                                int dice1 = new Random().nextInt(6) + 1;
                                int dice2 = new Random().nextInt(6) + 1;
                                room.setDice(new Integer[]{dice1, dice2});
                                Mahjong.MahjongStartResponse.Builder dealCard = Mahjong.MahjongStartResponse.newBuilder();
                                dealCard.setBanker(room.getBanker()).addDice(dice1).addDice(dice2);
                                response.setOperationType(GameBase.OperationType.START);
                                room.getSeats().stream().filter(seat -> MahjongTcpService.userClients.containsKey(seat.getUserId())).forEach(seat -> {
                                    dealCard.clearCards();
                                    dealCard.addAllCards(seat.getCards());
                                    response.setData(dealCard.build().toByteString());
                                    MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
                                });

                                GameBase.RoundResponse roundResponse = GameBase.RoundResponse.newBuilder().setID(room.getBanker()).build();
                                response.setOperationType(GameBase.OperationType.ROUND).setData(roundResponse.toByteString());
                                room.getSeats().stream().filter(seat -> MahjongTcpService.userClients.containsKey(seat.getUserId()))
                                        .forEach(seat -> MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId()));

                                Seat operationSeat = null;
                                for (Seat seat : room.getSeats()) {
                                    if (room.getOperationSeatNo() == seat.getSeatNo()) {
                                        operationSeat = seat;
                                        break;
                                    }
                                }

                                room.checkSelfGetCard(response, operationSeat);

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
                    logger.info(userId + "Action\n" + actionRequest + "\n");
                    GameBase.BaseAction.Builder actionResponse = GameBase.BaseAction.newBuilder().setID(userId);
                    if (redisService.exists("room" + roomNo)) {
                        while (!redisService.lock("lock_room" + roomNo)) {
                        }
                        Room room = JSON.parseObject(redisService.getCache("room" + roomNo), Room.class);
                        switch (actionRequest.getOperationId()) {
                            case PLAY_CARD:
                                Mahjong.MahjongPlayCard playCardRequest = Mahjong.MahjongPlayCard.parseFrom(actionRequest.getData());
                                final boolean[] shouldOperation = {false};
                                final Seat[] operationSeat = new Seat[1];
                                room.getSeats().stream().filter(seat -> seat.getUserId() == userId).forEach(seat -> {
                                    if (room.getOperationSeatNo() == seat.getSeatNo()) {
                                        shouldOperation[0] = true;
                                        operationSeat[0] = seat;
                                    } else {
                                        logger.warn("不该当前玩家操作" + userId);
                                    }
                                });
                                if (!shouldOperation[0]) {
                                    break;
                                }
                                Integer card = playCardRequest.getCard();
                                if (operationSeat[0].getCards().contains(card)) {
                                    operationSeat[0].getCards().remove(card);
                                    if (null == operationSeat[0].getPlayedCards()) {
                                        operationSeat[0].setPlayedCards(new ArrayList<>());
                                    }
                                    operationSeat[0].getPlayedCards().add(card);
                                    Mahjong.MahjongPlayCard.Builder builder = Mahjong.MahjongPlayCard.newBuilder().setCard(playCardRequest.getCard());

                                    actionResponse.setOperationId(GameBase.ActionId.PLAY_CARD).setData(builder.build().toByteString());

                                    response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                                    room.setLastOperation(userId);
                                    room.getHistoryList().add(new OperationHistory(userId, OperationHistoryType.PLAY_CARD, card));
                                    room.getSeats().stream().filter(seat -> MahjongTcpService.userClients.containsKey(seat.getUserId()))
                                            .forEach(seat -> MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId()));
                                    //先检查其它三家牌，是否有人能胡、杠、碰
                                    room.checkCard(card, response, redisService, userId);
                                } else {
                                    logger.warn("用户手中没有此牌" + userId);
                                }

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
                                room.getSeats().stream().filter(seat -> seat.getUserId() == userId &&
                                        room.getOperationSeatNo() != seat.getSeatNo()).forEach(seat -> seat.setOperation(1));
                                room.hu(userId, response, redisService);//胡
                                break;
                            case PASS:
                                room.getSeats().stream().filter(seat -> seat.getUserId() == userId &&
                                        room.getOperationSeatNo() != seat.getSeatNo()).forEach(seat -> {
                                    seat.setOperation(4);
                                    if (!room.passedChecked()) {//如果都操作完了，继续摸牌
                                        room.getCard(response, room.getNextSeat(), redisService);
                                    } else if (room.checkSurplus()) { //如果可以碰、杠牌，则碰、杠
                                        room.pengOrGang(actionResponse, response, redisService, userId);
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
                        Room room = JSON.parseObject(redisService.getCache("room" + roomNo), Room.class);
                        response.setOperationType(GameBase.OperationType.DISSOLVE).clearData();
                        for (Seat seat : room.getSeats()) {
                            if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                                messageReceive.send(response.build(), seat.getUserId());
                            }
                        }
                        room.roomOver(response, redisService);
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