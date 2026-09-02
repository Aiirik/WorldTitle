package com.worldtitle;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import com.google.inject.Provides;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.WorldChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "World Title"
)
public class WorldTitlePlugin extends Plugin
{
	private static final String WORLD_SUFFIX_PATTERN = " - (?:W\\d+|World \\d+|\\[\\d+\\])$";
	private static final int TITLE_RETRY_DELAY_MS = 250;
	private static final int TITLE_RETRY_COUNT = 8;
	// RuneLite can rewrite its title shortly after a hop; wait before applying ours to avoid visible flicker.
	private static final int WORLD_HOP_TITLE_DELAY_MS = 1000;

	@Inject
	private Client client;

	@Inject
	@Named("runelite.title")
	private String runeliteTitle;

	@Inject
	private WorldTitleConfig config;

	private Timer titleRetryTimer;
	private int titleRetriesRemaining;
	private Frame titleFrame;
	private String lastWorldSuffix;
	private final WindowAdapter titleFocusListener = new WindowAdapter()
	{
		@Override
		public void windowGainedFocus(WindowEvent event)
		{
			startTitleRetryTimerIfNeeded();
		}
	};

	@Provides
	WorldTitleConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WorldTitleConfig.class);
	}

	@Override
	protected void startUp()
	{
		updateTitle();
	}

	@Override
	protected void shutDown()
	{
		stopTitleRetryTimer();
		removeTitleFocusListener();
		resetTitle();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			startTitleRetryTimer(WORLD_HOP_TITLE_DELAY_MS);
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
	public void onConfigChanged(ConfigChanged event)
	{
		if ("world-title".equals(event.getGroup()))
		{
			updateTitle();
		}
	}

	private void updateTitle()
	{
		final int world = client.getWorld();
		final boolean showWorld = client.getGameState() == GameState.LOGGED_IN && world > 0;
		final String worldSuffix = formatWorldSuffix(world);

		SwingUtilities.invokeLater(() ->
		{
			final Frame frame = findClientFrame();
			if (frame == null)
			{
				return;
			}

			addTitleFocusListener(frame);

			final String baseTitle = stripWorldSuffix(frame.getTitle());
			if (showWorld && !isLoggedInTitleReady(baseTitle))
			{
				return;
			}

			final String title = showWorld ? baseTitle + worldSuffix : baseTitle;
			if (setTitleIfChanged(frame, title) || showWorld)
			{
				lastWorldSuffix = showWorld ? worldSuffix : null;
			}
			else
			{
				lastWorldSuffix = null;
			}
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

	private void addTitleFocusListener(Frame frame)
	{
		if (titleFrame == frame)
		{
			return;
		}

		removeTitleFocusListener();
		titleFrame = frame;
		titleFrame.addWindowFocusListener(titleFocusListener);
	}

	private void removeTitleFocusListener()
	{
		if (titleFrame != null)
		{
			titleFrame.removeWindowFocusListener(titleFocusListener);
			titleFrame = null;
		}
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
			frame.setTitle(title);
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
		return " - " + config.worldFormat().format(world);
	}

	private boolean isLoggedInTitleReady(String title)
	{
		return title.startsWith(runeliteTitle + " - ") && title.length() > runeliteTitle.length() + 3;
	}
}
