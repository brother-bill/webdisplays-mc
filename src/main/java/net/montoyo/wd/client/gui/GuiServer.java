/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.miniserv.Constants;
import net.montoyo.wd.miniserv.client.*;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.utilities.*;
import net.montoyo.wd.utilities.math.Vector3i;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.serialization.NameUUIDPair;
import net.montoyo.wd.utilities.serialization.Util;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Supplier;

import static net.montoyo.wd.client.gui.GuiMinePad.getChar;

public class GuiServer extends WDScreen {
	
	private static final ResourceLocation BG_IMAGE = ResourceLocation.fromNamespaceAndPath("webdisplays", "textures/gui/server_bg.png");
	private static final ResourceLocation FG_IMAGE = ResourceLocation.fromNamespaceAndPath("webdisplays", "textures/gui/server_fg.png");
	private static final HashMap<String, Method> COMMAND_MAP = new HashMap<>();
	private static final int MAX_LINE_LEN = 32;
	private static final int MAX_LINES = 12;
	
	private final Vector3i serverPos;
	private final NameUUIDPair owner;
	private final ArrayList<String> lines = new ArrayList<>();
	private String prompt = "";
	private String userPrompt;
	private int blinkTime;
	private String lastCmd;
	private boolean promptLocked;
	private volatile long queryTime;
	private ClientTask<?> currentTask;
	private int selectedLine = -1;
	
	//Access command
	private int accessTrials;
	private int accessTime;
	private int accessState = -1;
	private SimpleSoundInstance accessSound;
	
	//Upload wizard
	private boolean uploadWizard;
	private File uploadDir;
	private final ArrayList<File> uploadFiles = new ArrayList<>();
	private int uploadOffset;
	private boolean uploadFirstIsParent;
	private String uploadFilter = "";
	private long uploadFilterTime;
	
	public GuiServer(Vector3i vec, NameUUIDPair owner) {
		super(Component.nullToEmpty(null));
		serverPos = vec;
		this.owner = owner;
		userPrompt = "> ";
		
		if (COMMAND_MAP.isEmpty())
			buildCommandMap();
		
		lines.add("MiniServ 1.0");
		lines.add(tr("info"));
		uploadCD(FileSystemView.getFileSystemView().getDefaultDirectory());
	}
	
