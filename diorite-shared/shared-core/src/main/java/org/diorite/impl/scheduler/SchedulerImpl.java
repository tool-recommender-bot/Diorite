/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017. Diorite (by Bartłomiej Mazur (aka GotoFinal))
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.diorite.impl.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import org.diorite.SharedAPI;
import org.diorite.plugin.Plugin;
import org.diorite.scheduler.DioriteTask;
import org.diorite.scheduler.DioriteWorker;
import org.diorite.scheduler.Scheduler;
import org.diorite.scheduler.Synchronizable;
import org.diorite.scheduler.TaskBuilder;

@SuppressWarnings("ObjectEquality")
public class SchedulerImpl extends Scheduler
{
    // TODO: This is only test code, improve it later.
    private final GameTimeScheduler gts;
    private final RealTimeScheduler rts;

    public SchedulerImpl(Plugin internalPlugin)
    {
        super(internalPlugin);
        this.gts = new GameTimeScheduler(internalPlugin);
        this.rts = new RealTimeScheduler(internalPlugin);
    }

    @Override
    protected DioriteTask runTask(TaskBuilder builder, long startDelay) throws IllegalArgumentException
    {
        if (builder.isRealTime())
        {
            return this.rts.runTask(builder, startDelay);
        }
        return this.gts.runTask(builder, startDelay);
    }

    @Override
    public <T> Future<T> callSyncMethod(Plugin plugin, Callable<T> task)
    {
        return this.gts.callSyncMethod(plugin, task);
    }

    @Override
    public <T> Future<T> callSyncMethod(Plugin plugin, Callable<T> task, Synchronizable sync)
    {
        return this.gts.callSyncMethod(plugin, task, sync);
    }

    @Override
    public void cancelTask(int taskId)
    {
        this.gts.cancelTask(taskId);
        this.rts.cancelTask(taskId);
    }

    @Override
    public void cancelTasks(Plugin plugin)
    {
        this.gts.cancelTasks(plugin);
        this.rts.cancelTasks(plugin);
    }

    @Override
    public void cancelAllTasks()
    {
        this.gts.cancelAllTasks();
        this.rts.cancelAllTasks();
    }

    @Override
    public boolean isCurrentlyRunning(int taskId)
    {
        return this.gts.isCurrentlyRunning(taskId) || this.rts.isCurrentlyRunning(taskId);
    }

    @Override
    public boolean isQueued(int taskId)
    {
        return this.gts.isQueued(taskId) || this.rts.isQueued(taskId);
    }

    @Override
    public List<? extends DioriteWorker> getActiveWorkers()
    {
        List<DioriteWorker> workers = new ArrayList<>(this.gts.getActiveWorkers());
        workers.addAll(this.rts.getActiveWorkers());
        return workers;
    }

    @Override
    public List<? extends DioriteTask> getPendingTasks()
    {
        List<DioriteTask> workers = new ArrayList<>(this.gts.getPendingTasks());
        workers.addAll(this.rts.getPendingTasks());
        return workers;
    }

    protected DioriteTask handle(DioriteTaskImpl task, long delay)
    {
        throw new UnsupportedOperationException("This method should be never used directly");
    }

    public void tick(long currentTick, boolean withAsync)
    {
        this.gts.tick(currentTick, withAsync);
        this.rts.tick(currentTick, withAsync);
    }

    @SuppressWarnings("ClassHasNoToStringMethod")
    private abstract static class BaseScheduler extends Scheduler
    {
        protected final    AtomicInteger                               ids     = new AtomicInteger(1);
        protected volatile DioriteTaskImpl                             head    = new DioriteTaskImpl("{HEAD}", this.internalPlugin);
        protected final    AtomicReference<DioriteTaskImpl>            tail    = new AtomicReference<>(this.head);
        protected final    PriorityQueue<DioriteTaskImpl>              pending = new PriorityQueue<>(10, (o1, o2) -> (int) (o1.getNextRun() - o2.getNextRun()));
        protected final    List<DioriteTaskImpl>                       temp    = new ArrayList<>(100);
        protected final    ConcurrentHashMap<Integer, DioriteTaskImpl> runners = new ConcurrentHashMap<>(200);

        protected BaseScheduler(Plugin internalPlugin) {super(internalPlugin);}

        protected abstract DioriteTask handle(DioriteTaskImpl task, long delay);

        protected abstract void tick(long currentTick, boolean withAsync);

        protected int nextId()
        {
            return this.ids.incrementAndGet();
        }

