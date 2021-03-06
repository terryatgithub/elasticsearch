/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.watcher.trigger.schedule;

import org.elasticsearch.xpack.core.scheduler.Cron;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

public abstract class CronnableSchedule implements Schedule {

    private static final Comparator<Cron> CRON_COMPARATOR = Comparator.comparing(Cron::expression);

    protected final Cron[] crons;

    CronnableSchedule(String... expressions) {
        this(crons(expressions));
    }

    private CronnableSchedule(Cron... crons) {
        assert crons.length > 0;
        this.crons = crons;
        Arrays.sort(crons, CRON_COMPARATOR);
    }

    @Override
    public long nextScheduledTimeAfter(long startTime, long time) {
        assert time >= startTime;
        long nextTime = Long.MAX_VALUE;
        for (Cron cron : crons) {
            long nextValidTimeAfter = cron.getNextValidTimeAfter(time);

            boolean previousCronExpired = nextTime == -1;
            boolean currentCronValid = nextValidTimeAfter > -1;
            if (previousCronExpired && currentCronValid) {
                nextTime = nextValidTimeAfter;
            } else {
                nextTime = Math.min(nextTime, nextValidTimeAfter);
            }
        }
        return nextTime;
    }

    public Cron[] crons() {
        return crons;
    }

    @Override
    public int hashCode() {
        return Objects.hash((Object[]) crons);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final CronnableSchedule other = (CronnableSchedule) obj;
        return Objects.deepEquals(this.crons, other.crons);
    }

    static Cron[] crons(String... expressions) {
        Cron[] crons = new Cron[expressions.length];
        for (int i = 0; i < crons.length; i++) {
            crons[i] = new Cron(expressions[i]);
        }
        return crons;
    }
}
