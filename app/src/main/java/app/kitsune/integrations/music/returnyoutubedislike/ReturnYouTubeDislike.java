package app.kitsune.integrations.music.returnyoutubedislike;

import static app.kitsune.integrations.shared.returnyoutubedislike.ReturnYouTubeDislike.Vote;
import static app.kitsune.integrations.shared.utils.StringRef.str;
import static app.kitsune.integrations.shared.utils.Utils.isSDKAbove;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.icu.text.CompactDecimalFormat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import app.kitsune.integrations.music.settings.Settings;
import app.kitsune.integrations.shared.returnyoutubedislike.requests.RYDVoteData;
import app.kitsune.integrations.shared.returnyoutubedislike.requests.ReturnYouTubeDislikeApi;
import app.kitsune.integrations.shared.utils.Logger;
import app.kitsune.integrations.shared.utils.Utils;

/**
 * Because Litho creates spans using multiple threads, this entire class supports multithreading as well.
 */
public class ReturnYouTubeDislike {

    /**
     * Maximum amount of time to block the UI from updates while waiting for network call to complete.
     * <p>
     * Must be less than 5 seconds, as per:
     * <a href="https://developer.android.com/topic/performance/vitals/anr"/>
     */
    private static final long MAX_MILLISECONDS_TO_BLOCK_UI_WAITING_FOR_FETCH = 4000;

    /**
     * How long to retain successful RYD fetches.
     */
    private static final long CACHE_TIMEOUT_SUCCESS_MILLISECONDS = 7 * 60 * 1000; // 7 Minutes

    /**
     * How long to retain unsuccessful RYD fetches,
     * and also the minimum time before retrying again.
     */
    private static final long CACHE_TIMEOUT_FAILURE_MILLISECONDS = 3 * 60 * 1000; // 3 Minutes

    /**
     * Unique placeholder character, used to detect if a segmented span already has dislikes added to it.
     * Can be any almost any non-visible character.
     */
    private static final char MIDDLE_SEPARATOR_CHARACTER = '◎'; // 'bullseye'

    private static final int SEPARATOR_COLOR = 872415231;

    /**
     * Cached lookup of all video ids.
     */
    @GuardedBy("itself")
    private static final Map<String, ReturnYouTubeDislike> fetchCache = new HashMap<>();

    /**
     * Used to send votes, one by one, in the same order the user created them.
     */
    private static final ExecutorService voteSerialExecutor = Executors.newSingleThreadExecutor();

    /**
     * For formatting dislikes as number.
     */
    @GuardedBy("ReturnYouTubeDislike.class") // not thread safe
    private static CompactDecimalFormat dislikeCountFormatter;

    /**
     * For formatting dislikes as percentage.
     */
    @GuardedBy("ReturnYouTubeDislike.class")
    private static NumberFormat dislikePercentageFormatter;

    public static final Rect leftSeparatorBounds;
    private static final Rect middleSeparatorBounds;

