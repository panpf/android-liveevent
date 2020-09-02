package com.github.panpf.liveevent;

import androidx.annotation.Nullable;

/**
 * A simple callback that can receive from {@link LiveEvent}.
 *
 * @param <T> The type of the parameter
 *
 * @see LiveEvent LiveData - for a usage description.
 */
public interface Listener<T> {
    /**
     * Called when the data is changed.
     * @param t  The new data
     */
    void onChanged(@Nullable T t);
}