        @Override
        protected DioriteTask runTask(TaskBuilder builder, long startDelay) throws IllegalArgumentException
        {
            Plugin dioritePlugin = builder.getPlugin();
            Runnable task = builder.getRunnable();
            validate(dioritePlugin, task);
            if (builder.isSingle() && (startDelay != 0))
            {
                throw new IllegalArgumentException("Single task can't have additional delay.");
            }
            boolean isSingle = builder.isSingle();
            long delay = isSingle ? builder.getDelay() : startDelay;
            long period = isSingle ? - 1 : builder.getDelay();
            if (delay < 0)
            {
                delay = 0;
            }
            if (period == 0)
            {
                period = 1;
            }
            else if (period < - 1)
            {
                period = - 1;
            }
            if (builder.isAsync())
            {
                return this.handle(new DioriteAsyncTask(builder.getName(), this.runners, dioritePlugin, task, this.nextId(), period), delay);

            }
            return this.handle(
                    new DioriteTaskImpl(builder.getName(), dioritePlugin, task, builder.getSynchronizable(), builder.isSafeMode(), this.nextId(), period),
                    delay);
        }

        @Override
        public <T> Future<T> callSyncMethod(Plugin plugin, Callable<T> task)
        {
            return this.callSyncMethod(plugin, task, SharedAPI.getSharedAPI().getMainSynchronizable());
        }

        @Override
        public <T> Future<T> callSyncMethod(Plugin plugin, Callable<T> task, Synchronizable sync)
        {
            Validate.notNull(sync, "Can't synchronize to null object");
            validate(plugin, task);
            DioriteFuture<T> future = new DioriteFuture<>(task, plugin, sync, this.nextId());
            this.handle(future, 0);
            return future;
        }

        @Override
        public void cancelTask(int taskId)
        {
            if (taskId <= 0)
            {
                return;
            }
            DioriteTaskImpl task = this.runners.get(taskId);
            if (task != null)
            {
                task.forceCancel();
            }
            task = new DioriteTaskImpl(this.internalPlugin, new Runnable()
            {
                @Override
                public void run()
                {
                    if (! this.check(BaseScheduler.this.temp))
                    {
                        this.check(BaseScheduler.this.pending);
                    }
                }

                private boolean check(Iterable<DioriteTaskImpl> collection)
                {
                    Iterator<DioriteTaskImpl> tasks = collection.iterator();
                    while (tasks.hasNext())
                    {
                        DioriteTaskImpl task = tasks.next();
                        if (task.getTaskId() == taskId)
                        {
                            task.forceCancel();
                            tasks.remove();
                            if (! task.isAsync())
                            {
                                BaseScheduler.this.runners.remove(taskId);
                            }
                            return true;
                        }
                    }
                    return false;
                }
            });
            this.handle(task, 0);
            for (DioriteTaskImpl taskPending = this.head.getNext(); taskPending != null; taskPending = taskPending.getNext())
            {
                if (taskPending == task)
                {
                    return;
                }
                if (taskPending.getTaskId() == taskId)
                {
                    taskPending.forceCancel();
                }
            }
        }

        @Override
        public void cancelTasks(Plugin plugin)
        {
            Validate.notNull(plugin, "Cannot cancel tasks of null plugin");
            DioriteTaskImpl task = new DioriteTaskImpl(this.internalPlugin, new Runnable()
            {
                @Override
                public void run()
                {
                    this.check(BaseScheduler.this.pending);
                    this.check(BaseScheduler.this.temp);
                }

                void check(Iterable<DioriteTaskImpl> collection)
                {
                    Iterator<DioriteTaskImpl> tasks = collection.iterator();
                    while (tasks.hasNext())
                    {
                        DioriteTaskImpl task = tasks.next();
                        if (task.getOwner().equals(plugin))
                        {
                            task.forceCancel();
                            tasks.remove();
                            if (! task.isAsync())
                            {
                                BaseScheduler.this.runners.remove(task.getTaskId());
                            }
                        }
                    }
                }
            });
            this.handle(task, 0);
            for (DioriteTaskImpl taskPending = this.head.getNext(); taskPending != null; taskPending = taskPending.getNext())
            {
                if (taskPending == task)
                {
                    return;
                }
                if ((taskPending.getTaskId() != - 1) && taskPending.getOwner().equals(plugin))
                {
                    taskPending.forceCancel();
                }
            }
            this.runners.values().stream().filter(runner -> runner.getOwner().equals(plugin)).forEach(DioriteTaskImpl::forceCancel);
        }

