package mahjong.mode;

import java.util.List;

/**
 * Author pengyi
 * Date 17-3-21.
 */
public class GameResult {

    private List<ScoreType> scoreTypes;
    private double score;

    public GameResult() {
    }

    public GameResult(List<ScoreType> scoreTypes, double score) {
        this.scoreTypes = scoreTypes;
        this.score = score;
    }

    public List<ScoreType> getScoreTypes() {
        return scoreTypes;
    }

    public void setScoreTypes(List<ScoreType> scoreTypes) {
        this.scoreTypes = scoreTypes;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
