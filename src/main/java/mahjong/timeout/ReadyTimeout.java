package mahjong.timeout;

import com.alibaba.fastjson.JSON;
import mahjong.constant.Constant;
import mahjong.entrance.MahjongTcpService;
import mahjong.mode.GameBase;
import mahjong.mode.GameStatus;
import mahjong.mode.Room;
import mahjong.mode.Seat;
import mahjong.redis.RedisService;

/**
 * Created by pengyi
 * Date : 17-8-31.
 * desc:
 */
public class ReadyTimeout extends Thread {

    private Integer roomNo;
    private RedisService redisService;
    private GameBase.BaseConnection.Builder response;

    public ReadyTimeout(Integer roomNo, RedisService redisService) {
        this.roomNo = roomNo;
        this.redisService = redisService;
        this.response = GameBase.BaseConnection.newBuilder();
    }

    @Override
    public void run() {
        synchronized (this) {
            try {
                wait(Constant.readyTimeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (redisService.exists("room" + roomNo)) {
            while (!redisService.lock("lock_room" + roomNo)) {
            }
            Room room = JSON.parseObject(redisService.getCache("room" + roomNo), Room.class);
            if (0 == room.getGameStatus().compareTo(GameStatus.READYING) || 0 == room.getGameStatus().compareTo(GameStatus.WAITING)) {
                for (Seat seat : room.getSeats()) {
                    boolean hasNoReady = false;
                    if (!seat.isReady()) {
                        seat.setReady(true);
                        hasNoReady = true;
                        response.setOperationType(GameBase.OperationType.READY).setData(GameBase.ReadyResponse.newBuilder().setID(seat.getUserId()).build().toByteString());
                        room.getSeats().stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserId())).forEach(seat1 ->
                                MahjongTcpService.userClients.get(seat1.getUserId()).send(response.build(), seat1.getUserId()));
                    }

                    if (hasNoReady) {
                        room.start(response, redisService);
                    }
                }
            }
            redisService.addCache("room" + roomNo, JSON.toJSONString(room));
            redisService.unlock("lock_room" + roomNo);
        }
    }
}