    static {
        DisplayMetrics dp = Objects.requireNonNull(Utils.getContext()).getResources().getDisplayMetrics();

        leftSeparatorBounds = new Rect(0, 0,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.2f, dp),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25, dp));
        final int middleSeparatorSize =
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3.7f, dp);
        middleSeparatorBounds = new Rect(0, 0, middleSeparatorSize, middleSeparatorSize);

        ReturnYouTubeDislikeApi.toastOnConnectionError = Settings.RYD_TOAST_ON_CONNECTION_ERROR.get();
    }

    private final String videoId;

    /**
     * Stores the results of the vote api fetch, and used as a barrier to wait until fetch completes.
     * Absolutely cannot be holding any lock during calls to {@link Future#get()}.
     */
    private final Future<RYDVoteData> future;

    /**
     * Time this instance and the fetch future was created.
     */
    private final long timeFetched;

    /**
     * Optional current vote status of the UI.  Used to apply a user vote that was done on a previous video viewing.
     */
    @Nullable
    @GuardedBy("this")
    private Vote userVote;

    /**
     * Original dislike span, before modifications.
     */
    @Nullable
    @GuardedBy("this")
    private Spanned originalDislikeSpan;

    /**
     * Replacement like/dislike span that includes formatted dislikes.
     * Used to prevent recreating the same span multiple times.
     */
    @Nullable
    @GuardedBy("this")
    private SpannableString replacementLikeDislikeSpan;

    @NonNull
    private static SpannableString createDislikeSpan(@NonNull Spanned oldSpannable,
                                                     @NonNull RYDVoteData voteData) {
        CharSequence oldLikes = oldSpannable;

        // YouTube creators can hide the like count on a video,
        // and the like count appears as a device language specific string that says 'Like'.
        // Check if the string contains any numbers.
        if (!Utils.containsNumber(oldLikes)) {
            if (Settings.RYD_ESTIMATED_LIKE.get()) {
                // Likes are hidden by video creator
                //
                // RYD does not directly provide like data, but can use an estimated likes
                // using the same scale factor RYD applied to the raw dislikes.
                //
                // example video: https://www.youtube.com/watch?v=UnrU5vxCHxw
                // RYD data: https://returnyoutubedislikeapi.com/votes?videoId=UnrU5vxCHxw
                Logger.printDebug(() -> "Using estimated likes");
                oldLikes = formatDislikeCount(voteData.getLikeCount());
            } else {
                // Change the "Likes" string to show that likes and dislikes are hidden.
                String hiddenMessageString = str("revanced_ryd_video_likes_hidden_by_video_owner");
                return newSpanUsingStylingOfAnotherSpan(oldSpannable, hiddenMessageString);
            }
        }

        SpannableStringBuilder builder = new SpannableStringBuilder("\u2009\u2009");
        final boolean compactLayout = Settings.RYD_COMPACT_LAYOUT.get();

        if (!compactLayout) {
            String leftSeparatorString = "\u200E    "; // u200E = left to right character
            Spannable leftSeparatorSpan = new SpannableString(leftSeparatorString);
            ShapeDrawable shapeDrawable = new ShapeDrawable(new RectShape());
            shapeDrawable.getPaint().setColor(SEPARATOR_COLOR);
            shapeDrawable.setBounds(leftSeparatorBounds);
            leftSeparatorSpan.setSpan(new VerticallyCenteredImageSpan(shapeDrawable), 1, 2,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE); // drawable cannot overwrite RTL or LTR character
            builder.append(leftSeparatorSpan);
        }

        // likes
        builder.append(newSpanUsingStylingOfAnotherSpan(oldSpannable, oldLikes));

        // middle separator
        String middleSeparatorString = compactLayout
                ? "\u200E  " + MIDDLE_SEPARATOR_CHARACTER + "  "
                : "\u200E  \u2009" + MIDDLE_SEPARATOR_CHARACTER + "\u2009  "; // u2009 = 'narrow space' character
        final int shapeInsertionIndex = middleSeparatorString.length() / 2;
        Spannable middleSeparatorSpan = new SpannableString(middleSeparatorString);
        ShapeDrawable shapeDrawable = new ShapeDrawable(new OvalShape());
        shapeDrawable.getPaint().setColor(SEPARATOR_COLOR);
        shapeDrawable.setBounds(middleSeparatorBounds);
        // Use original text width if using Rolling Number,
        // to ensure the replacement styled span has the same width as the measured String,
        // otherwise layout can be broken (especially on devices with small system font sizes).
        middleSeparatorSpan.setSpan(
                new VerticallyCenteredImageSpan(shapeDrawable),
                shapeInsertionIndex, shapeInsertionIndex + 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        builder.append(middleSeparatorSpan);

        // dislikes
        builder.append(newSpannableWithDislikes(oldSpannable, voteData));

        return new SpannableString(builder);
    }

    /**
     * @return If the text is likely for a previously created likes/dislikes segmented span.
     */
    public static boolean isPreviouslyCreatedSegmentedSpan(@NonNull String text) {
        return text.indexOf(MIDDLE_SEPARATOR_CHARACTER) >= 0;
    }

    private static boolean spansHaveEqualTextAndColor(@NonNull Spanned one, @NonNull Spanned two) {
        // Cannot use equals on the span, because many of the inner styling spans do not implement equals.
        // Instead, compare the underlying text and the text color to handle when dark mode is changed.
        // Cannot compare the status of device dark mode, as Litho components are updated just before dark mode status changes.
        if (!one.toString().equals(two.toString())) {
            return false;
        }
        ForegroundColorSpan[] oneColors = one.getSpans(0, one.length(), ForegroundColorSpan.class);
        ForegroundColorSpan[] twoColors = two.getSpans(0, two.length(), ForegroundColorSpan.class);
        final int oneLength = oneColors.length;
        if (oneLength != twoColors.length) {
            return false;
        }
        for (int i = 0; i < oneLength; i++) {
            if (oneColors[i].getForegroundColor() != twoColors[i].getForegroundColor()) {
                return false;
            }
        }
        return true;
    }

    private static SpannableString newSpannableWithDislikes(@NonNull Spanned sourceStyling, @NonNull RYDVoteData voteData) {
        return newSpanUsingStylingOfAnotherSpan(sourceStyling,
                Settings.RYD_DISLIKE_PERCENTAGE.get()
                        ? formatDislikePercentage(voteData.getDislikePercentage())
                        : formatDislikeCount(voteData.getDislikeCount()));
    }

    private static SpannableString newSpanUsingStylingOfAnotherSpan(@NonNull Spanned sourceStyle, @NonNull CharSequence newSpanText) {
        SpannableString destination = new SpannableString(newSpanText);
        Object[] spans = sourceStyle.getSpans(0, sourceStyle.length(), Object.class);
        for (Object span : spans) {
            destination.setSpan(span, 0, destination.length(), sourceStyle.getSpanFlags(span));
        }
        return destination;
    }

    private static String formatDislikeCount(long dislikeCount) {
        if (isSDKAbove(24)) {
            if (dislikeCountFormatter == null) {
                // Note: Java number formatters will use the locale specific number characters.
                // such as Arabic which formats "1.234" into "۱,۲۳٤"
                // But YouTube disregards locale specific number characters
                // and instead shows english number characters everywhere.
                Locale locale = Objects.requireNonNull(Utils.getContext()).getResources().getConfiguration().getLocales().get(0);
                Logger.printDebug(() -> "Locale: " + locale);
                dislikeCountFormatter = CompactDecimalFormat.getInstance(locale, CompactDecimalFormat.CompactStyle.SHORT);
            }
            return dislikeCountFormatter.format(dislikeCount);
        } else {
            return String.valueOf(dislikeCount);
        }
    }

    private static String formatDislikePercentage(float dislikePercentage) {
        if (isSDKAbove(24)) {
            synchronized (ReturnYouTubeDislike.class) { // number formatter is not thread safe, must synchronize
                if (dislikePercentageFormatter == null) {
                    Locale locale = Objects.requireNonNull(Utils.getContext()).getResources().getConfiguration().getLocales().get(0);
                    Logger.printDebug(() -> "Locale: " + locale);
                    dislikePercentageFormatter = NumberFormat.getPercentInstance(locale);
                }
                if (dislikePercentage >= 0.01) { // at least 1%
                    dislikePercentageFormatter.setMaximumFractionDigits(0); // show only whole percentage points
                } else {
                    dislikePercentageFormatter.setMaximumFractionDigits(1); // show up to 1 digit precision
                }
                return dislikePercentageFormatter.format(dislikePercentage);
            }
        } else {
            return String.valueOf((int) (dislikePercentage * 100));
        }
    }

    @NonNull
    public static ReturnYouTubeDislike getFetchForVideoId(@Nullable String videoId) {
        Objects.requireNonNull(videoId);
        synchronized (fetchCache) {
            // Remove any expired entries.
            final long now = System.currentTimeMillis();
            if (isSDKAbove(24)) {
                fetchCache.values().removeIf(value -> {
                    final boolean expired = value.isExpired(now);
                    if (expired)
                        Logger.printDebug(() -> "Removing expired fetch: " + value.videoId);
                    return expired;
                });
            } else {
                final Iterator<Map.Entry<String, ReturnYouTubeDislike>> itr = fetchCache.entrySet().iterator();
                while (itr.hasNext()) {
                    final Map.Entry<String, ReturnYouTubeDislike> entry = itr.next();
                    if (entry.getValue().isExpired(now)) {
                        Logger.printDebug(() -> "Removing expired fetch: " + entry.getValue().videoId);
                        itr.remove();
                    }
                }
            }

            ReturnYouTubeDislike fetch = fetchCache.get(videoId);
            if (fetch == null) {
                fetch = new ReturnYouTubeDislike(videoId);
                fetchCache.put(videoId, fetch);
            }
            return fetch;
        }
    }

    /**
     * Should be called if the user changes dislikes appearance settings.
     */
    public static void clearAllUICaches() {
        synchronized (fetchCache) {
            for (ReturnYouTubeDislike fetch : fetchCache.values()) {
                fetch.clearUICache();
            }
        }
    }

    private ReturnYouTubeDislike(@NonNull String videoId) {
        this.videoId = Objects.requireNonNull(videoId);
        this.timeFetched = System.currentTimeMillis();
        this.future = Utils.submitOnBackgroundThread(() -> ReturnYouTubeDislikeApi.fetchVotes(videoId));
    }

    private boolean isExpired(long now) {
        final long timeSinceCreation = now - timeFetched;
        if (timeSinceCreation < CACHE_TIMEOUT_FAILURE_MILLISECONDS) {
            return false; // Not expired, even if the API call failed.
        }
        if (timeSinceCreation > CACHE_TIMEOUT_SUCCESS_MILLISECONDS) {
            return true; // Always expired.
        }
        // Only expired if the fetch failed (API null response).
        return (!fetchCompleted() || getFetchData(MAX_MILLISECONDS_TO_BLOCK_UI_WAITING_FOR_FETCH) == null);
    }

    @Nullable
    public RYDVoteData getFetchData(long maxTimeToWait) {
        try {
            return future.get(maxTimeToWait, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            Logger.printDebug(() -> "Waited but future was not complete after: " + maxTimeToWait + "ms");
        } catch (ExecutionException | InterruptedException ex) {
            Logger.printException(() -> "Future failure ", ex); // will never happen
        }
        return null;
    }

    /**
     * @return if the RYD fetch call has completed.
     */
    public boolean fetchCompleted() {
        return future.isDone();
    }

    private synchronized void clearUICache() {
        if (replacementLikeDislikeSpan != null) {
            Logger.printDebug(() -> "Clearing replacement span for: " + videoId);
        }
        replacementLikeDislikeSpan = null;
    }

    /**
     * Must call off main thread, as this will make a network call if user is not yet registered.
     *
     * @return ReturnYouTubeDislike user ID. If user registration has never happened
     * and the network call fails, this returns NULL.
     */
    @Nullable
    private static String getUserId() {
        Utils.verifyOffMainThread();

        String userId = Settings.RYD_USER_ID.get();
        if (!userId.isEmpty()) {
            return userId;
        }

        userId = ReturnYouTubeDislikeApi.registerAsNewUser();
        if (userId != null) {
            Settings.RYD_USER_ID.save(userId);
        }
        return userId;
    }

    @NonNull
    public String getVideoId() {
        return videoId;
    }

    /**
     * @return the replacement span containing dislikes, or the original span if RYD is not available.
     */
    @NonNull
    public synchronized Spanned getDislikesSpan(@NonNull Spanned original) {
        return waitForFetchAndUpdateReplacementSpan(original);
    }

    @NonNull
    private Spanned waitForFetchAndUpdateReplacementSpan(@NonNull Spanned original) {
        try {
            RYDVoteData votingData = getFetchData(MAX_MILLISECONDS_TO_BLOCK_UI_WAITING_FOR_FETCH);
            if (votingData == null) {
                Logger.printDebug(() -> "Cannot add dislike to UI (RYD data not available)");
                return original;
            }

            synchronized (this) {
                if (originalDislikeSpan != null && replacementLikeDislikeSpan != null) {
                    if (spansHaveEqualTextAndColor(original, replacementLikeDislikeSpan)) {
                        Logger.printDebug(() -> "Ignoring previously created dislikes span of data: " + videoId);
                        return original;
                    }
                    if (spansHaveEqualTextAndColor(original, originalDislikeSpan)) {
                        Logger.printDebug(() -> "Replacing span with previously created dislike span of data: " + videoId);
                        return replacementLikeDislikeSpan;
                    }
                }
                if (isPreviouslyCreatedSegmentedSpan(original.toString())) {
                    // need to recreate using original, as original has prior outdated dislike values
                    if (originalDislikeSpan == null) {
                        // Should never happen.
                        Logger.printDebug(() -> "Cannot add dislikes - original span is null. videoId: " + videoId);
                        return original;
                    }
                    original = originalDislikeSpan;
                }

                // No replacement span exist, create it now.

                if (userVote != null) {
                    votingData.updateUsingVote(userVote);
                }
                originalDislikeSpan = original;
                replacementLikeDislikeSpan = createDislikeSpan(original, votingData);
                Logger.printDebug(() -> "Replaced: '" + originalDislikeSpan + "' with: '"
                        + replacementLikeDislikeSpan + "'" + " using video: " + videoId);

                return replacementLikeDislikeSpan;
            }
        } catch (Exception e) {
            Logger.printException(() -> "waitForFetchAndUpdateReplacementSpan failure", e); // should never happen
        }
        return original;
    }

    public void sendVote(@NonNull Vote vote) {
        Utils.verifyOnMainThread();
        Objects.requireNonNull(vote);
        try {
            setUserVote(vote);

            voteSerialExecutor.execute(() -> {
                try { // Must wrap in try/catch to properly log exceptions.
                    ReturnYouTubeDislikeApi.sendVote(getUserId(), videoId, vote);
                } catch (Exception ex) {
                    Logger.printException(() -> "Failed to send vote", ex);
                }
            });
        } catch (Exception ex) {
            Logger.printException(() -> "Error trying to send vote", ex);
        }
    }

    /**
     * Sets the current user vote value, and does not send the vote to the RYD API.
     * <p>
     * Only used to set value if thumbs up/down is already selected on video load.
     */
    public void setUserVote(@NonNull Vote vote) {
        Objects.requireNonNull(vote);
        try {
            Logger.printDebug(() -> "setUserVote: " + vote);

            synchronized (this) {
                userVote = vote;
                clearUICache();
            }

            if (future.isDone()) {
                // Update the fetched vote data.
                RYDVoteData voteData = getFetchData(MAX_MILLISECONDS_TO_BLOCK_UI_WAITING_FOR_FETCH);
                if (voteData == null) {
                    // RYD fetch failed.
                    Logger.printDebug(() -> "Cannot update UI (vote data not available)");
                    return;
                }
                voteData.updateUsingVote(vote);
            } // Else, vote will be applied after fetch completes.

        } catch (Exception ex) {
            Logger.printException(() -> "setUserVote failure", ex);
        }
    }
}

/**
 * Vertically centers a Spanned Drawable.
 */
class VerticallyCenteredImageSpan extends ImageSpan {

    public VerticallyCenteredImageSpan(Drawable drawable) {
        super(drawable);
    }

    @Override
    public int getSize(@NonNull Paint paint, @NonNull CharSequence text,
                       int start, int end, @Nullable Paint.FontMetricsInt fontMetrics) {
        Drawable drawable = getDrawable();
        Rect bounds = drawable.getBounds();
        if (fontMetrics != null) {
            Paint.FontMetricsInt paintMetrics = paint.getFontMetricsInt();
            final int fontHeight = paintMetrics.descent - paintMetrics.ascent;
            final int drawHeight = bounds.bottom - bounds.top;
            final int halfDrawHeight = drawHeight / 2;
            final int yCenter = paintMetrics.ascent + fontHeight / 2;

            fontMetrics.ascent = yCenter - halfDrawHeight;
            fontMetrics.top = fontMetrics.ascent;
            fontMetrics.bottom = yCenter + halfDrawHeight;
            fontMetrics.descent = fontMetrics.bottom;
        }
        return bounds.right;
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end,
                     float x, int top, int y, int bottom, @NonNull Paint paint) {
        Drawable drawable = getDrawable();
        canvas.save();
        Paint.FontMetricsInt paintMetrics = paint.getFontMetricsInt();
        final int fontHeight = paintMetrics.descent - paintMetrics.ascent;
        final int yCenter = y + paintMetrics.descent - fontHeight / 2;
        final Rect drawBounds = drawable.getBounds();
        final int translateY = yCenter - (drawBounds.bottom - drawBounds.top) / 2;
        canvas.translate(x, translateY);
        drawable.draw(canvas);
        canvas.restore();
    }
}