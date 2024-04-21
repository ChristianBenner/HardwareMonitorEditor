package com.bennero.client.messages;

import com.bennero.common.PageData;
import com.bennero.common.messages.MessageType;
import com.bennero.common.messages.PageDataPositions;

import static com.bennero.common.Constants.*;
import static com.bennero.common.networking.NetworkUtils.writeStringToMessage;
import static com.bennero.common.networking.NetworkUtils.writeToMessage;

public class PageSetupMessage {
    public static byte[] create(PageData pageData) {
        byte[] bytes = new byte[MESSAGE_NUM_BYTES];
        byte pageId = pageData.getUniqueId();
        byte pageColourR = (byte) (pageData.getColour().getRed() * 255.0);
        byte pageColourG = (byte) (pageData.getColour().getGreen() * 255.0);
        byte pageColourB = (byte) (pageData.getColour().getBlue() * 255.0);
        byte titleColourR = (byte) (pageData.getTitleColour().getRed() * 255.0);
        byte titleColourG = (byte) (pageData.getTitleColour().getGreen() * 255.0);
        byte titleColourB = (byte) (pageData.getTitleColour().getBlue() * 255.0);
        byte subtitleColourR = (byte) (pageData.getSubtitleColour().getRed() * 255.0);
        byte subtitleColourG = (byte) (pageData.getSubtitleColour().getGreen() * 255.0);
        byte subtitleColourB = (byte) (pageData.getSubtitleColour().getBlue() * 255.0);
        byte pageRows = (byte) pageData.getRows();
        byte pageColumns = (byte) pageData.getColumns();
        byte nextPageId = pageData.getNextPageId();
        byte pageTransitionType = (byte) pageData.getTransitionType();
        int pageTransitionTime = pageData.getTransitionTime();
        int pageDurationMs = pageData.getDurationMs();
        String title = pageData.getTitle();
        byte titleEnabled = pageData.isTitleEnabled() ? (byte) 0x01 : (byte) 0x00;
        byte titleAlignment = (byte) pageData.getTitleAlignment();
        String subtitle = pageData.getSubtitle();
        byte subtitleEnabled = pageData.isSubtitleEnabled() ? (byte) 0x01 : (byte) 0x00;
        byte subtitleAlignment = (byte) pageData.getSubtitleAlignment();

        bytes[MESSAGE_TYPE_POS] = MessageType.PAGE_SETUP;
        bytes[PageDataPositions.ID_POS] = pageId;
        bytes[PageDataPositions.COLOUR_R_POS] = pageColourR;
        bytes[PageDataPositions.COLOUR_G_POS] = pageColourG;
        bytes[PageDataPositions.COLOUR_B_POS] = pageColourB;
        bytes[PageDataPositions.TITLE_COLOUR_R_POS] = titleColourR;
        bytes[PageDataPositions.TITLE_COLOUR_G_POS] = titleColourG;
        bytes[PageDataPositions.TITLE_COLOUR_B_POS] = titleColourB;
        bytes[PageDataPositions.SUBTITLE_COLOUR_R_POS] = subtitleColourR;
        bytes[PageDataPositions.SUBTITLE_COLOUR_G_POS] = subtitleColourG;
        bytes[PageDataPositions.SUBTITLE_COLOUR_B_POS] = subtitleColourB;
        bytes[PageDataPositions.ROWS_POS] = pageRows;
        bytes[PageDataPositions.COLUMNS_POS] = pageColumns;
        bytes[PageDataPositions.NEXT_ID_POS] = nextPageId;
        bytes[PageDataPositions.TRANSITION_TYPE_POS] = pageTransitionType;
        writeToMessage(bytes, PageDataPositions.TRANSITION_TIME_POS, pageTransitionTime);
        writeToMessage(bytes, PageDataPositions.DURATION_MS_POS, pageDurationMs);
        writeStringToMessage(bytes, PageDataPositions.TITLE_POS, title, NAME_STRING_NUM_BYTES);
        bytes[PageDataPositions.TITLE_ENABLED_POS] = titleEnabled;
        bytes[PageDataPositions.TITLE_ALIGNMENT_POS] = titleAlignment;
        writeStringToMessage(bytes, PageDataPositions.SUBTITLE_POS, subtitle, NAME_STRING_NUM_BYTES);
        bytes[PageDataPositions.SUBTITLE_POS_ENABLED_POS] = subtitleEnabled;
        bytes[PageDataPositions.SUBTITLE_POS_ALIGNMENT_POS] = subtitleAlignment;

        return bytes;
    }
}
