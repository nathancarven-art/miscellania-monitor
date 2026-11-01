package com.miscellaniamonitor;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("miscellaniamonitor")
public interface MiscellaniaMonitorConfig extends Config
{
	@ConfigItem(
		keyName = "approvalWarningThreshold",
		name = "Approval warning",
		description = "Show approval as low at or below this percentage"
	)
	default int approvalWarningThreshold()
	{
		return 50;
	}
}
