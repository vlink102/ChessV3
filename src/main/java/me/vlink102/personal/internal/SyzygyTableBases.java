package me.vlink102.personal.internal;

import me.vlink102.personal.game.PieceWrapper;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SyzygyTableBases {
    public SyzygyTableBases() throws IOException {
        //JSONObject object = getData("4k3/8/8/8/8/8/8/4KBN1 w - - 0 1");
    }

    /**
     * {@link StockFish#getBestMove(String, int)}
     */
    public void computerMove(PieceWrapper[][] board) {

    }

    public void printTableBaseInfo(JSONObject object) {
        System.out.println("--------------------");
        System.out.println("DTZ: " + object.getInt("dtz"));
        System.out.println("DTZ_PRECISE: " + object.getInt("precise_dtz"));
        System.out.println("DEPTH_TO_MATE: " + object.getInt("dtm"));
        moveStats(object);
        JSONObject move = object.getJSONArray("moves").getJSONObject(0);
        System.out.println("UCI: " + move.getString("uci"));
        System.out.println("SAN: " + move.getString("san"));
        System.out.println("DTZ: " + move.getInt("dtz"));
        System.out.println("DTZ_PRECISE: " + move.getInt("precise_dtz"));
        System.out.println("DEPTH_TO_MATE: " + move.getInt("dtm"));
        System.out.println("ZEROING: " + move.getBoolean("zeroing"));
        moveStats(move);
    }

    private void moveStats(JSONObject object) {
        System.out.println("CHECKMATE: " + object.getBoolean("checkmate"));
        System.out.println("STALEMATE: " + object.getBoolean("stalemate"));
        System.out.println("INSUFFICIENT_MATERIAL: " + object.getBoolean("insufficient_material"));
        System.out.println("CATEGORY: " + object.getString("category"));
        System.out.println("--------------------");
    }

    public JSONObject getData(String FEN) throws IOException {
        String reformattedFEN = FEN.replaceAll(" ", "_");
        URL url = new URL("http://tablebase.lichess.ovh/standard?fen=" + reformattedFEN);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        int status = connection.getResponseCode();
        System.out.println("Status given=" + status);
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        connection.disconnect();
        return new JSONObject(content.toString());
    }
}
