package com.worldtitle;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import com.google.inject.Provides;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.WorldChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.events.WorldsFetch;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldRegion;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.http.api.worlds.WorldType;

@PluginDescriptor(
	name = "World Title"
)
public class WorldTitlePlugin extends Plugin
{
	private static final String WORLD_SUFFIX_PATTERN = " - (?:W\\d+|World \\d+|\\[\\d+\\])$";
	private static final int TITLE_RETRY_DELAY_MS = 250;
	private static final int TITLE_RETRY_COUNT = 8;
	private static final int METADATA_RETRY_COUNT = 40;
	// RuneLite can rewrite its title shortly after a hop; wait before applying ours to avoid visible flicker.
	private static final int WORLD_HOP_TITLE_DELAY_MS = 1000;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	@Named("runelite.title")
	private String runeliteTitle;

	@Inject
	private WorldTitleConfig config;

	@Inject
	private RuneLiteConfig runeLiteConfig;

	@Inject
	private WorldService worldService;

	private Timer titleRetryTimer;
	private int titleRetriesRemaining;
	private Timer metadataRetryTimer;
	private int metadataRetriesRemaining;
	private Frame titleFrame;
	private String lastWorldSuffix;
	private volatile WorldResult worldResult;
	private ExecutorService metadataExecutor;
	private boolean updatingTitle;
	private final WindowAdapter titleFocusListener = new WindowAdapter()
	{
		@Override
		public void windowGainedFocus(WindowEvent event)
		{
			startTitleRetryTimerIfNeeded();
		}
	};
	private final PropertyChangeListener titleChangeListener = this::onFrameTitleChanged;