	private static String tr(String key, Object... args) {
		return I18n.get("webdisplays.server." + key, args);
	}
	
	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float ptt) {
		super.render(graphics, mouseX, mouseY, ptt);
		
		int x = (width - 256) / 2;
		int y = (height - 176) / 2;

//        RenderSystem.enableTexture();
		RenderSystem.setShaderTexture(0, BG_IMAGE);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		graphics.blit(BG_IMAGE, x, y, 0, 0, 256, 256);

		x += 18;
		y += 18;
		
		for (int i = 0; i < lines.size(); i++) {
			if (selectedLine == i) {
				drawWhiteQuad(x - 1, y - 2, font.width(lines.get(i)) + 1, 12);
				graphics.drawString(Minecraft.getInstance().font, lines.get(i), x, y, 0xFF129700, false);
			} else
				graphics.drawString(Minecraft.getInstance().font, lines.get(i), x, y, 0xFFFFFFFF, false);
			
			y += 12;
		}
		
		if (!promptLocked) {
			if (queue.isEmpty()) {
				x = graphics.drawString(Minecraft.getInstance().font, userPrompt, x, y, 0xFFFFFFFF, false);
				x = graphics.drawString(Minecraft.getInstance().font, prompt, x, y, 0xFFFFFFFF, false);
			} else {
				x = graphics.drawString(Minecraft.getInstance().font, tr("press_for_more"), x, y, 0xFFFFFFFF, false);
			}
		}
		
		if (!uploadWizard && blinkTime < 5)
			drawWhiteQuad(x + 1, y, 6, 8);

//        RenderSystem.enableTexture();
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.setShaderTexture(0, FG_IMAGE);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
//        blit(graphics,(width - 256) / 2, (height - 176) / 2, 0, 0, 256, 176);
	}
	
	private void drawWhiteQuad(int x, int y, int w, int h) {
		float xd = (float) x;
		float xd2 = (float) (x + w);
		float yd = (float) y;
		float yd2 = (float) (y + h);
		float zd = (float) getBlitOffset();

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		RenderSystem.setShader(GameRenderer::getPositionShader);
		Tesselator t = Tesselator.getInstance();
		BufferBuilder bb = t.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
		bb.addVertex(xd, yd2, zd);
		bb.addVertex(xd2, yd2, zd);
		bb.addVertex(xd2, yd, zd);
		bb.addVertex(xd, yd, zd);
		BufferUploader.drawWithShader(bb.buildOrThrow());
	}
	
	private float getBlitOffset() {
		return 0;
	}
	
	@Override
	public void tick() {
		super.tick();
		
		if (accessState >= 0) {
			if (--accessTime <= 0) {
				accessState++;
				
				if (accessState == 1) {
					if (lines.size() > 0)
						lines.remove(lines.size() - 1);
					
					lines.add("access: PERMISSION DENIED....and...");
					accessTime = 20;
				} else {
					if (accessSound == null) {
						accessSound = new SimpleSoundInstance(WebDisplays.INSTANCE.soundServer.getLocation(), SoundSource.MASTER, 1.0f, 1.0f, RandomSource.create(), true, 0, SoundInstance.Attenuation.NONE, 0.0f, 0.0f, 0.0f, false);
						minecraft.getSoundManager().play(accessSound);
					}
					
					writeLine("YOU DIDN'T SAY THE MAGIC WORD!");
					accessTime = 2;
				}
			}
		} else {
			blinkTime = (blinkTime + 1) % 10;
			
			if (currentTask != null) {
				long queryTime;
				synchronized (this) {
					queryTime = this.queryTime;
				}
				
				if (System.currentTimeMillis() - queryTime >= 10000) {
					writeLine(tr("timeout"));
					currentTask.cancel();
					clearTask();
				}
			}
			
			if (!uploadFilter.isEmpty() && System.currentTimeMillis() - uploadFilterTime >= 1000) {
				Log.info("Upload filter cleared");
				uploadFilter = "";
			}
		}
		
		final int maxl = uploadWizard ? MAX_LINES : (MAX_LINES - 1); //Cuz prompt is hidden
		if (!queue.isEmpty()) {
			while (!queue.isEmpty()) {
				if (lines.size() >= maxl)
					break;
				writeLine(queue.remove(0));
			}
		}
		
		while (lines.size() > maxl)
			lines.remove(0);
	}
	
	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		Supplier<Boolean> predicate = () -> super.keyReleased(keyCode, scanCode, modifiers);
		
		try {
			return handleKeyboardInput(keyCode, false, predicate);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE && !uploadWizard) {
			Minecraft.getInstance().setScreen(null);
			return true;
		}
		
		getChar(keyCode, scanCode).ifPresent(c -> {
			try {
				keyTyped(c, keyCode, modifiers);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		
		try {
			return handleKeyboardInput(keyCode, true, () -> true);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean handleKeyboardInput(int keyCode, boolean keyState, Supplier<Boolean> booleanSupplier) throws IOException {
		if (!queue.isEmpty())
			return false;
		
		if (uploadWizard) {
			if (keyState) {
				if (keyCode == GLFW.GLFW_KEY_UP) {
					if (selectedLine > 3)
						selectedLine--;
					else if (uploadOffset > 0) {
						uploadOffset--;
						updateUploadScreen();
					}
				} else if (keyCode == GLFW.GLFW_KEY_DOWN) {
					if (selectedLine < MAX_LINES - 1)
						selectedLine++;
					else if (uploadOffset + selectedLine - 2 < uploadFiles.size()) {
						uploadOffset++;
						updateUploadScreen();
					}
				} else if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
					selectedLine = 3;
					int dst = uploadOffset - (MAX_LINES - 3);
					if (dst < 0)
						dst = 0;
					
					selectFile(dst);
				} else if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
					selectedLine = 3;
					int dst = uploadOffset + (MAX_LINES - 3);
					if (dst >= uploadFiles.size())
						dst = uploadFiles.size() - 1;
					
					selectFile(dst);
				} else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
					File file = uploadFiles.get(uploadOffset + selectedLine - 3);
					
					if (file.isDirectory()) {
						uploadCD(file);
						updateUploadScreen();
					} else
						startFileUpload(file, true);
				} else if (keyCode == GLFW.GLFW_KEY_F5) {
					uploadCD(uploadDir);
					updateUploadScreen();
				}
			}
			
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				quitUploadWizard();
				return true; //Don't let the screen handle this
			}
			
			return booleanSupplier.get();
		} else {
			boolean value = booleanSupplier.get();
			
			if (keyState) {
				boolean ctrl = Screen.hasControlDown();
				
				if (keyCode == GLFW.GLFW_KEY_L && ctrl)
					lines.clear();
				else if (keyCode == GLFW.GLFW_KEY_V && ctrl) {
					prompt += Minecraft.getInstance().keyboardHandler.getClipboard();
					
					if (prompt.length() > MAX_LINE_LEN)
						prompt = prompt.substring(0, MAX_LINE_LEN);
				} else if (keyCode == GLFW.GLFW_KEY_UP) {
					if (lastCmd != null) {
						String tmp = prompt;
						prompt = lastCmd;
						lastCmd = tmp;
					}
				}
			}
			
			return value;
		}
	}
	
	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		return super.charTyped(codePoint, modifiers);
		
	}
	
	protected void keyTyped(char typedChar, int keyCode, int modifier) throws IOException {
		//this.charTyped(typedChar, modifier);
		
		if (keyCode == GLFW.GLFW_KEY_DOWN) {
			if (!queue.isEmpty()) {
				writeLine(queue.remove(0));
				return;
			}
		}
		if (!queue.isEmpty())
			return;
		
		if (uploadWizard) {
			boolean found = false;
			uploadFilter += Character.toLowerCase(typedChar);
			uploadFilterTime = System.currentTimeMillis();
			
			for (int i = uploadFirstIsParent ? 1 : 0; i < uploadFiles.size(); i++) {
				if (uploadFiles.get(i).getName().toLowerCase().startsWith(uploadFilter)) {
					selectFile(i);
					found = true;
					break;
				}
			}
			
			if (!found && uploadFilter.length() == 1)
				uploadFilter = "";
			
			return;
		} else if (promptLocked)
			return;
		
		if (keyCode == GLFW.GLFW_KEY_SPACE)
			typedChar = ' ';
		if (
				(typedChar == 'v' || typedChar == 'V') &&
						(modifier & 2) == 2
		) return;
		
		if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
			if (prompt.length() > 0)
				prompt = prompt.substring(0, prompt.length() - 1);
		} else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
			if (prompt.length() > 0) {
				writeLine(userPrompt + prompt);
				evaluateCommand(prompt);
				lastCmd = prompt;
				prompt = "";
			} else
				writeLine(userPrompt);
		} else if (prompt.length() + 1 < MAX_LINE_LEN && typedChar >= 32 && typedChar <= 126)
			prompt = prompt + typedChar;
		
		blinkTime = 0;
	}
	
	private void evaluateCommand(String str) {
		String[] args = str.trim().split("\\s+");
		Method handler = COMMAND_MAP.get(args[0].toLowerCase());
		
		if (handler == null) {
			writeLine(tr("unknowncmd"));
			return;
		}
		
		Object[] params;
		if (handler.getParameterCount() == 0)
			params = new Object[0];
		else {
			String[] args2 = new String[args.length - 1];
			System.arraycopy(args, 1, args2, 0, args2.length);
			params = new Object[]{args2};
		}
		
		try {
			handler.invoke(this, params);
		} catch (IllegalAccessException | InvocationTargetException e) {
			Log.errorEx("Caught exception while running command \"%s\"", e, str);
			writeLine(tr("error"));
		}
	}
	
	private void writeLine(String line) {
		final int maxl = uploadWizard ? MAX_LINES : (MAX_LINES - 1); //Cuz prompt is hidden
		while (lines.size() >= maxl)
			lines.remove(0);
		
		lines.add(line);
	}
	
	private static void buildCommandMap() {
		COMMAND_MAP.clear();
		
		Method[] methods = GuiServer.class.getMethods();
		for (Method m : methods) {
			CommandHandler cmd = m.getAnnotation(CommandHandler.class);
			
			if (cmd != null && Modifier.isPublic(m.getModifiers())) {
				if (m.getParameterCount() == 0 || (m.getParameterCount() == 1 && m.getParameterTypes()[0] == String[].class))
					COMMAND_MAP.put(cmd.value().toLowerCase(), m);
			}
		}
	}
	
	private void quitUploadWizard() {
		lines.clear();
		promptLocked = false;
		uploadWizard = false;
		selectedLine = -1;
	}
	
	@Override
	public void onClose() {
		super.onClose();
		
		if (accessSound != null)
			Minecraft.getInstance().getSoundManager().stop(accessSound);
	}
	
	private boolean queueTask(ClientTask<?> task) {
		if (Client.getInstance().addTask(task)) {
			promptLocked = true;
			queryTime = System.currentTimeMillis(); //No task is running so it's okay to have an unsynchronized access here
			currentTask = task;
			return true;
		} else {
			writeLine(tr("queryerr"));
			return false;
		}
	}
	
	private void clearTask() {
		promptLocked = false;
		currentTask = null;
	}
	
	private static String trimStringL(String str) {
		int delta = str.length() - MAX_LINE_LEN;
		if (delta <= 0)
			return str;
		
		return "..." + str.substring(delta + 3);
	}
	
	private static String trimStringR(String str) {
		return (str.length() <= MAX_LINE_LEN) ? str : (str.substring(0, MAX_LINE_LEN - 3) + "...");
	}
	
	@CommandHandler("clear")
	public void commandClear() {
		lines.clear();
	}
	
	@CommandHandler("help")
	public void commandHelp(String[] args) {
		queueRead = lines.size();
		
		if (args.length > 0) {
			String cmd = args[0].toLowerCase();
			
			if (COMMAND_MAP.containsKey(cmd))
				queueLine(tr("help." + cmd));
			else
				queueLine(tr("unknowncmd"));
		} else {
			for (String c : COMMAND_MAP.keySet())
				queueLine(c + " - " + tr("help." + c));
		}
	}
	
	@CommandHandler("exit")
	public void commandExit() {
		minecraft.setScreen(null);
	}
	
	@CommandHandler("access")
	public void commandAccess(String[] args) {
		boolean handled = false;
		
		if (args.length >= 1 && args[0].equalsIgnoreCase("security")) {
			if (args.length == 1 || (args.length == 2 && args[1].equalsIgnoreCase("grid")))
				handled = true;
		} else if (args.length == 3 && args[0].equalsIgnoreCase("main") && args[1].equalsIgnoreCase("security") && args[2].equalsIgnoreCase("grid"))
			handled = true;
		
		if (handled) {
			writeLine("access: PERMISSION DENIED.");
			
			if (++accessTrials >= 3) {
				promptLocked = true;
				accessState = 0;
				accessTime = 20;
			}
		} else
			writeLine(tr("argerror"));
	}
	
	@CommandHandler("owner")
	public void commandOwner() {
		writeLine(tr("ownername", owner.name));
		writeLine(tr("owneruuid"));
		writeLine(owner.uuid.toString());
	}
	
	@CommandHandler("quota")
	public void commandQuota() {
		if (!minecraft.player.getGameProfile().getId().equals(owner.uuid)) {
			writeLine(tr("errowner"));
			return;
		}
		
		ClientTaskGetQuota task = new ClientTaskGetQuota();
		task.setFinishCallback((t) -> {
			writeLine(tr("quota", Util.sizeString(t.getQuota()), Util.sizeString(t.getMaxQuota())));
			clearTask();
		});
		
		queueTask(task);
	}
	
	@CommandHandler("ls")
	public void commandList() {
		ClientTaskGetFileList task = new ClientTaskGetFileList(owner.uuid);
		task.setFinishCallback((t) -> {
			String[] files = t.getFileList();
			if (files != null)
				Arrays.stream(files).forEach(this::writeLine);
			
			clearTask();
		});
		
		queueTask(task);
	}
	
	@CommandHandler("url")
	public void commandURL(String[] args) {
		if (args.length < 1) {
			writeLine(tr("fnamearg"));
			return;
		}
		
		String fname = Util.join(args, " ");
		if (Util.isFileNameInvalid(fname)) {
			writeLine(tr("nameerr"));
			return;
		}
		
		ClientTaskCheckFile task = new ClientTaskCheckFile(owner.uuid, fname);
		task.setFinishCallback((t) -> {
			int status = t.getStatus();
			if (status == 0) {
				writeLine(tr("urlcopied"));
				Minecraft.getInstance().keyboardHandler.setClipboard(t.getURL());
			} else if (status == Constants.GETF_STATUS_NOT_FOUND)
				writeLine(tr("notfound"));
			else
				writeLine(tr("error2", status));
			
			clearTask();
		});
		
		queueTask(task);
	}
	
	private void uploadCD(File newDir) {
		try {
			uploadDir = newDir.getCanonicalFile();
		} catch (IOException ex) {
			uploadDir = newDir;
		}
		
		uploadFiles.clear();
		File parent = uploadDir.getParentFile();
		
		if (parent != null && parent.exists()) {
			uploadFiles.add(parent);
			uploadFirstIsParent = true;
		} else
			uploadFirstIsParent = false;
		
		File[] children = uploadDir.listFiles();
		if (children != null) {
			Collator c = Collator.getInstance();
			c.setStrength(Collator.SECONDARY);
			c.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
			
			Arrays.stream(children).filter(f -> !f.isHidden() && (f.isDirectory() || f.isFile())).sorted((a, b) -> c.compare(a.getName(), b.getName())).forEach(uploadFiles::add);
		}
		
		uploadOffset = 0;
		uploadFilter = "";
		
		if (uploadWizard)
			selectedLine = 3;
	}
	
	private void updateUploadScreen() {
		lines.clear();
		
		lines.add(tr("upload.info"));
		lines.add(trimStringL(uploadDir.getPath()));
		lines.add("");
		
		for (int i = uploadOffset; i < uploadFiles.size() && lines.size() < MAX_LINES; i++) {
			if (i == 0 && uploadFirstIsParent)
				lines.add(tr("upload.parent"));
			else
				lines.add(trimStringR(uploadFiles.get(i).getName()));
		}
	}
	
	private void selectFile(int i) {
		int pos = 3 + i - uploadOffset;
		if (pos >= 3 && pos < MAX_LINES) {
			selectedLine = pos;
			return;
		}
		
		uploadOffset = i;
		if (uploadOffset + MAX_LINES - 3 > uploadFiles.size())
			uploadOffset = uploadFiles.size() - MAX_LINES + 3;
		
		updateUploadScreen();
		selectedLine = 3 + i - uploadOffset;
	}
	
	@CommandHandler("upload")
	public void commandUpload(String[] args) {
		if (!minecraft.player.getGameProfile().getId().equals(owner.uuid)) {
			writeLine(tr("errowner"));
			return;
		}
		
		if (args.length > 0) {
			File fle = new File(Util.join(args, " "));
			if (!fle.exists()) {
				writeLine(tr("notfound"));
				return;
			}
			
			if (fle.isDirectory())
				uploadCD(fle);
			else if (fle.isFile()) {
				startFileUpload(fle, false);
				return;
			} else {
				writeLine(tr("notfound"));
				return;
			}
		}
		
		uploadWizard = true;
		promptLocked = true;
		uploadOffset = 0;
		selectedLine = 3;
		updateUploadScreen();
	}
	
	@CommandHandler("rm")
	public void commandDelete(String[] args) {
		if (!minecraft.player.getGameProfile().getId().equals(owner.uuid)) {
			writeLine(tr("errowner"));
			return;
		}
		
		if (args.length < 1) {
			writeLine(tr("fnamearg"));
			return;
		}
		
		String fname = Util.join(args, " ");
		if (Util.isFileNameInvalid(fname)) {
			writeLine(tr("nameerr"));
			return;
		}
		
		ClientTaskDeleteFile task = new ClientTaskDeleteFile(fname);
		task.setFinishCallback((t) -> {
			int status = t.getStatus();
			if (status == 1)
				writeLine(tr("notfound"));
			else if (status != 0)
				writeLine(tr("error"));
			
			clearTask();
		});
		
		queueTask(task);
	}
	
	@CommandHandler("reconnect")
	public void commandReconnect() {
		Client.getInstance().stop();
		WDNetworkRegistry.sendToServer(Client.getInstance().beginConnection());
	}
	
	private void startFileUpload(File f, boolean quit) {
		if (quit)
			quitUploadWizard();
		
		if (Util.isFileNameInvalid(f.getName()) || f.getName().length() >= MAX_LINE_LEN - 3) {
			writeLine(tr("nameerr"));
			return;
		}
		
		ClientTaskUploadFile task;
		try {
			task = new ClientTaskUploadFile(f);
		} catch (IOException ex) {
			writeLine(tr("error"));
			ex.printStackTrace();
			return;
		}
		
		task.setProgressCallback((cur, total) -> {
			synchronized (GuiServer.this) {
				queryTime = System.currentTimeMillis();
			}
		});
		
		task.setFinishCallback(t -> {
			int status = t.getUploadStatus();
			if (status == 0)
				writeLine(tr("upload.done"));
			else if (status == Constants.FUPA_STATUS_FILE_EXISTS)
				writeLine(tr("upload.exists"));
			else if (status == Constants.FUPA_STATUS_EXCEEDS_QUOTA)
				writeLine(tr("upload.quota"));
			else
				writeLine(tr("error2", status));
			
			clearTask();
		});
		
		if (queueTask(task))
			writeLine(tr("upload.uploading"));
	}
	
	@Override
	public boolean isForBlock(BlockPos bp, BlockSide side) {
		return serverPos.equalsBlockPos(bp);
	}
	
	@Nullable
	@Override
	public String getWikiPageName() {
		return "Server";
	}
	
	int queueRead = 0;
	ArrayList<String> queue = new ArrayList<>();
	
	private void queueLine(String line) {
		final int maxl = uploadWizard ? MAX_LINES : (MAX_LINES - 1); //Cuz prompt is hidden
		if (lines.size() < maxl)
			writeLine(line);
		else if (queueRead > 1) {
			writeLine(line);
			queueRead -= 1;
		} else queue.add(line);
	}
}