        @Override
        public void cancelAllTasks()
        {
            DioriteTaskImpl task = new DioriteTaskImpl(this.internalPlugin, () -> {
                Iterator<DioriteTaskImpl> it = BaseScheduler.this.runners.values().iterator();
                while (it.hasNext())
                {
                    DioriteTaskImpl task1 = it.next();
                    task1.forceCancel();
                    if (! task1.isAsync())
                    {
                        it.remove();
                    }
                }
                BaseScheduler.this.pending.clear();
                BaseScheduler.this.temp.clear();
            });
            this.handle(task, 0);
            for (DioriteTaskImpl taskPending = this.head.getNext(); taskPending != null; taskPending = taskPending.getNext())
            {
                if (taskPending == task)
                {
                    break;
                }
                taskPending.forceCancel();
            }
            this.runners.values().forEach(DioriteTaskImpl::forceCancel);
        }

        @Override
        public boolean isCurrentlyRunning(int taskId)
        {
            DioriteTaskImpl task = this.runners.get(taskId);
            if ((task == null) || ! task.isAsync())
            {
                return false;
            }
            DioriteAsyncTask asyncTask = (DioriteAsyncTask) task;
            synchronized (asyncTask.getWorkers())
            {
                return asyncTask.getWorkers().isEmpty();
            }
        }

        @Override
        public boolean isQueued(int taskId)
        {
            if (taskId <= 0)
            {
                return false;
            }
            for (DioriteTaskImpl task = this.head.getNext(); task != null; task = task.getNext())
            {
                if (task.getTaskId() == taskId)
                {
                    return task.getPeriod() >= - 1; // The task will run
                }
            }
            DioriteTaskImpl task = this.runners.get(taskId);
            return (task != null) && (task.getPeriod() >= - 1);
        }

        @Override
        public List<DioriteWorker> getActiveWorkers()
        {
            List<DioriteWorker> workers = new ArrayList<>(50);
            for (DioriteTaskImpl taskObj : this.runners.values())
            {
                if (! taskObj.isAsync())
                {
                    continue;
                }
                DioriteAsyncTask task = (DioriteAsyncTask) taskObj;
                synchronized (task.getWorkers())
                {
                    workers.addAll(task.getWorkers());
                }
            }
            return workers;
        }

        @Override
        public List<? extends DioriteTask> getPendingTasks()
        {
            Collection<DioriteTaskImpl> truePending = new ArrayList<>(200);
            for (DioriteTaskImpl task = this.head.getNext(); task != null; task = task.getNext())
            {
                if (task.getTaskId() != DioriteTaskImpl.STATE_CANCEL)
                {
                    truePending.add(task);
                }
            }
            List<DioriteTaskImpl> pending = this.runners.values().stream().filter(task -> task.getPeriod() >= - 1).collect(Collectors.toList());
            truePending.stream().filter(task -> (task.getPeriod() >= - 1) && ! pending.contains(task)).forEach(pending::add);
            return pending;
        }

        protected void parsePending()
        {
            DioriteTaskImpl head = this.head;
            DioriteTaskImpl task = head.getNext();
            DioriteTaskImpl lastTask = head;
            for (; task != null; task = (lastTask = task).getNext())
            {
                if (task.getTaskId() == DioriteTaskImpl.STATE_CANCEL)
                {
                    task.run();
                }
                else if (task.getPeriod() >= - 1)
                {
                    this.pending.add(task);
                    this.runners.put(task.getTaskId(), task);
                }
            }
            // We split this because of the way things are ordered for all of the async calls in SchedulerImpl
            // (it prevents race-conditions)
            for (task = head; task != lastTask; task = head)
            {
                if (task == null)
                {
                    break;
                }
                head = task.getNext();
                task.setNext(null);
            }
            this.head = lastTask;
        }

        protected void addTask(DioriteTaskImpl task)
        {
            AtomicReference<DioriteTaskImpl> tail = this.tail;
            DioriteTaskImpl tailTask = tail.get();
            while (! tail.compareAndSet(tailTask, task))
            {
                tailTask = tail.get();
            }
            tailTask.setNext(task);
        }

        protected boolean isReady(long time, boolean withAsync)
        {
            if (! this.pending.isEmpty())
            {
                DioriteTaskImpl task = this.pending.peek();
                boolean async = task.isAsync();
                if (task.getNextRun() <= time)
                {
                    if (async && withAsync) // async task are executed on server tick, don't execute them when ticking other parts of server.
                    {
                        return true;
                    }
                    Synchronizable sync = task.getSynchronizable();
                    return ((sync != null) && (sync.getLastTickThread() == Thread.currentThread()));
                }
            }
            return false;
        }
    }

    private static final class GameTimeScheduler extends BaseScheduler
    {
        private volatile long     currentTick = - 1;
        private final    Executor executor    = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("{DioriteTask-%1$d}").build());