	@Provides
	WorldTitleConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WorldTitleConfig.class);
	}

	@Override
	protected void startUp()
	{
		metadataExecutor = Executors.newSingleThreadExecutor();
		worldService.refresh();
		loadWorldsAsync();
		updateTitleWhenReady();
	}

	@Override
	protected void shutDown()
	{
		stopTitleRetryTimer();
		stopMetadataRetryTimer();
		if (metadataExecutor != null)
		{
			metadataExecutor.shutdownNow();
			metadataExecutor = null;
		}
		removeTitleFocusListener();
		resetTitle();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			updateTitleWhenReady();
			return;
		}

		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			updateTitle();
		}
	}

	@Subscribe
	public void onWorldChanged(WorldChanged event)
	{
		startTitleRetryTimer(WORLD_HOP_TITLE_DELAY_MS);
	}

	@Subscribe
	public void onWorldsFetch(WorldsFetch event)
	{
		worldResult = event.getWorldResult();
		stopMetadataRetryTimer();
		updateTitle();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if ("world-title".equals(event.getGroup()))
		{
			worldService.refresh();
			updateTitle();
			return;
		}

		if ("runelite".equals(event.getGroup())
			&& "usernameInTitle".equals(event.getKey())
			&& client.getGameState() == GameState.LOGGED_IN)
		{
			startTitleRetryTimer(TITLE_RETRY_DELAY_MS);
		}
	}

	private void updateTitle()
	{
		final int world = client.getWorld();
		final boolean showWorld = client.getGameState() == GameState.LOGGED_IN && world > 0;
		if (showWorld && needsWorldMetadata() && findWorld(world) == null)
		{
			startMetadataRetryTimer();
		}

		final String worldSuffix = formatWorldSuffix(world);
		final String baseTitle = getBaseTitle();

		if (showWorld && baseTitle == null)
		{
			return;
		}

		runOnSwingThread(() -> updateTitle(baseTitle, worldSuffix, showWorld));
	}

	private void updateTitle(String baseTitle, String worldSuffix, boolean showWorld)
	{
		final Frame frame = findClientFrame();
		if (frame == null)
		{
			return;
		}

		addTitleFocusListener(frame);

		final String title = showWorld ? baseTitle + worldSuffix : stripWorldSuffix(frame.getTitle());
		if (setTitleIfChanged(frame, title) || showWorld)
		{
			lastWorldSuffix = showWorld ? worldSuffix : null;
		}
		else
		{
			lastWorldSuffix = null;
		}
	}

	private void updateTitleWhenReady()
	{
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() != GameState.LOGGED_IN)
			{
				return true;
			}

			if (getBaseTitle() == null)
			{
				return false;
			}

			updateTitle();
			return true;
		});
	}

	private void resetTitle()
	{
		SwingUtilities.invokeLater(() ->
		{
			final Frame frame = findClientFrame();
			if (frame == null)
			{
				return;
			}

			setTitleIfChanged(frame, stripWorldSuffix(frame.getTitle()));
			lastWorldSuffix = null;
		});
	}

	private void startTitleRetryTimer(int initialDelay)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (titleRetryTimer != null && initialDelay > 0)
			{
				return;
			}

			stopTitleRetryTimer();
			titleRetriesRemaining = TITLE_RETRY_COUNT;
			titleRetryTimer = new Timer(TITLE_RETRY_DELAY_MS, event ->
			{
				updateTitle();
				if (--titleRetriesRemaining <= 0)
				{
					stopTitleRetryTimer();
				}
			});
			titleRetryTimer.setInitialDelay(initialDelay);
			titleRetryTimer.start();
		});
	}

	private void startTitleRetryTimerIfNeeded()
	{
		SwingUtilities.invokeLater(() ->
		{
			if (titleRetryTimer != null || titleHasExpectedWorldSuffix())
			{
				return;
			}

			startTitleRetryTimer(0);
		});
	}

	private void stopTitleRetryTimer()
	{
		if (titleRetryTimer != null)
		{
			titleRetryTimer.stop();
			titleRetryTimer = null;
		}
	}

	private void startMetadataRetryTimer()
	{
		SwingUtilities.invokeLater(() ->
		{
			if (metadataRetryTimer != null)
			{
				return;
			}

			metadataRetriesRemaining = METADATA_RETRY_COUNT;
			worldService.refresh();
			loadWorldsAsync();
			metadataRetryTimer = new Timer(TITLE_RETRY_DELAY_MS, event ->
			{
				updateTitle();
				if (--metadataRetriesRemaining <= 0
					|| !needsWorldMetadata()
					|| findWorld(client.getWorld()) != null)
				{
					stopMetadataRetryTimer();
				}
			});
			metadataRetryTimer.setInitialDelay(TITLE_RETRY_DELAY_MS);
			metadataRetryTimer.start();
		});
	}

	private void stopMetadataRetryTimer()
	{
		if (metadataRetryTimer != null)
		{
			metadataRetryTimer.stop();
			metadataRetryTimer = null;
		}
	}

	private void loadWorldsAsync()
	{
		if (metadataExecutor == null || metadataExecutor.isShutdown())
		{
			return;
		}

		try
		{
			metadataExecutor.submit(() ->
			{
				final WorldResult worlds = worldService.getWorlds();
				if (worlds != null)
				{
					worldResult = worlds;
					updateTitle();
				}
			});
		}
		catch (RejectedExecutionException ignored)
		{
			// Plugin shutdown can race with a pending metadata refresh.
		}
	}

	private void addTitleFocusListener(Frame frame)
	{
		if (titleFrame == frame)
		{
			return;
		}

		removeTitleFocusListener();
		titleFrame = frame;
		titleFrame.addWindowFocusListener(titleFocusListener);
		titleFrame.addPropertyChangeListener("title", titleChangeListener);
	}

	private void removeTitleFocusListener()
	{
		if (titleFrame != null)
		{
			titleFrame.removeWindowFocusListener(titleFocusListener);
			titleFrame.removePropertyChangeListener("title", titleChangeListener);
			titleFrame = null;
		}
	}

	private void onFrameTitleChanged(PropertyChangeEvent event)
	{
		if (!updatingTitle
			&& client.getGameState() == GameState.LOGGED_IN
			&& client.getWorld() > 0)
		{
			final String baseTitle = stripWorldSuffix(String.valueOf(event.getNewValue()));
			final String worldSuffix = formatWorldSuffix(client.getWorld());
			if (isFrameTitleReady(baseTitle))
			{
				updateTitle(baseTitle, worldSuffix, true);
			}
		}
	}

	private static void runOnSwingThread(Runnable runnable)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			runnable.run();
			return;
		}

		SwingUtilities.invokeLater(runnable);
	}

	private Frame findClientFrame()
	{
		if (isClientFrame(titleFrame))
		{
			return titleFrame;
		}

		for (Frame frame : Frame.getFrames())
		{
			if (isClientFrame(frame))
			{
				return frame;
			}
		}

		return null;
	}

	private boolean isClientFrame(Frame frame)
	{
		if (frame == null || !frame.isDisplayable())
		{
			return false;
		}

		final String baseTitle = stripWorldSuffix(frame.getTitle());
		return baseTitle.equals(runeliteTitle) || baseTitle.startsWith(runeliteTitle + " - ");
	}

	private boolean titleHasExpectedWorldSuffix()
	{
		if (client.getGameState() != GameState.LOGGED_IN || client.getWorld() <= 0)
		{
			return true;
		}

		final Frame frame = findClientFrame();
		return frame != null && frame.getTitle().endsWith(formatWorldSuffix(client.getWorld()));
	}

	private boolean setTitleIfChanged(Frame frame, String title)
	{
		if (!title.equals(frame.getTitle()))
		{
			updatingTitle = true;
			try
			{
				frame.setTitle(title);
			}
			finally
			{
				updatingTitle = false;
			}
			return true;
		}

		return false;
	}

	private String stripWorldSuffix(String title)
	{
		if (lastWorldSuffix != null && title.endsWith(lastWorldSuffix))
		{
			return title.substring(0, title.length() - lastWorldSuffix.length());
		}

		return title.replaceFirst(WORLD_SUFFIX_PATTERN, "");
	}

	private String formatWorldSuffix(int world)
	{
		final String separator = config.separator().separator();
		final StringBuilder details = new StringBuilder(config.worldFormat().format(world));
		final World worldInfo = findWorld(world);
		final String activity = getWorldActivity(worldInfo);

		if (config.showWorldActivity() && activity != null)
		{
			details.append(separator).append(activity);
		}

		if (config.showWorldRegion() && worldInfo != null)
		{
			details.append(" (").append(formatWorldRegion(worldInfo.getRegion())).append(')');
		}

		if (config.showPlayerCount() && worldInfo != null && worldInfo.getPlayers() >= 0)
		{
			details.append(separator).append(formatPlayerCount(worldInfo.getPlayers()));
		}

		if (config.showMembershipType() && worldInfo != null)
		{
			final String membership = config.membershipStyle()
				.format(worldInfo.getTypes().contains(WorldType.MEMBERS));
			if (membership.startsWith("("))
			{
				details.append(' ').append(membership);
			}
			else
			{
				details.append(separator).append(membership);
			}
		}

		return " - " + details;
	}

	private World findWorld(int world)
	{
		return worldResult == null ? null : worldResult.findWorld(world);
	}

	private boolean needsWorldMetadata()
	{
		return config.showWorldActivity()
			|| config.showWorldRegion()
			|| config.showPlayerCount()
			|| config.showMembershipType();
	}

	private static String getWorldActivity(World world)
	{
		if (world == null)
		{
			return null;
		}

		final String activity = world.getActivity();
		if (activity == null || "-".equals(activity))
		{
			return null;
		}

		return activity.trim();
	}

	private static String formatPlayerCount(int players)
	{
		return players + (players == 1 ? " player" : " players");
	}

	private static String formatWorldRegion(WorldRegion region)
	{
		if (region == WorldRegion.UNITED_KINGDOM)
		{
			return "UK";
		}

		return region.getAlpha2();
	}

	private String getBaseTitle()
	{
		if (!runeLiteConfig.usernameInTitle())
		{
			return runeliteTitle;
		}

		final Player player = client.getLocalPlayer();
		if (player == null || player.getName() == null || player.getName().isEmpty())
		{
			return null;
		}

		return runeliteTitle + " - " + player.getName();
	}

	private boolean isFrameTitleReady(String title)
	{
		if (!runeLiteConfig.usernameInTitle())
		{
			return title.equals(runeliteTitle);
		}

		return title.startsWith(runeliteTitle + " - ") && title.length() > runeliteTitle.length() + 3;
	}
}
