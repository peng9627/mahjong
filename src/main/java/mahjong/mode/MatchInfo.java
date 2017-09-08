package mahjong.mode;

import mahjong.entrance.MahjongTcpService;
import mahjong.redis.RedisService;

import java.util.*;

/**
 * Created by pengyi
 * Date : 17-9-1.
 * desc:
 */
public class MatchInfo {

    private int status;
    private Date startDate;
    private List<Integer> rooms;
    private Arena arena;
    private List<MatchUser> matchUsers;
    private boolean start;
    private List<MatchUser> waitUsers;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public List<Integer> getRooms() {
        return rooms;
    }

    public void setRooms(List<Integer> rooms) {
        this.rooms = rooms;
    }

    public Arena getArena() {
        return arena;
    }

    public void setArena(Arena arena) {
        this.arena = arena;
    }

    public List<MatchUser> getMatchUsers() {
        return matchUsers;
    }

    public void setMatchUsers(List<MatchUser> matchUsers) {
        this.matchUsers = matchUsers;
    }

    public boolean isStart() {
        return start;
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    public List<MatchUser> getWaitUsers() {
        return waitUsers;
    }

    public void setWaitUsers(List<MatchUser> waitUsers) {
        this.waitUsers = waitUsers;
    }

    public int addRoom(int gameTimes, RedisService redisService, List<User> users, Map<Integer, Integer> userIdScore,
                       GameBase.BaseConnection.Builder response, GameBase.MatchData.Builder matchData) {
        Room room = new Room();
        room.setBaseScore(1);
        room.setRoomNo(roomNo(redisService));
        room.setGameTimes(gameTimes);
        room.setCount(4);
        room.setGhost(2);
        room.setGameStatus(GameStatus.WAITING);
        room.setSeatNos(new ArrayList<>(Arrays.asList(1, 2, 3, 4)));
        while (4 > room.getSeats().size()) {
            User user = users.remove(0);
            room.addSeat(user, userIdScore.get(user.getUserId()));
            response.setOperationType(GameBase.OperationType.MATCH_DATA).setData(matchData.build().toByteString());
            if (MahjongTcpService.userClients.containsKey(user.getUserId())) {
                MahjongTcpService.userClients.get(user.getUserId()).send(response.build(), user.getUserId());
            }
            room.sendRoomInfo(GameBase.RoomCardIntoResponse.newBuilder(), response, user.getUserId());
        }
        room.sendSeatInfo(response);
        return Integer.parseInt(room.getRoomNo());
    }

    /**
     * 生成随机桌号
     *
     * @return 桌号
     */
    private String roomNo(RedisService redisService) {
        String roomNo = (new Random().nextInt(899999) + 100001) + "";
        if (redisService.exists("room" + roomNo + "")) {
            roomNo = roomNo(redisService);
        }
        return roomNo;
    }
}
