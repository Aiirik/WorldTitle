package com.worldtitle;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.WorldChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "World Title"
)
public class WorldTitlePlugin extends Plugin
{
	private static final String WORLD_SUFFIX_PATTERN = " - W\\d+$";
	private static final int TITLE_RETRY_DELAY_MS = 250;
	private static final int TITLE_RETRY_COUNT = 12;

	@Inject
	private Client client;

	@Inject
	@Named("runelite.title")
	private String runeliteTitle;

	private Timer titleRetryTimer;
	private int titleRetriesRemaining;
	private Frame titleFrame;
	private final WindowAdapter titleFocusListener = new WindowAdapter()
	{
		@Override
		public void windowGainedFocus(WindowEvent event)
		{
			startTitleRetryTimer();
		}
	};

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
			startTitleRetryTimer();
			return;
		}

		updateTitle();
	}

	@Subscribe
	public void onWorldChanged(WorldChanged event)
	{
		startTitleRetryTimer();
	}

	private void updateTitle()
	{
		final int world = client.getWorld();
		final boolean showWorld = client.getGameState() == GameState.LOGGED_IN && world > 0;

		SwingUtilities.invokeLater(() ->
		{
			final Frame frame = findClientFrame();
			if (frame == null)
			{
				return;
			}

			addTitleFocusListener(frame);

			final String baseTitle = stripWorldSuffix(frame.getTitle());
			if (showWorld && !hasUsername(baseTitle))
			{
				return;
			}

			frame.setTitle(showWorld ? baseTitle + " - W" + world : baseTitle);
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

			frame.setTitle(stripWorldSuffix(frame.getTitle()));
		});
	}

	private void startTitleRetryTimer()
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
			titleRetryTimer.setInitialDelay(0);
			titleRetryTimer.start();
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
		for (Frame frame : Frame.getFrames())
		{
			if (frame.isDisplayable() && stripWorldSuffix(frame.getTitle()).startsWith(runeliteTitle))
			{
				return frame;
			}
		}

		return null;
	}

	private static String stripWorldSuffix(String title)
	{
		return title.replaceFirst(WORLD_SUFFIX_PATTERN, "");
	}

	private static boolean hasUsername(String title)
	{
		return title.contains(" - ");
	}
}
