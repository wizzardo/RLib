package com.ss.rlib.util.ref;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The reference to float value.
 *
 * @author JavaSaBr
 */
final class TLFloatReference extends AbstractThreadLocalReference {

    /**
     * The value of this reference.
     */
    private float value;

    @Override
    public float getFloat() {
        return value;
    }

    @Override
    public double getDouble() {
        return value;
    }

    @Override
    public void setFloat(final float value) {
        this.value = value;
    }

    @Override
    public @NotNull ReferenceType getType() {
        return ReferenceType.FLOAT;
    }

    @Override
    public boolean equals(@Nullable final Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        final TLFloatReference that = (TLFloatReference) object;
        return Float.compare(that.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return (value != +0.0f ? Float.floatToIntBits(value) : 0);
    }

    @Override
    public void free() {
        this.value = 0;
    }

    @Override
    public String toString() {
        return "TLFloatReference{" +
                "value=" + value +
                "} " + super.toString();
    }
}
