package com.afkalerts;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class AfkAlertsPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(AfkAlertsPlugin.class);
        RuneLite.main(args);
    }
}
