package app.kitsune.integrations.youtube.sponsorblock.requests;

import static app.kitsune.integrations.shared.utils.StringRef.str;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import app.kitsune.integrations.shared.requests.Requester;
import app.kitsune.integrations.shared.requests.Route;
import app.kitsune.integrations.shared.sponsorblock.requests.SBRoutes;
import app.kitsune.integrations.shared.utils.Logger;
import app.kitsune.integrations.shared.utils.Utils;
import app.kitsune.integrations.youtube.settings.Settings;
import app.kitsune.integrations.youtube.sponsorblock.SponsorBlockSettings;
import app.kitsune.integrations.youtube.sponsorblock.objects.SegmentCategory;
import app.kitsune.integrations.youtube.sponsorblock.objects.SponsorSegment;
import app.kitsune.integrations.youtube.sponsorblock.objects.SponsorSegment.SegmentVote;
import app.kitsune.integrations.youtube.sponsorblock.objects.UserStats;

public class SBRequester {
    private static final String TIME_TEMPLATE = "%.3f";

    /**
     * TCP timeout
     */
    private static final int TIMEOUT_TCP_DEFAULT_MILLISECONDS = 7000;

    /**
     * HTTP response timeout
     */
    private static final int TIMEOUT_HTTP_DEFAULT_MILLISECONDS = 10000;

    /**
     * Response code of a successful API call
     */
    private static final int HTTP_STATUS_CODE_SUCCESS = 200;

    private SBRequester() {
    }

    private static void handleConnectionError(@NonNull String toastMessage, @Nullable Exception ex) {
        if (Settings.SB_TOAST_ON_CONNECTION_ERROR.get()) {
            Utils.showToastShort(toastMessage);
        }
        if (ex != null) {
            Logger.printInfo(() -> toastMessage, ex);
        }
    }

    @NonNull
    public static SponsorSegment[] getSegments(@NonNull String videoId) {
        Utils.verifyOffMainThread();
        List<SponsorSegment> segments = new ArrayList<>();
        try {
            HttpURLConnection connection = getConnectionFromRoute(SBRoutes.GET_SEGMENTS, videoId, SegmentCategory.sponsorBlockAPIFetchCategories);
            final int responseCode = connection.getResponseCode();

            if (responseCode == HTTP_STATUS_CODE_SUCCESS) {
                JSONArray responseArray = Requester.parseJSONArray(connection);
                final long minSegmentDuration = (long) (Settings.SB_SEGMENT_MIN_DURATION.get() * 1000);
                for (int i = 0, length = responseArray.length(); i < length; i++) {
                    JSONObject obj = (JSONObject) responseArray.get(i);
                    JSONArray segment = obj.getJSONArray("segment");
                    final long start = (long) (segment.getDouble(0) * 1000);
                    final long end = (long) (segment.getDouble(1) * 1000);

                    String uuid = obj.getString("UUID");
                    final boolean locked = obj.getInt("locked") == 1;
                    String categoryKey = obj.getString("category");
                    SegmentCategory category = SegmentCategory.byCategoryKey(categoryKey);
                    if (category == null) {
                        Logger.printException(() -> "Received unknown category: " + categoryKey); // should never happen
                    } else if ((end - start) >= minSegmentDuration || category == SegmentCategory.HIGHLIGHT) {
                        segments.add(new SponsorSegment(category, uuid, start, end, locked));
                    }
                }
                Logger.printDebug(() -> {
                    StringBuilder builder = new StringBuilder("Downloaded segments:");
                    for (SponsorSegment segment : segments) {
                        builder.append('\n').append(segment);
                    }
                    return builder.toString();
                });
                runVipCheckInBackgroundIfNeeded();
            } else if (responseCode == 404) {
                // no segments are found.  a normal response
                Logger.printDebug(() -> "No segments found for video: " + videoId);
            } else {
                handleConnectionError(str("revanced_sb_sponsorblock_connection_failure_status", responseCode), null);
                connection.disconnect(); // something went wrong, might as well disconnect
            }
        } catch (SocketTimeoutException ex) {
            handleConnectionError(str("revanced_sb_sponsorblock_connection_failure_timeout"), ex);
        } catch (IOException ex) {
            handleConnectionError(str("revanced_sb_sponsorblock_connection_failure_generic"), ex);
        } catch (Exception ex) {
            // Should never happen
            Logger.printException(() -> "getSegments failure", ex);
        }

        return segments.toArray(new SponsorSegment[0]);
    }

