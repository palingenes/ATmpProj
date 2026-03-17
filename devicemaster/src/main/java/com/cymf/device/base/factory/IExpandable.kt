package com.cymf.device.base.factory

/**
 * implement the interface if the item is expandable
 */
interface IExpandable<T> {

    var isExpanded: Boolean
    val subItems: MutableList<T>?
    /**
     * Get the level of this item. The level start from 0.
     * If you don't care about the level, just return a negative.
     */
    val level: Int
}
