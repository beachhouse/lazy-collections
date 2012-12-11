package org.jboss.beach.lazy.collections;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class LazyArrayList<E> extends AbstractList<E> {
    private class Itr implements Iterator<E> {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        int cursor = 0;

        /**
         * Index of element returned by most recent call to next or
         * previous.  Reset to -1 if this element is deleted by a call
         * to remove.
         */
        int lastRet = -1;

        /**
         * The modCount value that the iterator believes that the backing
         * List should have.  If this expectation is violated, the iterator
         * has detected concurrent modification.
         */
        int expectedModCount = modCount;

        public boolean hasNext() {
            sync.acquireShared(cursor + 1);
            return cursor != delegate.size();
        }

        public E next() {
            checkForComodification();
            try {
                int i = cursor;
                E next = get(i);
                lastRet = i;
                cursor = i + 1;
                return next;
            } catch (IndexOutOfBoundsException e) {
                checkForComodification();
                throw new NoSuchElementException();
            }
        }

        public void remove() {
            if (lastRet < 0)
                throw new IllegalStateException();
            checkForComodification();

            try {
                LazyArrayList.this.remove(lastRet);
                if (lastRet < cursor)
                    cursor--;
                lastRet = -1;
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException e) {
                throw new ConcurrentModificationException();
            }
        }

        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }

    private static final class Sync extends AbstractQueuedSynchronizer {
        @Override
        protected int tryAcquireShared(final int count) {
            return getState() >= count ? 1 : -1;
        }

        @Override
        protected boolean tryReleaseShared(final int arg) {
            // Decrement count; signal when transition to zero
            for (;;) {
                int c = getState();
                if (c == Integer.MAX_VALUE)
                    return false;
                long nextc = (long) c + (long) arg;
                if (nextc >= Integer.MAX_VALUE)
                    nextc = Integer.MAX_VALUE;
                if (compareAndSetState(c, (int) nextc)) {
                    //return nextc == Integer.MAX_VALUE;
                    return true;
                }
            }
        }
    }

    private final Sync sync = new Sync();
    private final List<E> delegate = new ArrayList<>();

    public void done() {
        sync.releaseShared(Integer.MAX_VALUE);
    }

    @Override
    public boolean add(final E e) {
        final boolean changed = delegate.add(e);
        sync.releaseShared(1);
        return changed;
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    @Override
    public E get(final int index) {
        sync.acquireShared(index);
        return delegate.get(index);
    }

    @Override
    public int size() {
        sync.acquireShared(Integer.MAX_VALUE);
        return delegate.size();
    }
}
