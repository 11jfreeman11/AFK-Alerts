package com.afkalerts;

/** State only: no client access, audio, or wall-clock dependency. */
final class AlertTracker
{
    private Integer previousPrayer;
    private long lastPrayerAlert;
    private boolean prayerPending;
    private Long aggressionDeadline;
    private boolean aggressionArmed;

    boolean prayer(int points, int threshold, int repeatSeconds, long now, boolean canSpeak)
    {
        if (points > threshold)
        {
            prayerPending = false;
            lastPrayerAlert = 0;
        }
        else if (previousPrayer != null && previousPrayer > threshold)
        {
            prayerPending = true;
        }
        boolean due = prayerPending || (lastPrayerAlert != 0 && repeatSeconds > 0
            && now - lastPrayerAlert >= repeatSeconds * 1000L);
        previousPrayer = points;
        if (points <= threshold && due && canSpeak)
        {
            prayerPending = false;
            lastPrayerAlert = now;
            return true;
        }
        return false;
    }

    boolean aggression(Long deadline, long now, boolean canSpeak)
    {
        if (deadline == null)
        {
            aggressionDeadline = null;
            aggressionArmed = false;
            return false;
        }
        if (!deadline.equals(aggressionDeadline))
        {
            aggressionDeadline = deadline;
            // An already expired timer on startup is not a newly observed expiry.
            aggressionArmed = deadline > now;
        }
        if (aggressionArmed && now >= deadline && canSpeak)
        {
            aggressionArmed = false;
            return true;
        }
        return false;
    }

    static final class Effect
    {
        private boolean active;
        private boolean pending;

        boolean update(boolean current, boolean canSpeak)
        {
            if (current)
            {
                pending = false;
            }
            else if (active)
            {
                pending = true;
            }
            active = current;
            if (pending && canSpeak)
            {
                pending = false;
                return true;
            }
            return false;
        }
    }
}
