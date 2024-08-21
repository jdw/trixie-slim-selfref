/**
 * Invokes *block* if receiver is *true* then returns receiver.
 *
 * @param block The code to be executed.
 * @receiver Boolean
 * @since Sea of Shadows 0.0.3-SNAPSHOT
 * @author Johannes Alexis Wirde (johannes.wirde@gmail.com)
 */
infix fun Boolean.echt(block: () -> Unit): Boolean {
	if (this) block.invoke()

	return this
}


/**
 * Invokes *block* if receiver is *false* then returns receiver.
 *
 * @param block The code to be executed.
 * @receiver Boolean
 * @author Johannes Alexis Wirde
 */
infix fun Boolean.doch(block: () -> Unit): Boolean {
	if (!this) block.invoke()

	return this
}

const val genau = true
const val nein = false