    public static void submitSegments(@NonNull String videoId, @NonNull String category,
                                      long startTime, long endTime, long videoLength) {
        Utils.verifyOffMainThread();
        try {
            String privateUserId = SponsorBlockSettings.getSBPrivateUserID();
            String start = String.format(Locale.US, TIME_TEMPLATE, startTime / 1000f);
            String end = String.format(Locale.US, TIME_TEMPLATE, endTime / 1000f);
            String duration = String.format(Locale.US, TIME_TEMPLATE, videoLength / 1000f);

            HttpURLConnection connection = getConnectionFromRoute(SBRoutes.SUBMIT_SEGMENTS, privateUserId, videoId, category, start, end, duration);
            final int responseCode = connection.getResponseCode();

            final String messageToToast = switch (responseCode) {
                case HTTP_STATUS_CODE_SUCCESS -> str("revanced_sb_submit_succeeded");
                case 409 -> str("revanced_sb_submit_failed_duplicate");
                case 403 ->
                        str("revanced_sb_submit_failed_forbidden", Requester.parseErrorStringAndDisconnect(connection));
                case 429 -> str("revanced_sb_submit_failed_rate_limit");
                case 400 ->
                        str("revanced_sb_submit_failed_invalid", Requester.parseErrorStringAndDisconnect(connection));
                default ->
                        str("revanced_sb_submit_failed_unknown_error", responseCode, connection.getResponseMessage());
            };
            Utils.showToastLong(messageToToast);
        } catch (SocketTimeoutException ex) {
            // Always show, even if show connection toasts is turned off
            Utils.showToastLong(str("revanced_sb_submit_failed_timeout"));
        } catch (IOException ex) {
            Utils.showToastLong(str("revanced_sb_submit_failed_unknown_error", 0, ex.getMessage()));
        } catch (Exception ex) {
            Logger.printException(() -> "failed to submit segments", ex);
        }
    }

    public static void sendSegmentSkippedViewedRequest(@NonNull SponsorSegment segment) {
        Utils.verifyOffMainThread();
        try {
            HttpURLConnection connection = getConnectionFromRoute(SBRoutes.VIEWED_SEGMENT, segment.UUID);
            final int responseCode = connection.getResponseCode();

            if (responseCode == HTTP_STATUS_CODE_SUCCESS) {
                Logger.printDebug(() -> "Successfully sent view count for segment: " + segment);
            } else {
                Logger.printDebug(() -> "Failed to sent view count for segment: " + segment.UUID
                        + " responseCode: " + responseCode); // debug level, no toast is shown
            }
        } catch (IOException ex) {
            Logger.printInfo(() -> "Failed to send view count", ex); // do not show a toast
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to send view count request", ex); // should never happen
        }
    }

    public static void voteForSegmentOnBackgroundThread(@NonNull SponsorSegment segment, @NonNull SegmentVote voteOption) {
        voteOrRequestCategoryChange(segment, voteOption, null);
    }

    public static void voteToChangeCategoryOnBackgroundThread(@NonNull SponsorSegment segment, @NonNull SegmentCategory categoryToVoteFor) {
        voteOrRequestCategoryChange(segment, SegmentVote.CATEGORY_CHANGE, categoryToVoteFor);
    }

