package com.oikoaudio.fire.display;

import static com.oikoaudio.fire.AkaiFireOikontrolExtension.DEVICE_ID;
import static com.oikoaudio.fire.AkaiFireOikontrolExtension.MAN_ID_AKAI;
import static com.oikoaudio.fire.AkaiFireOikontrolExtension.PRODUCT_ID;
import static com.oikoaudio.fire.AkaiFireOikontrolExtension.SE_EN;
import static com.oikoaudio.fire.AkaiFireOikontrolExtension.SE_OLED_RGB;
import static com.oikoaudio.fire.AkaiFireOikontrolExtension.SE_ST;

import java.util.Arrays;
import java.util.List;

import com.oikoaudio.fire.SysExUtil;
import com.bitwig.extension.controller.api.MidiOut;

public class OledDisplay {
	private static final int GENERAL_BAR_WIDTH = 126;
	private static final long DEFAULT_CLEAR_DELAY_MS = 1500;
	private static final long GRAPHICS_KEEPALIVE_MS = 3000;
	private static final int OLED_PAGE_COUNT = 8;
	private static final int OLED_PAGE_WIDTH = 128;
	private static final int OLED_IMAGE_BYTES = OLED_PAGE_COUNT * OLED_PAGE_WIDTH;
	private final byte[] oledBar = new byte[] { SE_ST, MAN_ID_AKAI, DEVICE_ID, PRODUCT_ID, 0x09, 0x00, 0x08, //
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, SE_EN };
	private final byte[] oledCmd = new byte[] { SE_ST, MAN_ID_AKAI, DEVICE_ID, PRODUCT_ID, SE_OLED_RGB, 00 };
	private final byte[] oledPack = new byte[] { SE_ST, MAN_ID_AKAI, DEVICE_ID, PRODUCT_ID, 0x0E };

	private final MidiOut midiOut;
	private final int[][] imagePageCache = new int[OLED_PAGE_COUNT][OLED_PAGE_WIDTH];
	private final boolean[] imagePageValid = new boolean[OLED_PAGE_COUNT];
	private final long[] imagePageLastSentMs = new long[OLED_PAGE_COUNT];
	private boolean inGraphicsMode = false;
	private long clearTask = -1;
	private long clearTaskDelayMs = DEFAULT_CLEAR_DELAY_MS;
	private long clearDelayMs = DEFAULT_CLEAR_DELAY_MS;
	private long transientMessageUntilMs = -1;
	private long transientMessageStartedAtMs = -1;
	private long layoutRevision = 0;
	private String footerLegend = null;
	private EncoderLegendPosition footerLegendPosition = EncoderLegendPosition.BOTTOM;
	private ScreenState screenState = ScreenState.BLANK;
	private long logoBlock;
	private Runnable idleAction;

	private enum ScreenState {
		BLANK,
		GRAPHICS,
		TRANSIENT_TEXT,
		PERSISTENT_TEXT
	}

	public enum Fill {
		Empty, Solid, Fifty, Hatch;
	}

	public enum TextJustification {
		LEFT((byte) 0), CENTER((byte) 1), RIGHT((byte) 2);

		private byte code;

		private TextJustification(final byte code) {
			this.code = code;
		}

		public byte getCode() {
			return code;
		}
	}

	public OledDisplay(final MidiOut midiOut) {
		super();
		this.midiOut = midiOut;
	}

	public void clearScreenDelayed() {
		clearScreenDelayed(clearDelayMs);
	}

	public void clearScreenDelayed(final long delayMs) {
		if (screenState == ScreenState.PERSISTENT_TEXT) {
			return;
		}
		clearTask = System.currentTimeMillis();
		clearTaskDelayMs = Math.max(0, delayMs);
		transientMessageStartedAtMs = clearTask;
		transientMessageUntilMs = clearTask + clearTaskDelayMs;
	}

	public boolean hasPendingClear() {
		return clearTask > 0;
	}

	public boolean hasPendingTransientMessage() {
		return hasPendingClear() || System.currentTimeMillis() < transientMessageUntilMs;
	}

