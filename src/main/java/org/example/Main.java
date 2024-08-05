package org.example;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

/**
 * Represents an exception for an invalid authorization token.
 */
class InvalidToken extends Exception {
    public InvalidToken(String message) {
        super(message);
    }
}

/**
 * Provides an API wrapper for interacting with the Blum API.
 */
class BlumAPI {
    private final Map<String, String> headers;

    /**
     * Constructs a new BlumAPI instance with the given authorization token.
     *
     * @param authorizationToken the authorization token to use for API requests
     */
    public BlumAPI(String authorizationToken) {
        this.headers = new HashMap<>();
        this.headers.put("Authorization", authorizationToken);
    }

    /**
     * Sends an HTTP request to the specified URL with the given method and optional payload.
     *
     * @param requestMethod the HTTP method to use (e.g., "get", "post")
     * @param url           the URL to send the request to
     * @param payload       the request payload (optional)
     * @return the HTTP response
     * @throws InvalidToken if the response status code is 401 (Unauthorized)
     * @throws IOException   if there is an error executing the request
     */
    private CloseableHttpResponse request(String requestMethod, String url, String payload) throws InvalidToken, IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            CloseableHttpResponse response;
            if ("get".equalsIgnoreCase(requestMethod)) {
                HttpGet httpGet = new HttpGet(url);
                httpGet.setHeaders(this.headers.entrySet().stream()
                        .map(e -> new org.apache.http.message.BasicHeader(e.getKey(), e.getValue()))
                        .toArray(org.apache.http.Header[]::new));
                response = httpClient.execute(httpGet);
            } else if ("post".equalsIgnoreCase(requestMethod)) {
                HttpPost httpPost = new HttpPost(url);
                httpPost.setHeaders(this.headers.entrySet().stream()
                        .map(e -> new org.apache.http.message.BasicHeader(e.getKey(), e.getValue()))
                        .toArray(org.apache.http.Header[]::new));
                if (payload != null) {
                    httpPost.setEntity(new StringEntity(payload));
                }
                response = httpClient.execute(httpPost);
            } else {
                throw new IllegalArgumentException("Invalid request method: " + requestMethod);
            }

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                throw new InvalidToken("Invalid authorization token");
            }
            return response;
        }
    }

    /**
     * Retrieves the user's profile information.
     *
     * @return the user's profile data as a JSONObject
     * @throws Exception if there is an error retrieving the user's profile
     */
    public JSONObject getMe() throws Exception {
        try (CloseableHttpResponse response = this.request("get", "https://gateway.blum.codes/v1/user/me", null)) {
//            if (!org.apache.http.client.utils.HttpClientUtils.isSuccess(response.getStatusLine().getStatusCode())) {
//                throw new Exception("Error retrieving user profile: " + EntityUtils.toString(response.getEntity()));
//            }
            return new JSONObject(EntityUtils.toString(response.getEntity()));
        }
    }

    /**
     * Retrieves the user's balance information.
     *
     * @return the user's balance data as a JSONObject
     * @throws Exception if there is an error retrieving the user's balance
     */
    public JSONObject getBalance() throws Exception {
        try (CloseableHttpResponse response = this.request("get", "https://game-domain.blum.codes/api/v1/user/balance", null)) {
//            if (!org.apache.http.client.utils.HttpClientUtils.isSuccess(response.getStatusLine().getStatusCode())) {
//                throw new Exception("Error retrieving user balance: " + EntityUtils.toString(response.getEntity()));
//            }
            return new JSONObject(EntityUtils.toString(response.getEntity()));
        }
    }

    /**
     * Initiates a game play.
     *
     * @return the game play response as a JSONObject
     * @throws Exception if there is an error initiating the game play
     */
    public JSONObject playGame() throws Exception {
        try (CloseableHttpResponse response = this.request("post", "https://game-domain.blum.codes/api/v1/game/play", null)) {
//            if (!org.apache.http.client.utils.HttpClientUtils.isSuccess(response.getStatusLine().getStatusCode())) {
//                throw new Exception("Error playing the game: " + EntityUtils.toString(response.getEntity()));
//            }
            return new JSONObject(EntityUtils.toString(response.getEntity()));
        }
    }

    /**
     * Claims a reward for the specified game.
     *
     * @param gameId the ID of the game
     * @param points the number of points to claim
     * @throws Exception if there is an error claiming the reward
     */
    public void claimReward(String gameId, int points) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("gameId", gameId);
        payload.put("points", points);
        try (CloseableHttpResponse response = this.request("post", "https://game-domain.blum.codes/api/v1/game/claim", new JSONObject(payload).toString())) {
//            if (!org.apache.http.client.utils.HttpClientUtils.isSuccess(response.getStatusLine().getStatusCode())) {
//                throw new Exception("Error claiming the reward: " + EntityUtils.toString(response.getEntity()));
//            }
        }
    }
}

