package folk.sisby.switchy.ui.util;

import folk.sisby.switchy.client.api.SwitchyFeedbackStatus;
import folk.sisby.switchy.client.api.SwitchyRequestFeedback;
import io.wispforest.owo.ops.TextOps;
import io.wispforest.owo.ui.util.Drawer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FeedbackToast implements Toast {
	private final SwitchyFeedbackStatus status;
	private final List<OrderedText> textLines;
	private final TextRenderer textRenderer;
	private final int duration;
	private final int width;
	private static final Map<SwitchyFeedbackStatus, Integer> colours = Map.of(
			SwitchyFeedbackStatus.SUCCESS, 0xA700FF00,
			SwitchyFeedbackStatus.INVALID, 0xA7AAAA00,
			SwitchyFeedbackStatus.FAILURE, 0xA7FF0000
	);

	public FeedbackToast(SwitchyRequestFeedback feedback, int duration) {
		this.duration = duration;
		status = feedback.status();
		textRenderer = MinecraftClient.getInstance().textRenderer;
		List<Text> texts = initText(feedback);
		width = Math.min(240, TextOps.width(textRenderer, texts) + 8);
		textLines = wrap(texts);
	}

	public static void report(SwitchyRequestFeedback feedback, int duration) {
		MinecraftClient.getInstance().getToastManager().add(new FeedbackToast(feedback, duration));
	}

	@Override
	public Visibility draw(MatrixStack matrices, ToastManager manager, long startTime) {
		Drawer.fill(matrices, 0, 0, getWidth(), getHeight(), 0x77000000);
		Drawer.drawRectOutline(matrices, 0, 0, getWidth(), getHeight(), colours.get(status));

		int xOffset = getWidth() / 2 - textRenderer.getWidth(textLines.get(0)) / 2;
		textRenderer.drawWithShadow(matrices, textLines.get(0), 4 + xOffset, 4, 0xFFFFFF);

		for (int i = 1; i < textLines.size(); i++) {
			textRenderer.draw(matrices, textLines.get(i), 4, 4 + i * 11, 0xFFFFFF);
		}

		return startTime > duration ? Visibility.HIDE : Visibility.SHOW;
	}

	@Override
	public int getHeight() {
		return 6 + textLines.size() * 11;
	}

	@Override
	public int getWidth() {
		return width;
	}

	private List<Text> initText(SwitchyRequestFeedback feedback) {
		List<Text> texts = new ArrayList<>();
		texts.add(Text.literal("----Switchy----").formatted(Formatting.BOLD, Formatting.AQUA));

		texts.addAll(feedback.messages());
		return texts;
	}

	private List<OrderedText> wrap(List<Text> messages) {
		List<OrderedText> list = new ArrayList<>();
		messages.forEach(text -> list.addAll(textRenderer.wrapLines(text, getWidth() - 8)));
		return list;
	}

	@Override
	public Object getType() {
		return Type.FEEDBACK_TYPE;
	}

	enum Type {
		FEEDBACK_TYPE
	}
}