    private static void voteOrRequestCategoryChange(@NonNull SponsorSegment segment, @NonNull SegmentVote voteOption, SegmentCategory categoryToVoteFor) {
        Utils.runOnBackgroundThread(() -> {
            try {
                String segmentUuid = segment.UUID;
                String uuid = SponsorBlockSettings.getSBPrivateUserID();
                HttpURLConnection connection = (voteOption == SegmentVote.CATEGORY_CHANGE)
                        ? getConnectionFromRoute(SBRoutes.VOTE_ON_SEGMENT_CATEGORY, uuid, segmentUuid, categoryToVoteFor.keyValue)
                        : getConnectionFromRoute(SBRoutes.VOTE_ON_SEGMENT_QUALITY, uuid, segmentUuid, String.valueOf(voteOption.apiVoteType));
                final int responseCode = connection.getResponseCode();

                switch (responseCode) {
                    case HTTP_STATUS_CODE_SUCCESS:
                        Logger.printDebug(() -> "Vote success for segment: " + segment);
                        break;
                    case 403:
                        Utils.showToastLong(
                                str("revanced_sb_vote_failed_forbidden", Requester.parseErrorStringAndDisconnect(connection)));
                        break;
                    default:
                        Utils.showToastLong(
                                str("revanced_sb_vote_failed_unknown_error", responseCode, connection.getResponseMessage()));
                        break;
                }
            } catch (SocketTimeoutException ex) {
                Utils.showToastShort(str("revanced_sb_vote_failed_timeout"));
            } catch (IOException ex) {
                Utils.showToastShort(str("revanced_sb_vote_failed_unknown_error", 0, ex.getMessage()));
            } catch (Exception ex) {
                Logger.printException(() -> "failed to vote for segment", ex); // should never happen
            }
        });
    }

    /**
     * @return NULL, if stats fetch failed
     */
    @Nullable
    public static UserStats retrieveUserStats() {
        Utils.verifyOffMainThread();
        try {
            UserStats stats = new UserStats(getJSONObject(SBRoutes.GET_USER_STATS, SponsorBlockSettings.getSBPrivateUserID()));
            Logger.printDebug(() -> "user stats: " + stats);
            return stats;
        } catch (IOException ex) {
            Logger.printInfo(() -> "failed to retrieve user stats", ex); // info level, do not show a toast
        } catch (Exception ex) {
            Logger.printException(() -> "failure retrieving user stats", ex); // should never happen
        }
        return null;
    }

    /**
     * @return NULL if the call was successful.  If unsuccessful, an error message is returned.
     */
    @Nullable
    public static String setUsername(@NonNull String username) {
        Utils.verifyOffMainThread();
        try {
            HttpURLConnection connection = getConnectionFromRoute(SBRoutes.CHANGE_USERNAME, SponsorBlockSettings.getSBPrivateUserID(), username);
            final int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();
            if (responseCode == HTTP_STATUS_CODE_SUCCESS) {
                return null;
            }
            return str("revanced_sb_stats_username_change_unknown_error", responseCode, responseMessage);
        } catch (Exception ex) { // should never happen
            Logger.printInfo(() -> "failed to set username", ex); // do not toast
            return str("revanced_sb_stats_username_change_unknown_error", 0, ex.getMessage());
        }
    }

    public static void runVipCheckInBackgroundIfNeeded() {
        if (!SponsorBlockSettings.userHasSBPrivateId()) {
            return; // User cannot be a VIP. User has never voted, created any segments, or has imported a SB user id.
        }
        long now = System.currentTimeMillis();
        if (now < (Settings.SB_LAST_VIP_CHECK.get() + TimeUnit.DAYS.toMillis(3))) {
            return;
        }
        Utils.runOnBackgroundThread(() -> {
            try {
                JSONObject json = getJSONObject(SBRoutes.IS_USER_VIP, SponsorBlockSettings.getSBPrivateUserID());
                boolean vip = json.getBoolean("vip");
                Settings.SB_USER_IS_VIP.save(vip);
                Settings.SB_LAST_VIP_CHECK.save(now);
            } catch (IOException ex) {
                Logger.printInfo(() -> "Failed to check VIP (network error)", ex); // info, so no error toast is shown
            } catch (Exception ex) {
                Logger.printException(() -> "Failed to check VIP", ex); // should never happen
            }
        });
    }

    // helpers

    private static HttpURLConnection getConnectionFromRoute(@NonNull Route route, String... params) throws IOException {
        HttpURLConnection connection = Requester.getConnectionFromRoute(Settings.SB_API_URL.get(), route, params);
        connection.setConnectTimeout(TIMEOUT_TCP_DEFAULT_MILLISECONDS);
        connection.setReadTimeout(TIMEOUT_HTTP_DEFAULT_MILLISECONDS);
        return connection;
    }

    private static JSONObject getJSONObject(@NonNull Route route, String... params) throws IOException, JSONException {
        return Requester.parseJSONObject(getConnectionFromRoute(route, params));
    }
}
