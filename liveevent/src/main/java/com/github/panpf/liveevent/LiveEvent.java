/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.panpf.liveevent;

import android.annotation.SuppressLint;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.internal.SafeIterableMap;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import java.util.Iterator;
import java.util.Map;

import static androidx.lifecycle.Lifecycle.State.DESTROYED;
import static androidx.lifecycle.Lifecycle.State.STARTED;

@SuppressLint("RestrictedApi")
public class LiveEvent<T> {
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Object mDataLock = new Object();
    static final int START_VERSION = -1;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final Object NOT_SET = new Object();

    private SafeIterableMap<Listener<? super T>, ListenerWrapper> mListeners =
            new SafeIterableMap<>();

    // how many listeners are in active state
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mActiveCount = 0;
    private volatile Object mData;
    // when setData is called, we set the pending data and actual data swap happens on the main
    // thread
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    volatile Object mPendingData = NOT_SET;
    private int mVersion;

    private boolean mDispatchingValue;
    @SuppressWarnings("FieldCanBeLocal")
    private boolean mDispatchInvalidated;
    private final Runnable mPostValueRunnable = new Runnable() {
        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            Object newValue;
            synchronized (mDataLock) {
                newValue = mPendingData;
                mPendingData = NOT_SET;
            }
            setValue((T) newValue);
        }
    };

    /**
     * Creates a LiveData initialized with the given {@code value}.
     *
     * @param value initial value
     */
    public LiveEvent(T value) {
        mData = value;
        mVersion = START_VERSION + 1;
    }

    /**
     * Creates a LiveData with no value assigned to it.
     */
    public LiveEvent() {
        mData = NOT_SET;
        mVersion = START_VERSION;
    }

    @SuppressWarnings("unchecked")
    private void considerNotify(ListenerWrapper listener) {
        if (!listener.mActive) {
            return;
        }
        // Check latest state b4 dispatch. Maybe it changed state but we didn't get the event yet.
        //
        // we still first check listener.active to keep it as the entrance for events. So even if
        // the listener moved to an active state, if we've not received that event, we better not
        // notify for a more predictable notification order.
        if (!listener.shouldBeActive()) {
            listener.activeStateChanged(false);
            return;
        }
        if (listener.mLastVersion >= mVersion) {
            return;
        }
        listener.mLastVersion = mVersion;
        listener.mListener.onChanged((T) mData);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void dispatchingValue(@Nullable ListenerWrapper initiator) {
        if (mDispatchingValue) {
            mDispatchInvalidated = true;
            return;
        }
        mDispatchingValue = true;
        do {
            mDispatchInvalidated = false;
            if (initiator != null) {
                considerNotify(initiator);
                initiator = null;
            } else {
                for (Iterator<Map.Entry<Listener<? super T>, ListenerWrapper>> iterator =
                     mListeners.iteratorWithAdditions(); iterator.hasNext(); ) {
                    considerNotify(iterator.next().getValue());
                    if (mDispatchInvalidated) {
                        break;
                    }
                }
            }
        } while (mDispatchInvalidated);
        mDispatchingValue = false;
    }

    /**
     * Adds the given listener to the listeners list within the lifespan of the given
     * owner. The events are dispatched on the main thread. If LiveEvent already has data
     * set, it will be delivered to the listener.
     * <p>
     * The listener will only receive events if the owner is in {@link Lifecycle.State#STARTED}
     * or {@link Lifecycle.State#RESUMED} state (active).
     * <p>
     * If the owner moves to the {@link Lifecycle.State#DESTROYED} state, the listener will
     * automatically be removed.
     * <p>
     * When data changes while the {@code owner} is not active, it will not receive any updates.
     * If it becomes active again, it will receive the last available data automatically.
     * <p>
     * LiveEvent keeps a strong reference to the listener and the owner as long as the
     * given LifecycleOwner is not destroyed. When it is destroyed, LiveEvent removes references to
     * the listener &amp; the owner.
     * <p>
     * If the given owner is already in {@link Lifecycle.State#DESTROYED} state, LiveEvent
     * ignores the call.
     * <p>
     * If the given owner, listener tuple is already in the list, the call is ignored.
     * If the listener is already in the list with another owner, LiveEvent throws an
     * {@link IllegalArgumentException}.
     *
     * @param owner    The LifecycleOwner which controls the listener
     * @param listener The listener that will receive the events
     */
    @MainThread
    public void listen(@NonNull LifecycleOwner owner, @NonNull Listener<? super T> listener) {
        assertMainThread("listen");
        if (owner.getLifecycle().getCurrentState() == DESTROYED) {
            // ignore
            return;
        }
        LifecycleBoundListener wrapper = new LifecycleBoundListener(owner, listener);
        wrapper.mLastVersion = mVersion;    // Prevent onChanged from being triggered immediately
        ListenerWrapper existing = mListeners.putIfAbsent(listener, wrapper);
        if (existing != null && !existing.isAttachedTo(owner)) {
            throw new IllegalArgumentException("Cannot add the same listener"
                    + " with different lifecycles");
        }
        if (existing != null) {
            return;
        }
        owner.getLifecycle().addObserver(wrapper);
    }

    /**
     * Adds the given listener to the listeners list within the lifespan of the given
     * owner. The events are dispatched on the main thread. If LiveEvent already has data
     * set, it will be delivered to the listener.
     * <p>
     * The listener will only receive events if the owner is in {@link Lifecycle.State#STARTED}
     * or {@link Lifecycle.State#RESUMED} state (active).
     * <p>
     * If the owner moves to the {@link Lifecycle.State#DESTROYED} state, the listener will
     * automatically be removed.
     * <p>
     * When data changes while the {@code owner} is not active, it will not receive any updates.
     * If it becomes active again, it will receive the last available data automatically.
     * <p>
     * LiveEvent keeps a strong reference to the listener and the owner as long as the
     * given LifecycleOwner is not destroyed. When it is destroyed, LiveEvent removes references to
     * the listener &amp; the owner.
     * <p>
     * If the given owner is already in {@link Lifecycle.State#DESTROYED} state, LiveEvent
     * ignores the call.
     * <p>
     * If the given owner, listener tuple is already in the list, the call is ignored.
     * If the listener is already in the list with another owner, LiveEvent throws an
     * {@link IllegalArgumentException}.
     *
     * @param owner    The LifecycleOwner which controls the listener
     * @param listener The listener that will receive the events
     */
    @MainThread
    public void listenSticky(@NonNull LifecycleOwner owner, @NonNull Listener<? super T> listener) {
        assertMainThread("listen");
        if (owner.getLifecycle().getCurrentState() == DESTROYED) {
            // ignore
            return;
        }
        LifecycleBoundListener wrapper = new LifecycleBoundListener(owner, listener);
        ListenerWrapper existing = mListeners.putIfAbsent(listener, wrapper);
        if (existing != null && !existing.isAttachedTo(owner)) {
            throw new IllegalArgumentException("Cannot add the same listener"
                    + " with different lifecycles");
        }
        if (existing != null) {
            return;
        }
        owner.getLifecycle().addObserver(wrapper);
    }

    /**
     * Adds the given listener to the listeners list. This call is similar to
     * {@link LiveEvent#listen(LifecycleOwner, Listener)} with a LifecycleOwner, which
     * is always active. This means that the given listener will receive all events and will never
     * be automatically removed. You should manually call {@link #removeListener(Listener)} to stop
     * listening this LiveEvent.
     * While LiveEvent has one of such listeners, it will be considered
     * as active.
     * <p>
     * If the listener was already added with an owner to this LiveEvent, LiveEvent throws an
     * {@link IllegalArgumentException}.
     *
     * @param listener The listener that will receive the events
     */
    @MainThread
    public void listenForever(@NonNull Listener<? super T> listener) {
        assertMainThread("listenForever");
        AlwaysActiveListener wrapper = new AlwaysActiveListener(listener);
        wrapper.mLastVersion = mVersion;    // Prevent onChanged from being triggered immediately
        ListenerWrapper existing = mListeners.putIfAbsent(listener, wrapper);
        if (existing instanceof LiveEvent.LifecycleBoundListener) {
            throw new IllegalArgumentException("Cannot add the same listener"
                    + " with different lifecycles");
        }
        if (existing != null) {
            return;
        }
        wrapper.activeStateChanged(true);
    }

    /**
     * Adds the given listener to the listeners list. This call is similar to
     * {@link LiveEvent#listen(LifecycleOwner, Listener)} with a LifecycleOwner, which
     * is always active. This means that the given listener will receive all events and will never
     * be automatically removed. You should manually call {@link #removeListener(Listener)} to stop
     * listening this LiveEvent.
     * While LiveEvent has one of such listeners, it will be considered
     * as active.
     * <p>
     * If the listener was already added with an owner to this LiveEvent, LiveEvent throws an
     * {@link IllegalArgumentException}.
     *
     * @param listener The listener that will receive the events
     */
    @MainThread
    public void listenForeverSticky(@NonNull Listener<? super T> listener) {
        assertMainThread("listenForever");
        AlwaysActiveListener wrapper = new AlwaysActiveListener(listener);
        ListenerWrapper existing = mListeners.putIfAbsent(listener, wrapper);
        if (existing instanceof LiveEvent.LifecycleBoundListener) {
            throw new IllegalArgumentException("Cannot add the same listener"
                    + " with different lifecycles");
        }
        if (existing != null) {
            return;
        }
        wrapper.activeStateChanged(true);
    }

    /**
     * Removes the given listener from the listeners list.
     *
     * @param listener The Listener to receive events.
     */
    @MainThread
    public void removeListener(@NonNull final Listener<? super T> listener) {
        assertMainThread("removeListener");
        ListenerWrapper removed = mListeners.remove(listener);
        if (removed == null) {
            return;
        }
        removed.detachListener();
        removed.activeStateChanged(false);
    }

    /**
     * Removes all listeners that are tied to the given {@link LifecycleOwner}.
     *
     * @param owner The {@code LifecycleOwner} scope for the listeners to be removed.
     */
    @MainThread
    public void removeListener(@NonNull final LifecycleOwner owner) {
        assertMainThread("removeListener");
        for (Map.Entry<Listener<? super T>, ListenerWrapper> entry : mListeners) {
            if (entry.getValue().isAttachedTo(owner)) {
                removeListener(entry.getKey());
            }
        }
    }

    /**
     * Posts a task to a main thread to set the given value. So if you have a following code
     * executed in the main thread:
     * <pre class="prettyprint">
     * liveData.postValue("a");
     * liveData.setValue("b");
     * </pre>
     * The value "b" would be set at first and later the main thread would override it with
     * the value "a".
     * <p>
     * If you called this method multiple times before a main thread executed a posted task, only
     * the last value would be dispatched.
     *
     * @param value The new value
     */
    public void postValue(T value) {
        boolean postTask;
        synchronized (mDataLock) {
            postTask = mPendingData == NOT_SET;
            mPendingData = value;
        }
        if (!postTask) {
            return;
        }
        ArchTaskExecutor.getInstance().postToMainThread(mPostValueRunnable);
    }

    /**
     * Sets the value. If there are active listeners, the value will be dispatched to them.
     * <p>
     * This method must be called from the main thread. If you need set a value from a background
     * thread, you can use {@link #postValue(Object)}
     *
     * @param value The new value
     */
    @MainThread
    public void setValue(T value) {
        assertMainThread("setValue");
        mVersion++;
        mData = value;
        dispatchingValue(null);
    }

    /**
     * Returns the current value.
     * Note that calling this method on a background thread does not guarantee that the latest
     * value set will be received.
     *
     * @return the current value
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public T getValue() {
        Object data = mData;
        if (data != NOT_SET) {
            return (T) data;
        }
        return null;
    }

    int getVersion() {
        return mVersion;
    }

    /**
     * Called when the number of active listeners change to 1 from 0.
     * <p>
     * This callback can be used to know that this LiveEvent is being used thus should be kept
     * up to date.
     */
    protected void onActive() {

    }

    /**
     * Called when the number of active listeners change from 1 to 0.
     * <p>
     * This does not mean that there are no listeners left, there may still be listeners but their
     * lifecycle states aren't {@link Lifecycle.State#STARTED} or {@link Lifecycle.State#RESUMED}
     * (like an Activity in the back stack).
     * <p>
     * You can check if there are listeners via {@link #hasListeners()}.
     */
    protected void onInactive() {

    }

    /**
     * Returns true if this LiveEvent has listeners.
     *
     * @return true if this LiveEvent has listeners
     */
    public boolean hasListeners() {
        return mListeners.size() > 0;
    }

    /**
     * Returns true if this LiveEvent has active listeners.
     *
     * @return true if this LiveEvent has active listeners
     */
    public boolean hasActiveListeners() {
        return mActiveCount > 0;
    }

    class LifecycleBoundListener extends ListenerWrapper implements LifecycleEventObserver {
        @NonNull
        final LifecycleOwner mOwner;

        LifecycleBoundListener(@NonNull LifecycleOwner owner, Listener<? super T> listener) {
            super(listener);
            mOwner = owner;
        }

        @Override
        boolean shouldBeActive() {
            return mOwner.getLifecycle().getCurrentState().isAtLeast(STARTED);
        }

        @Override
        public void onStateChanged(@NonNull LifecycleOwner source,
                @NonNull Lifecycle.Event event) {
            if (mOwner.getLifecycle().getCurrentState() == DESTROYED) {
                removeListener(mListener);
                return;
            }
            activeStateChanged(shouldBeActive());
        }

        @Override
        boolean isAttachedTo(LifecycleOwner owner) {
            return mOwner == owner;
        }

        @Override
        void detachListener() {
            mOwner.getLifecycle().removeObserver(this);
        }
    }

    private abstract class ListenerWrapper {
        final Listener<? super T> mListener;
        boolean mActive;
        int mLastVersion = START_VERSION;

        ListenerWrapper(Listener<? super T> listener) {
            mListener = listener;
        }

        abstract boolean shouldBeActive();

        boolean isAttachedTo(LifecycleOwner owner) {
            return false;
        }

        void detachListener() {
        }

        void activeStateChanged(boolean newActive) {
            if (newActive == mActive) {
                return;
            }
            // immediately set active state, so we'd never dispatch anything to inactive
            // owner
            mActive = newActive;
            boolean wasInactive = LiveEvent.this.mActiveCount == 0;
            LiveEvent.this.mActiveCount += mActive ? 1 : -1;
            if (wasInactive && mActive) {
                onActive();
            }
            if (LiveEvent.this.mActiveCount == 0 && !mActive) {
                onInactive();
            }
            if (mActive) {
                dispatchingValue(this);
            }
        }
    }

    private class AlwaysActiveListener extends ListenerWrapper {

        AlwaysActiveListener(Listener<? super T> listener) {
            super(listener);
        }

        @Override
        boolean shouldBeActive() {
            return true;
        }
    }

    static void assertMainThread(String methodName) {
        if (!ArchTaskExecutor.getInstance().isMainThread()) {
            throw new IllegalStateException("Cannot invoke " + methodName + " on a background"
                    + " thread");
        }
    }
}
