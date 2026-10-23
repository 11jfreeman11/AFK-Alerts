package com.afkalerts;

import org.junit.Test;
import static org.junit.Assert.*;

public class AlertTrackerTest
{
    @Test public void prayerIsInclusiveAndRearmsOnlyAfterRestoringAboveThreshold()
    {
        AlertTracker tracker = new AlertTracker();
        assertFalse(tracker.prayer(20, 10, 0, 1000, true));
        assertTrue(tracker.prayer(10, 10, 0, 2000, true));
        assertFalse(tracker.prayer(9, 10, 0, 3000, true));
        assertFalse(tracker.prayer(0, 10, 0, 4000, true));
        assertFalse(tracker.prayer(11, 10, 0, 5000, true));
        assertTrue(tracker.prayer(5, 10, 0, 6000, true));
    }

    @Test public void zeroThresholdWorksAndStartupDoesNotPretendToObserveCrossing()
    {
        AlertTracker tracker = new AlertTracker();
        assertFalse(tracker.prayer(0, 0, 0, 1000, true));
        assertFalse(tracker.prayer(1, 0, 0, 2000, true));
        assertTrue(tracker.prayer(0, 0, 0, 3000, true));
    }

    @Test public void remindersStopAfterRestoringPrayer()
    {
        AlertTracker tracker = new AlertTracker();
        tracker.prayer(20, 10, 10, 1000, true);
        assertTrue(tracker.prayer(10, 10, 10, 2000, true));
        assertFalse(tracker.prayer(9, 10, 10, 11999, true));
        assertTrue(tracker.prayer(9, 10, 10, 12000, true));
        assertFalse(tracker.prayer(20, 10, 10, 30000, true));
    }

    @Test public void backgroundOnlyDefersWithoutLosingOrRepeatingAlert()
    {
        AlertTracker tracker = new AlertTracker();
        tracker.prayer(20, 10, 0, 1000, false);
        assertFalse(tracker.prayer(10, 10, 0, 2000, false));
        assertTrue(tracker.prayer(8, 10, 0, 3000, true));
        assertFalse(tracker.prayer(8, 10, 0, 4000, true));
    }

    @Test public void restorationCancelsDeferredPrayer()
    {
        AlertTracker tracker = new AlertTracker();
        tracker.prayer(20, 10, 0, 1000, false);
        tracker.prayer(10, 10, 0, 2000, false);
        assertFalse(tracker.prayer(20, 10, 0, 3000, true));
    }

    @Test public void aggressionUsesObservedDeadlineAndFiresOnce()
    {
        AlertTracker tracker = new AlertTracker();
        assertFalse(tracker.aggression(2000L, 1000, true));
        assertTrue(tracker.aggression(2000L, 2000, true));
        assertFalse(tracker.aggression(2000L, 3000, true));
        assertFalse(tracker.aggression(5000L, 4000, true));
        assertTrue(tracker.aggression(5000L, 5000, true));
    }

    @Test public void aggressionRestartAndMissingTimerDoNotGiveFalseExpiry()
    {
        AlertTracker tracker = new AlertTracker();
        assertFalse(tracker.aggression(1000L, 2000, true));
        assertFalse(tracker.aggression(4000L, 3000, true));
        assertFalse(tracker.aggression(null, 4000, true));
        assertFalse(tracker.aggression(4000L, 5000, true));
    }

    @Test public void aggressionResetReplacesOldDeadline()
    {
        AlertTracker tracker = new AlertTracker();
        tracker.aggression(2000L, 1000, true);
        assertFalse(tracker.aggression(4000L, 2000, true));
        assertTrue(tracker.aggression(4000L, 4000, true));
    }

    @Test public void aggressionCanDeferUntilUnfocused()
    {
        AlertTracker tracker = new AlertTracker();
        tracker.aggression(2000L, 1000, false);
        assertFalse(tracker.aggression(2000L, 2000, false));
        assertTrue(tracker.aggression(2000L, 3000, true));
    }

    @Test public void buffRequiresObservedActiveTimerAndFiresOnce()
    {
        AlertTracker.Effect effect = new AlertTracker.Effect();
        assertFalse(effect.update(false, true));
        assertFalse(effect.update(true, true));
        assertFalse(effect.update(true, true));
        assertTrue(effect.update(false, true));
        assertFalse(effect.update(false, true));
        effect.update(true, true);
        assertTrue(effect.update(false, true));
    }

    @Test public void RedosingCancelsDeferredBuffExpiry()
    {
        AlertTracker.Effect effect = new AlertTracker.Effect();
        effect.update(true, true);
        assertFalse(effect.update(false, false));
        assertFalse(effect.update(true, true));
        assertTrue(effect.update(false, true));
    }
}