	public boolean hasRecentTransientMessage(final long quietPeriodMs) {
		return transientMessageStartedAtMs > 0
				&& System.currentTimeMillis() - transientMessageStartedAtMs < Math.max(0, quietPeriodMs);
	}

	public long layoutRevision() {
		return layoutRevision;
	}

	public void setClearDelayMs(final long clearDelayMs) {
		this.clearDelayMs = Math.max(0, clearDelayMs);
	}

	public void setIdleAction(final Runnable idleAction) {
		this.idleAction = idleAction;
	}

	public void setFooterLegend(final String footerLegend) {
		this.footerLegend = footerLegend == null || footerLegend.isBlank() ? null : footerLegend;
	}

	public EncoderLegendPosition footerLegendPosition() {
		return footerLegendPosition;
	}

	public void setFooterLegendPosition(final EncoderLegendPosition footerLegendPosition) {
		final EncoderLegendPosition normalized = footerLegendPosition == null
				? EncoderLegendPosition.BOTTOM
				: footerLegendPosition;
		if (this.footerLegendPosition != normalized) {
			this.footerLegendPosition = normalized;
			layoutRevision++;
		}
	}

	public void clearScreen() {
		beginBlankScreen();
		sendImage(SysExUtil.EMPTY_SCREEN);
		inGraphicsMode = false;
		screenState = ScreenState.BLANK;
	}

	public void sendImage(final int[] imageData) {
		beginGraphicsScreen();
		layoutRevision++;
		sendImageData(imageData);
		inGraphicsMode = true;
	}

	public void sendImageWithFooter(final int[] imageData, final String footerLegend) {
		beginGraphicsScreen();
		if (imageData.length != OLED_IMAGE_BYTES) {
			sendImage(imageData);
			setFooterLegend(footerLegend);
			renderFooterLegend();
			return;
		}
		layoutRevision++;
		sendImageData(imageData, firstImagePageWithFooter(), lastImagePageWithFooter());
		inGraphicsMode = false;
		setFooterLegend(footerLegend);
		renderFooterLegendPreservingImageCache();
		inGraphicsMode = true;
	}

	public void sendImageData(final int[] imageData) {
		beginGraphicsScreen();
		if (imageData.length != OLED_IMAGE_BYTES) {
			sendImagePages(imageData, 0, OLED_PAGE_COUNT - 1);
			invalidateImageCache();
			return;
		}

		sendImageData(imageData, 0, OLED_PAGE_COUNT - 1);
	}

	private void sendImageData(final int[] imageData, final int startPage, final int endPage) {
		final long now = System.currentTimeMillis();
		for (int page = Math.max(0, startPage); page <= Math.min(OLED_PAGE_COUNT - 1, endPage); page++) {
			final int offset = page * OLED_PAGE_WIDTH;
			if (imagePageValid[page]
					&& pageMatches(imageData, offset, imagePageCache[page])
					&& now - imagePageLastSentMs[page] < GRAPHICS_KEEPALIVE_MS) {
				continue;
			}
			sendImagePage(imageData, page);
			System.arraycopy(imageData, offset, imagePageCache[page], 0, OLED_PAGE_WIDTH);
			imagePageValid[page] = true;
			imagePageLastSentMs[page] = now;
		}
	}

	private boolean pageMatches(final int[] imageData, final int offset, final int[] cachedPage) {
		for (int i = 0; i < OLED_PAGE_WIDTH; i++) {
			if (imageData[offset + i] != cachedPage[i]) {
				return false;
			}
		}
		return true;
	}

	private void sendImagePage(final int[] imageData, final int page) {
		final int offset = page * OLED_PAGE_WIDTH;
		final int[] pageData = Arrays.copyOfRange(imageData, offset, offset + OLED_PAGE_WIDTH);
		sendImagePages(pageData, page, page);
	}

