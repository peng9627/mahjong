import com.google.protobuf.GeneratedMessageV3;
import mahjong.mode.GameBase;
import mahjong.mode.Mahjong;
import mahjong.mode.Xingning;
import mahjong.utils.ByteUtils;
import mahjong.utils.CoreStringUtils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Robot {
    private static byte[] md5Key = "2704031cd4814eb2a82e47bd1d9042c6".getBytes();

    private static int readInt(InputStream is) throws IOException {
        int ch1 = is.read();
        int ch2 = is.read();
        int ch3 = is.read();
        int ch4 = is.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }
        return (ch1 << 24 | ((ch2 << 16) & 0xff) | ((ch3 << 8) & 0xff) | (ch4 & 0xFF));
    }

    private static String readString(InputStream is) throws IOException {
        int len = readInt(is);
        byte[] bytes = new byte[len];
        is.read(bytes);
        return new String(bytes);
    }

    private static void send(OutputStream os, GeneratedMessageV3 messageV3) {
        try {
            String md5 = CoreStringUtils.md5(ByteUtils.addAll(md5Key, messageV3.toByteArray()), 32, false);
            messageV3.sendTo(os, md5);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void test(int id, String roomNo) {
        try {

            GameBase.BaseConnection.Builder request;
            GameBase.BaseAction.Builder action;
            GameBase.BaseConnection response;
            InputStream is;
            OutputStream os;

            Socket socket = new Socket("127.0.0.1", 10001);
            is = socket.getInputStream();
            os = socket.getOutputStream();
            request = GameBase.BaseConnection.newBuilder();
            action = GameBase.BaseAction.newBuilder();
            Mahjong.RoomCardIntoRequest intoRequest = Mahjong.RoomCardIntoRequest.newBuilder()
                    .setID(id).setRoomNo(roomNo).build();
            request.setOperationType(GameBase.OperationType.CONNECTION).setData(intoRequest.toByteString());
            send(os, request.build());

            String md5 = CoreStringUtils.md5(ByteUtils.addAll(md5Key, intoRequest.toByteArray()), 32, false);
            intoRequest.sendTo(socket.getOutputStream(), md5);

            List<Integer> cards = new ArrayList<>();

            while (true) {
                int len = readInt(is);
                md5 = readString(is);
                len -= md5.getBytes().length + 4;
                byte[] data = new byte[len];
                boolean check = true;
                if (0 != len) {
                    check = len == is.read(data) && CoreStringUtils.md5(ByteUtils.addAll(md5Key, data), 32, false).equalsIgnoreCase(md5);
                }
                if (check) {
                    response = GameBase.BaseConnection.parseFrom(data);
                    switch (request.getOperationType()) {
                        case CONNECTION:
                            break;
                        case GAME_INFO:
                            Xingning.GameInfo gameInfo = Xingning.GameInfo.parseFrom(response.getData());
                            if (0 == gameInfo.getGameStatus().compareTo(Xingning.GameStatus.WAITING)) {
                                request.setOperationType(GameBase.OperationType.READY).clear();
                                send(os, request.build());
                            }
                            break;
                        case ACTION:
                            GameBase.BaseAction baseAction = GameBase.BaseAction.parseFrom(data);
                            switch (baseAction.getOperationId()) {
                                case DEAL_CARD:
                                    Mahjong.DealCardResponse dealCardResponse = Mahjong.DealCardResponse.parseFrom(baseAction.getData());
                                    cards.addAll(dealCardResponse.getCardsList());
                                    break;
                                case GET_CARD:
                                    if (baseAction.getID() == id) {
                                        Mahjong.GetCardResponse getCardResponse = Mahjong.GetCardResponse.parseFrom(baseAction.getData());
                                        cards.add(getCardResponse.getCard());
                                    }
                                    break;
                                case PLAY_CARD:
                                    if (baseAction.getID() == id) {
                                        Mahjong.PlayCard playCardResponse = Mahjong.PlayCard.parseFrom(baseAction.getData());
                                        cards.remove(playCardResponse.getCard());
                                    }
                                    break;
                            }
                            break;
                        case ROUND:
                            GameBase.RoundResponse roundResponse = GameBase.RoundResponse.parseFrom(response.getData());
                            if (roundResponse.getID() == id) {
                                Mahjong.PlayCard playCardRequest = Mahjong.PlayCard.newBuilder()
                                        .setCard(cards.get(cards.size() - 1)).build();
                                action.setOperationId(GameBase.ActionId.PLAY_CARD).setData(playCardRequest.toByteString());
                                request.setOperationType(GameBase.OperationType.ACTION).setData(action.build().toByteString());
                                send(os, request.build());
                            }
                            break;
                        case ASK:
                            action.setOperationId(GameBase.ActionId.PASS).clearData();
                            request.setOperationType(GameBase.OperationType.ACTION).setData(action.build().toByteString());
                            send(os, request.build());

                        case RESULT:
                            synchronized (this) {

                            }
                            break;
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