        private GameTimeScheduler(Plugin internalPlugin)
        {
            super(internalPlugin);
        }

        @Override
        public void tick(long currentTick, boolean withAsync)
        {
            this.currentTick = currentTick;
            List<DioriteTaskImpl> temp = this.temp;
            this.parsePending();
            while (this.isReady(currentTick, withAsync))
            {
                DioriteTaskImpl task = this.pending.remove();
                if (task.getPeriod() < - 1)
                {
                    if (! task.isAsync())
                    {
                        this.runners.remove(task.getTaskId(), task);
                    }
                    this.parsePending();
                    continue;
                }
                if (! task.isAsync())
                {
                    try
                    {
                        task.run();
                    }
                    catch (Throwable throwable)
                    {
                        // TODO: add better logging
                        System.err.println(String.format("Task #%s for %s generated an exception", task.getTaskId(), task.getOwner().getName()));
                        throwable.printStackTrace();
                    }
                    this.parsePending();
                }
                else
                {
                    this.executor.execute(task);
                }
                long period = task.getPeriod();
                if (period > 0)
                {
                    task.setNextRun(currentTick + period);
                    temp.add(task);
                }
                else if (! task.isAsync())
                {
                    this.runners.remove(task.getTaskId());
                }
            }
            this.pending.addAll(temp);
            temp.clear();
        }

        @Override
        protected DioriteTask handle(DioriteTaskImpl task, long delay)
        {
            task.setNextRun(this.currentTick + delay);
            this.addTask(task);
            return task;
        }

        @Override
        public String toString()
        {
            return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append("ids", this.ids).append("currentTick", this.currentTick).toString();
        }
    }

    private static final class RealTimeScheduler extends BaseScheduler
    {
        private volatile long                     time     = System.currentTimeMillis();
        private final    ScheduledExecutorService executor =
                Executors.newScheduledThreadPool(2, new ThreadFactoryBuilder().setNameFormat("{DioriteTaskRT-%1$d}").build());

        private RealTimeScheduler(Plugin internalPlugin)
        {
            super(internalPlugin);
        }

        @Override
        public void tick(long curTick, boolean withAsync)
        {
            long time = System.currentTimeMillis();
            this.time = time;
            List<DioriteTaskImpl> temp = this.temp;
            this.parsePending();
            while (this.isReady(time, withAsync))
            {
                DioriteTaskImpl task = this.pending.remove();
                if (task.getPeriod() < - 1)
                {
                    if (! task.isAsync())
                    {
                        this.runners.remove(task.getTaskId(), task);
                    }
                    this.parsePending();
                    continue;
                }
                if (! task.isAsync())
                {
                    try
                    {
                        task.run();
                    }
                    catch (Throwable throwable)
                    {
                        // TODO: add better logging
                        System.err.println(String.format("Task #%s for %s generated an exception", task.getTaskId(), task.getOwner().getName()));
                        throwable.printStackTrace();
                    }
                    this.parsePending();
                }
                // don't execuite async task, it's handled by other code.
                long period = task.getPeriod();
                if (period > 0)
                {
                    task.setNextRun(time + period);
                    temp.add(task);
                }
                else if (! task.isAsync())
                {
                    this.runners.remove(task.getTaskId());
                }
            }
            this.pending.addAll(temp);
            temp.clear();
        }

        @Override
        protected DioriteTask handle(DioriteTaskImpl task, long delay)
        {
            if (task instanceof DioriteAsyncTask)
            {
                DioriteAsyncTask aTask = (DioriteAsyncTask) task;
                if (aTask.getPeriod() == DioriteTaskImpl.STATE_SINGLE)
                {
                    this.executor.schedule(aTask, delay, TimeUnit.MILLISECONDS);
                }
                else
                {
                    this.executor.scheduleAtFixedRate(() -> {
                        aTask.run();
                        long period = task.getPeriod();
                        if (period > 0)
                        {
                            aTask.setNextRun(System.currentTimeMillis() + period);
                        }
                    }, delay, aTask.getPeriod(), TimeUnit.MILLISECONDS);
                }
            }
            task.setNextRun(this.time + delay);
            this.addTask(task);
            return task;
        }

        @Override
        public String toString()
        {
            return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append("ids", this.ids).append("time", this.time).toString();
        }
    }

    private static void validate(Plugin dioritePlugin, Object task)
    {
        Validate.notNull(dioritePlugin, "Plugin cannot be null");
        Validate.notNull(task, "Task cannot be null");
        // TODO: check if plugin is enabled.
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append("gts", this.gts).append("rts", this.rts).toString();
    }
}