	private void sendImagePages(final int[] imageData, final int startPage, final int endPage) {
		final byte[] bytelist = SysExUtil.toBytePack(imageData);
		final int datalen = bytelist.length + 4;
		final byte[] sysex = new byte[oledPack.length + bytelist.length + 7];

		System.arraycopy(oledPack, 0, sysex, 0, oledPack.length);
		System.arraycopy(bytelist, 0, sysex, 11, bytelist.length);
		sysex[5] = (byte) (datalen >> 7 & 0x7F);
		sysex[6] = (byte) (datalen & 0x7F);
		sysex[7] = (byte) startPage;
		sysex[8] = (byte) endPage;
		sysex[9] = (byte) 0;
		sysex[10] = (byte) 127;
		sysex[sysex.length - 1] = SE_EN;
		midiOut.sendSysex(sysex);
	}

	private void invalidateImageCache() {
		Arrays.fill(imagePageValid, false);
	}

	public void showBar(final boolean outline, final int width, final int height, final Fill foreground,
			final Fill background, final int offset, final int start, final int end) {
		oledBar[7] = (byte) width;
		oledBar[8] = (byte) height;
		oledBar[9] = (byte) (outline ? 1 : 0);
		oledBar[10] = (byte) foreground.ordinal();
		oledBar[11] = (byte) background.ordinal();
		oledBar[12] = (byte) offset;
		oledBar[13] = (byte) start;
		oledBar[14] = (byte) end;
		midiOut.sendSysex(oledBar);
		invalidateImageCache();
	}

	public void detailInfo(final String title, final String lines) {
		clearScreen();
		markTransientMessage();
		final String[] line = lines.split("\\n");
		sendContentString(1, TextJustification.CENTER, 0, title);
		for (int i = 0; i < 7; i++) {
			final String l = i < line.length ? line[i] : "";
			sendContentString(0, TextJustification.LEFT, 2 + i * 1, l);
		}
		if (line.length <= 5) {
			renderFooterLegend();
		}
	}

	public void lineInfo(final String title, final String lines) {
		clearScreen();
		markTransientMessage();
		final String[] line = lines.split("\\n");
		sendContentString(2, TextJustification.CENTER, 0, title);
		for (int i = 0; i < 3; i++) {
			final String l = i < line.length ? line[i] : "";
			sendContentString(1, TextJustification.LEFT, 2 + i * 2, l);
		}
		renderFooterLegend();
	}

	public void functionInfo(final String details, final String functionName, final String lines) {
		markTransientMessage();
		sendContentString(0, TextJustification.CENTER, 0, details);
		sendContentString(2, TextJustification.CENTER, 1, functionName);
		sendContentString(0, TextJustification.LEFT, 3, "");
		final String[] line = lines.split("\\n");
		for (int i = 0; i < 4; i++) {
			final String l = i < line.length ? line[i] : "";
			sendContentString(0, TextJustification.LEFT, 4 + i * 1, l);
		}
	}

	public void showInfo(final DisplayInfo info, final Object... values) {
		if (logoBlock != -1 && System.currentTimeMillis() - logoBlock < 3000) {
			return;
		}
		final List<Line> lines = info.getLines();
		for (final Line line : lines) {
			showLine(line);
		}
	}

	public void showLine(final Line line) {
		sendContentString(line.getSize(), line.getJustification(), line.getOffset(), line.getViewText());
	}

	public void paramInfo(final String paramName, final String details) {
		markTransientMessage();
		sendContentString(0, TextJustification.CENTER, 0, details);
		sendContentString(2, TextJustification.CENTER, 1, paramName);
		sendContentString(2, TextJustification.CENTER, 3, "");
		sendContentString(3, TextJustification.CENTER, 5, "");
		renderFooterLegend();
	}

	public void paramInfo(final String paramName, final String value, final String details) {
		markTransientMessage();
		sendContentString(0, TextJustification.CENTER, 0, details);
		sendContentString(2, TextJustification.CENTER, 1, paramName);
		sendContentString(0, TextJustification.CENTER, 3, "");
		sendContentString(2, TextJustification.CENTER, 4, value);
		sendContentString(0, TextJustification.CENTER, 6, "");
		renderFooterLegend();
	}

	public void paramInfo(final String paramName, final int value, final String details, final int min, final int max) {
		paramInfo(paramName, value, details, min, max, null);
	}

