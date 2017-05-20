package rocks.stalin.android.app.framework.concurrent.observable;

import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

public class ObservableCollection<T> implements Collection<T> {
    private Collection<T> collection;
    private AddListener<T> addListener;
    private RemoveListener<T> removeListener;
    private ClearListener clearListener;

    public ObservableCollection(Collection<T> collection) {
        this.collection = collection;
    }

    public void setAddListener(AddListener<T> listener) {
        this.addListener = listener;
    }

    public void setRemoveListener(RemoveListener<T> listener) {
        removeListener = listener;
    }

    public void setClearListener(ClearListener listener){
        clearListener = listener;
    }

    @Override
    public int size() {
        return collection.size();
    }

    @Override
    public boolean isEmpty() {
        return collection.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return collection.contains(o);
    }

    @NonNull
    @Override
    public Iterator<T> iterator() {
        return collection.iterator();
    }

    @NonNull
    @Override
    public Object[] toArray() {
        return collection.toArray();
    }

    @NonNull
    @Override
    public <T1> T1[] toArray(@NonNull T1[] a) {
        return collection.toArray(a);
    }

    @Override
    public boolean add(T t) {
        boolean result = collection.add(t);
        addListener.elementAdded(t);
        return result;
    }

    @Override
    public boolean remove(Object o) {
        boolean result = collection.remove(o);
        removeListener.elementRemoved((T) o);
        return result;
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        return collection.containsAll(c);
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends T> c) {
        boolean result = collection.addAll(c);
        for(T element  : c)
            addListener.elementAdded(element);
        return result;
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        boolean result = collection.removeAll(c);
        for(Object element  : c)
            removeListener.elementRemoved((T) element);
        return result;
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        throw new UnsupportedOperationException("Observable collections don't allow you to retain");
    }

    @Override
    public void clear() {
        collection.clear();
        clearListener.cleared();
    }

    public interface AddListener<T> {
        void elementAdded(T element);
    }

    public interface RemoveListener<T> {
        void elementRemoved(T element);
    }

    public interface ClearListener {
        void cleared();
    }
}
