package app.kitsune.integrations.youtube.patches.components;

import androidx.annotation.Nullable;

import java.net.URLDecoder;

import app.kitsune.integrations.shared.patches.components.ByteArrayFilterGroup;
import app.kitsune.integrations.shared.patches.components.ByteArrayFilterGroupList;
import app.kitsune.integrations.shared.patches.components.Filter;
import app.kitsune.integrations.shared.patches.components.StringFilterGroup;
import app.kitsune.integrations.shared.utils.Logger;
import app.kitsune.integrations.youtube.patches.utils.ReturnYouTubeChannelNamePatch;
import app.kitsune.integrations.youtube.settings.Settings;

@SuppressWarnings({"unused", "CharsetObjectCanBeUsed"})
public final class ReturnYouTubeChannelNameFilterPatch extends Filter {
    private static final String DELIMITING_CHARACTER = "❙";
    private static final String CHANNEL_ID_IDENTIFIER_CHARACTER = "UC";
    private static final String CHANNEL_ID_IDENTIFIER_WITH_DELIMITING_CHARACTER =
            DELIMITING_CHARACTER + CHANNEL_ID_IDENTIFIER_CHARACTER;
    private static final String HANDLE_IDENTIFIER_CHARACTER = "@";
    private static final String HANDLE_IDENTIFIER_WITH_DELIMITING_CHARACTER =
            HANDLE_IDENTIFIER_CHARACTER + CHANNEL_ID_IDENTIFIER_CHARACTER;

    private final ByteArrayFilterGroupList shortsChannelBarAvatarFilterGroup = new ByteArrayFilterGroupList();

    public ReturnYouTubeChannelNameFilterPatch() {
        addPathCallbacks(
                new StringFilterGroup(Settings.REPLACE_CHANNEL_HANDLE, "|reel_channel_bar_inner.eml|")
        );
        shortsChannelBarAvatarFilterGroup.addAll(
                new ByteArrayFilterGroup(Settings.REPLACE_CHANNEL_HANDLE, "/@")
        );
    }

    @Override
    public boolean isFiltered(String path, @Nullable String identifier, String allValue, byte[] protobufBufferArray,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (shortsChannelBarAvatarFilterGroup.check(protobufBufferArray).isFiltered()) {
            setLastShortsChannelId(protobufBufferArray);
        }

        return false;
    }

    private void setLastShortsChannelId(byte[] protobufBufferArray) {
        try {
            String[] splitArr;
            final String bufferString = findAsciiStrings(protobufBufferArray);
            splitArr = bufferString.split(CHANNEL_ID_IDENTIFIER_WITH_DELIMITING_CHARACTER);
            if (splitArr.length < 2) {
                return;
            }
            final String splitedBufferString = CHANNEL_ID_IDENTIFIER_CHARACTER + splitArr[1];
            splitArr = splitedBufferString.split(HANDLE_IDENTIFIER_WITH_DELIMITING_CHARACTER);
            if (splitArr.length < 2) {
                return;
            }
            splitArr = splitArr[1].split(DELIMITING_CHARACTER);
            if (splitArr.length < 1) {
                return;
            }
            final String cachedHandle = HANDLE_IDENTIFIER_CHARACTER + splitArr[0];
            splitArr = splitedBufferString.split(DELIMITING_CHARACTER);
            if (splitArr.length < 1) {
                return;
            }
            final String channelId = splitArr[0].replaceAll("\"", "").trim();
            final String handle = URLDecoder.decode(cachedHandle, "UTF-8").trim();

            ReturnYouTubeChannelNamePatch.setLastShortsChannelId(handle, channelId);
        } catch (Exception ex) {
            Logger.printException(() -> "setLastShortsChannelId failed", ex);
        }
    }

    private String findAsciiStrings(byte[] buffer) {
        StringBuilder builder = new StringBuilder(Math.max(100, buffer.length / 2));
        builder.append("");

        // Valid ASCII values (ignore control characters).
        final int minimumAscii = 32;  // 32 = space character
        final int maximumAscii = 126; // 127 = delete character
        final int minimumAsciiStringLength = 4; // Minimum length of an ASCII string to include.
        String delimitingCharacter = "❙"; // Non ascii character, to allow easier log filtering.

        final int length = buffer.length;
        int start = 0;
        int end = 0;
        while (end < length) {
            int value = buffer[end];
            if (value < minimumAscii || value > maximumAscii || end == length - 1) {
                if (end - start >= minimumAsciiStringLength) {
                    for (int i = start; i < end; i++) {
                        builder.append((char) buffer[i]);
                    }
                    builder.append(delimitingCharacter);
                }
                start = end + 1;
            }
            end++;
        }
        return builder.toString();
    }
}
