package haven;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class GitHubVersionFetcher {
    private static final int TIMEOUT_SECONDS = 5;
    private static final AtomicBoolean isCancelled = new AtomicBoolean(false);

    public static void fetchLatestVersion(String owner, String repo, VersionCallback callback) {
        // Set loading state
        callback.onVersionFetched("Loading...");

        // Reset cancellation state
        isCancelled.set(false);

        // Use ExecutorService to manage the task
        ExecutorService executor = Executors.newSingleThreadExecutor();
        java.util.concurrent.Future<String> future = executor.submit(() -> getLatestReleaseVersion(owner, repo));

        // Set a timeout for the future task
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            if (!future.isDone()) {
                callback.onVersionFetched("Failed"); // Update to failed if not completed
                future.cancel(true); // Cancel the task if needed
                isCancelled.set(true); // Mark as cancelled
            }
        }, TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Retrieve the result
        try {
            String version = future.get(); // This will block until the result is available
            callback.onVersionFetched(version); // Call the callback with the version
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            callback.onVersionFetched("Failed"); // Update to failed on interruption
        } catch (ExecutionException e) {
            callback.onVersionFetched("Failed"); // Update to failed on exceptions
        } finally {
            executor.shutdown(); // Clean up the executor
            scheduler.shutdown(); // Clean up the scheduler
        }
    }

    private static String getLatestReleaseVersion(String owner, String repo) throws Exception {
        // Check if the task was cancelled before proceeding
        if (isCancelled.get()) {
            throw new InterruptedException("Task was cancelled");
        }

        String urlString = String.format("https://api.github.com/repos/%s/%s/releases/latest", owner, repo);
        HttpURLConnection connection = null;
        BufferedReader br = null;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            if (connection.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + connection.getResponseCode());
            }

            br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String output;

            while ((output = br.readLine()) != null) {
                response.append(output);
            }

            return parseTagName(response.toString()); // Pass the entire response to parse
        } finally {
            if (br != null) {
                br.close(); // Close BufferedReader
            }
            if (connection != null) {
                connection.disconnect(); // Close the connection
            }
        }
    }

    private static String parseTagName(String jsonResponse) {
        String tagNameKey = "\"tag_name\":";
        int startIndex = jsonResponse.indexOf(tagNameKey) + tagNameKey.length();
        int endIndex = jsonResponse.indexOf("\"", startIndex + 1);
        return jsonResponse.substring(startIndex + 1, endIndex); // Extract the version string
    }

    // Define the callback interface as a nested interface
    public interface VersionCallback {
        void onVersionFetched(String version);
    }
}
