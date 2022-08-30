package org.unitouhou.util

import kotlinx.serialization.Serializable


@Serializable
class MutableQueue<E>  {
    constructor(){
        queueData = mutableListOf()
    }
    constructor(elements: Collection<E>){
        queueData = elements.toMutableList()
    }

    private var queueData = mutableListOf<E>()
    
     val size: Int
        get() =  queueData.size

    /**
     * Adds the specified element to the end of this queue.
     *
     * @return `true` because the queue is always modified as the result of this operation.
     */
     fun add(element: E): Boolean {
        return queueData.add(element)
    }

    /**
     * Retrieves and removes the head of this queue,
     * or returns `null` if this queue is empty.
     *
     * @return the head of this queue, or `null` if this queue is empty
     */
     fun poll(): E? {
        return queueData.removeFirstOrNull()
    }

    /**
     * Retrieves, but does not remove, the head of this queue,
     * or returns `null` if this queue is empty.
     *
     * @return the head of this queue, or `null` if this queue is empty
     */
     fun peek(): E? {
        return queueData.getOrNull(0)
    }

     fun clear() {
        queueData.clear()
    }

    fun isEmpty(): Boolean {
        return queueData.isEmpty()
    }

    fun iterator(): MutableIterator<E> {
        return queueData.listIterator()
    }
}

inline fun <T> mutableQueueOf(): MutableQueue<T> = MutableQueue(mutableListOf())

fun <T> mutableQueueOf(vararg elements: T): MutableQueue<T> =
    if (elements.size == 0) mutableQueueOf() else MutableQueue(elements.toMutableList())
