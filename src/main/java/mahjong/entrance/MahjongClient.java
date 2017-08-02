package mahjong.entrance;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.MessageLite;
import mahjong.mode.*;
import mahjong.redis.RedisService;
import mahjong.utils.ByteUtils;
import mahjong.utils.CoreStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created date 2016/3/25
 * Author pengyi
 */
public class MahjongClient implements Runnable {

    private final InputStream is;
    private final OutputStream os;
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private Socket s;
    private String user;
    private RedisService redisService;
    private String roomNo;
    private Boolean connect;
    private byte[] md5Key = "2704031cd4814eb2a82e47bd1d9042c6".getBytes();

    private GameBase.BaseConnection request;
    private GameBase.BaseConnection.Builder response;

    MahjongClient(Socket s, RedisService redisService) {
        this.s = s;
        connect = true;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = s.getInputStream();
            outputStream = s.getOutputStream();
            this.redisService = redisService;
        } catch (EOFException e) {
            logger.info("socket.shutdown.message");
            close();
        } catch (IOException e) {
            logger.info("socket.connection.fail.message" + e.getMessage());
            close();
        }
        is = inputStream;
        os = outputStream;
        request = GameBase.BaseConnection.newBuilder().build();
        response = GameBase.BaseConnection.newBuilder();
    }

    private void send(MessageLite lite, String username) {
        try {
            if (MahjongTcpService.userClients.containsKey(username)) {
                synchronized (MahjongTcpService.userClients.get(username).os) {
                    OutputStream os = MahjongTcpService.userClients.get(username).os;
                    String md5 = CoreStringUtils.md5(ByteUtils.addAll(md5Key, lite.toByteArray()), 32, false);
                    int len = lite.toByteArray().length + md5.getBytes().length + 4;
                    writeInt(os, len);
                    writeString(os, md5);
                    os.write(lite.toByteArray());
                    logger.info("mahjong send:len=" + len + "user=" + username);
                }
            }
        } catch (IOException e) {
            logger.info("socket.server.sendMessage.fail.message" + username + e.getMessage());
//            client.close();
        }
    }

    private void close() {
        connect = false;
        try {
            if (is != null)
                is.close();
            if (os != null)
                os.close();
            if (s != null) {
                s.close();
            }
            if (null != user) {
//                exit();
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private int readInt(InputStream is) throws IOException {
        int ch1 = is.read();
        int ch2 = is.read();
        int ch3 = is.read();
        int ch4 = is.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }
        return (ch1 << 24 | ((ch2 << 16) & 0xff) | ((ch3 << 8) & 0xff) | (ch4 & 0xFF));
    }

    private String readString(InputStream is) throws IOException {
        int len = readInt(is);
        byte[] bytes = new byte[len];
        is.read(bytes);
        return new String(bytes);
    }

    private void writeInt(OutputStream stream, int value) throws IOException {
        stream.write((value >>> 24) & 0xFF);
        stream.write((value >>> 16) & 0xFF);
        stream.write((value >>> 8) & 0xFF);
        stream.write((value) & 0xFF);
    }

    private void writeString(OutputStream stream, String value) throws IOException {
        byte[] bytes = value.getBytes();
        writeInt(stream, bytes.length);
        stream.write(bytes);
    }

    @Override
    public void run() {
        try {
            while (connect) {

                int len = readInt(is);
                String md5 = readString(is);
                len -= md5.getBytes().length + 4;
                byte[] data = new byte[len];
                boolean check = true;
                if (0 != len) {
                    check = len == is.read(data) && CoreStringUtils.md5(ByteUtils.addAll(md5Key, data), 32, false).equalsIgnoreCase(md5);
                }
                if (check) {
                    request = GameBase.BaseConnection.parseFrom(data);
                    switch (request.getOperationType()) {
                        case CONNECTION:
                            //加入玩家数据
                            if (redisService.exists("maintenance")) {
                                break;
                            }
                            Mahjong.RoomCardIntoRequest intoRequest = Mahjong.RoomCardIntoRequest.parseFrom(request.getData());
                            user = intoRequest.getId();
                            roomNo = intoRequest.getRoomNo();
                            MahjongTcpService.userClients.put(user, this);
                            Mahjong.RoomCardIntoResponse.Builder intoResponseBuilder = Mahjong.RoomCardIntoResponse.newBuilder();
                            if (redisService.exists("room" + roomNo)) {
                                redisService.lock("lock_room" + roomNo);
                                Room room = JSON.parseObject(redisService.getCache("room" + roomNo), Room.class);
                                //房间是否已存在当前用户，存在则为重连
                                final boolean[] find = {false};
                                room.getSeats().stream().filter(seat -> seat.getUserName().equals(user)).forEach(seat -> find[0] = true);
                                if (!find[0]) {
                                    if (room.getCount() < room.getSeats().size()) {
                                        User user = new User();
                                        user.setUsername(this.user);
                                        room.addSeat(user);
                                    } else {
                                        intoResponseBuilder.setError(GameBase.ErrorCode.COUNT_FULL);
                                        response.setOperationType(GameBase.OperationType.CONNECTION).setData(intoResponseBuilder.build().toByteString());
                                        send(response.build(), user);
                                        redisService.unlock("lock_room" + roomNo);
                                        break;
                                    }
                                }
                                response.setOperationType(GameBase.OperationType.ROOM_INFO).setData(intoResponseBuilder.build().toByteString());
                                send(response.build(), user);

                                Mahjong.RoomSeatsInfo.Builder roomSeatsInfo = Mahjong.RoomSeatsInfo.newBuilder();
                                for (Seat seat1 : room.getSeats()) {
                                    Mahjong.SeatResponse.Builder seatResponse = Mahjong.SeatResponse.newBuilder();
                                    seatResponse.setSeatNo(seat1.getSeatNo());
                                    seatResponse.setID(seat1.getUserName());
                                    seatResponse.setGold(seat1.getGold());
                                    seatResponse.setIsReady(seat1.isReady());
                                    seatResponse.setAreaString(seat1.getAreaString());
                                    roomSeatsInfo.addSeats(seatResponse.build());
                                }
                                response.setOperationType(GameBase.OperationType.SEAT_INFO).setData(roomSeatsInfo.build().toByteString());
                                send(response.build(), user);

                                if (0 == room.getGameStatus().compareTo(GameStatus.PLAYING)) {
                                    Mahjong.GameInfo.Builder gameInfo = Mahjong.GameInfo.newBuilder().setGameStatus(Mahjong.GameStatus.PLAYING);
                                    gameInfo.setOperationUser(room.getSeats().get(room.getOperationSeat() - 1).getUserName());
                                    gameInfo.setLastOperationUser(room.getLastOperation());
                                    for (Seat seat1 : room.getSeats()) {
                                        Mahjong.SeatGameInfo.Builder seatResponse = Mahjong.SeatGameInfo.newBuilder();
                                        seatResponse.setID(seat1.getUserName());
                                        seatResponse.setScore(seat1.getScore());
                                        seatResponse.setIsRobot(seat1.isRobot());
                                        if (null != seat1.getCards()) {
                                            if (seat1.getUserName().equals(user)) {
                                                seatResponse.addAllCards(seat1.getCards());
                                            } else {
                                                seatResponse.setCardsSize(seat1.getCards().size());
                                            }
                                        }

                                        if (null != seat1.getInvertedCards()) {
                                            seatResponse.addAllInvertedCards(seat1.getInvertedCards());
                                        }

                                        if (null != seat1.getPlayedCards()) {
                                            seatResponse.addAllPlayedCards(seat1.getPlayedCards());
                                        }
                                        gameInfo.addSeats(seatResponse.build());
                                    }
                                    response.setOperationType(GameBase.OperationType.GAME_INFO).setData(gameInfo.build().toByteString());
                                    send(response.build(), user);

                                    //才开始的时候检测是否该当前玩家出牌
                                    if (room.getHistoryList().size() == 0 && room.getBanker().equals(user)) {
                                        Mahjong.RoundResponse roundResponse = Mahjong.RoundResponse.newBuilder().setID(user).build();
                                        response.setOperationType(GameBase.OperationType.ROUND).setData(roundResponse.toByteString());
                                        send(response.build(), user);
                                    } else {
                                        OperationHistory operationHistory = room.getHistoryList().get(room.getHistoryList().size() - 1);
                                        switch (operationHistory.getHistoryType()) {
                                            case GET_CARD:
                                            case PENG:
                                                Mahjong.RoundResponse roundResponse = Mahjong.RoundResponse.newBuilder().setID(user).build();
                                                response.setOperationType(GameBase.OperationType.ROUND).setData(roundResponse.toByteString());
                                                send(response.build(), user);
                                                break;
                                            case PLAY_CARD:
                                                if (!operationHistory.getUserName().equals(user)) {
                                                    checkSeatCan(room, operationHistory.getCard());
                                                }
                                                break;
                                        }
                                    }
                                }
                                redisService.addCache("room" + roomNo, JSON.toJSONString(room));
                                redisService.unlock("lock_room" + roomNo);
                            } else {
                                intoResponseBuilder.setError(GameBase.ErrorCode.ROOM_NOT_EXIST);
                                response.setOperationType(GameBase.OperationType.CONNECTION).setData(intoResponseBuilder.build().toByteString());
                                send(response.build(), user);
                            }
                            break;
                        case READY:
                            Mahjong.BaseAction.Builder actionResponse = Mahjong.BaseAction.newBuilder();
                            if (redisService.exists("room" + roomNo)) {
                                redisService.lock("lock_room" + roomNo);
                                Room room = JSON.parseObject(redisService.getCache("room" + roomNo), Room.class);
                                room.getSeats().stream().filter(seat -> seat.getUserName().equals(user) && !seat.isReady()).forEach(seat -> {
                                    seat.setReady(true);
                                    response.setOperationType(GameBase.OperationType.READY).setData(Mahjong.ReadyResponse.newBuilder().setID(seat.getUserName()).build().toByteString());
                                    room.getSeats().stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserName())).forEach(seat1 ->
                                            MahjongTcpService.userClients.get(seat1.getUserName()).send(response.build(), seat1.getUserName()));
                                });
                                boolean allReady = true;
                                for (Seat seat : room.getSeats()) {
                                    if (!seat.isReady()) {
                                        allReady = false;
                                        break;
                                    }
                                }
                                if (allReady && room.getCount() == room.getSeats().size()) {
                                    room.setGameStatus(GameStatus.PLAYING);
                                    room.dealCard();
                                    //骰子
                                    int dice1 = new Random().nextInt(6) + 1;
                                    int dice2 = new Random().nextInt(6) + 1;
                                    room.setDice(new Integer[]{dice1, dice2});
                                    Mahjong.DealCardResponse.Builder dealCard = Mahjong.DealCardResponse.newBuilder();
                                    dealCard.setBanker(room.getBanker()).addDice(dice1).addDice(dice2);
                                    actionResponse.setOperationId(GameBase.OperationId.DEAL_CARD);
                                    response.setOperationType(GameBase.OperationType.ACTION);
                                    room.getSeats().stream().filter(seat -> MahjongTcpService.userClients.containsKey(seat.getUserName())).forEach(seat -> {
                                        dealCard.clearCards();
                                        dealCard.addAllCards(seat.getCards());
                                        actionResponse.setData(dealCard.build().toByteString());
                                        response.setData(actionResponse.build().toByteString());
                                        MahjongTcpService.userClients.get(seat.getUserName()).send(response.build(), seat.getUserName());
                                    });


                                    Mahjong.RoundResponse roundResponse = Mahjong.RoundResponse.newBuilder().setID(room.getBanker()).build();
                                    response.setOperationType(GameBase.OperationType.ROUND).setData(roundResponse.toByteString());
                                    room.getSeats().stream().filter(seat -> MahjongTcpService.userClients.containsKey(seat.getUserName()))
                                            .forEach(seat -> MahjongTcpService.userClients.get(seat.getUserName()).send(response.build(), seat.getUserName()));
                                }
                                redisService.addCache("room" + roomNo, JSON.toJSONString(room));
                            } else {
                                logger.warn("房间不存在");
                            }
                            break;
                        case COMPLETED:
                            if (redisService.exists("room" + roomNo)) {
                                redisService.lock("lock_room" + roomNo);
                                Room room = JSON.parseObject(redisService.getCache("room" + roomNo), Room.class);
                                room.getSeats().stream().filter(seat -> seat.getUserName().equals(user) && !seat.isCompleted())
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
                            } else {
                                logger.warn("房间不存在");
                            }
                            break;
                        case ACTION:
                            Mahjong.BaseAction actionRequest = Mahjong.BaseAction.parseFrom(request.getData());
                            actionResponse = Mahjong.BaseAction.newBuilder();
                            if (redisService.exists("room" + roomNo)) {
                                redisService.lock("lock_room" + roomNo);
                                Room room = JSON.parseObject(redisService.getCache("room" + roomNo), Room.class);
                                switch (actionRequest.getOperationId()) {
                                    case PLAY_CARD:
                                        Mahjong.PlayCardRequest playCardRequest = Mahjong.PlayCardRequest.parseFrom(actionRequest.getData());
                                        final boolean[] shouldOperation = {false};
                                        final Seat[] operationSeat = new Seat[1];
                                        room.getSeats().stream().filter(seat -> seat.getUserName().equals(user)).forEach(seat -> {
                                            if (room.getOperationSeat() == seat.getSeatNo()) {
                                                shouldOperation[0] = true;
                                                operationSeat[0] = seat;
                                            } else {
                                                logger.warn("不该当前玩家操作" + user);
                                            }
                                        });
                                        if (!shouldOperation[0]) {
                                            break;
                                        }
                                        Integer card = playCardRequest.getCard();
                                        if (operationSeat[0].getCards().contains(card)) {
                                            operationSeat[0].getCards().remove(card);
                                            operationSeat[0].getPlayedCards().add(card);
                                            Mahjong.PlayCardResponse.Builder builder = Mahjong.PlayCardResponse.newBuilder()
                                                    .setID(operationSeat[0].getUserName()).setCard(playCardRequest.getCard());

                                            actionResponse.setOperationId(GameBase.OperationId.PLAY_CARD).setData(builder.build().toByteString());

                                            response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                                            room.setLastOperation(user);
                                            room.getHistoryList().add(new OperationHistory(user, OperationHistoryType.PLAY_CARD, card));
                                            room.getSeats().stream().filter(seat -> MahjongTcpService.userClients.containsKey(seat.getUserName()))
                                                    .forEach(seat -> MahjongTcpService.userClients.get(seat.getUserName()).send(response.build(), seat.getUserName()));
                                            //先检查其它三家牌，是否有人能胡、杠、碰
                                            checkCard(room, card);
                                        } else {
                                            logger.warn("用户手中没有此牌" + user);
                                        }

                                        break;
                                    case TOUCH:
                                        room.getSeats().stream().filter(seat -> seat.getUserName().equals(user) &&
                                                room.getOperationSeat() == seat.getSeatNo()).forEach(seat -> seat.setOperation(3));
                                        if (checkSurplus(room)) { //如果可以碰、杠牌，则碰、杠
                                            pengOrGang(room, actionResponse);
                                        }
                                        break;
                                    case AN_GANG:
                                    case BA_GANG:
                                        Mahjong.GangRequest gangRequest = Mahjong.GangRequest.parseFrom(actionRequest.getData());
                                        selfGang(room, actionResponse, gangRequest.getCard());
                                        break;
                                    case DIAN_GANG:
                                        room.getSeats().stream().filter(seat -> seat.getUserName().equals(user) &&
                                                room.getOperationSeat() == seat.getSeatNo()).forEach(seat -> seat.setOperation(2));
                                        if (checkSurplus(room)) { //如果可以碰、杠牌，则碰、杠
                                            pengOrGang(room, actionResponse);
                                        }
                                        break;
                                    case HU:
                                        room.getSeats().stream().filter(seat -> seat.getUserName().equals(user) &&
                                                room.getOperationSeat() == seat.getSeatNo()).forEach(seat -> seat.setOperation(1));
                                        hu(room);//胡
                                        break;
                                    case PASS:
                                        room.getSeats().stream().filter(seat -> seat.getUserName().equals(user) &&
                                                room.getOperationSeat() == seat.getSeatNo()).forEach(seat -> seat.setOperation(4));

                                        if (!passedChecked(room)) {//如果都操作完了，继续摸牌
                                            getCard(room);
                                        } else if (checkSurplus(room)) { //如果可以碰、杠牌，则碰、杠
                                            pengOrGang(room, actionResponse);
                                        }
                                        break;
                                }
                                redisService.addCache("room" + roomNo, JSON.toJSONString(room));
                                redisService.unlock("lock_room" + roomNo);
                            } else {
                                logger.warn("房间不存在");
                            }
                            break;
                        case REPLAY:
                            Mahjong.ReplayResponse.Builder replayResponse = Mahjong.ReplayResponse.newBuilder();
                            if (redisService.exists("room" + roomNo)) {
                                redisService.lock("lock_room" + roomNo);
                                Room room = JSON.parseObject(redisService.getCache("room" + roomNo), Room.class);
                                for (OperationHistory operationHistory : room.getHistoryList()) {
                                    Mahjong.OperationHistory.Builder builder = Mahjong.OperationHistory.newBuilder();
                                    builder.setID(operationHistory.getUserName());
                                    builder.setCard(operationHistory.getCard());
                                    switch (operationHistory.getHistoryType()) {
                                        case GET_CARD:
                                            builder.setOperationId(GameBase.OperationId.GET_CARD);
                                            break;
                                        case PLAY_CARD:
                                            builder.setOperationId(GameBase.OperationId.PLAY_CARD);
                                            break;
                                        case PENG:
                                            builder.setOperationId(GameBase.OperationId.TOUCH);
                                            break;
                                        case AN_GANG:
                                            builder.setOperationId(GameBase.OperationId.AN_GANG);
                                            break;
                                        case DIAN_GANG:
                                            builder.setOperationId(GameBase.OperationId.DIAN_GANG);
                                            break;
                                        case BA_GANG:
                                            builder.setOperationId(GameBase.OperationId.BA_GANG);
                                            break;
                                        case HU:
                                            builder.setOperationId(GameBase.OperationId.HU);
                                            break;
                                    }
                                    replayResponse.addHistory(builder);
                                }
                                response.setOperationType(GameBase.OperationType.REPLAY).setData(replayResponse.build().toByteString());
                                send(response.build(), user);
                                redisService.unlock("lock_room" + roomNo);
                            }
                            break;
                        case EXIT:
                            break;
                    }
                }
            }
        } catch (EOFException e) {
            logger.info("socket.shutdown.message");
            close();
        } catch (IOException e) {
            logger.info("socket.dirty.shutdown.message" + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            logger.info("socket.dirty.shutdown.message");
            e.printStackTrace();
        }
    }

    /**
     * 出牌后检查是否有人能胡、杠、碰
     *
     * @param room 桌数据
     * @param card 当前出的牌
     */

    private void checkCard(Room room, Integer card) {
        Mahjong.AskResponse.Builder builder = Mahjong.AskResponse.newBuilder();
        //先检查胡，胡优先
        room.getSeats().stream().filter(seat -> seat.getSeatNo() != room.getOperationSeat()).forEach(seat -> {
            builder.clearOperationId();
            List<Integer> temp = new ArrayList<>();
            temp.addAll(seat.getCards());

            //当前玩家手里有几张牌，3张可碰可杠，两张只能碰
            int containSize = Card.containSize(temp, card);
            if (3 == containSize && 0 < room.getSurplusCards().size()) {
                builder.addOperationId(GameBase.OperationId.TOUCH);
                builder.addOperationId(GameBase.OperationId.DIAN_GANG);
            } else if (2 == containSize) {
                builder.addOperationId(GameBase.OperationId.TOUCH);
            }
            //当前玩家是否可以胡牌
            temp.add(card);
            if (MahjongUtil.hu(temp)) {
                builder.addOperationId(GameBase.OperationId.HU);
            }
            if (0 != builder.getOperationIdCount()) {
                if (MahjongTcpService.userClients.containsKey(seat.getUserName())) {
                    response.clear();
                    response.setOperationType(GameBase.OperationType.ASK).setData(builder.build().toByteString());
                    MahjongTcpService.userClients.get(seat.getUserName()).send(response.build(), seat.getUserName());
                }
                //TODO 出牌超时
//                new OperationTimeout(deskNo, card).start();
            } else {//如果没有人可以胡、碰、杠，游戏继续，下家摸牌；
                getCard(room);
            }
        });
    }

    /**
     * 出牌后检查是否有人能胡、杠、碰
     *
     * @param room 桌数据
     * @param card 当前出的牌
     */
    private void checkSeatCan(Room room, Integer card) {
        Mahjong.AskResponse.Builder builder = Mahjong.AskResponse.newBuilder();
        //先检查胡，胡优先
        room.getSeats().stream().filter(seat -> seat.getUserName().equals(user)).forEach(seat -> {
            builder.clearOperationId();
            List<Integer> temp = new ArrayList<>();
            temp.addAll(seat.getCards());

            //当前玩家手里有几张牌，3张可碰可杠，两张只能碰
            int containSize = Card.containSize(temp, card);
            if (3 == containSize && 0 < room.getSurplusCards().size()) {
                builder.addOperationId(GameBase.OperationId.TOUCH);
                builder.addOperationId(GameBase.OperationId.DIAN_GANG);
            } else if (2 == containSize) {
                builder.addOperationId(GameBase.OperationId.TOUCH);
            }
            //当前玩家是否可以胡牌
            temp.add(card);
            if (MahjongUtil.hu(temp)) {
                builder.addOperationId(GameBase.OperationId.HU);
            }
            if (0 != builder.getOperationIdCount()) {
                if (MahjongTcpService.userClients.containsKey(seat.getUserName())) {
                    response.clear();
                    response.setOperationType(GameBase.OperationType.ASK).setData(builder.build().toByteString());
                    MahjongTcpService.userClients.get(seat.getUserName()).send(response.build(), seat.getUserName());
                }
                //TODO 出牌超时
//                new OperationTimeout(deskNo, card).start();
            }
        });
    }

    /**
     * 当有人胡、碰、杠后，再次检查是否还有人胡、碰、杠
     *
     * @param room 桌数据
     */
    private boolean checkSurplus(Room room) {
        //找到那张牌
        final Integer[] card = new Integer[1];
        room.getSeats().stream().filter(seat -> seat.getSeatNo() == room.getOperationSeat())
                .forEach(seat -> card[0] = seat.getPlayedCards().get(seat.getPlayedCards().size() - 1));
        final boolean[] hu = {false};
        //先检查胡，胡优先
        room.getSeats().stream().filter(seat -> seat.getSeatNo() != room.getOperationSeat()).forEach(seat -> {
            List<Integer> temp = new ArrayList<>();
            temp.addAll(seat.getCards());

            //当前玩家是否可以胡牌
            temp.add(card[0]);
            if (MahjongUtil.hu(temp) && seat.getOperation() != 4) {
                hu[0] = true;
            }
        });
        return !hu[0];
    }

    /**
     * 检查是否还需要操作
     *
     * @param room 桌数据
     */
    private boolean passedChecked(Room room) {
        //找到那张牌
        final Integer[] card = new Integer[1];
        room.getSeats().stream().filter(seat -> seat.getSeatNo() == room.getOperationSeat())
                .forEach(seat -> card[0] = seat.getPlayedCards().get(seat.getPlayedCards().size() - 1));
        final boolean[] hasNoOperation = {false};
        //先检查胡，胡优先
        room.getSeats().stream().filter(seat -> seat.getSeatNo() != room.getOperationSeat()).forEach(seat -> {
            List<Integer> temp = new ArrayList<>();
            temp.addAll(seat.getCards());

            //当前玩家是否可以胡牌
            temp.add(card[0]);
            if (MahjongUtil.hu(temp) && seat.getOperation() != 4) {
                hasNoOperation[0] = true;
            }

            //当前玩家手里有几张牌，3张可碰可杠，两张只能碰
            int containSize = Card.containSize(temp, card[0]);
            if (4 == containSize && 0 < room.getSurplusCards().size() && seat.getOperation() != 4) {
                hasNoOperation[0] = true;
            } else if (3 == containSize && seat.getOperation() != 4) {
                hasNoOperation[0] = true;
            }
        });

        return hasNoOperation[0];
    }

    /**
     * 检测单个玩家是否可以碰或者港
     *
     * @param room 桌数据
     */
    private void pengOrGang(Room room, Mahjong.BaseAction.Builder actionResponse) {
        //找到那张牌
        final Integer[] card = new Integer[1];
        room.getSeats().stream().filter(seat -> seat.getSeatNo() == room.getOperationSeat()).forEach(seat ->
                card[0] = seat.getPlayedCards().get(seat.getPlayedCards().size() - 1));
        //碰或者杠
        room.getSeats().stream().filter(seat -> seat.getSeatNo() != room.getOperationSeat()).forEach(seat -> {
            List<Integer> temp = new ArrayList<>();
            temp.addAll(seat.getCards());

            //当前玩家手里有几张牌，3张可碰可杠，两张只能碰
            int containSize = Card.containSize(temp, card[0]);
            if (3 == containSize && 0 < room.getSurplusCards().size() && seat.getOperation() == 2) {//杠牌
                List<Integer> cardList = new ArrayList<>();
                cardList.add(card[0]);
                cardList.add(card[0]);
                cardList.add(card[0]);
                seat.getCards().removeAll(cardList);
                cardList.add(card[0]);
                seat.getInvertedCards().addAll(cardList);

                //添加结算
                List<ScoreType> scoreTypes = new ArrayList<>();
                scoreTypes.add(ScoreType.DIAN_GANG);
                room.getSeats().get(room.getOperationSeat() - 1).getGameResults().add(new GameResult(scoreTypes, -3));
                seat.getGameResults().add(new GameResult(scoreTypes, 3));
                room.getHistoryList().add(new OperationHistory(user, OperationHistoryType.DIAN_GANG, card[0]));

                actionResponse.setOperationId(GameBase.OperationId.DIAN_GANG).setData(Mahjong.GangResponse.newBuilder()
                        .setID(seat.getUserName()).build().toByteString());
                response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                room.getSeats().stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserName()))
                        .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserName()).send(response.build(), seat1.getUserName()));

                //点杠后需要摸牌
                getCard(room, seat.getSeatNo());
            } else if (2 <= containSize && seat.getOperation() == 3) {//碰
                List<Integer> cardList = new ArrayList<>();
                cardList.add(card[0]);
                cardList.add(card[0]);
                seat.getCards().removeAll(cardList);
                cardList.add(card[0]);
                seat.getInvertedCards().addAll(cardList);
                room.setOperationSeat(seat.getSeatNo());
                room.getHistoryList().add(new OperationHistory(user, OperationHistoryType.PENG, card[0]));

                actionResponse.setOperationId(GameBase.OperationId.TOUCH).setData(Mahjong.PengResponse.newBuilder().setID(seat.getUserName()).build().toByteString());
                response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                room.getSeats().stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserName()))
                        .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserName()).send(response.build(), seat1.getUserName()));

                Mahjong.RoundResponse roundResponse = Mahjong.RoundResponse.newBuilder().setID(seat.getUserName()).build();
                response.setOperationType(GameBase.OperationType.ROUND).setData(roundResponse.toByteString());
                room.getSeats().stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserName()))
                        .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserName()).send(response.build(), seat1.getUserName()));
            }
        });
    }

    /**
     * 暗杠或者扒杠
     *
     * @param room 桌数据
     */
    private void selfGang(Room room, Mahjong.BaseAction.Builder actionResponse, Integer card) {
        //碰或者杠
        room.getSeats().stream().filter(seat -> seat.getSeatNo() == room.getOperationSeat()).forEach(seat -> {
            if (4 == Card.containSize(seat.getCards(), card)) {//暗杠
                seat.getCards().remove(card);
                seat.getCards().remove(card);
                seat.getCards().remove(card);
                seat.getCards().remove(card);

                seat.getInvertedCards().add(card);
                seat.getInvertedCards().add(card);
                seat.getInvertedCards().add(card);
                seat.getInvertedCards().add(card);
                seat.getInvertedIndex().add(0);


                List<ScoreType> scoreTypes = new ArrayList<>();
                scoreTypes.add(ScoreType.AN_GANG);

                final int[] loseSize = {0};
                room.getSeats().stream().filter(seat1 -> seat1.getSeatNo() != seat.getSeatNo())
                        .forEach(seat1 -> {
                            seat.getGameResults().add(new GameResult(scoreTypes, -2));
                            loseSize[0]++;
                        });
                seat.getGameResults().add(new GameResult(scoreTypes, 2 * loseSize[0]));


                actionResponse.setOperationId(GameBase.OperationId.AN_GANG).setData(Mahjong.GangResponse.newBuilder().setID(seat.getUserName()).setCard(card).build().toByteString());
                response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                room.getSeats().stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserName()))
                        .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserName()).send(response.build(), seat1.getUserName()));
                getCard(room, seat.getSeatNo());
            } else if (3 == Card.containSize(seat.getInvertedCards(), card) && 1 == Card.containSize(seat.getCards(), card)) {//扒杠
                seat.getCards().remove(card);
                Integer card1 = seat.getInvertedCards().get(0);
                for (int i = 0, size = seat.getInvertedCards().size(), index = 0; i < size; ) {
                    if (card1.intValue() != seat.getInvertedCards().get(i)) {
                        card1 = seat.getInvertedCards().get(i);
                        index++;
                        i += 3;
                    } else {
                        i++;
                    }
                    if (card == card1.intValue()) {
                        seat.getInvertedCards().add(index, card);
                        seat.getInvertedIndex().set(index, seat.getSeatNo());
                        break;
                    }
                }
                seat.getInvertedCards().add(card);

                List<ScoreType> scoreTypes = new ArrayList<>();
                scoreTypes.add(ScoreType.BA_GANG);

                final int[] loseSize = {0};
                room.getSeats().stream().filter(seat1 -> seat1.getSeatNo() != seat.getSeatNo())
                        .forEach(seat1 -> {
                            seat1.getGameResults().add(new GameResult(scoreTypes, -1));
                            loseSize[0]++;
                        });
                seat.getGameResults().add(new GameResult(scoreTypes, loseSize[0]));


                actionResponse.setOperationId(GameBase.OperationId.BA_GANG).setData(Mahjong.GangResponse.newBuilder().setID(seat.getUserName()).setCard(card).build().toByteString());
                response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                room.getSeats().stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserName()))
                        .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserName()).send(response.build(), seat1.getUserName()));
                getCard(room, seat.getSeatNo());
            }
        });
    }


    private void hu(Room room) {
        //和牌的人
        final Seat[] huSeat = new Seat[1];
        room.getSeats().stream().filter(seat -> seat.getUserName().equals(user))
                .forEach(seat -> huSeat[0] = seat);
        //检查是自摸还是点炮,自摸输家是其它三家
        if (MahjongUtil.hu(huSeat[0].getCards())) {

            List<ScoreType> scoreTypes = MahjongUtil.getHuType(huSeat[0].getCards(), huSeat[0].getInvertedCards());
            int score = MahjongUtil.getScore(scoreTypes);

            //天胡
            if (room.getHistoryList().size() == 0 && score < 20) {
                scoreTypes.clear();
                scoreTypes.add(ScoreType.TIAN_HU);
                score = 20;
            } else {
                scoreTypes.add(ScoreType.ZIMO_HU);
                score += 2;
            }
            int loseSize[] = {0};
            room.getSeats().stream().filter(seat -> !seat.getUserName().equals(user))
                    .forEach(seat -> {
                        seat.getGameResults().add(new GameResult(scoreTypes, loseSize[0]));
                        loseSize[0]++;
                    });

            huSeat[0].getGameResults().add(new GameResult(scoreTypes, loseSize[0] * score));

            gameOver(room);

            return;
        }

        //找到那张牌
        final Integer[] card = new Integer[1];
        room.getSeats().stream().filter(seat -> seat.getSeatNo() == room.getOperationSeat())
                .forEach(seat -> card[0] = seat.getPlayedCards().get(seat.getPlayedCards().size() - 1));
        //先检查胡，胡优先
        room.getSeats().stream().filter(seat -> seat.getSeatNo() != room.getOperationSeat() && seat.getUserName().equals(user)).forEach(seat -> {
            List<Integer> temp = new ArrayList<>();
            temp.addAll(seat.getCards());

            //当前玩家是否可以胡牌
            temp.add(card[0]);
            if (MahjongUtil.hu(temp) && seat.getOperation() == 1) {

                List<ScoreType> scoreTypes = MahjongUtil.getHuType(huSeat[0].getCards(), huSeat[0].getInvertedCards());
                int score = MahjongUtil.getScore(scoreTypes);
                //地胡
                if (room.getHistoryList().size() == 1 && score < 20) {
                    scoreTypes.clear();
                    scoreTypes.add(ScoreType.DI_HU);
                    score = 20;
                }

                room.getSeats().get(room.getOperationSeat() - 1).getGameResults().add(new GameResult(scoreTypes, -score));
                seat.getGameResults().add(new GameResult(scoreTypes, score));
                //胡牌
                gameOver(room);

                //TODO 检查是否游戏是否结束
//                checkEnd(room);
            }
        });
    }

    /**
     * 摸牌
     *
     * @param room 桌数据
     */
    private void getCard(Room room) {
        getCard(room, room.getNextSeat());
    }

    /**
     * 摸牌
     *
     * @param room 桌数据
     */
    private void getCard(Room room, int seatNo) {
        if (0 == room.getSurplusCards().size()) {
            gameOver(room);
        }
        Mahjong.BaseAction.Builder actionResponse = Mahjong.BaseAction.newBuilder().setOperationId(GameBase.OperationId.GET_CARD);
        room.setOperationSeat(seatNo);
        int cardIndex = (int) (Math.random() * room.getSurplusCards().size());
        Integer card1 = room.getSurplusCards().get(cardIndex);
        room.getSurplusCards().remove(cardIndex);
        final String[] username = new String[1];
        room.getSeats().stream().filter(seat -> seat.getSeatNo() == seatNo).forEach(seat -> username[0] = seat.getUserName());
        Mahjong.GetCardResponse.Builder builder1 = Mahjong.GetCardResponse.newBuilder().setID(username[0]);
        builder1.setCard(card1);
        room.getSeats().forEach(seat -> {
            if (seat.getSeatNo() == seatNo) {
                seat.getCards().add(card1);
                checkSelfGetCard(seat, card1);
                actionResponse.setData(builder1.build().toByteString());
            } else {
                actionResponse.clearData();
            }
            if (MahjongTcpService.userClients.containsKey(seat.getUserName())) {
                response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                MahjongTcpService.userClients.get(seat.getUserName()).send(response.build(), seat.getUserName());
            }
        });

        Mahjong.RoundResponse roundResponse = Mahjong.RoundResponse.newBuilder().setID(username[0]).build();
        response.setOperationType(GameBase.OperationType.ROUND).setData(roundResponse.toByteString());
        room.getSeats().stream().filter(seat -> MahjongTcpService.userClients.containsKey(seat.getUserName()))
                .forEach(seat -> MahjongTcpService.userClients.get(seat.getUserName()).send(response.build(), seat.getUserName()));
    }

    /**
     * 摸牌后检测是否可以自摸、暗杠、扒杠
     *
     * @param seat 座位
     * @param card 摸的牌
     */
    private void checkSelfGetCard(Seat seat, Integer card) {
        Mahjong.AskResponse.Builder builder = Mahjong.AskResponse.newBuilder();
        if (MahjongUtil.hu(seat.getCards())) {
            builder.addOperationId(GameBase.OperationId.HU);
        }
        //暗杠
        if (null != MahjongUtil.checkGang(seat.getCards())) {
            builder.addOperationId(GameBase.OperationId.AN_GANG);
        }
        //扒杠
        if (seat.getInvertedCards().contains(card)) {
            builder.addOperationId(GameBase.OperationId.BA_GANG);
        }
        if (0 != builder.getOperationIdCount()) {
            if (MahjongTcpService.userClients.containsKey(seat.getUserName())) {
                response.clear();
                response.setOperationType(GameBase.OperationType.ASK).setData(builder.build().toByteString());
                MahjongTcpService.userClients.get(seat.getUserName()).send(response.build(), seat.getUserName());
            }
            //TODO 出牌超时
//                    new SelfOperationTimeout(deskNo).start();
        } else {
            //TODO 出牌超时
//                    new PlayTimeout(seat.getSeatNo(), deskNo).start();
        }
    }

    private void gameOver(Room room) {
        //TODO 扣款
        Mahjong.ResultResponse.Builder resultResponse = Mahjong.ResultResponse.newBuilder();
        room.getSeats().forEach(seat -> {
            Mahjong.UserResult.Builder userResult = Mahjong.UserResult.newBuilder();
            userResult.setID(seat.getUserName());
            userResult.addAllCards(seat.getCards());
            seat.getGameResults().forEach(gameResult -> {
                Mahjong.Result.Builder result = Mahjong.Result.newBuilder();
                result.setScore(gameResult.getScore());
                gameResult.getScoreTypes().forEach(scoreType -> result.addScoreTypes(scoreType.getValues()));
                userResult.addResult(result);
            });
            resultResponse.addUserResult(userResult);
        });

        response.clear();
        response.setOperationType(GameBase.OperationType.RESULT).setData(resultResponse.build().toByteString());
        room.getSeats().stream().filter(seat -> MahjongTcpService.userClients.containsKey(seat.getUserName()))
                .forEach(seat -> MahjongTcpService.userClients.get(seat.getUserName()).send(response.build(), seat.getUserName()));

        //删除该桌
        redisService.delete("room" + roomNo);
        redisService.lock("lock_room_nos" + roomNo);
        List<String> roomNos = JSON.parseArray(redisService.getCache("room_nos"), String.class);
        roomNos.remove(roomNo);
        redisService.addCache("room_nos", JSON.toJSONString(roomNos), 86400);
        redisService.unlock("lock_room_nos" + roomNo);
    }
}