	public void parameterInfo(final String element, final String parameterName, final double value,
			final String displayValue, final boolean biPolar) {
		markTransientMessage();
		sendContentString(0, TextJustification.CENTER, 0, element);
		sendContentString(2, TextJustification.CENTER, 1, parameterName);
		sendContentString(2, TextJustification.CENTER, 3, displayValue);
		sendContentString(0, TextJustification.CENTER, 5, "");
		if (biPolar) {
			barValue(value - 0.5, -0.5, 0.5);
		} else {
			barValue(value, 0, 1);
		}
		renderFooterLegend();
	}

	public void paramInfo(final String paramName, final int value, final String details, final int min, final int max,
			final Integer offValue) {
		markTransientMessage();
		sendContentString(0, TextJustification.CENTER, 0, details);
		sendContentString(2, TextJustification.CENTER, 1, paramName);
		if (offValue != null && value == offValue.intValue()) {
			sendContentString(2, TextJustification.CENTER, 3, "Off");
		} else {
			sendContentString(2, TextJustification.CENTER, 3, Integer.toString(value));
		}
		sendContentString(0, TextJustification.CENTER, 5, "");

		final int range = max - min;
		final double unit = (double) GENERAL_BAR_WIDTH / (double) range;
		final int bar = (int) Math.round(min + unit * (value - min));
		showBar(true, GENERAL_BAR_WIDTH, 1, Fill.Fifty, Fill.Empty, 6, 0, bar);
		renderFooterLegend();
	}

	public void valueInfo(final String title, final String value) {
		clearScreen();
		valueInfoNoClear(title, value);
	}

	public void valueInfoNoClear(final String title, final String value) {
		markTransientMessage();
		drawValueInfoNoClear(title, value);
	}

	public void valueInfoPersistentNoClear(final String title, final String value) {
		beginPersistentTextScreen();
		drawValueInfoNoClear(title, value);
		beginPersistentTextScreen();
	}

	private void drawValueInfoNoClear(final String title, final String value) {
		sendContentString(2, TextJustification.CENTER, 0, title);
		sendContentString(3, TextJustification.CENTER, 2, value);
		sendContentString(5, TextJustification.CENTER, 5, "");
		renderFooterLegend();
	}

	public void paramInfoDouble(final String paramName, final double value, final String details, final double min,
			final double max) {
		markTransientMessage();
		sendContentString(0, TextJustification.CENTER, 0, details);
		sendContentString(2, TextJustification.CENTER, 1, paramName);
		sendContentString(2, TextJustification.CENTER, 3, Integer.toString((int) value));
		sendContentString(0, TextJustification.CENTER, 5, "");
		barValue(value, min, max);
		renderFooterLegend();
	}

	public void paramInfoPercent(final String paramName, final double value, final String details, final double min,
			final double max) {
		markTransientMessage();
		sendContentString(0, TextJustification.CENTER, 0, details);
		sendContentString(2, TextJustification.CENTER, 1, paramName);
		sendContentString(2, TextJustification.CENTER, 3, toPercent(value));
		sendContentString(0, TextJustification.CENTER, 5, "");
		barValue(value, min, max);
		renderFooterLegend();
	}

	public void paramInfoDuration(final String paramName, final double duration, final String details,
			final double gridRes) {
		markTransientMessage();
		final double stepLen = duration / gridRes;
		sendContentString(0, TextJustification.CENTER, 0, details);
		sendContentString(2, TextJustification.CENTER, 1, paramName);
		sendContentString(2, TextJustification.CENTER, 3, String.format("%.2f", stepLen));
		sendContentString(0, TextJustification.CENTER, 5, "");
		sendContentString(3, TextJustification.CENTER, 6, "");
		renderFooterLegend();
	}

	private void barValue(final double value, final double min, final double max) {
		final double range = max - min;
		final double unit = GENERAL_BAR_WIDTH / range;
		int start = 0;
		int end = 0;
		if (min < 0) {
			if (value < 0) {
				end = GENERAL_BAR_WIDTH / 2;
				start = end + (int) Math.round(unit * value);
			} else {
				start = GENERAL_BAR_WIDTH / 2;
				end = start + (int) Math.round(unit * value);
			}
		} else {
			end = (int) Math.round(unit * value);
		}
		showBar(true, GENERAL_BAR_WIDTH, 1, Fill.Fifty, Fill.Empty, 6, start, end);
	}

