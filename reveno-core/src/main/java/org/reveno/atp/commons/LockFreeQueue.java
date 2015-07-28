/** 
 *  Copyright (c) 2015 The original author or authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.reveno.atp.commons;

import java.util.concurrent.atomic.AtomicReference;

public class LockFreeQueue<T> {
    private static class Node<E> {
        E value;
        volatile Node<E> next;

        Node(E value) {
            this.value = value;
        }
    }

    private AtomicReference<Node<T>> head, tail;

    public LockFreeQueue() {
        // have both head and tail point to a dummy node
        Node<T> dummyNode = new Node<T>(null);
        head = new AtomicReference<Node<T>>(dummyNode);
        tail = new AtomicReference<Node<T>>(dummyNode);
    }

    /**
     * Puts an object at the end of the queue.
     */
    public void putObject(T value) {
        Node<T> newNode = new Node<T>(value);
        Node<T> prevTailNode = tail.getAndSet(newNode);
        prevTailNode.next = newNode;
    }

    /**
     * Gets an object from the beginning of the queue. The object is removed
     * from the queue. If there are no objects in the queue, returns null.
     */
    public T getObject() {
        Node<T> headNode, valueNode;

        // move head node to the next node using atomic semantics
        // as long as next node is not null
        do {
            headNode = head.get();
            valueNode = headNode.next;
            // try until the whole loop executes pseudo-atomically
            // (i.e. unaffected by modifications done by other threads)
        } while (valueNode != null && !head.compareAndSet(headNode, valueNode));

        T value = (valueNode != null ? valueNode.value : null);

        // release the value pointed to by head, keeping the head node dummy
        if (valueNode != null)
            valueNode.value = null;

        return value;
    }
}