/**
 * The main entry point of the application.
 */
public class Main {
    private static final int MIN_POINTS = 170;
    private static final int MAX_POINTS = 220;
    private static final int MIN_SLEEP_TIME = 30;
    private static final int MAX_SLEEP_TIME = 40;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Blum: ");
        String authorizationToken = scanner.nextLine();

        try {
            BlumAPI blumAPI = new BlumAPI(authorizationToken);
            String username = blumAPI.getMe().getString("username");

            while (true) {
                System.out.println("\nHello, " + username + "! \n1. Balance\n2. Claim points for a game that has started.");
                System.out.print("Make your choice (1 - 2): ");
                String choice = scanner.nextLine();

                if ("1".equals(choice)) {
                    JSONObject balanceData = blumAPI.getBalance();
                    double availableBalance = balanceData.getDouble("availableBalance");
                    int gamePasses = balanceData.getInt("playPasses");
                    int gamesCount;

                    System.out.printf("\nYour account balance: %.2f\nNumber of available games on your account: %d\n", availableBalance, gamePasses);

                    while (true) {
                        System.out.print("\nEnter the number of games you want to play: ");
                        gamesCount = Integer.parseInt(scanner.nextLine());
                        if (gamesCount > 0 && gamesCount <= gamePasses) {
                            break;
                        }
                        System.out.println("Enter a valid number!");
                    }

                    for (int gameNumber = 1; gameNumber <= gamesCount; gameNumber++) {
                        System.out.printf("\n[+] Game number %d is being processed!\n", gameNumber);
                        JSONObject gameResponse = blumAPI.playGame();
                        String gameId = gameResponse.getString("gameId");
                        int points = new Random().nextInt(MAX_POINTS - MIN_POINTS + 1) + MIN_POINTS;
                        int sleepTime = new Random().nextInt(MAX_SLEEP_TIME - MIN_SLEEP_TIME + 1) + MIN_SLEEP_TIME;

                        System.out.printf("[+] Game number %d has started successfully!\nYour game ID: %s\n", gameNumber, gameId);
                        System.out.printf("[+] Waiting %d seconds for the game to finish...\n", sleepTime);
                        Thread.sleep(sleepTime * 1000L);
                        blumAPI.claimReward(gameId, points);
                        availableBalance += points;
                        System.out.printf("[+] Game number %d has been successfully processed!\n\nYou received: %d\nBalance: %.2f\n", gameNumber, points, availableBalance);
                        Thread.sleep(1000L);
                    }

                    System.out.println("\n[+] All games have been successfully processed!");
                    System.out.print("Press ENTER to continue...");
                    scanner.nextLine();
                } else if ("2".equals(choice)) {
                    System.out.print("Enter your game ID: ");
                    String gameId = scanner.nextLine();
                    int points = new Random().nextInt(MAX_POINTS - MIN_POINTS + 1) + MIN_POINTS;
                    blumAPI.claimReward(gameId, points);
                    System.out.printf("\n[+] Success! You received %d points!\nGame ID: %s\n", points, gameId);
                    System.out.print("Press ENTER to continue...");
                    scanner.nextLine();
                } else {
                    System.out.println("\nInvalid choice!\nTry again!");
                    Thread.sleep(1500L);
                }
            }
        } catch (InvalidToken e) {
            System.out.printf("\nError:\n1. Invalid token!\n2. Try again later!\n\nMessage: %s\n", e.getMessage());
        } catch (Exception e) {
            System.out.printf("\nERROR! %s\n", e.getMessage());
        }
    }
}