	private void renderFooterLegend() {
		if (footerLegend != null) {
			sendString(0, TextJustification.LEFT, footerLegendPosition.row(), footerLegend);
		}
	}

	private void renderFooterLegendPreservingImageCache() {
		if (footerLegend != null) {
			sendString(0, TextJustification.LEFT, footerLegendPosition.row(), footerLegend, false, false);
			imagePageValid[footerLegendPosition.row()] = false;
		}
	}

	private int firstImagePageWithFooter() {
		return footerLegendPosition == EncoderLegendPosition.TOP ? 1 : 0;
	}

	private int lastImagePageWithFooter() {
		return footerLegendPosition == EncoderLegendPosition.TOP ? OLED_PAGE_COUNT - 1 : OLED_PAGE_COUNT - 2;
	}

	private String toPercent(final double chance) {
		final int val = (int) Math.round(chance * 100);
		return Integer.toString(val) + "%";
	}

	private void markTransientMessage() {
		screenState = ScreenState.TRANSIENT_TEXT;
		transientMessageStartedAtMs = System.currentTimeMillis();
		transientMessageUntilMs = transientMessageStartedAtMs + clearDelayMs;
	}

	private void beginBlankScreen() {
		screenState = ScreenState.BLANK;
		clearTask = -1;
		transientMessageUntilMs = -1;
		transientMessageStartedAtMs = -1;
	}

	private void beginGraphicsScreen() {
		screenState = ScreenState.GRAPHICS;
		clearTask = -1;
		transientMessageUntilMs = -1;
		transientMessageStartedAtMs = -1;
	}

	private void beginPersistentTextScreen() {
		screenState = ScreenState.PERSISTENT_TEXT;
		clearTask = -1;
		transientMessageUntilMs = -1;
		transientMessageStartedAtMs = -1;
	}

	private void sendContentString(final int fontSize, final TextJustification justification, final int placement,
			final String text) {
		sendString(fontSize, justification, contentPlacement(placement), text);
	}

	private int contentPlacement(final int placement) {
		return footerLegendPosition == EncoderLegendPosition.TOP ? placement + 2 : placement;
	}

	public void sendString(final int fontSize, final TextJustification justification, final int placement,
			final String text) {
		sendString(fontSize, justification, placement, text, true, true);
	}

	private void sendString(final int fontSize, final TextJustification justification, final int placement,
			final String text, final boolean clearGraphics, final boolean invalidateAllImages) {
		if (clearGraphics && inGraphicsMode) {
			final ScreenState targetState = screenState;
			clearScreen();
			screenState = targetState;
		}
		if (placement > 7 || fontSize > 3) {
			return;
		}
		final String fitText = text.length() > 20 ? text.substring(0, 20) : text;
		final byte[] sysex = new byte[fitText.length() + oledCmd.length + 5];
		System.arraycopy(oledCmd, 0, sysex, 0, oledCmd.length);
		sysex[sysex.length - 1] = SE_EN;
		sysex[6] = (byte) (fitText.length() + 3);
		sysex[7] = (byte) fontSize;
		sysex[8] = justification.getCode();
		sysex[9] = (byte) placement;
		for (int i = 0; i < fitText.length(); i++) {
			sysex[10 + i] = (byte) fitText.charAt(i);
		}
		midiOut.sendSysex(sysex);
		if (invalidateAllImages) {
			invalidateImageCache();
		}
		inGraphicsMode = false;
		clearTask = -1;
	}

	public void notifyBlink(final int blinkTicks) {
		if (clearTask > 0 && System.currentTimeMillis() - clearTask > clearTaskDelayMs) {
			clearTask = -1;
			transientMessageUntilMs = -1;
			transientMessageStartedAtMs = -1;
			if (idleAction != null) {
				idleAction.run();
			} else {
				clearScreen();
			}
		}
		if (logoBlock > 0 && System.currentTimeMillis() - logoBlock > 3000) {
			logoBlock = -1;
		}
	}

}
