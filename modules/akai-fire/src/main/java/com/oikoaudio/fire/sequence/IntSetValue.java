package com.oikoaudio.fire.sequence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.oikoaudio.fire.NoteValue.IntValueListener;

public class IntSetValue {
    private final Set<Integer> values = new HashSet<>();
    private final List<IntValueListener> sizeListener = new ArrayList<>();
    
    public IntSetValue() {
    }
    
    public void addSizeValueListener(final IntValueListener listener) {
        sizeListener.add(listener);
    }
    
    public Stream<Integer> stream() {
        return values.stream();
    }

    public boolean contains(final int index) {
        return values.contains(index);
    }
    
    public void remove(final int index) {
        final int oldSize = values.size();
        values.remove(index);
        final int newSize = values.size();
        if (oldSize != newSize) {
            sizeListener.forEach(l -> l.handleChange(newSize));
        }
    }
    
    public void add(final int index) {
        final int oldSize = values.size();
        values.add(index);
        final int newSize = values.size();
        if (oldSize != newSize) {
            sizeListener.forEach(l -> l.handleChange(newSize));
        }
    }
    
}
